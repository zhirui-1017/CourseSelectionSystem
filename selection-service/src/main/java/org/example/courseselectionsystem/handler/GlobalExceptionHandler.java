package org.example.courseselectionsystem.handler;

import org.example.courseselectionsystem.common.Result;
import org.example.courseselectionsystem.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理系统中的异常情况
 */
@ControllerAdvice
@Component("legacyGlobalExceptionHandler")
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务异常
     * @param request HTTP请求
     * @param ex 业务异常
     * @return 错误响应结果
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Result<?> handleBusinessException(HttpServletRequest request, BusinessException ex) {
        logger.warn("业务异常处理：请求路径={}, 错误信息={}", request.getRequestURI(), ex.getMessage());
        return Result.fail(400, ex.getMessage());
    }

    /**
     * 处理参数验证异常
     * @param request HTTP请求
     * @param ex 参数验证异常
     * @return 错误响应结果
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public Result<?> handleMethodArgumentNotValidException(HttpServletRequest request, MethodArgumentNotValidException ex) {
        BindingResult bindingResult = ex.getBindingResult();
        String errorMessage = bindingResult.getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        
        logger.warn("参数验证异常处理：请求路径={}, 错误信息={}", request.getRequestURI(), errorMessage);
        return Result.fail(400, errorMessage);
    }

    /**
     * 处理其他异常
     * @param request HTTP请求
     * @param ex 异常
     * @return 错误响应结果
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public Result<?> handleOtherException(HttpServletRequest request, Exception ex) {
        logger.error("系统异常处理：请求路径={}, 错误信息={}", request.getRequestURI(), ex.getMessage(), ex);
        return Result.fail(500, "系统内部错误，请联系管理员");
    }
}
