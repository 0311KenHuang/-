package com.envmon.api;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 常驻 Web 进程：接口 + 时序聚合定时任务（env-ts 的 TsAggregateJob） */
@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.envmon")
@MapperScan("com.envmon.dao.mapper")
public class EnvApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnvApiApplication.class, args);
    }
}
