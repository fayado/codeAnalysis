package com.example.demo.model;

/**
 * 用户实体模型，表示系统中的用户信息。
 *
 * 包含用户的基本属性：ID、姓名和邮箱。
 * 作为数据传输对象（DTO）在 Controller 和 Service 层之间传递。
 */
public class User {

    /** 用户唯一标识 */
    private Long id;

    /** 用户姓名 */
    private String name;

    /** 用户邮箱地址 */
    private String email;

    /** 无参构造函数，用于 JSON 反序列化 */
    public User() {}

    /**
     * 全参构造函数。
     *
     * @param id    用户 ID
     * @param name  用户姓名
     * @param email 用户邮箱
     */
    public User(Long id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    /** 获取用户 ID */
    public Long getId() { return id; }

    /** 设置用户 ID */
    public void setId(Long id) { this.id = id; }

    /** 获取用户姓名 */
    public String getName() { return name; }

    /** 设置用户姓名 */
    public void setName(String name) { this.name = name; }

    /** 获取用户邮箱 */
    public String getEmail() { return email; }

    /** 设置用户邮箱 */
    public void setEmail(String email) { this.email = email; }
}
