package org.example.courseselectionsystem.controller;

import org.example.courseselectionsystem.common.Result;
import org.example.courseselectionsystem.entity.Student;
import org.example.courseselectionsystem.entity.Teacher;
import org.example.courseselectionsystem.exception.BusinessException;
import org.example.courseselectionsystem.service.StudentService;
import org.example.courseselectionsystem.service.TeacherService;
import org.example.courseselectionsystem.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/login")
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private TeacherService teacherService;

    @GetMapping
    public String toLogin() {
        return "login";
    }

    @PostMapping("/auth")
    @ResponseBody
    public Result<String> login(@RequestParam String username,
                                @RequestParam String password,
                                @RequestParam String role,
                                HttpServletRequest request) {
        try {
            boolean loginSuccess = userService.login(username, password, role);
            if (!loginSuccess) {
                return Result.fail(401, "用户名或密码错误");
            }

            HttpSession session = request.getSession();
            session.setAttribute("role", role);
            session.setAttribute("loginName", username);

            if ("student".equals(role)) {
                Student student = studentService.getStudentByStudentNo(username);
                session.setAttribute("username", student.getStudentNo());
                session.setAttribute("userId", student.getId());
                session.setAttribute("user", student);
            } else if ("teacher".equals(role)) {
                Teacher teacher = teacherService.getTeacherByTeacherNo(username);
                session.setAttribute("username", teacher.getTeacherNo());
                session.setAttribute("userId", teacher.getId());
                session.setAttribute("user", teacher);
            } else if ("admin".equals(role)) {
                session.setAttribute("username", username);
                session.setAttribute("userId", 0);
            }

            authenticateSession(request, username, role);
            return Result.success(getRedirectUrlByRole(role));
        } catch (BusinessException e) {
            logger.warn("Login failed: {}", e.getMessage());
            return Result.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            logger.error("Login failed unexpectedly, username: {}, role: {}", username, role, e);
            return Result.fail(500, "登录失败，请稍后重试");
        }
    }

    @GetMapping("/current")
    @ResponseBody
    public Result<Map<String, Object>> currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            return Result.unauthorized("未登录");
        }

        Map<String, Object> user = new HashMap<>();
        user.put("username", session.getAttribute("username"));
        user.put("loginName", session.getAttribute("loginName"));
        user.put("role", session.getAttribute("role"));
        user.put("userId", session.getAttribute("userId"));
        user.put("user", session.getAttribute("user"));
        return Result.success(user);
    }

    @GetMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create("/login"));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    private String getRedirectUrlByRole(String role) {
        switch (role) {
            case "student":
                return "/student/index.html";
            case "teacher":
                return "/teacher/index.html";
            case "admin":
                return "/admin/index.html";
            default:
                return "/login";
        }
    }

    private void authenticateSession(HttpServletRequest request, String username, String role) {
        String authority = "ROLE_" + role.toUpperCase();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority(authority))
                );
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        request.getSession().setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                securityContext
        );
    }
}
