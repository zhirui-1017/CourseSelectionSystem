package org.example.courseselectionsystem.config;

import org.example.courseselectionsystem.auth.UserContextFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 自动配置 —— 在每个微服务中注册 UserContextFilter
 * <p>
 * 该配置类会被 Spring Boot 自动扫描并加载。
 * 仅在 Web 应用中生效（排除 Gateway 等非 Servlet 环境）。
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class UserContextFilterAutoConfiguration {

    @Bean
    public FilterRegistrationBean<UserContextFilter> userContextFilterRegistration() {
        FilterRegistrationBean<UserContextFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new UserContextFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(1); // 高优先级
        registration.setName("userContextFilter");
        return registration;
    }
}
