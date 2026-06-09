package org.example.courseselectionsystem.vo;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResultTest {

    @Test
    void calculatesPagesAndNavigationState() {
        PageResult<String> result = new PageResult<>(2, 10, 25, List.of("A", "B"));

        assertThat(result.getItems()).containsExactly("A", "B");
        assertThat(result.getTotal()).isEqualTo(25);
        assertThat(result.getPages()).isEqualTo(3);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.hasPrevious()).isTrue();
    }

    @Test
    void handlesZeroPageSize() {
        PageResult<String> result = new PageResult<>(1, 0, 25, List.of());

        assertThat(result.getPages()).isZero();
        assertThat(result.hasNext()).isFalse();
    }
}
