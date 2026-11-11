package com.minthive.service.impl;

import com.minthive.common.BusinessException;
import com.minthive.common.ResultCode;
import com.minthive.media.MediaAccessService;
import com.minthive.media.MediaLocator;
import com.minthive.security.UserContext;
import com.minthive.service.FileService;
import com.minthive.util.MinioUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Map;

/**
 * 文件服务实现
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class FileServiceImpl implements FileService {

    /**
     * 允许上传的扩展名 → 服务端强制写入的 Content-Type
     *
     * <p>白名单之外的扩展名一律拒绝；存储 Content-Type 由服务端按扩展名决定，
     * 绝不透传客户端声明的值——否则上传 .html/.svg 等会被 MinIO 当页面服务，形成存储型 XSS</p>
     */
    private static final Map<String, String> ALLOWED_CONTENT_TYPES = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "gif", "image/gif",
            "webp", "image/webp",
            "mp4", "video/mp4");

    private final MinioUtil minioUtil;
    private final MediaLocator locator;
    private final MediaAccessService mediaAccess;

    @Value("${spring.servlet.multipart.max-file-size:20MB}")
    private DataSize maxFileSize = DataSize.ofMegabytes(20);

    /** 上传返回稳定 canonical 路径；不返回 MinIO 直链或登录令牌。 */
    @Override
    public String upload(MultipartFile file) {
        Long owner = currentUser();
        mediaAccess.requireActiveViewer(owner);
        if (file == null || file.isEmpty()) throw new BusinessException(ResultCode.PARAM_ERROR, "文件不能为空");
        if (file.getSize() > maxFileSize.toBytes()) throw new BusinessException(ResultCode.FILE_TOO_LARGE);
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT)
                : "";
        String contentType = ALLOWED_CONTENT_TYPES.get(extension);
        if (contentType == null) {
            throw new BusinessException(ResultCode.FILE_TYPE_NOT_ALLOWED,
                    "仅支持图片(jpg/png/gif/webp)和视频(mp4)文件");
        }
        try {
            return locator.fromName(minioUtil.upload(file, contentType, owner)).canonical();
        } catch (Exception e) {
            log.error("媒体上传失败", e);
            throw new BusinessException(ResultCode.FILE_UPLOAD_ERROR, "媒体上传失败或私有桶检查失败");
        }
    }

    /** 删除只能由对象元数据所有者或当前有效管理员执行；旧桶归属未知，禁止删除。 */
    @Override
    public void delete(String fileUrl) {
        Long viewer = currentUser();
        MediaLocator.Ref ref;
        try {
            ref = locator.parse(fileUrl);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "无效媒体路径");
        }
        if (ref == null) throw new BusinessException(ResultCode.PARAM_ERROR, "不是受控媒体路径");
        mediaAccess.requireDeletable(ref, viewer);
        try {
            minioUtil.delete(ref);
        } catch (Exception e) {
            log.error("媒体删除失败", e);
            throw new BusinessException(ResultCode.SERVER_ERROR, "媒体删除失败");
        }
    }

    private Long currentUser() {
        if (UserContext.get() == null || UserContext.get().getUserId() == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return UserContext.get().getUserId();
    }
}
