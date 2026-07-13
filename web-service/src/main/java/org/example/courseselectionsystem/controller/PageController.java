package org.example.courseselectionsystem.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 页面路由控制器（web-service 瘦身后保留）
 * <p>
 * 负责处理前端页面跳转，不包含任何业务逻辑。
 * 所有 API 请求已由 Gateway 分发到对应的微服务。
 */
@Controller
public class PageController {

    @GetMapping("/")
    public String index() {
        return "redirect:/login.html";
    }

    @GetMapping("/login.html")
    public String login() {
        return "forward:/login.html";
    }

    // ==================== 管理员页面 ====================

    @GetMapping("/admin/index.html")
    public String adminIndex() {
        return "forward:/admin/index.html";
    }

    @GetMapping("/admin/**")
    public String adminPages() {
        return "forward:/admin/index.html";
    }

    // ==================== 学生页面 ====================

    @GetMapping("/student/index.html")
    public String studentIndex() {
        return "forward:/student/index.html";
    }

    @GetMapping("/student/**")
    public String studentPages() {
        return "forward:/student/index.html";
    }

    // ==================== 教师页面 ====================

    @GetMapping("/teacher/index.html")
    public String teacherIndex() {
        return "forward:/teacher/index.html";
    }

    @GetMapping("/teacher/**")
    public String teacherPages() {
        return "forward:/teacher/index.html";
    }

    // ==================== 健康检查 ====================

    @GetMapping("/health")
    @org.springframework.web.bind.annotation.ResponseBody
    public String health() {
        return "OK";
    }
}
