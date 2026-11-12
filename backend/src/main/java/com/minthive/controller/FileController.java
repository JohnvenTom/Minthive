package com.minthive.controller;

import com.minthive.common.Result;
import com.minthive.media.MediaContentService;
import com.minthive.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 文件上传控制器
 */
@Tag(name = "文件接口")
@RestController
@RequestMapping("/api/file")
public class FileController {

    private final FileService fileService;
    private final MediaContentService mediaContent;

    public FileController(FileService fileService, MediaContentService mediaContent) {
        this.fileService = fileService;
        this.mediaContent = mediaContent;
    }

    /** 仅此 GET 路径可绕过登录 JWT；缺失/无效媒体票据一律拒绝。 */
    @Operation(summary = "使用短时媒体票据流式读取图片/视频")
    @GetMapping("/content/{objectToken:[A-Za-z0-9_-]+}")
    public void content(@PathVariable String objectToken, @RequestParam(required = false) String ticket,
                        HttpServletRequest request, HttpServletResponse response) throws IOException {
        mediaContent.serve(objectToken, ticket, request, response);
    }

    /**
     * 上传文件
     *
     * @param file 文件
     * @return 可访问 URL
     */
    @Operation(summary = "上传文件")
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        return Result.success(fileService.upload(file));
    }

    /**
     * 删除文件
     *
     * @param url 文件访问 URL
     */
    @Operation(summary = "删除文件")
    @DeleteMapping("/delete")
    public Result<Void> delete(@RequestParam("url") String url) {
        fileService.delete(url);
        return Result.success();
    }
}
