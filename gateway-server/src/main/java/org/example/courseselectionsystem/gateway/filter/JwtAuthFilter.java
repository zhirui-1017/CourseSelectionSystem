package org.example.courseselectionsystem.gateway.filter;

import org.example.courseselectionsystem.auth.AuthConstants;
import org.example.courseselectionsystem.auth.JwtPayload;
import org.example.courseselectionsystem.auth.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * JWT 全局认证过滤器
 * <p>
 * 所有经过 Gateway 的请求，先验证 JWT Token（白名单路径除外），
 * 解析后的用户信息通过 Header 传递给下游微服务。
 */
@Component
@Order(-100) // 高优先级，在其他过滤器之前执行
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    /**
     * 白名单路径——不需要 JWT 认证
     */
    private static final Set<String> WHITELIST_PATHS = new HashSet<>(Arrays.asList(
            "/login", "/login.html", "/register",
            "/api/v1/auth/login", "/api/v1/auth/register",
            "/api/v1/users/login", "/api/v1/users/register",
            "/static", "/css", "/js", "/images", "/lib",
            "/webjars", "/favicon.ico",
            "/actuator", "/fallback"
    ));

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 白名单路径直接放行
        if (isWhitelisted(path)) {
            log.debug("白名单路径放行: {}", path);
            return chain.filter(exchange);
        }

        // 静态资源请求放行（.html, .css, .js, .png, .jpg 等）
        if (isStaticResource(path)) {
            return chain.filter(exchange);
        }

        // 从 Header 中获取 JWT Token
        String authHeader = request.getHeaders().getFirst(AuthConstants.HEADER_AUTHORIZATION);
        String token = JwtUtil.extractToken(authHeader);

        if (token == null || token.isEmpty()) {
            log.warn("请求缺少 Token: path={}", path);
            return unauthorized(exchange, "未提供认证 Token");
        }

        // 验证 Token
        JwtPayload payload = JwtUtil.parseToken(token);
        if (payload == null) {
            log.warn("Token 无效或已过期: path={}", path);
            return unauthorized(exchange, "Token 无效或已过期");
        }

        log.debug("JWT 认证通过: userId={}, username={}, role={}, path={}",
                payload.getUserId(), payload.getUsername(), payload.getRole(), path);

        // 将用户信息写入请求 Header，传递给下游微服务
        ServerHttpRequest mutatedRequest = request.mutate()
                .header(AuthConstants.HEADER_USER_ID, String.valueOf(payload.getUserId()))
                .header(AuthConstants.HEADER_USERNAME, payload.getUsername())
                .header(AuthConstants.HEADER_ROLE, payload.getRole())
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return -100;
    }

    /**
     * 判断请求路径是否在白名单中（前缀匹配）
     */
    private boolean isWhitelisted(String path) {
        if (path == null) {
            return false;
        }
        for (String whitelistPath : WHITELIST_PATHS) {
            if (path.startsWith(whitelistPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否为静态资源请求
     */
    private boolean isStaticResource(String path) {
        if (path == null) {
            return false;
        }
        String lower = path.toLowerCase();
        return lower.matches(".*\\.(html|css|js|png|jpg|jpeg|gif|svg|ico|woff|woff2|ttf|eot|map)$")
                || lower.startsWith("/static/")
                || lower.startsWith("/lib/")
                || lower.startsWith("/webjars/");
    }

    /**
     * 返回 401 未授权响应
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().set(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");
        String body = String.format("{\"code\":401,\"message\":\"%s\",\"success\":false,\"data\":null}", message);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }
}
