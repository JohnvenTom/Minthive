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

import java.util.Arrays;
import java.util.List;

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

    /**
     * 全局搜索
     *
     * <p>根据 type 参数分发到对应的搜索方法，支持用户/帖子/圈子/话题等类型</p>
     *
     * @param keyword 搜索关键词
     * @param type    搜索类型：all(全部)/user(用户)/post(帖子)/circle(圈子)/topic(话题)，默认all
     * @param current 当前页码，默认1
     * @return 分页搜索结果
     */
    @Operation(summary = "全局搜索")
    @GetMapping
    public Result<?> search(@RequestParam String keyword,
                            @RequestParam(defaultValue = "all") String type,
                            @RequestParam(defaultValue = "1") long current) {
        long size = 10;
        switch (type.toLowerCase()) {
            case "user":
                return Result.success(userService.searchByKeyword(keyword, current, size));
            case "post":
                return Result.success(postService.searchByKeyword(keyword, current, size));
            case "circle":
                return Result.success(circleService.page(current, size, null, keyword));
            case "topic":
                // 话题搜索暂无独立服务，返回空结果
                return Result.success(new Page<>(current, size));
            case "all":
            default:
                // all 类型：并行搜索多种资源并合并结果（简化为仅搜索用户）
                return Result.success(userService.searchByKeyword(keyword, current, size));
        }
    }

    /**
     * 获取热门搜索词
     *
     * <p>简单实现：返回预设的热门词数组（后续可接入热搜统计）</p>
     *
     * @return 热门搜索词列表
     */
    @Operation(summary = "热门搜索词")
    @GetMapping("/hot")
    public Result<List<String>> hotSearch() {
        List<String> hotKeywords = Arrays.asList(
                "Vue3", "React", "Java", "SpringBoot", "前端", "后端"
        );
        return Result.success(hotKeywords);
    }
}
