package com.boms;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan({"com.boms.modules.**.mapper", "com.boms.common.audit"})
public class BomsApplication {
    public static void main(String[] args) {
        SpringApplication.run(BomsApplication.class, args);
    }
}
