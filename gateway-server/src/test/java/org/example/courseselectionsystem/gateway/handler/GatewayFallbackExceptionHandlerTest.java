package org.example.courseselectionsystem.gateway.handler;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayFallbackExceptionHandlerTest {

    @Test
    void handleWritesStandardFallbackJson() {
        GatewayFallbackExceptionHandler handler = new GatewayFallbackExceptionHandler();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/courses/list").build()
        );

        handler.handle(exchange, new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No service instance")).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(exchange.getResponse().getBodyAsString().block())
                .isEqualTo(GatewayFallbackExceptionHandler.FALLBACK_BODY);
    }
}
