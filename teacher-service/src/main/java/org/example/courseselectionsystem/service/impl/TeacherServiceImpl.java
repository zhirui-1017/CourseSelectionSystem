package org.example.courseselectionsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.courseselectionsystem.common.Constants;
import org.example.courseselectionsystem.entity.Teacher;
import org.example.courseselectionsystem.exception.BusinessException;
import org.example.courseselectionsystem.mapper.TeacherMapper;
import org.example.courseselectionsystem.service.TeacherService;
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
 * 教师服务实现类
 * 实现教师相关的业务逻辑
 */
@Service
@Transactional(readOnly = true)
public class TeacherServiceImpl implements TeacherService {

    private static final Logger logger = LoggerFactory.getLogger(TeacherServiceImpl.class);

    @Autowired
    private TeacherMapper teacherMapper;

    @Override
    @Transactional(readOnly = false)
    public boolean addTeacher(Teacher teacher) {
        // 参数验证
        validateTeacherParams(teacher);
        
        // 检查工号是否已存在
        checkTeacherNoExist(teacher.getTeacherNo(), null);
        
        // 设置默认密码为工号后6位
        String password = teacher.getTeacherNo().length() > 6 ? 
                teacher.getTeacherNo().substring(teacher.getTeacherNo().length() - 6) : 
                teacher.getTeacherNo();
        teacher.setPassword(password);
        
        // 添加教师
        int result = teacherMapper.insert(teacher);
        if (result <= 0) {
            logger.error("添加教师失败: {}", teacher);
            throw new BusinessException(Constants.FAIL_CODE, "添加教师失败");
        }
        return true;
    }

    @Override
    @Transactional(readOnly = false)
    public boolean updateTeacher(Teacher teacher) {
        // 参数验证
        validateTeacherParams(teacher);
        
        // 检查教师是否存在
        checkTeacherExist(teacher.getId());
        
        // 检查工号是否已存在（排除自身）
        checkTeacherNoExist(teacher.getTeacherNo(), teacher.getId());
        
        // 更新教师信息
        int result = teacherMapper.updateById(teacher);
        if (result <= 0) {
            logger.error("更新教师失败: {}", teacher);
            throw new BusinessException(Constants.FAIL_CODE, "更新教师失败");
        }
        return true;
    }

    @Override
    @Transactional(readOnly = false)
    public boolean deleteTeacher(Long id) {
        // 检查教师是否存在
        checkTeacherExist(id);
        
        // 删除教师
        int result = teacherMapper.deleteById(id);
        if (result <= 0) {
            logger.error("删除教师失败，教师ID: {}", id);
            throw new BusinessException(Constants.FAIL_CODE, "删除教师失败");
        }
        return true;
    }

    @Override
    public Teacher getTeacherById(Long id) {
        Teacher teacher = teacherMapper.selectById(id);
        if (teacher == null) {
            throw new BusinessException(Constants.NOT_FOUND_CODE, "教师不存在");
        }
        return teacher;
    }

    @Override
    public Teacher getTeacherByTeacherNo(String teacherNo) {
        Teacher teacher = teacherMapper.selectByTeacherNo(teacherNo);
        if (teacher == null) {
            throw new BusinessException(Constants.NOT_FOUND_CODE, "教师不存在");
        }
        return teacher;
    }

    @Override
    public List<Teacher> getAllTeachers() {
        return teacherMapper.selectList(null);
    }

    @Override
    public PageResult<Teacher> getTeachersByPage(PageRequest pageRequest) {
        // 构建分页参数
        Page<Teacher> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        
        // 构建查询条件
        QueryWrapper<Teacher> queryWrapper = new QueryWrapper<>();
        if (StringUtils.hasText(pageRequest.getSearchField()) && StringUtils.hasText(pageRequest.getSearchValue())) {
            if ("name".equals(pageRequest.getSearchField())) {
                queryWrapper.like("name", pageRequest.getSearchValue());
            } else if ("teacherNo".equals(pageRequest.getSearchField())) {
                queryWrapper.eq("teacher_no", pageRequest.getSearchValue());
            }
        }
        
        // 排序
        if (StringUtils.hasText(pageRequest.getSortField())) {
            boolean isAsc = "asc".equalsIgnoreCase(pageRequest.getSortOrder());
            queryWrapper.orderBy(true, isAsc, pageRequest.getSortField());
        }
        
        // 分页查询
        IPage<Teacher> pageResult = teacherMapper.selectPage(page, queryWrapper);
        
        // 构建返回结果
        return new PageResult<>(
                (int) pageResult.getCurrent(),
                (int) pageResult.getSize(),
                pageResult.getTotal(),
                pageResult.getRecords()
        );
    }

    @Override
    public List<Teacher> getTeachersByDepartmentId(Long departmentId) {
        return teacherMapper.selectByDepartmentId(departmentId);
    }

    @Override
    public List<Teacher> getTeachersByCollegeId(Long collegeId) {
        return teacherMapper.selectByCollegeId(collegeId);
    }

    @Override
    public List<Teacher> searchTeachersByName(String name) {
        return teacherMapper.selectByNameLike(name);
    }

    @Override
    @Transactional(readOnly = false)
    public boolean resetTeacherPassword(Long id) {
        Teacher teacher = getTeacherById(id);
        String teacherNo = teacher.getTeacherNo();
        String password = teacherNo.length() > 6
                ? teacherNo.substring(teacherNo.length() - 6)
                : teacherNo;
        teacher.setPassword(password);
        int result = teacherMapper.updateById(teacher);
        if (result <= 0) {
            logger.error("重置教师密码失败，教师ID: {}", id);
            throw new BusinessException(Constants.FAIL_CODE, "重置教师密码失败");
        }
        return true;
    }

    @Override
    @Transactional(readOnly = false)
    public boolean changePassword(Long id, String oldPassword, String newPassword) {
        if (!StringUtils.hasText(newPassword)) {
            throw new BusinessException(Constants.PARAM_ERROR_CODE, "新密码不能为空");
        }
        Teacher teacher = getTeacherById(id);
        if (!StringUtils.hasText(oldPassword) || !oldPassword.equals(teacher.getPassword())) {
            throw new BusinessException(Constants.PARAM_ERROR_CODE, "旧密码不正确");
        }
        teacher.setPassword(newPassword);
        int result = teacherMapper.updateById(teacher);
        if (result <= 0) {
            logger.error("修改教师密码失败，教师ID: {}", id);
            throw new BusinessException(Constants.FAIL_CODE, "修改教师密码失败");
        }
        return true;
    }

    /**
     * 验证教师参数
     * @param teacher 教师信息
     */
    private void validateTeacherParams(Teacher teacher) {
        if (teacher == null) {
            throw new BusinessException(Constants.PARAM_ERROR_CODE, "教师信息不能为空");
        }
        
        if (!StringUtils.hasText(teacher.getTeacherNo())) {
            throw new BusinessException(Constants.PARAM_ERROR_CODE, "工号不能为空");
        }
        
        // 验证工号格式
        if (!Pattern.matches(Constants.TEACHER_NO_REGEX, teacher.getTeacherNo())) {
            throw new BusinessException(Constants.PARAM_ERROR_CODE, "工号格式不正确，应为6位数字");
        }
        
        if (!StringUtils.hasText(teacher.getName())) {
            throw new BusinessException(Constants.PARAM_ERROR_CODE, "姓名不能为空");
        }
        
        if (teacher.getDepartmentId() == null) {
            throw new BusinessException(Constants.PARAM_ERROR_CODE, "所属系不能为空");
        }
    }

    /**
     * 检查教师是否存在
     * @param id 教师ID
     */
    private void checkTeacherExist(Long id) {
        Teacher existingTeacher = teacherMapper.selectById(id);
        if (existingTeacher == null) {
            throw new BusinessException(Constants.NOT_FOUND_CODE, "教师不存在");
        }
    }

    /**
     * 检查工号是否已存在
     * @param teacherNo 工号
     * @param excludeId 排除的教师ID（用于更新操作）
     */
    private void checkTeacherNoExist(String teacherNo, Long excludeId) {
        int count = teacherMapper.countByTeacherNo(teacherNo, excludeId);
        if (count > 0) {
            throw new BusinessException(Constants.PARAM_ERROR_CODE, "工号已存在");
        }
    }
}
