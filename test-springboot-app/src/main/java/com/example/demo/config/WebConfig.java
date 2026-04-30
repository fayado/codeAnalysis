package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置类，配置 CORS（跨域资源共享）策略。
 *
 * 使用 @Configuration 注解标记为配置类，实现 WebMvcConfigurer 接口
 * 以自定义 Spring MVC 的行为。
 *
 * CORS 配置：
 * - 允许的路径：/api/** 下的所有接口
 * - 允许的来源：所有来源（*）
 * - 允许的 HTTP 方法：GET、POST、PUT、DELETE
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 配置 CORS 跨域映射。
     *
     * @param registry CORS 注册器
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE");
    }
}
