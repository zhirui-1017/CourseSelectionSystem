package org.example.courseselectionsystem.service;

import org.example.courseselectionsystem.entity.User;
import org.example.courseselectionsystem.vo.LoginRequest;
import org.example.courseselectionsystem.vo.PageRequest;
import org.example.courseselectionsystem.vo.RegisterRequest;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 用户登录
     * @param loginRequest 登录请求参数
     * @return 登录结果
     */
    Map<String, Object> login(LoginRequest loginRequest);
    
    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @param role 角色
     * @return 登录成功返回true，否则返回false
     */
    boolean login(String username, String password, String role);

    /**
     * 用户注册
     * @param registerRequest 注册请求参数
     * @return 注册结果
     */
    boolean register(RegisterRequest registerRequest);

    /**
     * 获取用户信息
     * @param userId 用户ID
     * @return 用户对象
     */
    User getUserById(Long userId);

    /**
     * 根据用户名获取用户
     * @param username 用户名
     * @return 用户对象
     */
    User getUserByUsername(String username);

    /**
     * 更新用户信息
     * @param user 用户对象
     * @return 更新后的用户对象
     */
    User updateUser(User user);

    /**
     * 分页查询用户列表
     * @param pageRequest 分页请求参数
     * @param username 用户名
     * @param realName 真实姓名
     * @param userType 用户类型
     * @return 用户分页列表
     */
    Page<User> getUserList(PageRequest pageRequest, String username, String realName, Integer userType);

    /**
     * 重置用户密码
     * @param userId 用户ID
     * @param password 新密码
     * @return 操作结果
     */
    boolean resetPassword(Long userId, String password);

    /**
     * 修改密码
     * @param userId 用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @return 操作结果
     */
    boolean changePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 启用/禁用用户
     * @param userId 用户ID
     * @param status 状态：1-启用，2-禁用
     * @return 操作结果
     */
    boolean changeStatus(Long userId, Integer status);

    /**
     * 删除用户
     * @param userId 用户ID
     * @return 操作结果
     */
    boolean deleteUser(Long userId);

    /**
     * 批量删除用户
     * @param userIds 用户ID列表
     * @return 操作结果
     */
    boolean batchDeleteUser(List<Long> userIds);

    /**
     * 根据用户类型获取用户列表
     * @param userType 用户类型：1-学生，2-教师，3-管理员
     * @return 用户列表
     */
    List<User> getUsersByUserType(Integer userType);
}
