package org.example.courseselectionsystem.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.example.courseselectionsystem.auth.AuthConstants;
import org.example.courseselectionsystem.auth.JwtPayload;
import org.example.courseselectionsystem.auth.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Feign 认证拦截器
 * <p>
 * 当服务通过 Feign 调用另一个微服务时，自动将当前用户信息写入请求头，
 * 让下游服务也能获取到用户上下文。
 */
public class FeignAuthInterceptor implements RequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(FeignAuthInterceptor.class);

    @Override
    public void apply(RequestTemplate template) {
        JwtPayload payload = UserContext.get();
        if (payload != null && payload.getUserId() != null) {
            template.header(AuthConstants.HEADER_USER_ID, String.valueOf(payload.getUserId()));
            template.header(AuthConstants.HEADER_USERNAME, payload.getUsername() != null ? payload.getUsername() : "");
            template.header(AuthConstants.HEADER_ROLE, payload.getRole() != null ? payload.getRole() : "");
            log.debug("Feign 请求头已设置: userId={}, username={}, role={}",
                    payload.getUserId(), payload.getUsername(), payload.getRole());
        } else {
            log.debug("UserContext 为空，跳过 Feign 认证头设置");
        }
    }
}
