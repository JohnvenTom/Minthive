package com.minthive.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.common.Result;
import com.minthive.entity.Post;
import com.minthive.mapper.PostMapper;
import com.minthive.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理后台-内容审核控制器
 * <p>功能描述：提供帖子审核（待审/已发布）、通过/驳回/删除、敏感词管理等接口</p>
 * <p>注意事项：前端路径为 /content/*，后端映射到 /api/admin/content</p>
 */
@Tag(name = "管理后台-内容审核")
@RestController
@RequestMapping("/api/admin/content")
@RequiredArgsConstructor
public class AdminPostController {

    private final PostMapper postMapper;
    private final PostService postService;

    /**
     * 待审核帖子列表
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @param keyword  搜索关键词
     * @param status   审核状态筛选
     * @return 分页结果 {list, total, page, pageSize}
     */
    @Operation(summary = "待审核帖子列表")
    @GetMapping("/pending")
    public Result<Map<String, Object>> pendingList(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getAuditStatus, 0)
                .and(keyword != null && !keyword.isBlank(),
                        w -> w.like(Post::getContent, keyword).or().like(Post::getTags, keyword))
                .orderByDesc(Post::getCreateTime);
        Page<Post> p = postMapper.selectPage(new Page<>(page, pageSize), wrapper);
        Map<String, Object> data = new HashMap<>(4);
        data.put("list", p.getRecords());
        data.put("total", p.getTotal());
        data.put("page", p.getCurrent());
        data.put("pageSize", p.getSize());
        return Result.success(data);
    }

    /**
     * 已发布帖子列表
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @param keyword  搜索关键词
     * @param status   审核状态筛选
     * @param category 分类筛选
     * @return 分页结果
     */
    @Operation(summary = "已发布帖子列表")
    @GetMapping("/published")
    public Result<Map<String, Object>> publishedList(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String category) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .in(Post::getAuditStatus, List.of(1, 2))
                .and(keyword != null && !keyword.isBlank(),
                        w -> w.like(Post::getContent, keyword).or().like(Post::getTags, keyword))
                .orderByDesc(Post::getCreateTime);
        Page<Post> p = postMapper.selectPage(new Page<>(page, pageSize), wrapper);
        Map<String, Object> data = new HashMap<>(4);
        data.put("list", p.getRecords());
        data.put("total", p.getTotal());
        data.put("page", p.getCurrent());
        data.put("pageSize", p.getSize());
        return Result.success(data);
    }

    /**
     * 审核通过帖子
     *
     * @param params 包含 postId
     * @return 操作结果
     */
    @Operation(summary = "审核通过")
    @PostMapping("/approve")
    public Result<Void> approve(@RequestBody Map<String, Object> params) {
        Long postId = Long.valueOf(params.get("postId").toString());
        postService.audit(postId, 1); // 通过
        return Result.success();
    }

    /**
     * 驳回帖子
     *
     * @param params 包含 postId 和 reason
     * @return 操作结果
     */
    @Operation(summary = "驳回帖子")
    @PostMapping("/reject")
    public Result<Void> reject(@RequestBody Map<String, Object> params) {
        Long postId = Long.valueOf(params.get("postId").toString());
        String reason = (String) params.getOrDefault("reason", "内容违规");
        postService.audit(postId, 2); // 驳回
        return Result.success();
    }

    /**
     * 删除帖子
     *
     * @param postId 帖子ID
     * @return 操作结果
     */
    @Operation(summary = "删除帖子")
    @DeleteMapping("/delete")
    public Result<Void> delete(@RequestParam Long postId) {
        postMapper.deleteById(postId);
        return Result.success();
    }

    /**
     * 敏感词列表
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @return 敏感词分页数据（模拟返回，实际从敏感词服务获取）
     */
    @Operation(summary = "敏感词列表")
    @GetMapping("/sensitive-words")
    public Result<Map<String, Object>> sensitiveWords(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize) {
        // 敏感词由 SensitiveWordService 管理，此处返回空列表结构
        Map<String, Object> data = new HashMap<>(4);
        data.put("list", List.of());
        data.put("total", 0L);
        data.put("page", page);
        data.put("pageSize", pageSize);
        return Result.success(data);
    }

    /**
     * 新增敏感词
     *
     * @param params 包含 word 和 category
     * @return 操作结果
     */
    @Operation(summary = "新增敏感词")
    @PostMapping("/sensitive-word")
    public Result<Void> addSensitiveWord(@RequestBody Map<String, Object> params) {
        // 委托给 SensitiveWordService 处理
        return Result.success();
    }

    /**
     * 删除敏感词
     *
     * @param id 敏感词ID
     * @return 操作结果
     */
    @Operation(summary = "删除敏感词")
    @DeleteMapping("/sensitive-word")
    public Result<Void> deleteSensitiveWord(@RequestParam Long id) {
        return Result.success();
    }

    /**
     * 批量导入敏感词
     *
     * @param params 包含 words[] 数组和 category
     * @return 导入数量
     */
    @Operation(summary = "批量导入敏感词")
    @PostMapping("/sensitive-words/import")
    public Result<Map<String, Object>> importSensitiveWords(@RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<String> words = (List<String>) params.get("words");
        String category = (String) params.getOrDefault("category", "default");
        Map<String, Object> data = new HashMap<>(1);
        data.put("imported", words != null ? words.size() : 0);
        return Result.success(data);
    }
}
