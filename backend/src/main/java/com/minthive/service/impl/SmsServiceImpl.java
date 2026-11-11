package com.minthive.service.impl;

import com.minthive.common.BusinessException;
import com.minthive.common.ResultCode;
import com.minthive.service.SmsService;
import com.minthive.sms.SmsSender;
import com.minthive.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/**
 * 短信验证码服务实现
 *
 * <p>功能描述：生成6位随机验证码，通过可配置的短信通道下发，存储到Redis（5分钟有效）</p>
 * <p>注意事项：发送失败时不写入验证码，用户可立即重试；验证码明文只在本地开发日志开关打开时输出</p>
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SmsServiceImpl implements SmsService {

    private final RedisUtil redisUtil;

    private final SmsSender smsSender;

    /** 验证码长度 */
    private static final int CODE_LENGTH = 6;

    /** 安全随机数生成器 */
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 发送短信验证码
     *
     * <p>流程：</p>
     * <ol>
     *   <li>检查发送频率限制（60秒内不可重复发送同一手机号）</li>
     *   <li>生成6位随机数字验证码</li>
     *   <li>通过配置的短信通道下发</li>
     *   <li>下发成功后写入 Redis（5分钟过期）并记录冷却时间</li>
     * </ol>
     *
     * @param phone 目标手机号
     * @param scene 发送场景（login / register / reset）
     * @throws BusinessException 60秒内重复发送或短信通道发送失败时抛出
     */
    @Override
    public void sendCode(String phone, String scene) {
        // 检查发送频率：60秒内不可重复发送
        if (redisUtil.isSmsRateLimited(phone)) {
            throw new BusinessException(ResultCode.SMS_SEND_FREQUENT);
        }

        // 生成6位随机数字验证码
        String code = generateCode(CODE_LENGTH);

        // 先下发；失败时直接抛出，不保存验证码，也不进入冷却，用户可以立即重试
        smsSender.send(phone, code, scene);

        // 存入Redis，5分钟过期
        redisUtil.cacheSmsCode(phone, code);

        // 标记发送频率（60秒冷却）
        redisUtil.markSmsSent(phone);
    }

    /**
     * 校验短信验证码（一次性使用）
     *
     * <p>从Redis取出存储的验证码与用户输入比对，校验通过后立即删除</p>
     *
     * @param phone     手机号
     * @param inputCode 用户输入的验证码
     * @return true=校验通过, false=验证码错误或已过期
     */
    @Override
    public boolean verifyCode(String phone, String inputCode) {
        String storedCode = redisUtil.getAndDeleteSmsCode(phone);
        if (storedCode == null) {
            log.warn("验证码校验失败: 手机号={}，原因=已过期或未发送", mask(phone));
            return false;
        }
        if (!storedCode.equals(inputCode)) {
            // 只记录手机号掩码与失败原因，不输出验证码明文
            log.warn("验证码校验失败: 手机号={}，原因=验证码不匹配", mask(phone));
            return false;
        }
        log.info("验证码校验通过: 手机号={}", mask(phone));
        return true;
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

    /**
     * 生成指定位数的随机数字验证码
     *
     * @param length 验证码位数
     * @return 纯数字字符串
     */
    private String generateCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }
}
