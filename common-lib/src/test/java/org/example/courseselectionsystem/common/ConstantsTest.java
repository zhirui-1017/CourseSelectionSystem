package org.example.courseselectionsystem.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConstantsTest {

    @Test
    void exposesSharedStatusCodesAndMessages() {
        assertThat(Constants.SUCCESS_CODE).isEqualTo(200);
        assertThat(Constants.PARAM_ERROR_CODE).isEqualTo(400);
        assertThat(Constants.NOT_FOUND_CODE).isEqualTo(404);
        assertThat(Constants.DUPLICATE_CODE).isEqualTo(409);
        assertThat(Constants.SUCCESS_MESSAGE).isEqualTo("操作成功");
    }

    @Test
    void exposesSharedUserAndPaginationConstants() {
        assertThat(Constants.ADMIN_USERNAME).isEqualTo("admin");
        assertThat(Constants.ROLE_STUDENT).isEqualTo("ROLE_STUDENT");
        assertThat(Constants.DEFAULT_PAGE_NUM).isEqualTo(1);
        assertThat(Constants.DEFAULT_PAGE_SIZE).isEqualTo(10);
        assertThat(Constants.MAX_PAGE_SIZE).isEqualTo(100);
    }

    @Test
    void exposesSharedValidationPatterns() {
        assertThat("S1001").matches(Constants.STUDENT_NO_REGEX);
        assertThat("T001").matches(Constants.TEACHER_NO_REGEX);
        assertThat("abc123").matches(Constants.PASSWORD_REGEX);
    }
}
