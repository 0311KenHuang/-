package com.envmon.common.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要写审计的操作。
 * 监管系统里"没留痕"等于"没发生"，所有写操作都应标注。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audit {

    /** 模块名，如 station / auth / ops */
    String module();

    /** 动作，如 CREATE / UPDATE / DELETE / EXPORT / LOGIN / CONTROL */
    String action();
}
