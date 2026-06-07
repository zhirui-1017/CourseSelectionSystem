package org.example.courseselectionsystem.util;

import lombok.Data;

/**
 * 统一响应结果类
 * 封装API响应数据，包含状态码、消息和数据
 */
@Data
public class Result<T> {

    /**
     * 状态码：0-成功，非0-失败
     */
    private int code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 构造方法
     */
    public Result() {
    }

    /**
     * 构造方法
     */
    public Result(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 构造方法
     */
    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 成功结果
     */
    public static <T> Result<T> success() {
        return new Result(0, "操作成功");
    }

    /**
     * 成功结果带数据
     */
    public static <T> Result<T> success(T data) {
        return new Result(0, "操作成功", data);
    }

    /**
     * 成功结果带自定义消息
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result(0, message, data);
    }

    /**
     * 错误结果
     */
    public static <T> Result<T> error(int code, String message) {
        return new Result(code, message);
    }

    /**
     * 参数错误结果
     */
    public static <T> Result<T> paramError(String message) {
        return new Result(400, message);
    }

    /**
     * 未找到资源结果
     */
    public static <T> Result<T> notFound(String message) {
        return new Result(404, message);
    }

    /**
     * 服务器错误结果
     */
    public static <T> Result<T> serverError() {
        return new Result(500, "服务器内部错误");
    }
}
