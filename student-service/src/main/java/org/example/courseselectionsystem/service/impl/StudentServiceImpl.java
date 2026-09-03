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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
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
    private static final int MAX_PAGE_SIZE = 1000;
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
            Map.entry("className", "class_id"),
            Map.entry("classId", "class_id"),
            Map.entry("status", "status"),
            Map.entry("createdAt", "created_at"),
            Map.entry("createTime", "created_at"),
            Map.entry("updatedAt", "updated_at"),
            Map.entry("updateTime", "updated_at")
    );

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    @Transactional(readOnly = false)
    public boolean addStudent(Student student) {
        // 参数验证
        validateStudentParams(student, true);
        
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
        // 参数验证（更新时学号非必填，保持原学号不变）
        validateStudentParams(student, false);
        
        // 检查学生是否存在
        checkStudentExist(student.getId());
        
        // 检查学号是否已存在（排除自身），仅当携带学号时校验
        if (StringUtils.hasText(student.getStudentNo())) {
            checkStudentNoExist(student.getStudentNo(), student.getId());
        }
        
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
        
        // 级联清理关联数据：删除该学生的选课记录与课程评价，解除班级班长关联
        int courseSelDeleted = jdbcTemplate.update("delete from course_selection where student_id = ?", id);
        int courseEvalDeleted = jdbcTemplate.update("delete from course_evaluation where student_id = ?", id);
        int monitorCleared = jdbcTemplate.update("update class_info set monitor_id = null where monitor_id = ?", id);
        logger.info("删除学生前清理关联数据，学生ID: {}，选课 {} 条、评价 {} 条、班长关联 {} 处",
                id, courseSelDeleted, courseEvalDeleted, monitorCleared);
        
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
                // cloud 库 student 表无 class_name 列，改由 class_id 关联 class_info 过滤
                queryWrapper.apply("class_id IN (SELECT id FROM class_info WHERE class_name LIKE CONCAT('%', {0}, '%'))", searchValue.trim());
                break;
            default:
                break;
        }
    }

    private void applyFilters(QueryWrapper<Student> queryWrapper, PageRequest request) {
        // 通用关键字：同时匹配学号与姓名（前端搜索框传入 keyword）
        String keyword = textParam(request, "keyword", "searchKey", "q");
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            queryWrapper.and(w -> w.like("student_no", kw).or().like("name", kw));
        }

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
            // cloud 库 student 表无 class_name 列，改由 class_id 关联 class_info 过滤
            queryWrapper.apply("class_id IN (SELECT id FROM class_info WHERE class_name LIKE CONCAT('%', {0}, '%'))", className.trim());
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

    @Override
    @Transactional(readOnly = false)
    public boolean resetPassword(Long id) {
        Student student = getStudentById(id);
        String studentNo = student.getStudentNo();
        String password = studentNo.length() > 6
                ? studentNo.substring(studentNo.length() - 6)
                : studentNo;
        student.setPassword(passwordEncoder.encode(password));
        int result = studentMapper.updateById(student);
        if (result <= 0) {
            logger.error("重置学生密码失败，学生ID: {}", id);
            throw new BusinessException(Constants.FAIL_CODE, "重置学生密码失败");
        }
        return true;
    }

    @Override
    @Transactional(readOnly = false)
    public boolean changePassword(Long id, String oldPassword, String newPassword) {
        if (!StringUtils.hasText(newPassword)) {
            throw new BusinessException(Constants.PARAM_ERROR_CODE, "新密码不能为空");
        }
        Student student = getStudentById(id);
        if (!passwordMatches(oldPassword, student.getPassword())) {
            throw new BusinessException(Constants.PARAM_ERROR_CODE, "旧密码不正确");
        }
        student.setPassword(passwordEncoder.encode(newPassword));
        int result = studentMapper.updateById(student);
        if (result <= 0) {
            logger.error("修改学生密码失败，学生ID: {}", id);
            throw new BusinessException(Constants.FAIL_CODE, "修改学生密码失败");
        }
        return true;
    }

    /**
     * 校验原密码：兼容明文存储与 BCrypt 加密存储的账号
     * 系统内注册用户密码为 BCrypt 加密，管理员添加/重置的用户密码可能为明文
     * @param rawPassword 用户输入的原始密码
     * @param storedPassword 数据库中存储的密码
     * @return 是否匹配
     */
    private boolean passwordMatches(String rawPassword, String storedPassword) {
        if (!StringUtils.hasText(rawPassword) || !StringUtils.hasText(storedPassword)) {
            return false;
        }
        if (storedPassword.equals(rawPassword)) {
            return true;
        }
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$")
                || storedPassword.startsWith("$2y$")) {
            try {
                return passwordEncoder.matches(rawPassword, storedPassword);
            } catch (IllegalArgumentException ex) {
                logger.warn("Invalid BCrypt password format for student");
                return false;
            }
        }
        return false;
    }

    /**
     * 验证学生参数
     * @param student 学生信息
     * @param requireStudentNo 是否必须提供学号（新增必填，更新可选）
     */
    private void validateStudentParams(Student student, boolean requireStudentNo) {
        if (student == null) {
            throw new BusinessException(Constants.PARAM_ERROR_CODE, "学生信息不能为空");
        }
        
        if (requireStudentNo) {
            if (!StringUtils.hasText(student.getStudentNo())) {
                throw new BusinessException(Constants.PARAM_ERROR_CODE, "学号不能为空");
            }
            // 验证学号格式
            if (!Pattern.matches(Constants.STUDENT_NO_REGEX, student.getStudentNo())) {
                throw new BusinessException(Constants.PARAM_ERROR_CODE, "学号格式不正确，应为8位数字");
            }
        } else if (StringUtils.hasText(student.getStudentNo())) {
            // 更新时若携带学号也校验格式
            if (!Pattern.matches(Constants.STUDENT_NO_REGEX, student.getStudentNo())) {
                throw new BusinessException(Constants.PARAM_ERROR_CODE, "学号格式不正确，应为8位数字");
            }
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

    @Override
    public long count() {
        return studentMapper.selectCount(null);
    }

    @Override
    public long countRecent(int days) {
        QueryWrapper<Student> wrapper = new QueryWrapper<>();
        wrapper.ge("create_time", LocalDateTime.now().minusDays(Math.max(days, 1)));
        return studentMapper.selectCount(wrapper);
    }
}
