package com.envmon.dao.dto;

import lombok.Data;

import java.io.Serializable;

/** 通用条数统计：total / valid */
@Data
public class CountStat implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer totalCnt;
    private Integer validCnt;
}
