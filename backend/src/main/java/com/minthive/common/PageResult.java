package com.minthive.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 分页结果封装
 *
 * @param <T> 数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    /** 总记录数 */
    private Long total;

    /** 当前页码 */
    private Long current;

    /** 每页大小 */
    private Long size;

    /** 总页数 */
    private Long pages;

    /** 当前页数据列表 */
    private List<T> records;

    /**
     * 构建分页结果
     *
     * @param total    总记录数
     * @param current  当前页
     * @param size     每页大小
     * @param records  数据列表
     * @param <T>      数据类型
     * @return PageResult 实例
     */
    public static <T> PageResult<T> of(Long total, Long current, Long size, List<T> records) {
        PageResult<T> result = new PageResult<>();
        result.setTotal(total);
        result.setCurrent(current);
        result.setSize(size);
        result.setRecords(records);
        result.setPages(size == 0 ? 0 : (total + size - 1) / size);
        return result;
    }
}
