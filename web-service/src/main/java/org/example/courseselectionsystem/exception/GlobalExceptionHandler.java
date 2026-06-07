package org.example.courseselectionsystem.exception;

import org.example.courseselectionsystem.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;

/**
 * 全局异常处理器
 */
@ControllerAdvice
@ResponseBody
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务异常
     */
    @ExceptionHandler(value = BusinessException.class)
    public Result handleBusinessException(BusinessException e, HttpServletRequest request) {
        logger.warn("业务异常：{}", e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理请求参数绑定异常
     */
    @ExceptionHandler(value = BindException.class)
    public Result handleBindException(BindException e, HttpServletRequest request) {
        StringBuilder errorMsg = new StringBuilder();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            errorMsg.append(error.getField()).append(":").append(error.getDefaultMessage()).append("; ");
        }
        logger.warn("请求参数绑定异常：{}", errorMsg.toString());
        return Result.fail(400, errorMsg.toString());
    }

    /**
     * 处理方法参数验证异常
     */
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public Result handleMethodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletRequest request) {
        StringBuilder errorMsg = new StringBuilder();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            errorMsg.append(error.getField()).append(":").append(error.getDefaultMessage()).append("; ");
        }
        logger.warn("方法参数验证异常：{}", errorMsg.toString());
        return Result.fail(400, errorMsg.toString());
    }

    /**
     * 处理404异常
     */
    @ExceptionHandler(value = NoHandlerFoundException.class)
    public Result handleNoHandlerFoundException(NoHandlerFoundException e, HttpServletRequest request) {
        logger.warn("请求路径不存在：{}", request.getRequestURI());
        return Result.notFound("请求的资源不存在");
    }

    /**
     * 处理未授权异常
     */
    @ExceptionHandler(value = org.springframework.security.access.AccessDeniedException.class)
    public Result handleAccessDeniedException(org.springframework.security.access.AccessDeniedException e, HttpServletRequest request) {
        logger.warn("访问被拒绝：{}", request.getRequestURI());
        return Result.forbidden("没有权限访问该资源");
    }

    /**
     * 处理其他未知异常
     */
    @ExceptionHandler(value = Exception.class)
    public Result handleException(Exception e, HttpServletRequest request) {
        logger.error("系统异常：{}", e.getMessage(), e);
        // 生产环境中不应该直接返回异常堆栈信息给前端
        return Result.serverError("系统内部错误，请稍后重试");
    }
}