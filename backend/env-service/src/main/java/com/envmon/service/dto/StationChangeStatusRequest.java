package com.envmon.service.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

/** 站房启停状态变更入参（替代裸 Map，字段约束前置到入口层） */
@Data
public class StationChangeStatusRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** NORMAL / STOPPED */
    @NotBlank(message = "目标状态不能为空")
    @Pattern(regexp = "NORMAL|STOPPED", message = "目标状态只能是 NORMAL 或 STOPPED")
    private String bizStatus;
    /** 停运原因：监管审计要点，STOPPED 时必填（Service 层校验非空语义） */
    @Size(max = 255, message = "停运原因最长 255 字")
    private String stopReason;
}
