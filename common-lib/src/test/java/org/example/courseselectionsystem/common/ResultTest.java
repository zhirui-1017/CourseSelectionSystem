package org.example.courseselectionsystem.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResultTest {

    @Test
    void successCreatesStandardSuccessfulBody() {
        Result<String> result = Result.success("payload");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getMessage()).isEqualTo("操作成功");
        assertThat(result.getData()).isEqualTo("payload");
        assertThat(result.getSuccess()).isTrue();
    }

    @Test
    void failCreatesStandardFailedBody() {
        Result<Void> result = Result.fail(503, "服务暂时不可用，请稍后重试");

        assertThat(result.getCode()).isEqualTo(503);
        assertThat(result.getMessage()).isEqualTo("服务暂时不可用，请稍后重试");
        assertThat(result.getData()).isNull();
        assertThat(result.getSuccess()).isFalse();
    }
}
