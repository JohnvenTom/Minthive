package com.minthive.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务接口
 */
public interface FileService {

    /**
     * 上传文件
     *
     * @param file 文件
     * @return 可访问 URL
     */
    String upload(MultipartFile file);
}
