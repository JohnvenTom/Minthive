package com.minthive.service.impl;

import com.minthive.common.BusinessException;
import com.minthive.common.ResultCode;
import com.minthive.service.SmsService;
import com.minthive.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/**
 * 短信验证码服务实现
 *
 * <p>功能描述：生成6位随机验证码，打印到后端控制台，存储到Redis（开发模式）</p>
 * <p>注意事项：生产环境应替换为真实短信服务商API调用（如阿里云SMS、腾讯云SMS等）</p>
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SmsServiceImpl implements SmsService {

    private final RedisUtil redisUtil;

    /** 验证码长度 */
    private static final int CODE_LENGTH = 6;

    /** 安全随机数生成器 */
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 发送短信验证码（开发模式：打印到控制台）
     *
     * <p>流程：</p>
     * <ol>
     *   <li>检查发送频率限制（60秒内不可重复发送同一手机号）</li>
     *   <li>生成6位随机数字验证码</li>
     *   <li>存入Redis，设置5分钟过期</li>
     *   <li>打印到后端控制台供开发者查看</li>
     * </ol>
     *
     * @param phone 目标手机号
     * @param scene 发送场景（login / register / reset）
     * @throws BusinessException 60秒内重复发送时抛出频率限制异常
     */
    @Override
    public void sendCode(String phone, String scene) {
        // 检查发送频率：60秒内不可重复发送
        if (redisUtil.isSmsRateLimited(phone)) {
            throw new BusinessException(ResultCode.SMS_SEND_FREQUENT);
        }

        // 生成6位随机数字验证码
        String code = generateCode(CODE_LENGTH);

        // 存入Redis，5分钟过期
        redisUtil.cacheSmsCode(phone, code);

        // 标记发送频率（60秒冷却）
        redisUtil.markSmsSent(phone);

        // 打印到控制台（开发模式）
        log.info("========== 【短信验证码】 ==========");
        log.info("手机号: {}", phone);
        log.info("验证码: {} （5分钟内有效）", code);
        log.info("场景: {}", scene);
        log.info("====================================");
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
            log.warn("验证码校验失败: 手机号={}，原因=已过期或未发送", phone);
            return false;
        }
        if (!storedCode.equals(inputCode)) {
            log.warn("验证码校验失败: 手机号={}，输入={}，正确={}", phone, inputCode, storedCode);
            return false;
        }
        log.info("验证码校验通过: 手机号={}", phone);
        return true;
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
