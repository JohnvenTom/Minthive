package com.minthive.service;

/**
 * 短信验证码服务接口
 *
 * <p>功能描述：定义验证码生成、发送、校验等业务方法</p>
 * <p>注意事项：当前为开发模式，验证码打印到控制台而非真实发送短信</p>
 */
public interface SmsService {

    /**
     * 发送短信验证码
     *
     * <p>生成6位随机数字验证码，存储到Redis（5分钟有效期），并打印到后端控制台</p>
     *
     * @param phone 目标手机号
     * @param scene 发送场景（login-登录 / register-注册 / reset-重置密码）
     * @throws com.minthive.common.BusinessException 发送频率超限时抛出
     */
    void sendCode(String phone, String scene);

    /**
     * 校验短信验证码
     *
     * <p>从Redis中取出验证码进行比对，校验通过后立即删除（一次性使用）</p>
     *
     * @param phone   手机号
     * @param inputCode 用户输入的验证码
     * @return true 校验通过，false 校验失败
     */
    boolean verifyCode(String phone, String inputCode);
}
