package com.minthive.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minthive.media.MediaLocator;
import com.minthive.util.MinioUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Base64;

/**
 * 图片内容审核服务
 *
 * <p>功能描述：从受控媒体地址读取帖子首张图片，转成 Base64 后交给多模态模型审核。
 * 只读取服务端存储中的对象，不接受任意外部 URL，避免 SSRF</p>
 *
 * <p>注意事项：读取失败、超出大小上限或模型不可用时统一返回 unavailable，
 * 由调用方转人工审核（待审），不会把异常当成"安全"</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageModerationService {

    /** 单张图片审核大小上限：3MB，避免 Base64 后请求体过大 */
    private static final long MAX_IMAGE_BYTES = 3L * 1024 * 1024;

    /** 最多审核的图片张数 */
    private static final int MAX_IMAGES = 2;

    private final AiContext aiContext;

    private final MediaLocator mediaLocator;

    private final MinioUtil minioUtil;

    private final ObjectMapper objectMapper;

    /**
     * 审核帖子中的图片
     *
     * <p>逐张审核前 {@value #MAX_IMAGES} 张图片：任意一张不可用即返回不可用，
     * 任意一张违规即返回违规；全部安全才返回通过</p>
     *
     * @param text       帖子文本，可为空
     * @param imagesJson 图片 JSON 数组字符串
     * @return 审核结果
     */
    public AiService.AiDetectResult moderate(String text, String imagesJson) {
        JsonNode images;
        try {
            images = objectMapper.readTree(imagesJson == null ? "[]" : imagesJson);
        } catch (Exception e) {
            return AiService.AiDetectResult.unavailable("图片列表格式无效");
        }
        if (images == null || !images.isArray() || images.isEmpty()) {
            return AiService.AiDetectResult.unavailable("图片列表为空");
        }
        int checked = 0;
        for (JsonNode item : images) {
            if (checked >= MAX_IMAGES) break;
            if (!item.isTextual()) continue;
            checked++;
            AiService.AiDetectResult result = moderateSingle(text, item.asText());
            if (!result.available || result.violation) {
                return result;
            }
        }
        if (checked == 0) return AiService.AiDetectResult.unavailable("未找到可审核的图片");
        return new AiService.AiDetectResult(false, "无", "low");
    }

    /**
     * 审核单张图片
     *
     * @param text 帖子文本
     * @param url  数据库中的图片地址
     * @return 单张图片审核结果
     */
    private AiService.AiDetectResult moderateSingle(String text, String url) {
        try {
            MediaLocator.Ref ref = mediaLocator.parse(url);
            if (ref == null) return AiService.AiDetectResult.unavailable("图片地址不受控，无法审核");
            MinioUtil.Metadata meta = minioUtil.stat(ref);
            if (meta.size() <= 0 || meta.size() > MAX_IMAGE_BYTES) {
                return AiService.AiDetectResult.unavailable("图片超过审核大小上限");
            }
            byte[] bytes;
            try (InputStream input = minioUtil.open(ref, 0, meta.size())) {
                bytes = input.readAllBytes();
            }
            String base64 = Base64.getEncoder().encodeToString(bytes);
            return aiContext.detectContent(text, base64);
        } catch (Exception e) {
            log.warn("图片审核读取失败: {}", e.getMessage());
            return AiService.AiDetectResult.unavailable("图片审核读取失败");
        }
    }
}
