package org.example.courseselectionsystem.gateway.filter;

import org.example.courseselectionsystem.gateway.handler.GatewayFallbackExceptionHandler;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultRequest;
import org.springframework.cloud.client.loadbalancer.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.RequestData;
import org.springframework.cloud.client.loadbalancer.RequestDataContext;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

@Component
public class GatewayFallbackResponseFilter implements GlobalFilter, Ordered {

    private static final int BEFORE_REACTIVE_LOAD_BALANCER = 10149;
    private static final String DEFAULT_HINT = "default";

    private final LoadBalancerClientFactory clientFactory;

    public GatewayFallbackResponseFilter(LoadBalancerClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpResponse originalResponse = exchange.getResponse();
        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> setComplete() {
                if (!isCommitted() && HttpStatus.SERVICE_UNAVAILABLE.equals(getStatusCode())) {
                    return writeFallback(this);
                }
                return super.setComplete();
            }
        };
        ServerWebExchange decoratedExchange = exchange.mutate().response(decoratedResponse).build();
        return writeFallbackWhenNoServiceInstance(decoratedExchange)
                .flatMap(wroteFallback -> {
                    if (wroteFallback) {
                        return Mono.empty();
                    }
                    return chain.filter(decoratedExchange)
                            .onErrorResume(this::isServiceUnavailable, ex -> writeFallback(originalResponse))
                            .then(Mono.defer(() -> {
                                if (!decoratedResponse.isCommitted()
                                        && HttpStatus.SERVICE_UNAVAILABLE.equals(decoratedResponse.getStatusCode())) {
                                    return writeFallback(decoratedResponse);
                                }
                                return Mono.empty();
                            }));
                });
    }

    @Override
    public int getOrder() {
        return BEFORE_REACTIVE_LOAD_BALANCER;
    }

    private Mono<Void> writeFallback(ServerHttpResponse response) {
        if (response.isCommitted()) {
            return Mono.empty();
        }
        response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes = GatewayFallbackExceptionHandler.FALLBACK_BODY.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    private boolean isServiceUnavailable(Throwable ex) {
        return ex instanceof ResponseStatusException
                && HttpStatus.SERVICE_UNAVAILABLE.equals(((ResponseStatusException) ex).getStatus());
    }

    private Mono<Boolean> writeFallbackWhenNoServiceInstance(ServerWebExchange exchange) {
        URI requestUrl = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
        if (requestUrl == null || !"lb".equalsIgnoreCase(requestUrl.getScheme())) {
            return Mono.just(false);
        }

        String serviceId = requestUrl.getHost();
        ReactorServiceInstanceLoadBalancer loadBalancer =
                clientFactory.getInstance(serviceId, ReactorServiceInstanceLoadBalancer.class);
        if (loadBalancer == null) {
            return writeFallback(exchange.getResponse()).thenReturn(true);
        }

        DefaultRequest<RequestDataContext> request = new DefaultRequest<>(
                new RequestDataContext(new RequestData(exchange.getRequest()), getHint(serviceId))
        );
        return loadBalancer.choose(request)
                .defaultIfEmpty(new EmptyResponse())
                .flatMap(response -> hasServiceInstance(response)
                        ? Mono.just(false)
                        : writeFallback(exchange.getResponse()).thenReturn(true))
                .onErrorResume(this::isServiceUnavailable, ex -> writeFallback(exchange.getResponse()).thenReturn(true));
    }

    private String getHint(String serviceId) {
        LoadBalancerProperties properties = clientFactory.getProperties(serviceId);
        if (properties == null) {
            return DEFAULT_HINT;
        }
        Map<String, String> hints = properties.getHint();
        String defaultHint = hints.getOrDefault(DEFAULT_HINT, DEFAULT_HINT);
        return hints.getOrDefault(serviceId, defaultHint);
    }

    private boolean hasServiceInstance(Response<ServiceInstance> response) {
        return response != null && response.hasServer();
    }
}
