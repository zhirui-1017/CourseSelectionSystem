package org.example.courseselectionsystem.interceptor;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

/**
 * 登录拦截器
 * 用于拦截未登录的用户请求
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {
    
    // 不需要拦截的URL列表
    private static final List<String> EXCLUDE_URLS = Arrays.asList(
            "/user/login", 
            "/login",
            "/login.html",
            "/user/register",
            "/static/**",
            "/css/**",
            "/js/**",
            "/images/**",
            "/actuator/**"
    );
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取请求URL
        String requestUrl = request.getRequestURI();
        
        // 判断是否是不需要拦截的URL
        for (String url : EXCLUDE_URLS) {
            if (requestUrl.startsWith(url)) {
                return true;
            }
        }
        
        // 判断是否为静态资源请求
        if (isStaticResource(requestUrl)) {
            return true;
        }
        
        // 获取session
        HttpSession session = request.getSession(false);
        
        // 判断用户是否登录
        if (session == null || session.getAttribute("username") == null) {
            // 如果是AJAX请求
            if (isAjaxRequest(request)) {
                response.setCharacterEncoding("UTF-8");
                response.setContentType("application/json; charset=utf-8");
                response.getWriter().write("{\"code\": 401, \"message\": \"未登录或登录已过期，请重新登录\", \"data\": null}");
            } else {
                // 重定向到登录页面
                redirectToLogin(request, response);
            }
            return false;
        }
        
        // 验证用户权限
        if (!checkPermission(request, session)) {
            // 如果是AJAX请求
            if (isAjaxRequest(request)) {
                response.setCharacterEncoding("UTF-8");
                response.setContentType("application/json; charset=utf-8");
                response.getWriter().write("{\"code\": 403, \"message\": \"权限不足，无法访问该资源\", \"data\": null}");
            } else {
                // 重定向到无权限提示页面
                response.sendRedirect(request.getContextPath() + "/error/403");
            }
            return false;
        }
        
        return true;
    }
    
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        // 可以在这里添加一些通用的模型数据
        if (modelAndView != null) {
            HttpSession session = request.getSession();
            String username = (String) session.getAttribute("username");
            String role = (String) session.getAttribute("role");
            
            modelAndView.addObject("currentUsername", username);
            modelAndView.addObject("currentRole", role);
        }
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求处理完成后的清理工作
    }
    
    /**
     * 判断是否为静态资源请求
     */
    private boolean isStaticResource(String requestUrl) {
        String[] staticSuffixes = {".css", ".js", ".jpg", ".jpeg", ".png", ".gif", ".ico", ".html", ".htm"};
        for (String suffix : staticSuffixes) {
            if (requestUrl.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 判断是否为AJAX请求
     */
    private boolean isAjaxRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        String xRequestedWith = request.getHeader("X-Requested-With");
        
        return (accept != null && accept.contains("application/json"))
                || (xRequestedWith != null && xRequestedWith.contains("XMLHttpRequest"));
    }
    
    /**
     * 重定向到登录页面
     */
    private void redirectToLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 保存当前请求URL，登录成功后可以重定向回来
        String currentUrl = request.getRequestURI();
        request.getSession().setAttribute("redirectUrl", currentUrl);
        
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", URI.create("/login").toString());
    }
    
    /**
     * 检查用户权限
     */
    private boolean checkPermission(HttpServletRequest request, HttpSession session) {
        String requestUrl = request.getRequestURI();
        String role = (String) session.getAttribute("role");
        
        // 管理员可以访问所有资源
        if ("admin".equals(role)) {
            return true;
        }
        
        // 学生只能访问学生相关资源
        if ("student".equals(role) && requestUrl.startsWith("/student/")) {
            return true;
        }
        
        // 教师只能访问教师相关资源
        if ("teacher".equals(role) && requestUrl.startsWith("/teacher/")) {
            return true;
        }
        
        // 所有登录用户都可以访问的公共资源
        if (requestUrl.startsWith("/user/")) {
            return true;
        }
        
        return false;
    }
}
