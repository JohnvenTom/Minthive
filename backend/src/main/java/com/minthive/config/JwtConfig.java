package com.minthive.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 配置类
 *
 * <p>功能描述：读取 jwt 配置项（密钥、过期时间、请求头、前缀）</p>
 * <p>注意事项：生产环境务必通过环境变量覆盖 secret</p>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    /** JWT 签名密钥 */
    private String secret;

    /** 过期时间(秒)，默认 7 天 */
    private Long expire;

    /** 请求头名称 */
    private String header;

    /** Token 前缀 */
    private String prefix;
}
