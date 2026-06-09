package org.example.courseselectionsystem.exception;

import org.example.courseselectionsystem.common.Result;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest();

    @Test
    void handleBusinessExceptionPreservesBusinessCode() {
        BusinessException exception = new BusinessException(Result.NOT_FOUND, "用户不存在");

        Result<?> result = handler.handleBusinessException(exception, request);

        assertThat(result.getCode()).isEqualTo(Result.NOT_FOUND);
        assertThat(result.getMessage()).isEqualTo("用户不存在");
        assertThat(result.getSuccess()).isFalse();
    }

    @Test
    void handleAccessDeniedExceptionReturnsForbiddenResult() {
        request.setRequestURI("/admin/index");

        Result<?> result = handler.handleAccessDeniedException(new AccessDeniedException("denied"), request);

        assertThat(result.getCode()).isEqualTo(403);
        assertThat(result.getMessage()).isEqualTo("没有权限访问该资源");
        assertThat(result.getSuccess()).isFalse();
    }
}
