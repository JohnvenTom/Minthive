package com.minthive.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;

import java.io.Serializable;

/**
 * 统一响应结果封装
 *
 * @param <T> 业务数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> implements Serializable {

    /** 状态码 */
    private Integer code;

    /** 提示信息 */
    private String msg;

    /** 业务数据 */
    private T data;

    /** 链路追踪ID */
    private String traceId;

    /**
     * 成功响应(无数据)
     *
     * @param <T> 数据类型
     * @return Result 实例
     */
    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMsg(), null, currentTraceId());
    }

    /**
     * 成功响应(带数据)
     *
     * @param data 业务数据
     * @param <T>  数据类型
     * @return Result 实例
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMsg(), data, currentTraceId());
    }

    /**
     * 成功响应(自定义提示+数据)
     *
     * @param msg  提示信息
     * @param data 业务数据
     * @param <T>  数据类型
     * @return Result 实例
     */
    public static <T> Result<T> success(String msg, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), msg, data, currentTraceId());
    }

    /**
     * 失败响应(指定错误码)
     *
     * @param resultCode 错误码枚举
     * @param <T>        数据类型
     * @return Result 实例
     */
    public static <T> Result<T> error(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMsg(), null, currentTraceId());
    }

    /**
     * 失败响应(自定义错误码+提示)
     *
     * @param code 状态码
     * @param msg  提示信息
     * @param <T>  数据类型
     * @return Result 实例
     */
    public static <T> Result<T> error(Integer code, String msg) {
        return new Result<>(code, msg, null, currentTraceId());
    }

    /**
     * 失败响应(错误码+自定义提示)
     *
     * @param resultCode 错误码枚举
     * @param msg        自定义提示
     * @param <T>        数据类型
     * @return Result 实例
     */
    public static <T> Result<T> error(ResultCode resultCode, String msg) {
        return new Result<>(resultCode.getCode(), msg, null, currentTraceId());
    }

    /**
     * 失败响应(错误码+自定义提示+业务数据)
     *
     * @param resultCode 错误码枚举
     * @param msg        自定义提示
     * @param data       需要随失败一并返回的业务数据
     * @param <T>        数据类型
     * @return Result 实例
     */
    public static <T> Result<T> error(ResultCode resultCode, String msg, T data) {
        return new Result<>(resultCode.getCode(), msg, data, currentTraceId());
    }

    /**
     * 读取当前请求的链路追踪ID
     *
     * <p>功能描述：从 MDC 中取出 TraceIdFilter 写入的追踪标识，供响应体回传，
     * 便于把用户反馈、日志与具体请求关联起来</p>
     *
     * @return 当前请求的 traceId；非请求线程（如定时任务）下返回 null
     */
    private static String currentTraceId() {
        return MDC.get(TraceIdFilter.MDC_KEY);
    }
}
