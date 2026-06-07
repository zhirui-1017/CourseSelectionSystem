package org.example.courseselectionsystem.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * Spring Security配置类
 * 配置认证和授权规则
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    /**
     * 配置HTTP安全
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                // 允许访问静态资源
                .antMatchers("/static/**", "/webjars/**", "/", "/login", "/login/auth", "/register").permitAll()
                .antMatchers("/css/**", "/js/**", "/images/**").permitAll()
                .antMatchers("/admin/*.html", "/student/*.html", "/teacher/*.html").permitAll()
                // API接口需要认证
                .antMatchers("/api/**").authenticated()
                // 其他请求需要认证
                .anyRequest().authenticated()
                .and()
                .formLogin()
                .loginPage("/login")
                .permitAll()
                .and()
                .logout()
                .permitAll()
                .and()
                .csrf().disable(); // 暂时禁用CSRF保护，生产环境建议启用
    }
}
