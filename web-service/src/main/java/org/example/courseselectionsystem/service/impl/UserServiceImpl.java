package org.example.courseselectionsystem.service.impl;

import org.example.courseselectionsystem.common.Constants;
import org.example.courseselectionsystem.entity.Admin;
import org.example.courseselectionsystem.entity.Student;
import org.example.courseselectionsystem.entity.Teacher;
import org.example.courseselectionsystem.entity.User;
import org.example.courseselectionsystem.exception.BusinessException;
import org.example.courseselectionsystem.mapper.StudentMapper;
import org.example.courseselectionsystem.mapper.TeacherMapper;
import org.example.courseselectionsystem.repository.AdminRepository;
import org.example.courseselectionsystem.service.UserService;
import org.example.courseselectionsystem.vo.LoginRequest;
import org.example.courseselectionsystem.vo.PageRequest;
import org.example.courseselectionsystem.vo.RegisterRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户服务实现类
 * 实现用户相关的业务逻辑
 */
@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private AdminRepository adminRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // 由于代码中没有显示JwtTokenUtil的定义，暂时注释掉token生成逻辑
    // private JwtTokenUtil jwtTokenUtil;

    @Override
    public Map<String, Object> login(LoginRequest loginRequest) {
        logger.info("用户登录请求处理开始");
        
        if (loginRequest == null) {
            throw new BusinessException(Constants.PARAM_ERROR_CODE, "登录请求参数不能为空");
        }
        
        // 获取用户名和密码 - 使用直接字段访问
        String username = loginRequest.username;
        String password = loginRequest.password;
        
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(Constants.PARAM_ERROR_CODE, "用户名不能为空");
        }
        if (!StringUtils.hasText(password)) {
            throw new BusinessException(Constants.PARAM_ERROR_CODE, "密码不能为空");
        }

        // 学生登录逻辑 - 暂时默认走学生登录路径
        User user = new User();
        user.username = username;
        
        Student student = studentMapper.selectByStudentNo(username);
        if (student == null) {
            throw new BusinessException(Constants.NOT_FOUND_CODE, "用户不存在");
        }
        
        // 临时跳过密码验证逻辑，因为Student实体已移除密码字段
        // TODO: 实现统一的用户认证机制
        
        user.id = student.id;
        user.realName = student.name;
        user.departmentId = student.departmentId;
        user.majorId = student.majorId;
        user.userType = 1; // 1-学生
        user.userCode = student.studentNo;
        
        // 生成token - 暂时注释掉，因为缺少JwtTokenUtil的定义
        // String token = jwtTokenUtil.generateToken(username);
        String token = "temporary-token-" + System.currentTimeMillis(); // 临时token
        
        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", user);
        
        return result;
    }

    @Override
    public boolean register(RegisterRequest registerRequest) {
        // 参数验证
        if (registerRequest == null) {
            throw new BusinessException(Constants.PARAM_ERROR_CODE, "注册请求不能为空");
        }
        if (!StringUtils.hasText(registerRequest.username)) {
            throw new BusinessException(Constants.PARAM_ERROR_CODE, "用户名不能为空");
        }
        if (!StringUtils.hasText(registerRequest.password)) {
            throw new BusinessException(Constants.PARAM_ERROR_CODE, "密码不能为空");
        }
        if (!StringUtils.hasText(registerRequest.role)) {
            throw new BusinessException(Constants.PARAM_ERROR_CODE, "角色不能为空");
        }

        // 暂时只支持学生注册
        if (Constants.ROLE_STUDENT.equals(registerRequest.role)) {
            // 检查学号是否已存在
            Student existingStudent = studentMapper.selectByStudentNo(registerRequest.username);
            if (existingStudent != null) {
                throw new BusinessException(Constants.DUPLICATE_CODE, "学号已存在");
            }
            
            // TODO: 学生注册逻辑需要与StudentService配合实现
            // 这里暂时返回未实现
            throw new BusinessException(Constants.NOT_IMPLEMENTED_CODE, "学生注册功能暂未实现");
        } else {
            throw new BusinessException(Constants.PARAM_ERROR_CODE, "仅支持学生注册");
        }
    }

    @Override
    public User getUserById(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(Constants.PARAM_ERROR_CODE, "用户ID不能为空");
        }
        
        // 尝试从学生表查询
        Student student = studentMapper.selectById(userId);
        if (student != null) {
            User user = new User();
            user.id = student.id;
            user.username = student.studentNo;
            user.realName = student.name;
            user.userType = 1; // 1-学生
            user.userCode = student.studentNo;
            user.departmentId = student.departmentId;
            user.majorId = student.majorId;
            return user;
        }
        
        // 尝试从教师表查询
        Teacher teacher = teacherMapper.selectById(userId);
        if (teacher != null) {
            User user = new User();
            user.setId(teacher.getId());
            user.setUsername(teacher.getTeacherNo());
            user.setRealName(teacher.getName());
            user.setUserType(2); // 2-教师
            user.setUserCode(teacher.getTeacherNo());
            user.setDepartmentId(teacher.getDepartmentId());
            return user;
        }
        
        // 管理员特殊处理
        if (userId == 0) {
            User user = new User();
            user.setId(0L);
            user.setUsername(Constants.ADMIN_USERNAME);
            user.setRealName("管理员");
            user.setUserType(3); // 3-管理员
            return user;
        }
        
        throw new BusinessException(Constants.NOT_FOUND_CODE, "用户不存在");
    }

    @Override
    public User getUserByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(Constants.PARAM_ERROR_CODE, "用户名不能为空");
        }
        
        // 尝试从学生表查询
        Student student = studentMapper.selectByStudentNo(username);
        if (student != null) {
            User user = new User();
            user.setId(student.getId());
            user.setUsername(student.getStudentNo());
            user.setRealName(student.getName());
            user.setUserType(1); // 1-学生
            user.setUserCode(student.getStudentNo());
            user.setDepartmentId(student.getDepartmentId());
            user.setMajorId(student.getMajorId());
            return user;
        }
        
        // 尝试从教师表查询
        Teacher teacher = teacherMapper.selectByTeacherNo(username);
        if (teacher != null) {
            User user = new User();
            user.setId(teacher.getId());
            user.setUsername(teacher.getTeacherNo());
            user.setRealName(teacher.getName());
            user.setUserType(2); // 2-教师
            user.setUserCode(teacher.getTeacherNo());
            user.setDepartmentId(teacher.getDepartmentId());
            return user;
        }
        
        // 管理员特殊处理
        if (Constants.ADMIN_USERNAME.equals(username)) {
            User user = new User();
            user.id = 0L;
            user.username = Constants.ADMIN_USERNAME;
            user.realName = "管理员";
            user.userType = 3; // 3-管理员
            return user;
        }
        
        throw new BusinessException(Constants.NOT_FOUND_CODE, "用户不存在");
    }

    @Override
    @Transactional(readOnly = false)
    public User updateUser(User user) {
        if (user == null) {
            throw new BusinessException(Constants.PARAM_ERROR_CODE, "用户信息不能为空");
        }
        if (user.getId() == null || user.getId() <= 0) {
            throw new BusinessException(Constants.PARAM_ERROR_CODE, "用户ID不能为空");
        }
        
        // 根据用户类型更新用户信息
        if (1 == user.userType) { // 学生类型
            Student student = studentMapper.selectById(user.getId());
            if (student == null) {
                throw new BusinessException(Constants.NOT_FOUND_CODE, "学生不存在");
            }
            
            // 更新学生信息
            if (StringUtils.hasText(user.realName)) {
                student.name = user.realName;
            }
            if (user.departmentId != null) {
                student.departmentId = user.departmentId;
            }
            if (user.majorId != null) {
                student.majorId = user.majorId;
            }
            
            int result = studentMapper.updateById(student);
            if (result <= 0) {
                logger.error("更新学生信息失败，学生ID: {}", user.id);
                throw new BusinessException(Constants.FAIL_CODE, "更新用户信息失败");
            }
            
            return getUserById(user.id);
        } else if (2 == user.userType) { // 教师类型
            Teacher teacher = teacherMapper.selectById(user.id);
            if (teacher == null) {
                throw new BusinessException(Constants.NOT_FOUND_CODE, "教师不存在");
            }
            
            // 更新教师信息
            if (StringUtils.hasText(user.realName)) {
                teacher.name = user.realName;
            }
            if (user.departmentId != null) {
                teacher.departmentId = user.departmentId;
            }
            
            int result = teacherMapper.updateById(teacher);
            if (result <= 0) {
                logger.error("更新教师信息失败，教师ID: {}", user.id);
                throw new BusinessException(Constants.FAIL_CODE, "更新用户信息失败");
            }
            
            return getUserById(user.id);
        } else {
            throw new BusinessException(Constants.PARAM_ERROR_CODE, "不支持该用户类型的信息更新");
        }
    }

    @Override
    public Page<User> getUserList(PageRequest pageRequest, String username, String realName, Integer userType) {
        // TODO: 实现分页查询用户列表
        // 这里需要与StudentMapper和TeacherMapper配合实现，暂时返回未实现
        throw new BusinessException(Constants.NOT_IMPLEMENTED_CODE, "分页查询用户列表功能暂未实现");
    }

    @Override
    public List<User> getUsersByUserType(Integer userType) {
        logger.info("根据用户类型获取用户列表，用户类型: {}", userType);
        
        if (userType == null) {
            throw new BusinessException(Constants.PARAM_ERROR_CODE, "用户类型不能为空");
        }
        
        List<User> userList = new ArrayList<>();
        
        // 根据用户类型查询对应的实体
        if (userType == 1) { // 学生用户
            List<Student> studentList = studentMapper.selectAll();
            for (Student student : studentList) {
                User user = new User();
                user.setId(student.getId());
                user.setUsername(student.getStudentNo());
                user.setRealName(student.getName());
                user.setUserType(1); // 1-学生
                user.setUserCode(student.getStudentNo());
                user.setDepartmentId(student.getDepartmentId());
                user.setMajorId(student.getMajorId());
                userList.add(user);
            }
        } else if (userType == 2) { // 教师用户
            List<Teacher> teacherList = teacherMapper.selectAll();
            for (Teacher teacher : teacherList) {
                User user = new User();
                user.setId(teacher.getId());
                user.setUsername(teacher.getTeacherNo());
                user.setRealName(teacher.getName());
                user.setUserType(2); // 2-教师
                user.setUserCode(teacher.getTeacherNo());
                user.setDepartmentId(teacher.getDepartmentId());
                userList.add(user);
            }
        } else {
            throw new BusinessException(Constants.PARAM_ERROR_CODE, "不支持的用户类型");
        }
        
        logger.info("获取用户列表完成，共查询到 {} 条数据", userList.size());
        return userList;
    }

    @Override
    @Transactional(readOnly = false)
    public boolean resetPassword(Long userId, String password) {
        // 此方法暂时不实现，因为Student和Teacher实体中没有password字段
        throw new BusinessException(Constants.NOT_IMPLEMENTED_CODE, "重置密码功能暂未实现");
    }

    @Override
    @Transactional(readOnly = false)
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        // 此方法暂时不实现，因为Student和Teacher实体中没有password字段
        throw new BusinessException(Constants.NOT_IMPLEMENTED_CODE, "修改密码功能暂未实现");
    }

    @Override
    @Transactional(readOnly = false)
    public boolean changeStatus(Long userId, Integer status) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(Constants.PARAM_ERROR_CODE, "用户ID不能为空");
        }
        if (status == null || (status != 1 && status != 2)) {
            throw new BusinessException(Constants.PARAM_ERROR_CODE, "无效的状态值");
        }
        
        // 尝试从学生表查询
        Student student = studentMapper.selectById(userId);
        if (student != null) {
            student.setStatus(status);
            int result = studentMapper.updateById(student);
            if (result > 0) {
                logger.info("修改学生状态成功，学生ID: {}, 状态: {}", userId, status);
                return true;
            }
            throw new BusinessException(Constants.FAIL_CODE, "修改状态失败");
        }
        
        // 尝试从教师表查询
        Teacher teacher = teacherMapper.selectById(userId);
        if (teacher != null) {
            teacher.setStatus(status);
            int result = teacherMapper.updateById(teacher);
            if (result > 0) {
                logger.info("修改教师状态成功，教师ID: {}, 状态: {}", userId, status);
                return true;
            }
            throw new BusinessException(Constants.FAIL_CODE, "修改状态失败");
        }
        
        throw new BusinessException(Constants.NOT_FOUND_CODE, "用户不存在");
    }

    @Override
    @Transactional(readOnly = false)
    public boolean deleteUser(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(Constants.PARAM_ERROR_CODE, "用户ID不能为空");
        }
        
        // 尝试从学生表查询并删除
        Student student = studentMapper.selectById(userId);
        if (student != null) {
            int result = studentMapper.deleteById(userId);
            if (result > 0) {
                logger.info("删除学生成功，学生ID: {}", userId);
                return true;
            }
            throw new BusinessException(Constants.FAIL_CODE, "删除用户失败");
        }
        
        // 尝试从教师表查询并删除
        Teacher teacher = teacherMapper.selectById(userId);
        if (teacher != null) {
            int result = teacherMapper.deleteById(userId);
            if (result > 0) {
                logger.info("删除教师成功，教师ID: {}", userId);
                return true;
            }
            throw new BusinessException(Constants.FAIL_CODE, "删除用户失败");
        }
        
        throw new BusinessException(Constants.NOT_FOUND_CODE, "用户不存在");
    }

    @Override
    @Transactional(readOnly = false)
    public boolean batchDeleteUser(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            throw new BusinessException(Constants.PARAM_ERROR_CODE, "用户ID列表不能为空");
        }
        
        // 这里简化处理，逐个删除
        for (Long userId : userIds) {
            deleteUser(userId);
        }
        
        logger.info("批量删除用户成功，删除数量: {}", userIds.size());
        return true;
    }
    
    @Override
    public boolean login(String username, String password, String role) {
        logger.info("用户登录请求处理开始，用户名: {}", username);
        
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(Constants.PARAM_ERROR_CODE, "用户名不能为空");
        }
        if (!StringUtils.hasText(password)) {
            throw new BusinessException(Constants.PARAM_ERROR_CODE, "密码不能为空");
        }
        if (!StringUtils.hasText(role)) {
            throw new BusinessException(Constants.PARAM_ERROR_CODE, "角色不能为空");
        }

        // 根据不同角色进行登录验证
        if ("student".equals(role)) {
            Student student = studentMapper.selectByStudentNo(username);
            if (student != null && isEnabled(student.getStatus()) && passwordMatches(password, student.getPassword())) {
                logger.info("Student login success, studentNo: {}", username);
                return true;
            }
            logger.warn("Student login failed, studentNo: {}", username);
            return false;
        }
        if ("teacher".equals(role)) {
            Teacher teacher = teacherMapper.selectByTeacherNo(username);
            if (teacher != null && isEnabled(teacher.getStatus()) && passwordMatches(password, teacher.getPassword())) {
                logger.info("Teacher login success, teacherNo: {}", username);
                return true;
            }
            logger.warn("Teacher login failed, teacherNo: {}", username);
            return false;
        }
        if ("admin".equals(role)) {
            Admin admin = adminRepository.findByUsername(username).orElse(null);
            if (admin != null && isEnabled(admin.getStatus()) && passwordMatches(password, admin.getPassword())) {
                logger.info("Admin login success, username: {}", username);
                return true;
            }
            if ("admin".equals(username) && "admin123".equals(password)) {
                logger.info("Admin login success with legacy fallback account");
                return true;
            }
            logger.warn("Admin login failed, username: {}", username);
            return false;
        }

        if ("student".equals(role)) {
            Student student = studentMapper.selectByStudentNo(username);
            if (student != null) {
                // 学生登录逻辑，这里暂时简化处理
                logger.info("学生登录成功，学号: {}", username);
                return true;
            }
        } else if ("teacher".equals(role)) {
            // 教师登录逻辑，这里暂时简化处理
            logger.info("教师登录验证，工号: {}", username);
            return true;
        } else if ("admin".equals(role)) {
            // 管理员登录逻辑
            if ("admin".equals(username) && "admin123".equals(password)) {
                logger.info("管理员登录成功");
                return true;
            }
        }
        
        logger.warn("登录验证失败，用户名: {}", username);
        return false;
    }

    private boolean isEnabled(Integer status) {
        return status == null || status == 1;
    }

    private boolean passwordMatches(String rawPassword, String storedPassword) {
        if (!StringUtils.hasText(storedPassword)) {
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
                logger.warn("Invalid BCrypt password format");
                return false;
            }
        }
        return storedPassword.equals(rawPassword);
    }
}
