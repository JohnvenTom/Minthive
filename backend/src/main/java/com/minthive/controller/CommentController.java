package com.minthive.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.common.Result;
import com.minthive.entity.Comment;
import com.minthive.security.UserContext;
import com.minthive.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * 评论控制器
 */
@Tag(name = "评论接口")
@RestController
@RequestMapping("/api/comment")
public class CommentController {

    private final CommentService commentService;

    /**
     * 构造器注入 CommentService
     *
     * @param commentService 评论服务
     */
    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * 发表评论
     *
     * @param comment 评论实体
     * @return 评论实体
     */
    @Operation(summary = "发表评论")
    @PostMapping
    public Result<Comment> publish(@RequestBody Comment comment) {
        comment.setUserId(UserContext.getUserId());
        return Result.success(commentService.publish(comment));
    }

    /**
     * 分页查询帖子评论
     *
     * @param postId  帖子ID
     * @param current 当前页
     * @param size    每页大小
     * @return 分页结果
     */
    @Operation(summary = "分页查询帖子评论")
    @GetMapping("/page")
    public Result<Page<Comment>> page(@RequestParam Long postId,
                                      @RequestParam(defaultValue = "1") long current,
                                      @RequestParam(defaultValue = "10") long size) {
        return Result.success(commentService.pageByPost(current, size, postId));
    }

    /**
     * 删除评论
     *
     * @param id 评论ID
     * @return 操作结果
     */
    @Operation(summary = "删除评论")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        commentService.delete(id, UserContext.getUserId());
        return Result.success();
    }
}
