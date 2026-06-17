package com.minthive.common;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * <p>功能描述：统一捕获并处理 Controller 层抛出的异常，返回标准化 Result 响应</p>
 * <p>注意事项：禁止将后端堆栈信息直接返回前端</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     *
     * @param e 业务异常
     * @return 统一响应
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常(@Valid)
     *
     * @param e 参数校验异常
     * @return 统一响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", msg);
        return Result.error(ResultCode.PARAM_ERROR, msg);
    }

    /**
     * 处理绑定异常
     *
     * @param e 绑定异常
     * @return 统一响应
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String msg = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数绑定失败: {}", msg);
        return Result.error(ResultCode.PARAM_ERROR, msg);
    }

    /**
     * 处理约束违反异常
     *
     * @param e 约束违反异常
     * @return 统一响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(v -> v.getMessage())
                .collect(Collectors.joining("; "));
        log.warn("约束校验失败: {}", msg);
        return Result.error(ResultCode.PARAM_ERROR, msg);
    }

    /**
     * 处理缺少请求参数异常
     *
     * @param e 缺少参数异常
     * @return 统一响应
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("缺少请求参数: {}", e.getParameterName());
        return Result.error(ResultCode.PARAM_ERROR, "缺少必要参数: " + e.getParameterName());
    }

    /**
     * 处理文件上传超限异常
     *
     * @param e 文件超限异常
     * @return 统一响应
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        log.warn("文件大小超限: {}", e.getMessage());
        return Result.error(ResultCode.FILE_TOO_LARGE);
    }

    /**
     * 处理404异常（无Handler）
     *
     * @param e NoHandlerFoundException
     * @return 统一响应
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNotFound(NoHandlerFoundException e) {
        return Result.error(ResultCode.NOT_FOUND);
    }

    /**
     * 处理静态资源未找到异常
     *
     * <p>当请求路径未匹配任何Controller且静态资源也不存在时触发</p>
     *
     * @param e NoResourceFoundException
     * @return 统一响应
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleResourceNotFound(NoResourceFoundException e) {
        log.warn("静态资源未找到: {}", e.getResourcePath());
        return Result.error(ResultCode.NOT_FOUND);
    }

    /**
     * 处理非法参数异常
     *
     * @param e 非法参数异常
     * @return 统一响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArg(IllegalArgumentException e) {
        log.warn("非法参数: {}", e.getMessage());
        return Result.error(ResultCode.PARAM_ERROR, e.getMessage());
    }

    /**
     * 兜底处理其他未捕获异常
     *
     * @param e 未知异常
     * @return 统一响应
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常: ", e);
        return Result.error(ResultCode.SERVER_ERROR, "系统繁忙，请稍后再试");
    }
}
