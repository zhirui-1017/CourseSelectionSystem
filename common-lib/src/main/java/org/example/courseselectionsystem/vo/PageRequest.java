package org.example.courseselectionsystem.vo;

import lombok.Data;

import java.util.Map;

@Data
public class PageRequest {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String orderByColumn;
    private Boolean isAsc = true;
    private String searchField;
    private String searchValue;
    private Map<String, Object> params;

    public String getSortField() {
        return orderByColumn;
    }

    public String getSortOrder() {
        return Boolean.TRUE.equals(isAsc) ? "asc" : "desc";
    }

    public void setIsAsc(Boolean isAsc) {
        this.isAsc = isAsc;
    }

    public void setIsAsc(String isAsc) {
        this.isAsc = !"desc".equalsIgnoreCase(isAsc);
    }
}
