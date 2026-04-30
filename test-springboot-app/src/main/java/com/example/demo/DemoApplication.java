package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 应用入口类。
 *
 * 使用 @SpringBootApplication 注解启用自动配置、组件扫描和配置类功能。
 * 该应用是一个简单的用户管理 REST API 服务，用于测试类使用情况分析工具。
 *
 * 启动方式：mvn spring-boot:run 或 java -jar demo.jar
 */
@SpringBootApplication
public class DemoApplication {

    /**
     * 应用主入口方法。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
