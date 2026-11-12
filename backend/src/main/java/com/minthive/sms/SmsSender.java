package com.minthive.sms;

/**
 * 短信发送通道
 *
 * <p>功能描述：抽象验证码短信的下发实现，屏蔽具体服务商差异</p>
 *
 * <p>注意事项：实现方在发送失败时应抛出业务异常，调用方不会保存验证码</p>
 */
public interface SmsSender {

    /**
     * 发送验证码短信
     *
     * @param phone 手机号
     * @param code  验证码
     * @param scene 场景（login / register / reset）
     */
    void send(String phone, String code, String scene);
}
