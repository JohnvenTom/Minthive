package com.minthive.util;

import com.minthive.config.MinioConfig;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketPolicyArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * MinIO 对象存储工具类
 *
 * <p>功能描述：封装文件上传、删除、URL 生成等操作</p>
 * <p>注意事项：所有图片/视频统一存入本地 MinIO，禁止使用第三方云存储</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MinioUtil {

    private final MinioClient minioClient;

    private final MinioConfig minioConfig;

    /**
     * 上传文件到 MinIO
     *
     * @param file 上传的文件
     * @return 可访问的文件 URL
     * @throws Exception 上传异常
     */
    public String upload(MultipartFile file) throws Exception {
        // 确保存储桶存在
        ensureBucket();
        // 生成对象名: 按日期分目录
        String originalFilename = file.getOriginalFilename();
        String suffix = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String objectName = datePath + "/" + UUID.randomUUID().toString().replace("-", "") + suffix;

        // 上传
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioConfig.getBucket())
                    .object(objectName)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        }
        // 返回可访问 URL
        return buildUrl(objectName);
    }

    /**
     * 删除 MinIO 中的对象
     *
     * @param objectName 对象名
     */
    public void delete(String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioConfig.getBucket())
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            log.error("MinIO删除对象失败: objectName={}", objectName, e);
        }
    }

    /**
     * 确保存储桶存在，不存在则创建
     *
     * @throws Exception 异常
     */
    private void ensureBucket() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(minioConfig.getBucket())
                .build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(minioConfig.getBucket())
                    .build());
            log.info("MinIO 存储桶创建成功: {}", minioConfig.getBucket());
        }
        setBucketPublicPolicy();
    }

    private void setBucketPublicPolicy() throws Exception {
        String bucket = minioConfig.getBucket();
        String policy = """
                {
                  "Version": "2012-10-17",
                  "Statement": [{
                    "Effect": "Allow",
                    "Principal": {"AWS": ["*"]},
                    "Action": ["s3:GetObject"],
                    "Resource": ["arn:aws:s3:::%s/*"]
                  }]
                }
                """.formatted(bucket);
        minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                .bucket(bucket)
                .config(policy)
                .build());
    }

    /**
     * 构建可访问的 URL
     *
     * @param objectName 对象名
     * @return URL 字符串
     */
    private String buildUrl(String objectName) {
        return minioConfig.getPublicUrl() + "/" + minioConfig.getBucket() + "/" + objectName;
    }

    /**
     * 从文件 URL 中提取 objectName
     *
     * @param fileUrl 文件访问 URL
     * @return objectName，提取失败返回 null
     */
    public String extractObjectName(String fileUrl) {
        try {
            String prefix = minioConfig.getPublicUrl() + "/" + minioConfig.getBucket() + "/";
            if (fileUrl.startsWith(prefix)) {
                return fileUrl.substring(prefix.length());
            }
        } catch (Exception e) {
            log.error("提取objectName失败: fileUrl={}", fileUrl, e);
        }
        return null;
    }
}
