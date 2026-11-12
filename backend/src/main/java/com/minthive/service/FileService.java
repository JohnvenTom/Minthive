package com.minthive.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务接口
 */
public interface FileService {

    /**
     * 上传到独立私有桶，持久化当前用户归属。
     *
     * @param file 文件
     * @return 无票据的 canonical 应用相对路径；HTTP 响应 advice 按访问者签发短时媒体票据
     */
    String upload(MultipartFile file);

    /**
     * 删除文件
     *
     * @param fileUrl 文件访问 URL
     */
    void delete(String fileUrl);
}
