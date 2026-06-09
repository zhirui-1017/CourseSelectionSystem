package org.example.courseselectionsystem.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessExceptionTest {

    @Test
    void messageConstructorUsesDefaultParamErrorCode() {
        BusinessException exception = new BusinessException("参数错误");

        assertThat(exception.getCode()).isEqualTo(400);
        assertThat(exception.getMessage()).isEqualTo("参数错误");
    }

    @Test
    void codeConstructorPreservesBusinessCode() {
        BusinessException exception = new BusinessException(404, "资源不存在");

        assertThat(exception.getCode()).isEqualTo(404);
        assertThat(exception.getMessage()).isEqualTo("资源不存在");
    }

    @Test
    void causeConstructorPreservesCause() {
        IllegalStateException cause = new IllegalStateException("root");
        BusinessException exception = new BusinessException(500, "业务失败", cause);

        assertThat(exception.getCode()).isEqualTo(500);
        assertThat(exception.getCause()).isSameAs(cause);
    }
}
