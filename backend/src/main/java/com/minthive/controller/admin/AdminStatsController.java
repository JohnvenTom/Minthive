package com.minthive.controller.admin;

import com.minthive.common.Result;
import com.minthive.mapper.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理后台-数据统计控制器
 */
@Tag(name = "管理后台-数据统计")
@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final UserMapper userMapper;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final CircleMapper circleMapper;
    private final ReportMapper reportMapper;

    /**
     * 平台核心数据统计
     *
     * @return 统计数据（用户数、帖子数、评论数、圈子数、举报数）
     */
    @Operation(summary = "平台核心数据统计")
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("userCount", userMapper.selectCount(null));
        stats.put("postCount", postMapper.selectCount(null));
        stats.put("commentCount", commentMapper.selectCount(null));
        stats.put("circleCount", circleMapper.selectCount(null));
        stats.put("reportCount", reportMapper.selectCount(null));
        return Result.success(stats);
    }
}
