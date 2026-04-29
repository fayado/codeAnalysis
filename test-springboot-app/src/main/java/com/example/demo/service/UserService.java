package com.example.demo.service;

import com.example.demo.model.User;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 用户服务，提供用户管理的业务逻辑
 */
@Service
public class UserService {

    private final List<User> users = new ArrayList<>();

    public UserService() {
        // 初始化测试数据
        users.add(new User(1L, "张三", "zhangsan@example.com"));
        users.add(new User(2L, "李四", "lisi@example.com"));
        users.add(new User(3L, "王五", "wangwu@example.com"));
    }

    /**
     * 获取所有用户
     */
    public List<User> findAll() {
        return new ArrayList<>(users);
    }

    /**
     * 根据 ID 查找用户
     */
    public Optional<User> findById(Long id) {
        return users.stream().filter(u -> u.getId().equals(id)).findFirst();
    }

    /**
     * 创建新用户
     */
    public User create(User user) {
        long maxId = users.stream().mapToLong(User::getId).max().orElse(0L);
        user.setId(maxId + 1);
        users.add(user);
        return user;
    }
}
