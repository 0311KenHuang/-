package com.envmon.service.ops;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/** 单据号生成：计划/任务/记录共用一个序列，单机唯一 */
@Component
public class OpsNoGenerator {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final AtomicInteger seq = new AtomicInteger();

    public String next(String prefix) {
        int n = Math.abs(seq.incrementAndGet() % 10000);
        return prefix + LocalDateTime.now().format(STAMP) + String.format("%04d", n);
    }
}
