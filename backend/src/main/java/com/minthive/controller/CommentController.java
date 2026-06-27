package com.minthive.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.common.Constants;
import com.minthive.common.Result;
import com.minthive.common.Constants;
import com.minthive.entity.Comment;
import com.minthive.security.UserContext;
import com.minthive.service.CommentService;
import com.minthive.service.LikeCollectService;
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

    private final LikeCollectService likeCollectService;

    /**
     * 构造器注入
     *
     * @param commentService     评论服务
     * @param likeCollectService 点赞收藏服务
     */
    public CommentController(CommentService commentService, LikeCollectService likeCollectService) {
        this.commentService = commentService;
        this.likeCollectService = likeCollectService;
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
        // /api/comment/page 为公开接口（excludePathPatterns），未登录时 UserContext 未设置
        Long userId = null;
        try {
            userId = UserContext.getUserId();
        } catch (Exception e) {
            // 未登录时 userId 保持 null，不填充 liked 状态
        }
        return Result.success(commentService.pageByPost(current, size, postId, userId));
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

    /**
     * 点赞评论
     *
     * <p>功能描述：对指定评论进行点赞操作，调用 likeCollectService 记录点赞关系</p>
     *
     * @param id 评论ID
     * @return 操作结果
     */
    @Operation(summary = "点赞评论")
    @PostMapping("/{id}/like")
    public Result<Void> like(@PathVariable Long id) {
        likeCollectService.like(UserContext.getUserId(), id, Constants.LC_TYPE_LIKE_COMMENT);
        return Result.success();
    }

    /**
     * 取消点赞评论
     *
     * <p>功能描述：取消对指定评论的点赞操作，调用 likeCollectService 删除点赞记录</p>
     *
     * @param id 评论ID
     * @return 操作结果
     */
    @Operation(summary = "取消点赞评论")
    @DeleteMapping("/{id}/like")
    public Result<Void> unlike(@PathVariable Long id) {
        likeCollectService.unlike(UserContext.getUserId(), id, Constants.LC_TYPE_LIKE_COMMENT);
        return Result.success();
    }
}
