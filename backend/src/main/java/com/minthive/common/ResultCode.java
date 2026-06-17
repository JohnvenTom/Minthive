package com.minthive.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 系统状态码枚举
 *
 * <p>功能描述：统一管理所有业务状态码及提示信息</p>
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    /** 成功 */
    SUCCESS(200, "请求成功"),

    /** 参数错误 */
    PARAM_ERROR(400, "参数错误"),

    /** 未登录 */
    UNAUTHORIZED(401, "未登录或登录已过期"),

    /** 无权限 */
    FORBIDDEN(403, "无权限访问"),

    /** AI 接口限流 */
    AI_RATE_LIMITED(403, "AI调用次数已达每日上限"),

    /** 资源不存在 */
    NOT_FOUND(404, "资源不存在"),

    /** 方法不支持 */
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),

    /** 服务器错误 */
    SERVER_ERROR(500, "服务器内部错误"),

    /** 业务异常 */
    BUSINESS_ERROR(1000, "业务处理失败"),

    /** 用户已存在 */
    USER_EXISTS(1001, "用户已存在"),

    /** 用户不存在 */
    USER_NOT_EXISTS(1002, "用户不存在"),

    /** 密码错误 */
    PASSWORD_ERROR(1003, "账号或密码错误"),

    /** 账号被封禁 */
    USER_BANNED(1004, "账号已被封禁"),

    /** Token 无效 */
    TOKEN_INVALID(1005, "Token无效"),

    /** Token 过期 */
    TOKEN_EXPIRED(1006, "Token已过期"),

    /** 内容违规 */
    CONTENT_VIOLATION(1007, "内容包含违规信息"),

    /** 帖子不存在 */
    POST_NOT_EXISTS(1008, "帖子不存在"),

    /** 圈子不存在 */
    CIRCLE_NOT_EXISTS(1009, "圈子不存在"),

    /** 圈子已满 */
    CIRCLE_FULL(1010, "圈子成员已满"),

    /** 文件上传失败 */
    FILE_UPLOAD_ERROR(1011, "文件上传失败"),

    /** 文件过大 */
    FILE_TOO_LARGE(1012, "文件大小超过限制"),

    /** AI 服务异常 */
    AI_SERVICE_ERROR(1013, "AI服务暂时不可用"),

    /** 敏感词命中 */
    SENSITIVE_WORD(1014, "内容包含敏感词"),

    /** 重复操作 */
    DUPLICATE_OPERATION(1015, "请勿重复操作"),

    /** 频率限制 */
    RATE_LIMITED(1016, "操作过于频繁，请稍后再试");

    /** 状态码 */
    private final Integer code;

    /** 提示信息 */
    private final String msg;
}
