package com.minthive.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMsg(), null, null);
    }

    /**
     * 成功响应(带数据)
     *
     * @param data 业务数据
     * @param <T>  数据类型
     * @return Result 实例
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMsg(), data, null);
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
        return new Result<>(ResultCode.SUCCESS.getCode(), msg, data, null);
    }

    /**
     * 失败响应(指定错误码)
     *
     * @param resultCode 错误码枚举
     * @param <T>        数据类型
     * @return Result 实例
     */
    public static <T> Result<T> error(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMsg(), null, null);
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
        return new Result<>(code, msg, null, null);
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
        return new Result<>(resultCode.getCode(), msg, null, null);
    }
}
