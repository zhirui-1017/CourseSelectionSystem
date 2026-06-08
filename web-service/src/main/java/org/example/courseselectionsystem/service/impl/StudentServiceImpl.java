package org.example.courseselectionsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.courseselectionsystem.common.Constants;
import org.example.courseselectionsystem.entity.Student;
import org.example.courseselectionsystem.exception.BusinessException;
import org.example.courseselectionsystem.mapper.StudentMapper;
import org.example.courseselectionsystem.service.StudentService;
import org.example.courseselectionsystem.vo.PageRequest;
import org.example.courseselectionsystem.vo.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 学生服务实现类
 * 实现学生相关的业务逻辑
 */
@Service
@Transactional(readOnly = true)
public class StudentServiceImpl implements StudentService {

    private static final Logger logger = LoggerFactory.getLogger(StudentServiceImpl.class);
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final Map<String, String> SORT_COLUMNS = Map.ofEntries(
            Map.entry("id", "id"),
            Map.entry("studentNo", "student_no"),
            Map.entry("student_no", "student_no"),
            Map.entry("studentId", "student_no"),
            Map.entry("username", "student_no"),
            Map.entry("name", "name"),
            Map.entry("studentName", "name"),
            Map.entry("gender", "gender"),
            Map.entry("phone", "phone"),
            Map.entry("email", "email"),
            Map.entry("majorId", "major_id"),
            Map.entry("major", "major_id"),
            Map.entry("collegeId", "college_id"),
            Map.entry("college", "college_id"),
            Map.entry("className", "class_name"),
            Map.entry("classId", "class_id"),
            Map.entry("status", "status"),
            Map.entry("createdAt", "created_at"),
            Map.entry("createTime", "created_at"),
            Map.entry("updatedAt", "updated_at"),
            Map.entry("updateTime", "updated_at")
    );

    @Autowired
    private StudentMapper studentMapper;

    @Override
    @Transactional(readOnly = false)
    public boolean addStudent(Student student) {
        // 参数验证
        validateStudentParams(student);
        
        // 检查学号是否已存在
        checkStudentNoExist(student.getStudentNo(), null);
        
        // 添加学生
        int result = studentMapper.insert(student);
        if (result <= 0) {
            logger.error("添加学生失败: {}", student);
            throw new BusinessException(Constants.FAIL_CODE, "添加学生失败");
        }
        return true;
    }

    @Override
    @Transactional(readOnly = false)
    public boolean updateStudent(Student student) {
        // 参数验证
        validateStudentParams(student);
        
        // 检查学生是否存在
        checkStudentExist(student.getId());
        
        // 检查学号是否已存在（排除自身）
        checkStudentNoExist(student.getStudentNo(), student.getId());
        
        // 更新学生信息
        int result = studentMapper.updateById(student);
        if (result <= 0) {
            logger.error("更新学生失败: {}", student);
            throw new BusinessException(Constants.FAIL_CODE, "更新学生失败");
        }
        return true;
    }

    @Override
    @Transactional(readOnly = false)
    public boolean deleteStudent(Long id) {
        // 检查学生是否存在
        checkStudentExist(id);
        
        // 删除学生
        int result = studentMapper.deleteById(id);
        if (result <= 0) {
            logger.error("删除学生失败，学生ID: {}", id);
            throw new BusinessException(Constants.FAIL_CODE, "删除学生失败");
        }
        return true;
    }

    @Override
    public Student getStudentById(Long id) {
        Student student = studentMapper.selectById(id);
        if (student == null) {
            throw new BusinessException(Constants.NOT_FOUND_CODE, "学生不存在");
        }
        return student;
    }

    @Override
    public Student getStudentByStudentNo(String studentNo) {
        Student student = studentMapper.selectByStudentNo(studentNo);
        if (student == null) {
            throw new BusinessException(Constants.NOT_FOUND_CODE, "学生不存在");
        }
        return student;
    }

    @Override
    public List<Student> getAllStudents() {
        return studentMapper.selectList(null);
    }

    @Override
    public PageResult<Student> getStudentsByPage(PageRequest pageRequest) {
        // 构建分页参数
        PageRequest request = pageRequest == null ? new PageRequest() : pageRequest;
        Page<Student> page = new Page<>(normalizePageNum(request.getPageNum()), normalizePageSize(request.getPageSize()));
        
        // 构建查询条件
        QueryWrapper<Student> queryWrapper = new QueryWrapper<>();
        applySearch(queryWrapper, request);
        applyFilters(queryWrapper, request);
        
        // 排序
        queryWrapper.orderBy(true, "asc".equalsIgnoreCase(request.getSortOrder()), sortColumn(request));
        
        // 分页查询
        IPage<Student> pageResult = studentMapper.selectPage(page, queryWrapper);
        
        // 构建返回结果
        return new PageResult<>(
                (int) pageResult.getCurrent(),
                (int) pageResult.getSize(),
                pageResult.getTotal(),
                pageResult.getRecords()
        );
    }

    @Override
    public List<Student> getStudentsByMajorId(Long majorId) {
        return studentMapper.selectByMajorId(majorId);
    }

    @Override
    public List<Student> getStudentsByDepartmentId(Long departmentId) {
        return studentMapper.selectByDepartmentId(departmentId);
    }

    @Override
    public List<Student> getStudentsByCollegeId(Long collegeId) {
        return studentMapper.selectByCollegeId(collegeId);
    }

    @Override
    public List<Student> searchStudentsByName(String name) {
        return studentMapper.selectByNameLike(name);
    }

    private void applySearch(QueryWrapper<Student> queryWrapper, PageRequest request) {
        String searchField = firstText(request.getSearchField(), textParam(request, "searchField"));
        String searchValue = firstText(request.getSearchValue(), textParam(request, "searchValue"));
        if (!StringUtils.hasText(searchField) || !StringUtils.hasText(searchValue)) {
            return;
        }
        switch (searchField.trim()) {
            case "name":
            case "studentName":
                queryWrapper.like("name", searchValue.trim());
                break;
            case "studentNo":
            case "studentId":
            case "username":
                queryWrapper.like("student_no", searchValue.trim());
                break;
            case "phone":
                queryWrapper.like("phone", searchValue.trim());
                break;
            case "email":
                queryWrapper.like("email", searchValue.trim());
                break;
            case "className":
                queryWrapper.like("class_name", searchValue.trim());
                break;
            default:
                break;
        }
    }

    private void applyFilters(QueryWrapper<Student> queryWrapper, PageRequest request) {
        String studentName = textParam(request, "studentName", "name");
        if (StringUtils.hasText(studentName)) {
            queryWrapper.like("name", studentName.trim());
        }

        String studentNo = textParam(request, "studentNo", "studentId", "username");
        if (StringUtils.hasText(studentNo)) {
            queryWrapper.like("student_no", studentNo.trim());
        }

        Long collegeId = longParam(request, "collegeId");
        if (collegeId != null) {
            queryWrapper.eq("college_id", collegeId);
        }

        Long majorId = longParam(request, "majorId");
        if (majorId != null) {
            queryWrapper.eq("major_id", majorId);
        }

        Long departmentId = longParam(request, "departmentId");
        if (departmentId != null) {
            queryWrapper.apply("major_id IN (SELECT id FROM major WHERE department_id = {0})", departmentId);
        }

        Long classId = longParam(request, "classId");
        if (classId != null) {
            queryWrapper.eq("class_id", classId);
        }

        String className = textParam(request, "className");
        if (StringUtils.hasText(className)) {
            queryWrapper.like("class_name", className.trim());
        }

        String gender = textParam(request, "gender");
        if (StringUtils.hasText(gender)) {
            queryWrapper.eq("gender", gender.trim());
        }

        Integer status = intParam(request, "status");
        if (status != null) {
            queryWrapper.eq("status", status);
        }
    }

    private int normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private String sortColumn(PageRequest request) {
        String sortField = firstText(request.getSortField(), textParam(request, "sortField", "orderByColumn"));
        if (!StringUtils.hasText(sortField)) {
            return "id";
        }
        return SORT_COLUMNS.getOrDefault(sortField.trim(), "id");
    }

    private String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private String textParam(PageRequest request, String... keys) {
        Map<String, Object> params = request.getParams();
        if (params == null || params.isEmpty()) {
            return null;
        }
        for (String key : keys) {
            Object value = params.get(key);
            if (value != null && StringUtils.hasText(String.valueOf(value))) {
                return String.valueOf(value);
            }
        }
        return null;
    }

    private Long longParam(PageRequest request, String... keys) {
        String value = textParam(request, keys);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer intParam(PageRequest request, String... keys) {
        String value = textParam(request, keys);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 验证学生参数
     * @param student 学生信息
     */
    private void validateStudentParams(Student student) {
        if (student == null) {
            throw new BusinessException(Constants.PARAM_ERROR_CODE, "学生信息不能为空");
        }
        
        if (!StringUtils.hasText(student.getStudentNo())) {
            throw new BusinessException(Constants.PARAM_ERROR_CODE, "学号不能为空");
        }
        
        // 验证学号格式
        if (!Pattern.matches(Constants.STUDENT_NO_REGEX, student.getStudentNo())) {
            throw new BusinessException(Constants.PARAM_ERROR_CODE, "学号格式不正确，应为8位数字");
        }
        
        if (!StringUtils.hasText(student.getName())) {
            throw new BusinessException(Constants.PARAM_ERROR_CODE, "姓名不能为空");
        }
        
        if (student.getMajorId() == null) {
            throw new BusinessException(Constants.PARAM_ERROR_CODE, "专业不能为空");
        }
    }

    /**
     * 检查学生是否存在
     * @param id 学生ID
     */
    private void checkStudentExist(Long id) {
        Student existingStudent = studentMapper.selectById(id);
        if (existingStudent == null) {
            throw new BusinessException(Constants.NOT_FOUND_CODE, "学生不存在");
        }
    }

    /**
     * 检查学号是否已存在
     * @param studentNo 学号
     * @param excludeId 排除的学生ID（用于更新操作）
     */
    private void checkStudentNoExist(String studentNo, Long excludeId) {
        int count = studentMapper.countByStudentNo(studentNo, excludeId);
        if (count > 0) {
            throw new BusinessException(Constants.PARAM_ERROR_CODE, "学号已存在");
        }
    }
}
