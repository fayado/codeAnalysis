package com.example.demo.model;

/**
 * 通用 API 响应包装类，用于统一 REST API 的响应格式。
 *
 * 采用泛型设计，可以包装任意类型的数据。响应结构：
 * - code：状态码（200 表示成功，其他表示错误）
 * - message：响应消息
 * - data：响应数据（成功时有值，错误时为 null）
 *
 * 使用示例：
 * <pre>
 * // 成功响应
 * ApiResponse.success(userList)
 * // 错误响应
 * ApiResponse.error(404, "用户不存在")
 * </pre>
 *
 * @param <T> 响应数据的类型
 */
public class ApiResponse<T> {

    /** 响应状态码，200 表示成功 */
    private int code;

    /** 响应消息 */
    private String message;

    /** 响应数据 */
    private T data;

    /** 无参构造函数，用于 JSON 反序列化 */
    public ApiResponse() {}

    /**
     * 全参构造函数。
     *
     * @param code    状态码
     * @param message 响应消息
     * @param data    响应数据
     */
    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 创建成功响应。
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 成功响应对象（code=200, message="success"）
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data);
    }

    /**
     * 创建错误响应。
     *
     * @param code    错误状态码
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 错误响应对象（data=null）
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    /** 获取状态码 */
    public int getCode() { return code; }

    /** 设置状态码 */
    public void setCode(int code) { this.code = code; }

    /** 获取响应消息 */
    public String getMessage() { return message; }

    /** 设置响应消息 */
    public void setMessage(String message) { this.message = message; }

    /** 获取响应数据 */
    public T getData() { return data; }

    /** 设置响应数据 */
    public void setData(T data) { this.data = data; }
}
