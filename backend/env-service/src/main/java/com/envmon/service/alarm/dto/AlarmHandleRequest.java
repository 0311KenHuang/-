package com.envmon.service.alarm.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

/** 报警处置入参 */
@Data
public class AlarmHandleRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** ACK / CLOSE / FALSE_ALARM */
    @NotBlank(message = "处置动作不能为空")
    @Pattern(regexp = "ACK|CLOSE|FALSE_ALARM", message = "处置动作只能是 ACK、CLOSE 或 FALSE_ALARM")
    private String action;
    @Size(max = 500, message = "备注最长 500 字")
    private String remark;
}
