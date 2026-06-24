package com.minthive.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 对象存储配置类
 *
 * <p>功能描述：读取 minio 配置并构建 MinioClient Bean</p>
 * <p>注意事项：本地私有化部署，endpoint 指向本地 MinIO 服务</p>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinioConfig {

    /** MinIO 服务地址（客户端连接用） */
    private String endpoint;

    /** 图片访问地址（前端展示用，为空则取 endpoint） */
    private String publicUrl;

    /** 访问密钥 */
    private String accessKey;

    /** 秘密密钥 */
    private String secretKey;

    /** 默认存储桶名称 */
    private String bucket;

    /**
     * 获取图片访问地址
     *
     * @return publicUrl 或 endpoint
     */
    public String getPublicUrl() {
        return publicUrl != null && !publicUrl.isEmpty() ? publicUrl : endpoint;
    }

    /**
     * 构建 MinioClient 客户端实例
     *
     * @return MinioClient 实例
     */
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
