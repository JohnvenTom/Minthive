package com.minthive.common;

import lombok.Getter;

/**
 * 业务异常类
 *
 * <p>功能描述：业务逻辑异常，携带错误码与提示信息，由全局异常处理器统一处理</p>
 * <p>注意事项：业务中遇到非正常流程应抛出此异常，避免返回原始错误堆栈</p>
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 错误码 */
    private final Integer code;

    /**
     * 构造业务异常
     *
     * @param resultCode 错误码枚举
     */
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMsg());
        this.code = resultCode.getCode();
    }

    /**
     * 构造业务异常(自定义提示)
     *
     * @param resultCode 错误码枚举
     * @param message    自定义提示
     */
    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    /**
     * 构造业务异常(自定义错误码+提示)
     *
     * @param code    错误码
     * @param message 提示信息
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
