package org.example.courseselectionsystem.gateway.controller;

import org.example.courseselectionsystem.common.Result;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class FallbackControllerTest {

    @Test
    void fallbackReturnsHttp503WithStandardBody() {
        FallbackController controller = new FallbackController();

        ResponseEntity<Result<Void>> response = controller.fallback();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(503);
        assertThat(response.getBody().getSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("服务暂时不可用，请稍后重试");
    }
}
