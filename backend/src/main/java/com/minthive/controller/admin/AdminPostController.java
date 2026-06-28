package com.minthive.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.common.Constants;
import com.minthive.common.Result;
import com.minthive.entity.Post;
import com.minthive.mapper.AdminPostMapper;
import com.minthive.mapper.PostMapper;
import com.minthive.service.PostService;
import com.minthive.service.SystemMsgService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理后台-内容审核控制器
 * <p>功能描述：提供帖子审核（待审/已发布）、通过/驳回/删除等接口</p>
 */
@Tag(name = "管理后台-内容审核")
@RestController
@RequestMapping("/api/admin/content")
@RequiredArgsConstructor
public class AdminPostController {

    private final PostMapper postMapper;
    private final AdminPostMapper adminPostMapper;
    private final PostService postService;
    private final SystemMsgService systemMsgService;

    /**
     * 待审核帖子列表（关联用户+圈子，字段对齐前端 PostInfo）
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @param keyword  搜索关键词
     * @return 分页结果 {list, total, page, pageSize}
     */
    @Operation(summary = "待审核帖子列表")
    @GetMapping("/pending")
    public Result<Map<String, Object>> pendingList(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        IPage<Map<String, Object>> p = adminPostMapper.selectPendingPostList(
                new Page<>(page, pageSize), keyword);
        Map<String, Object> data = new HashMap<>(4);
        data.put("list", p.getRecords());
        data.put("total", p.getTotal());
        data.put("page", p.getCurrent());
        data.put("pageSize", p.getSize());
        return Result.success(data);
    }

    /**
     * 已发布帖子列表（关联用户+圈子，字段对齐前端 PostInfo）
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @param keyword  搜索关键词
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
        IPage<Map<String, Object>> p = adminPostMapper.selectPublishedPostList(
                new Page<>(page, pageSize), keyword);
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
        Post post = postMapper.selectById(postId);
        postService.audit(postId, 1); // 通过
        if (post != null) {
            String preview = post.getContent();
            if (preview != null && preview.length() > 30) preview = preview.substring(0, 30) + "...";
            systemMsgService.push(post.getUserId(), Constants.SYS_MSG_TYPE_AUDIT,
                    "你的帖子「" + preview + "」已通过审核", postId);
        }
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
        Post post = postMapper.selectById(postId);
        postService.audit(postId, 2); // 驳回
        if (post != null) {
            String preview = post.getContent();
            if (preview != null && preview.length() > 30) preview = preview.substring(0, 30) + "...";
            systemMsgService.push(post.getUserId(), Constants.SYS_MSG_TYPE_AUDIT,
                    "你的帖子「" + preview + "」被驳回，原因：" + reason, postId);
        }
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

}
