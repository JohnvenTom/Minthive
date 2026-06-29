package com.minthive.controller;

import com.minthive.common.Result;
import com.minthive.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传控制器
 */
@Tag(name = "文件接口")
@RestController
@RequestMapping("/api/file")
public class FileController {

    private final FileService fileService;

    /**
     * 构造器注入 FileService
     *
     * @param fileService 文件服务
     */
    public FileController(FileService fileService) {
        this.fileService = fileService;
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
