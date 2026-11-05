package com.envmon.ingest;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.envmon")
@MapperScan("com.envmon.dao.mapper")
public class EnvIngestApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnvIngestApplication.class, args);
    }
}
