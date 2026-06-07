package org.example.courseselectionsystem.common;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * API统一响应结果包装类
 */
@Data
@NoArgsConstructor
public class Result<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int PARAM_ERROR = 400;
    public static final int NOT_FOUND = 404;
    public static final int FAIL = 500;
    public static final int NOT_LOGIN = 401;

    /**
     * 响应状态码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 请求是否成功
     */
    private Boolean success;

    /**
     * 构造函数
     */
    public Result(Integer code, String message, T data, Boolean success) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.success = success;
    }
    /**
     * 成功响应
     */
    public static <T> Result<T> success(T data) {
        return new Result<T>(200, "操作成功", data, true);
    }

    /**
     * 成功响应（无数据）
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<T>(200, message, data, true);
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 失败响应
     */
    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<T>(code, message, null, false);
    }

    /**
     * 失败响应，默认400错误码
     */
    public static <T> Result<T> fail(String message) {
        return fail(400, message);
    }

    /**
     * 未授权响应
     */
    public static <T> Result<T> error(String message) {
        return fail(500, message);
    }

    public static <T> Result<T> error(Integer code, String message) {
        return fail(code, message);
    }

    public static <T> Result<T> unauthorized(String message) {
        return new Result<T>(401, message, null, false);
    }

    /**
     * 无权限响应
     */
    public static <T> Result<T> forbidden(String message) {
        return new Result<T>(403, message, null, false);
    }

    /**
     * 资源不存在响应
     */
    public static <T> Result<T> notFound(String message) {
        return new Result<T>(404, message, null, false);
    }

    /**
     * 服务器内部错误响应
     */
    public static <T> Result<T> serverError(String message) {
        return new Result<T>(500, message, null, false);
    }
}
