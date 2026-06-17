package com.minthive.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.common.Result;
import com.minthive.entity.Circle;
import com.minthive.entity.Post;
import com.minthive.entity.User;
import com.minthive.service.CircleService;
import com.minthive.service.PostService;
import com.minthive.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 搜索控制器
 */
@Tag(name = "搜索接口")
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final UserService userService;
    private final PostService postService;
    private final CircleService circleService;

    /**
     * 搜索用户
     *
     * @param keyword 关键词
     * @param current 当前页
     * @param size    每页大小
     * @return 分页结果
     */
    @Operation(summary = "搜索用户")
    @GetMapping("/user")
    public Result<Page<User>> searchUser(@RequestParam String keyword,
                                         @RequestParam(defaultValue = "1") long current,
                                         @RequestParam(defaultValue = "10") long size) {
        return Result.success(userService.searchByKeyword(keyword, current, size));
    }

    /**
     * 搜索帖子
     *
     * @param keyword 关键词
     * @param current 当前页
     * @param size    每页大小
     * @return 分页结果
     */
    @Operation(summary = "搜索帖子")
    @GetMapping("/post")
    public Result<Page<Post>> searchPost(@RequestParam String keyword,
                                         @RequestParam(defaultValue = "1") long current,
                                         @RequestParam(defaultValue = "10") long size) {
        return Result.success(postService.searchByKeyword(keyword, current, size));
    }

    /**
     * 搜索圈子
     *
     * @param keyword  关键词
     * @param current  当前页
     * @param size     每页大小
     * @return 分页结果
     */
    @Operation(summary = "搜索圈子")
    @GetMapping("/circle")
    public Result<Page<Circle>> searchCircle(@RequestParam String keyword,
                                             @RequestParam(defaultValue = "1") long current,
                                             @RequestParam(defaultValue = "10") long size) {
        return Result.success(circleService.page(current, size, null, keyword));
    }
}
