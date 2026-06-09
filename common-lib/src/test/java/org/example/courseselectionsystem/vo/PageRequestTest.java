package org.example.courseselectionsystem.vo;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PageRequestTest {

    @Test
    void exposesDefaultPageAndSortValues() {
        PageRequest request = new PageRequest();

        assertThat(request.getPageNum()).isEqualTo(1);
        assertThat(request.getPageSize()).isEqualTo(10);
        assertThat(request.getSortOrder()).isEqualTo("asc");
        assertThat(request.getSortField()).isNull();
    }

    @Test
    void parsesStringSortDirectionForMvcBindingCompatibility() {
        PageRequest request = new PageRequest();

        request.setIsAsc("desc");

        assertThat(request.getSortOrder()).isEqualTo("desc");
    }

    @Test
    void acceptsBooleanSortDirectionForJsonBindingCompatibility() {
        PageRequest request = new PageRequest();

        request.setIsAsc(false);

        assertThat(request.getSortOrder()).isEqualTo("desc");
    }

    @Test
    void keepsSearchFieldsAndExtraParams() {
        PageRequest request = new PageRequest();
        request.setSearchField("name");
        request.setSearchValue("Alice");
        request.setParams(Map.of("status", 1));

        assertThat(request.getSearchField()).isEqualTo("name");
        assertThat(request.getSearchValue()).isEqualTo("Alice");
        assertThat(request.getParams()).containsEntry("status", 1);
    }
}
