package org.example.courseselectionsystem.vo;

import java.io.Serializable;

/**
 * 统一响应结果类
 * 用于封装API接口的返回数据格式
 * @param <T> 数据泛型
 */
public class Result<T> implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 状态码：200表示成功，其他表示失败
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
     * 构造函数
     */
    public Result() {
        super();
    }
    
    /**
     * 构造函数
     * @param code 状态码
     * @param message 响应消息
     * @param data 响应数据
     */
    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }
    
    /**
     * 成功响应
     * @param data 响应数据
     * @param <T> 数据泛型
     * @return Result对象
     */
    public static <T> Result<T> success(T data) {
        return new Result<T>(200, "success", data);
    }
    
    /**
     * 成功响应
     * @param message 响应消息
     * @param data 响应数据
     * @param <T> 数据泛型
     * @return Result对象
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<T>(200, message, data);
    }
    
    /**
     * 失败响应
     * @param code 状态码
     * @param message 响应消息
     * @param <T> 数据泛型
     * @return Result对象
     */
    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<T>(code, message, null);
    }
    
    /**
     * 失败响应
     * @param message 响应消息
     * @param <T> 数据泛型
     * @return Result对象
     */
    public static <T> Result<T> fail(String message) {
        return new Result<T>(500, message, null);
    }
    
    /**
     * 获取状态码
     * @return 状态码
     */
    public Integer getCode() {
        return code;
    }
    
    /**
     * 设置状态码
     * @param code 状态码
     */
    public void setCode(Integer code) {
        this.code = code;
    }
    
    /**
     * 获取响应消息
     * @return 响应消息
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * 设置响应消息
     * @param message 响应消息
     */
    public void setMessage(String message) {
        this.message = message;
    }
    
    /**
     * 获取响应数据
     * @return 响应数据
     */
    public T getData() {
        return data;
    }
    
    /**
     * 设置响应数据
     * @param data 响应数据
     */
    public void setData(T data) {
        this.data = data;
    }
    
    @Override
    public String toString() {
        return "Result{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", data=" + data +
                '}';
    }
}
