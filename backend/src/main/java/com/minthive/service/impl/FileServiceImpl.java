package com.minthive.service.impl;

import com.minthive.common.BusinessException;
import com.minthive.common.ResultCode;
import com.minthive.service.FileService;
import com.minthive.util.MinioUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务实现
 */
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
}
