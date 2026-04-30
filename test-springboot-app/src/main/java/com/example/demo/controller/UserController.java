package com.example.demo.controller;

import com.example.demo.model.ApiResponse;
import com.example.demo.model.User;
import com.example.demo.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理 REST 控制器，提供用户相关的 HTTP API 接口。
 *
 * 使用 @RestController 注解标记为 RESTful 控制器，
 * 所有方法的返回值自动序列化为 JSON 格式。
 *
 * API 端点：
 * - GET    /api/users      — 获取所有用户
 * - GET    /api/users/{id} — 根据 ID 获取用户
 * - POST   /api/users      — 创建新用户
 *
 * 通过构造函数注入 UserService，遵循 Spring 推荐的依赖注入方式。
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    /** 用户业务服务 */
    private final UserService userService;

    /**
     * 构造函数注入 UserService。
     *
     * @param userService 用户业务服务
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 获取所有用户列表。
     *
     * @return 包含用户列表的 API 响应
     */
    @GetMapping
    public ApiResponse<List<User>> list() {
        return ApiResponse.success(userService.findAll());
    }

    /**
     * 根据用户 ID 获取用户信息。
     *
     * @param id 用户 ID（从 URL 路径中提取）
     * @return 包含用户信息的 API 响应，用户不存在时返回 404 错误
     */
    @GetMapping("/{id}")
    public ApiResponse<User> get(@PathVariable Long id) {
        return userService.findById(id)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "用户不存在"));
    }

    /**
     * 创建新用户。
     *
     * @param user 请求体中的用户信息（JSON 格式）
     * @return 包含创建后用户信息的 API 响应
     */
    @PostMapping
    public ApiResponse<User> create(@RequestBody User user) {
        return ApiResponse.success(userService.create(user));
    }
}
