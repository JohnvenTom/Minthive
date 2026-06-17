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

/**
 * 管理后台-帖子管理控制器
 */
@Tag(name = "管理后台-帖子管理")
@RestController
@RequestMapping("/api/admin/post")
@RequiredArgsConstructor
public class AdminPostController {

    private final PostService postService;
    private final PostMapper postMapper;

    /**
     * 分页查询待审核帖子
     *
     * @param current 当前页
     * @param size    每页大小
     * @return 分页结果
     */
    @Operation(summary = "分页查询待审核帖子")
    @GetMapping("/page")
    public Result<Page<Post>> page(@RequestParam(defaultValue = "1") long current,
                                   @RequestParam(defaultValue = "10") long size) {
        Page<Post> page = postMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<Post>().eq(Post::getAuditStatus, 0).orderByDesc(Post::getCreateTime));
        return Result.success(page);
    }

    /**
     * 审核帖子
     *
     * @param id     帖子ID
     * @param status 审核状态
     * @return 操作结果
     */
    @Operation(summary = "审核帖子")
    @PutMapping("/audit/{id}")
    public Result<Void> audit(@PathVariable Long id, @RequestParam Integer status) {
        postService.audit(id, status);
        return Result.success();
    }

    /**
     * 删除帖子(管理员权限)
     *
     * @param id 帖子ID
     * @return 操作结果
     */
    @Operation(summary = "管理员删除帖子")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        postMapper.deleteById(id);
        return Result.success();
    }
}
