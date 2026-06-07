package org.example.courseselectionsystem.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Result<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int PARAM_ERROR = 400;
    public static final int NOT_FOUND = 404;
    public static final int FAIL = 500;
    public static final int NOT_LOGIN = 401;

    private Integer code;
    private String message;
    private T data;
    private Boolean success;

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data, true);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data, true);
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null, false);
    }

    public static <T> Result<T> fail(String message) {
        return fail(400, message);
    }

    public static <T> Result<T> error(String message) {
        return fail(500, message);
    }

    public static <T> Result<T> error(Integer code, String message) {
        return fail(code, message);
    }

    public static <T> Result<T> unauthorized(String message) {
        return fail(401, message);
    }

    public static <T> Result<T> forbidden(String message) {
        return fail(403, message);
    }

    public static <T> Result<T> notFound(String message) {
        return fail(404, message);
    }

    public static <T> Result<T> serverError(String message) {
        return fail(500, message);
    }
}
