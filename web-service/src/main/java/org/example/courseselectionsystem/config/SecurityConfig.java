package org.example.courseselectionsystem.config;

import org.springframework.http.HttpStatus;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                .antMatchers(
                        "/static/**", "/webjars/**", "/", "/login", "/login.html", "/login/auth",
                        "/login/current", "/login/logout", "/register", "/actuator/**"
                ).permitAll()
                .antMatchers("/css/**", "/js/**", "/images/**").permitAll()
                .antMatchers("/admin/*.html", "/student/*.html", "/teacher/*.html").permitAll()
                .antMatchers("/api/**").authenticated()
                .anyRequest().authenticated()
                .and()
                .exceptionHandling()
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpStatus.FOUND.value());
                    response.setHeader("Location", "/login");
                })
                .and()
                .formLogin()
                .loginPage("/login")
                .permitAll()
                .and()
                .logout()
                .permitAll()
                .and()
                .csrf().disable();
    }
}
