package org.example.courseselectionsystem.gateway.filter;

import org.example.courseselectionsystem.gateway.handler.GatewayFallbackExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.loadbalancer.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GatewayFallbackResponseFilterTest {

    @Test
    void writesFallbackBodyForUncommittedServiceUnavailableResponse() {
        GatewayFallbackResponseFilter filter = new GatewayFallbackResponseFilter(mock(LoadBalancerClientFactory.class));
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/courses/list").build()
        );
        GatewayFilterChain chain = webExchange -> {
            webExchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
            return webExchange.getResponse().setComplete();
        };

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(exchange.getResponse().getBodyAsString().block())
                .isEqualTo(GatewayFallbackExceptionHandler.FALLBACK_BODY);
    }

    @Test
    void writesFallbackBodyWhenLoadBalancerRaisesServiceUnavailable() {
        GatewayFallbackResponseFilter filter = new GatewayFallbackResponseFilter(mock(LoadBalancerClientFactory.class));
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/courses/list").build()
        );
        GatewayFilterChain chain = webExchange -> Mono.error(
                new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No service instance")
        );

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(exchange.getResponse().getBodyAsString().block())
                .isEqualTo(GatewayFallbackExceptionHandler.FALLBACK_BODY);
    }

    @Test
    void writesFallbackBodyBeforeGatewayLoadBalancerWhenNoServiceInstanceExists() {
        LoadBalancerClientFactory clientFactory = mock(LoadBalancerClientFactory.class);
        ReactorServiceInstanceLoadBalancer loadBalancer = mock(ReactorServiceInstanceLoadBalancer.class);
        when(clientFactory.getInstance(eq("course-service"), eq(ReactorServiceInstanceLoadBalancer.class)))
                .thenReturn(loadBalancer);
        when(loadBalancer.choose(org.mockito.ArgumentMatchers.<Request<?>>any()))
                .thenReturn(Mono.just(new EmptyResponse()));

        GatewayFallbackResponseFilter filter = new GatewayFallbackResponseFilter(clientFactory);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/courses/list").build()
        );
        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, URI.create("lb://course-service"));
        GatewayFilterChain chain = webExchange -> {
            throw new AssertionError("Gateway chain should not continue when no service instance exists");
        };

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(exchange.getResponse().getBodyAsString().block())
                .isEqualTo(GatewayFallbackExceptionHandler.FALLBACK_BODY);
    }

}
