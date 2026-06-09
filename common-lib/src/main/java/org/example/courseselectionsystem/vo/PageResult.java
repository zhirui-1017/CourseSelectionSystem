package org.example.courseselectionsystem.vo;

import java.io.Serializable;
import java.util.List;

public class PageResult<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int pageNum;
    private final int pageSize;
    private final long total;
    private final List<T> items;

    public PageResult() {
        this(1, 10, 0, null);
    }

    public PageResult(int pageNum, int pageSize, long total, List<T> items) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.total = total;
        this.items = items;
    }

    public List<T> getItems() {
        return items;
    }

    public long getTotal() {
        return total;
    }

    public int getPageNum() {
        return pageNum;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getPages() {
        if (getPageSize() == 0) {
            return 0;
        }
        return (int) Math.ceil((double) getTotal() / getPageSize());
    }

    public boolean hasNext() {
        return getPageNum() < getPages();
    }

    public boolean hasPrevious() {
        return getPageNum() > 1;
    }
}
