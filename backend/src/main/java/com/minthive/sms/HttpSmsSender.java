package com.minthive.sms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minthive.common.BusinessException;
import com.minthive.common.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * HTTP 短信通道
 *
 * <p>功能描述：把验证码通过配置的 HTTP 网关下发，网关地址与令牌由环境变量提供，
 * 便于对接阿里云/腾讯云或自建短信网关的转发服务</p>
 *
 * <p>注意事项：网关返回非 2xx 时抛出业务异常，调用方不会保存验证码；
 * 令牌仅放在请求头，不写入日志</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "sms.provider", havingValue = "http")
public class HttpSmsSender implements SmsSender {

    private final ObjectMapper objectMapper;

    private final HttpClient httpClient;

    /** 短信网关地址 */
    @Value("${sms.http.url:}")
    private String url;

    /** 网关鉴权令牌 */
    @Value("${sms.http.token:}")
    private String token;

    /** 网关超时毫秒 */
    @Value("${sms.http.timeout:5000}")
    private long timeoutMillis;

    /**
     * 构造器注入 JSON 工具并初始化 HTTP 客户端
     *
     * @param objectMapper JSON 工具
     */
    public HttpSmsSender(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * 通过 HTTP 网关发送验证码
     *
     * @param phone 手机号
     * @param code  验证码
     * @param scene 场景
     * @throws BusinessException 网关未配置或发送失败时抛出
     */
    @Override
    public void send(String phone, String code, String scene) {
        if (url == null || url.isBlank()) {
            throw new BusinessException(ResultCode.SMS_SEND_FAILED, "短信服务未配置");
        }
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "phone", phone,
                    "code", code,
                    "scene", scene));
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(timeoutMillis))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body));
            if (token != null && !token.isBlank()) {
                builder.header("Authorization", "Bearer " + token);
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("短信网关返回失败 status={}", response.statusCode());
                throw new BusinessException(ResultCode.SMS_SEND_FAILED);
            }
            log.info("短信验证码已提交网关: 手机号={} 场景={}", mask(phone), scene);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("短信网关调用异常: {}", e.getMessage());
            throw new BusinessException(ResultCode.SMS_SEND_FAILED);
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
