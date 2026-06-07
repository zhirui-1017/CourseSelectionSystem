package org.example.courseselectionsystem.controller;

import org.example.courseselectionsystem.common.Result;
import org.example.courseselectionsystem.entity.User;
import org.example.courseselectionsystem.service.UserService;
import org.example.courseselectionsystem.vo.LoginRequest;
import org.example.courseselectionsystem.vo.PageRequest;
import org.example.courseselectionsystem.vo.RegisterRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 用户登录
     * @param loginRequest 登录请求参数
     * @return 登录结果
     */
    @PostMapping("/login")
    public Result login(@Valid @RequestBody LoginRequest loginRequest) {
        Map<String, Object> loginResult = userService.login(loginRequest);
        return Result.success(loginResult);
    }

    /**
     * 用户注册
     * @param registerRequest 注册请求参数
     * @return 注册结果
     */
    @PostMapping("/register")
    public Result register(@Valid @RequestBody RegisterRequest registerRequest) {
        boolean result = userService.register(registerRequest);
        return Result.success(result);
    }

    /**
     * 获取用户信息
     * @param userId 用户ID
     * @return 用户信息
     */
    @GetMapping("/{userId}")
    public Result getUserById(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        return Result.success(user);
    }

    /**
     * 根据用户名获取用户
     * @param username 用户名
     * @return 用户信息
     */
    @GetMapping("/username/{username}")
    public Result getUserByUsername(@PathVariable String username) {
        User user = userService.getUserByUsername(username);
        return Result.success(user);
    }

    /**
     * 更新用户信息
     * @param userId 用户ID
     * @param user 用户信息
     * @return 更新结果
     */
    @PutMapping("/{userId}")
    public Result updateUser(@PathVariable Long userId, @RequestBody User user) {
        user.setId(userId);
        User updatedUser = userService.updateUser(user);
        return Result.success(updatedUser);
    }

    /**
     * 获取用户列表
     * @param pageRequest 分页请求参数
     * @param username 用户名(可选)
     * @param realName 真实姓名(可选)
     * @param userType 用户类型(可选)
     * @return 用户列表
     */
    @GetMapping("/list")
    public Result getUserList(PageRequest pageRequest, 
                              @RequestParam(required = false) String username, 
                              @RequestParam(required = false) String realName,
                              @RequestParam(required = false) Integer userType) {
        Page<User> users = userService.getUserList(pageRequest, username, realName, userType);
        return Result.success(users);
    }

    /**
     * 重置用户密码
     * @param userId 用户ID
     * @return 操作结果
     */
    @PutMapping("/{userId}/reset-password")
    public Result resetPassword(@PathVariable Long userId, @RequestParam(defaultValue = "123456") String password) {
        boolean result = userService.resetPassword(userId, password);
        return Result.success(result);
    }

    /**
     * 修改密码
     * @param userId 用户ID
     * @param passwordInfo 密码信息
     * @return 修改结果
     */
    @PutMapping("/{userId}/change-password")
    public Result changePassword(@PathVariable Long userId, @RequestBody Map<String, String> passwordInfo) {
        String oldPassword = passwordInfo.get("oldPassword");
        String newPassword = passwordInfo.get("newPassword");
        boolean result = userService.changePassword(userId, oldPassword, newPassword);
        return Result.success(result);
    }

    /**
     * 修改用户状态
     * @param userId 用户ID
     * @param status 状态
     * @return 修改结果
     */
    @PutMapping("/{userId}/status")
    public Result changeStatus(@PathVariable Long userId, @RequestParam Integer status) {
        boolean result = userService.changeStatus(userId, status);
        return Result.success(result);
    }

    /**
     * 删除用户
     * @param userId 用户ID
     * @return 删除结果
     */
    @DeleteMapping("/{userId}")
    public Result deleteUser(@PathVariable Long userId) {
        boolean result = userService.deleteUser(userId);
        return Result.success(result);
    }

    /**
     * 批量删除用户
     * @param userIds 用户ID列表
     * @return 删除结果
     */
    @DeleteMapping("/batch")
    public Result batchDeleteUsers(@RequestBody List<Long> userIds) {
        boolean result = userService.batchDeleteUser(userIds);
        return Result.success(result);
    }

    /**
     * 根据用户类型获取用户列表
     * @param userType 用户类型
     * @return 用户列表
     */
    @GetMapping("/by-type/{userType}")
    public Result getUsersByUserType(@PathVariable Integer userType) {
        return Result.success(userService.getUsersByUserType(userType));
    }
}
