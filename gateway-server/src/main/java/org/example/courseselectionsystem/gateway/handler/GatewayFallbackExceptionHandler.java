package org.example.courseselectionsystem.gateway.handler;

import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
@Order(-3)
public class GatewayFallbackExceptionHandler implements ErrorWebExceptionHandler {

    public static final String FALLBACK_BODY = "{\"code\":503,\"message\":\"\u670d\u52a1\u6682\u65f6\u4e0d\u53ef\u7528\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5\",\"data\":null,\"success\":false}";

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (!isServiceUnavailable(ex)) {
            return Mono.error(ex);
        }
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        byte[] bytes = FALLBACK_BODY.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private boolean isServiceUnavailable(Throwable ex) {
        return ex instanceof ResponseStatusException
                && HttpStatus.SERVICE_UNAVAILABLE.equals(((ResponseStatusException) ex).getStatus());
    }
}
