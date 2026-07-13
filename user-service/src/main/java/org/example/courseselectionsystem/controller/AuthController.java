package org.example.courseselectionsystem.controller;

import org.example.courseselectionsystem.auth.AuthConstants;
import org.example.courseselectionsystem.auth.JwtUtil;
import org.example.courseselectionsystem.common.Constants;
import org.example.courseselectionsystem.common.Result;
import org.example.courseselectionsystem.entity.User;
import org.example.courseselectionsystem.service.UserService;
import org.example.courseselectionsystem.vo.LoginRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器 —— 提供 JWT 登录/登出接口
 * <p>
 * 对应路径: /api/v1/auth/**
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserService userService;

    /**
     * 用户登录（返回 JWT Token）
     *
     * @param loginRequest 登录请求（username + password）
     * @return { token, user }
     */
    @PostMapping("/login")
    public Result login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("JWT 登录请求: username={}", loginRequest.getUsername());
        Map<String, Object> loginResult = userService.login(loginRequest);
        return Result.success(loginResult);
    }

    /**
     * 用户登出（JWT 模式下由前端清除 Token 即可，后端提供空实现）
     */
    @PostMapping("/logout")
    public Result logout() {
        return Result.success("已登出");
    }

    /**
     * 获取当前登录用户信息
     * <p>
     * 需要 Gateway 已经将 JWT 中的用户信息通过 Header 传递下来，
     * 或由前端直接解析 JWT payload。
     */
    @GetMapping("/current-user")
    public Result getCurrentUser() {
        // 当前用户信息由 Gateway 在 Header 中传递
        // 此处返回提示，实际信息由前端从 localStorage 中读取
        return Result.success("请从 token 中获取用户信息");
    }
}
