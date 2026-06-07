package org.example.courseselectionsystem.controller;

import org.example.courseselectionsystem.common.Result;
import org.example.courseselectionsystem.entity.Student;
import org.example.courseselectionsystem.entity.Teacher;
import org.example.courseselectionsystem.service.StudentService;
import org.example.courseselectionsystem.service.TeacherService;
import org.example.courseselectionsystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * 登录控制器
 * 处理用户登录相关的请求
 */
@Controller
@RequestMapping("/login")
public class LoginController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private StudentService studentService;
    
    @Autowired
    private TeacherService teacherService;

    /**
     * 跳转到登录页面
     * @return 登录页面
     */
    @GetMapping
    public String toLogin() {
        return "login";
    }

    /**
     * 处理登录请求
     * @param username 用户名（学号/工号）
     * @param password 密码
     * @param role 角色（学生/教师/管理员）
     * @param request 请求对象
     * @return 登录结果
     */
    @PostMapping("/auth")
    @ResponseBody
    public Result<String> login(@RequestParam String username, 
                              @RequestParam String password, 
                              @RequestParam String role, 
                              HttpServletRequest request) {
        try {
            // 验证用户登录信息
            boolean loginSuccess = userService.login(username, password, role);
            if (!loginSuccess) {
                return Result.fail(401, "用户名或密码错误");
            }
            
            // 登录成功，将用户信息存入session
            HttpSession session = request.getSession();
            session.setAttribute("username", username);
            session.setAttribute("role", role);
            
            // 根据角色获取用户信息
            if ("student".equals(role)) {
                Student student = studentService.getStudentByStudentNo(username);
                session.setAttribute("userId", student.getId());
                session.setAttribute("user", student);
            } else if ("teacher".equals(role)) {
                Teacher teacher = teacherService.getTeacherByTeacherNo(username);
                session.setAttribute("userId", teacher.getId());
                session.setAttribute("user", teacher);
            } else if ("admin".equals(role)) {
                session.setAttribute("userId", 0); // 管理员ID为0
            }
            
            // 返回重定向地址
            String redirectUrl = getRedirectUrlByRole(role);
            return Result.success(redirectUrl);
        } catch (Exception e) {
            return Result.fail(500, "登录失败，请稍后重试");
        }
    }

    /**
     * 处理退出登录
     * @param request 请求对象
     * @return 重定向到登录页面
     */
    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession();
        session.invalidate(); // 使session失效
        return "redirect:/login";
    }
    
    /**
     * 根据角色获取登录成功后重定向的URL
     * @param role 角色
     * @return 重定向URL
     */
    private String getRedirectUrlByRole(String role) {
        switch (role) {
            case "student":
                return "/student/index";
            case "teacher":
                return "/teacher/index";
            case "admin":
                return "/admin/index";
            default:
                return "/login";
        }
    }
}
