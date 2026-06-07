package org.example.courseselectionsystem.vo;

import java.io.Serializable;
import java.util.List;

/**
 * 分页结果接口
 * 用于统一分页数据结构
 * @param <T> 数据类型
 */
public class PageResult<T> implements Serializable {
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
    /**
     * 获取当前页的数据列表
     * @return 当前页的数据列表
     */
    public List<T> getItems() {
        return items;
    }

    /**
     * 获取总记录数
     * @return 总记录数
     */
    public long getTotal() {
        return total;
    }

    /**
     * 获取当前页码
     * @return 当前页码
     */
    public int getPageNum() {
        return pageNum;
    }

    /**
     * 获取每页大小
     * @return 每页大小
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * 获取总页数
     * @return 总页数
     */
    public int getPages() {
        if (getPageSize() == 0) {
            return 0;
        }
        return (int) Math.ceil((double) getTotal() / getPageSize());
    }

    /**
     * 判断是否有下一页
     * @return 是否有下一页
     */
    public boolean hasNext() {
        return getPageNum() < getPages();
    }

    /**
     * 判断是否有上一页
     * @return 是否有上一页
     */
    public boolean hasPrevious() {
        return getPageNum() > 1;
    }
}
