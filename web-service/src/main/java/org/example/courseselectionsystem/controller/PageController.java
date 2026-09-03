package org.example.courseselectionsystem.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;

/**
 * 页面路由控制器（web-service 瘦身后保留）
 * <p>
 * 仅保留根路径跳转、退出登录、健康检查；
 * 所有静态 HTML 页面由 Spring Boot 默认静态资源处理器直接提供，
 * 避免使用 forward: 字符串导致请求自转发死循环（StackOverflowError）。
 */
@Controller
public class PageController {

    @GetMapping("/")
    public String index() {
        return "redirect:/login.html";
    }

    /**
     * 兼容 /login（不带 .html）的访问路径
     * 直接跳转到静态资源 /login.html，避免 Spring Boot 默认返回 404 Whitelabel 错误页
     */
    @GetMapping("/login")
    public String login() {
        return "redirect:/login.html";
    }

    /**
     * 前端页面使用 href="/login/logout" (GET) 退出登录
     * 清除 Session 和 localStorage 中的 JWT Token 后重定向到登录页
     */
    @GetMapping("/login/logout")
    @ResponseBody
    public String logout(HttpSession session) {
        if (session != null) {
            session.invalidate();
        }
        // 返回 HTML 页面清除 localStorage 中的 JWT 相关数据后跳转登录页
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\">"
                + "<script>"
                + "localStorage.removeItem('token');"
                + "localStorage.removeItem('userInfo');"
                + "localStorage.removeItem('username');"
                + "localStorage.removeItem('selectedRole');"
                + "window.location.href = '/login.html';"
                + "</script></head><body></body></html>";
    }

    @GetMapping("/health")
    @ResponseBody
    public String health() {
        return "OK";
    }
}
