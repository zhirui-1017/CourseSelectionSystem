package org.example.courseselectionsystem.vo;

import lombok.Data;

/**
 * 分页请求参数
 */
@Data
public class PageRequest {
    /**
     * 当前页码，默认第1页
     */
    private Integer pageNum = 1;

    /**
     * 每页显示数量，默认10条
     */
    private Integer pageSize = 10;

    /**
     * 排序字段
     */
    private String orderByColumn;

    /**
     * 排序方向：asc 或 desc
     */
    private Boolean isAsc = true;
    private java.util.Map<String, Object> params;

    public String getSearchField() {
        return orderByColumn;
    }

    public String getSearchValue() {
        return null;
    }

    public String getSortField() {
        return orderByColumn;
    }

    public String getSortOrder() {
        return Boolean.TRUE.equals(isAsc) ? "asc" : "desc";
    }

    public void setIsAsc(String isAsc) {
        this.isAsc = !"desc".equalsIgnoreCase(isAsc);
    }
}
