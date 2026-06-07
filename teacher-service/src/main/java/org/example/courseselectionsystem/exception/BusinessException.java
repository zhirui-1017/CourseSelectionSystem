package org.example.courseselectionsystem.exception;

import lombok.Getter;

/**
 * 业务异常类，用于处理业务逻辑中的异常情况
 */
@Getter
public class BusinessException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private Integer code;

    /**
     * 构造方法
     * @param message 错误消息
     */
    public BusinessException(String message) {
        super(message);
        this.code = 400; // 默认错误码
    }

    /**
     * 构造方法
     * @param code 错误码
     * @param message 错误消息
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 构造方法
     * @param message 错误消息
     * @param cause 异常原因
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = 400;
    }

    /**
     * 构造方法
     * @param code 错误码
     * @param message 错误消息
     * @param cause 异常原因
     */
    public BusinessException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}