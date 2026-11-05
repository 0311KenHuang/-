package com.envmon.common.api;

import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/** 统一分页结构 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<T> list;
    private long total;
    private long page;
    private long pageSize;

    public static <T> PageResult<T> of(List<T> list, long total, long page, long pageSize) {
        PageResult<T> r = new PageResult<T>();
        r.list = list == null ? Collections.<T>emptyList() : list;
        r.total = total;
        r.page = page;
        r.pageSize = pageSize;
        return r;
    }
}
