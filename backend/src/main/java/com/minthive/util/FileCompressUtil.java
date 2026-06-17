package com.minthive.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * 文件压缩工具类
 *
 * <p>功能描述：图片质量压缩，控制上传文件大小</p>
 * <p>注意事项：仅支持 JPG/PNG/GIF 等常见图片格式</p>
 */
@Slf4j
@Component
public class FileCompressUtil {

    /**
     * 压缩图片(按质量比例)
     *
     * @param inputBytes 原始图片字节数组
     * @param formatName 图片格式(jpg/png)
     * @param quality    压缩质量 0.0~1.0
     * @return 压缩后字节数组
     * @throws Exception 压缩异常
     */
    public byte[] compressImage(byte[] inputBytes, String formatName, float quality) throws Exception {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(inputBytes);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            BufferedImage image = ImageIO.read(bais);
            if (image == null) {
                return inputBytes;
            }
            ImageWriter writer = ImageIO.getImageWritersByFormatName(formatName).next();
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality);
            }
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(image, null, null), param);
                writer.dispose();
            }
            return baos.toByteArray();
        } catch (Exception e) {
            log.warn("图片压缩失败，返回原图: {}", e.getMessage());
            return inputBytes;
        }
    }

    /**
     * 压缩图片(从输入流)
     *
     * @param inputStream 原始图片输入流
     * @param formatName  图片格式
     * @param quality     压缩质量
     * @return 压缩后字节数组
     * @throws Exception 异常
     */
    public byte[] compressImage(InputStream inputStream, String formatName, float quality) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int n;
        while ((n = inputStream.read(data)) != -1) {
            buffer.write(data, 0, n);
        }
        return compressImage(buffer.toByteArray(), formatName, quality);
    }
}
