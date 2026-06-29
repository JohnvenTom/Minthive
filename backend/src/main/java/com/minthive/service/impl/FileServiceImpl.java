package com.minthive.service.impl;

import com.minthive.common.BusinessException;
import com.minthive.common.ResultCode;
import com.minthive.service.FileService;
import com.minthive.util.MinioUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务实现
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class FileServiceImpl implements FileService {

    private final MinioUtil minioUtil;

    /**
     * 上传文件到 MinIO
     *
     * @param file 文件
     * @return 可访问 URL
     * @throws BusinessException 上传失败时抛出
     */
    @Override
    public String upload(MultipartFile file) {
        try {
            return minioUtil.upload(file);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.FILE_UPLOAD_ERROR, e.getMessage());
        }
    }

    /**
     * 删除 MinIO 中的文件
     *
     * @param fileUrl 文件访问 URL，从 URL 中提取 objectName 后删除
     */
    @Override
    public void delete(String fileUrl) {
        try {
            String objectName = minioUtil.extractObjectName(fileUrl);
            if (objectName != null) {
                minioUtil.delete(objectName);
            }
        } catch (Exception e) {
            log.error("删除文件失败: fileUrl={}", fileUrl, e);
        }
    }
}
