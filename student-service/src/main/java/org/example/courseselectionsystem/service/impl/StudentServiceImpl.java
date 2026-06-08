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
import java.util.regex.Pattern;

/**
 * 学生服务实现类
 * 实现学生相关的业务逻辑
 */
@Service
@Transactional(readOnly = true)
public class StudentServiceImpl implements StudentService {

    private static final Logger logger = LoggerFactory.getLogger(StudentServiceImpl.class);

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
        Page<Student> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        
        // 构建查询条件
        QueryWrapper<Student> queryWrapper = new QueryWrapper<>();
        if (StringUtils.hasText(pageRequest.getSearchField()) && StringUtils.hasText(pageRequest.getSearchValue())) {
            if ("name".equals(pageRequest.getSearchField())) {
                queryWrapper.like("name", pageRequest.getSearchValue());
            } else if ("studentNo".equals(pageRequest.getSearchField())) {
                queryWrapper.eq("student_no", pageRequest.getSearchValue());
            }
        }
        
        // 排序
        if (StringUtils.hasText(pageRequest.getSortField())) {
            boolean isAsc = "asc".equalsIgnoreCase(pageRequest.getSortOrder());
            queryWrapper.orderBy(true, isAsc, pageRequest.getSortField());
        }
        
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

    @Override
    @Transactional(readOnly = false)
    public boolean resetPassword(Long id) {
        Student student = getStudentById(id);
        String studentNo = student.getStudentNo();
        String password = studentNo.length() > 6
                ? studentNo.substring(studentNo.length() - 6)
                : studentNo;
        student.setPassword(password);
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
        if (!StringUtils.hasText(oldPassword) || !oldPassword.equals(student.getPassword())) {
            throw new BusinessException(Constants.PARAM_ERROR_CODE, "旧密码不正确");
        }
        student.setPassword(newPassword);
        int result = studentMapper.updateById(student);
        if (result <= 0) {
            logger.error("修改学生密码失败，学生ID: {}", id);
            throw new BusinessException(Constants.FAIL_CODE, "修改学生密码失败");
        }
        return true;
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
