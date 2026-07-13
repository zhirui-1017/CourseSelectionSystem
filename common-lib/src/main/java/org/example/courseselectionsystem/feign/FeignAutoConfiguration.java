package org.example.courseselectionsystem.feign;

import feign.RequestInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign 自动配置 —— 注册 FeignAuthInterceptor
 * <p>
 * 仅在 classpath 中存在 Feign 相关类时生效。
 */
@Configuration
@ConditionalOnClass(feign.RequestInterceptor.class)
public class FeignAutoConfiguration {

    @Bean
    public RequestInterceptor feignAuthInterceptor() {
        return new FeignAuthInterceptor();
    }
}
