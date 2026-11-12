package com.minthive.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 本地日志短信通道
 *
 * <p>功能描述：未配置真实短信服务商时使用，仅记录手机号掩码；
 * 是否输出明文验证码由 sms.log-code 控制，供本地开发调试</p>
 *
 * <p>注意事项：生产环境必须配置 sms.provider=http，否则用户收不到验证码</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "sms.provider", havingValue = "log", matchIfMissing = true)
public class LogSmsSender implements SmsSender {

    /** 是否在日志中输出明文验证码 */
    @Value("${sms.log-code:false}")
    private boolean logCode;

    /**
     * 记录验证码发送日志
     *
     * @param phone 手机号
     * @param code  验证码
     * @param scene 场景
     */
    @Override
    public void send(String phone, String code, String scene) {
        if (logCode) {
            log.info("【短信验证码-本地】手机号={} 验证码={} 场景={}（5分钟有效）", mask(phone), code, scene);
        } else {
            log.warn("【短信验证码-本地】手机号={} 场景={}：未配置短信服务商，验证码未下发", mask(phone), scene);
        }
    }

    /**
     * 手机号掩码
     *
     * @param phone 手机号
     * @return 形如 138****0000 的掩码
     */
    private String mask(String phone) {
        if (phone == null || phone.length() < 7) return "***";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
