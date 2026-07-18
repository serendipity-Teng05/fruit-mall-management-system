package com.example.fruitmallmanagementsystem;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.example.fruitmallmanagementsystem.mapper") // 👈 关键：扫描刚才创建的接口
public class FruitMallManagementSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(FruitMallManagementSystemApplication.class, args);
    }
}
