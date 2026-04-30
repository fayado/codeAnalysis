package com.example.demo.service;

import com.example.demo.model.User;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 用户业务服务类，提供用户管理的核心业务逻辑。
 *
 * 使用 @Service 注解标记为 Spring 业务层组件，由 Spring 容器自动管理。
 * 当前使用内存列表模拟数据存储，实际项目中应替换为数据库访问层（Repository）。
 *
 * 提供的功能：
 * - 查询所有用户
 * - 根据 ID 查询用户
 * - 创建新用户（自动分配 ID）
 */
@Service
public class UserService {

    /** 内存中的用户数据列表，模拟数据库存储 */
    private final List<User> users = new ArrayList<>();

    /**
     * 构造函数，初始化测试数据。
     * 添加三个示例用户用于开发和测试。
     */
    public UserService() {
        users.add(new User(1L, "张三", "zhangsan@example.com"));
        users.add(new User(2L, "李四", "lisi@example.com"));
        users.add(new User(3L, "王五", "wangwu@example.com"));
    }

    /**
     * 获取所有用户列表。
     * 返回列表的副本，避免外部修改影响内部数据。
     *
     * @return 用户列表的副本
     */
    public List<User> findAll() {
        return new ArrayList<>(users);
    }

    /**
     * 根据用户 ID 查找用户。
     *
     * @param id 用户 ID
     * @return 包含用户的 Optional，如果未找到则为空
     */
    public Optional<User> findById(Long id) {
        return users.stream().filter(u -> u.getId().equals(id)).findFirst();
    }

    /**
     * 创建新用户。
     * 自动分配比当前最大 ID 大 1 的新 ID。
     *
     * @param user 用户信息（ID 会被自动覆盖）
     * @return 创建后的用户信息（包含分配的 ID）
     */
    public User create(User user) {
        long maxId = users.stream().mapToLong(User::getId).max().orElse(0L);
        user.setId(maxId + 1);
        users.add(user);
        return user;
    }
}
