package org.example.courseselectionsystem.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 用户上下文过滤器
 * <p>
 * 从 Gateway 传递的请求头中解析当前用户信息，存入 ThreadLocal（UserContext）。
 * 请求处理完成后自动清理。
 * <p>
 * 需要注册方式（选其一）：
 * <pre>
 *   // Spring Boot 组件扫描
 *   @Component
 *   public class UserContextFilter extends org.example.courseselectionsystem.auth.UserContextFilter {}
 *
 *   // 或通过 FilterRegistrationBean 手动注册
 * </pre>
 */
public class UserContextFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(UserContextFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        try {
            // 从请求头读取 Gateway 传递的用户信息
            String userId = httpRequest.getHeader(AuthConstants.HEADER_USER_ID);
            String username = httpRequest.getHeader(AuthConstants.HEADER_USERNAME);
            String role = httpRequest.getHeader(AuthConstants.HEADER_ROLE);

            if (userId != null && username != null && role != null) {
                UserContext.set(Long.parseLong(userId), username, role);
                log.debug("UserContext 已设置: userId={}, username={}, role={}", userId, username, role);
            }
        } catch (Exception e) {
            log.warn("解析用户请求头失败", e);
        }

        try {
            chain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }
}
