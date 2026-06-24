package com.minthive.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.common.Result;
import com.minthive.entity.Circle;
import com.minthive.entity.Post;
import com.minthive.entity.User;
import com.minthive.service.CircleService;
import com.minthive.service.PostService;
import com.minthive.service.UserService;
import com.minthive.mapper.CircleUserMapper;
import com.minthive.mapper.UserMapper;
import com.minthive.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final CircleUserMapper circleUserMapper;
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

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
     * @return 分页结果（含 joined/ownerName/ownerAvatar 等完整字段）
     */
    @Operation(summary = "搜索圈子")
    @GetMapping("/circle")
    public Result<Page<Map<String, Object>>> searchCircle(@RequestParam String keyword,
                                                           @RequestParam(defaultValue = "1") long current,
                                                           @RequestParam(defaultValue = "10") long size,
                                                           HttpServletRequest request) {
        Page<Circle> page = circleService.page(current, size, null, keyword);
        // 获取当前登录用户ID（用于填充 joined 状态）
        Long currentUserId = resolveUserId(request);
        // 将 Circle 实体转换为 Map，补充非持久化字段
        Page<Map<String, Object>> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<Map<String, Object>> records = page.getRecords().stream().map(circle -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", circle.getId());
            map.put("ownerId", circle.getOwnerId());
            // 查询圈主昵称和头像
            if (circle.getOwnerId() != null) {
                User owner = userMapper.selectById(circle.getOwnerId());
                map.put("ownerName", owner != null ? owner.getNickname() : "未知用户");
                map.put("ownerAvatar", owner != null ? owner.getAvatar() : "");
            } else {
                map.put("ownerName", "未知用户");
                map.put("ownerAvatar", "");
            }
            map.put("name", circle.getName());
            map.put("categoryId", circle.getCategoryId());
            map.put("categoryName", circle.getCategoryName());
            map.put("intro", circle.getIntro());
            map.put("avatar", circle.getAvatar());
            map.put("banner", circle.getBanner());
            // type: Integer(0/1) → String("public"/"private")
            map.put("type", circle.getType() != null && circle.getType() == 1 ? "private" : "public");
            map.put("memberCount", circle.getMemberCount());
            map.put("postCount", circle.getPostCount() != null ? circle.getPostCount() : 0);
            map.put("status", circle.getStatus());
            map.put("notice", circle.getNotice());
            // 填充当前用户是否已加入
            boolean joined = false;
            if (currentUserId != null) {
                long count = circleUserMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.minthive.entity.CircleUser>()
                        .eq(com.minthive.entity.CircleUser::getCircleId, circle.getId())
                        .eq(com.minthive.entity.CircleUser::getUserId, currentUserId)
                );
                joined = count > 0;
            }
            map.put("joined", joined);
            return map;
        }).toList();
        result.setRecords(records);
        return Result.success(result);
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
                            @RequestParam(defaultValue = "1") long current,
                            HttpServletRequest request) {
        long size = 10;
        switch (type.toLowerCase()) {
            case "user":
                return Result.success(userService.searchByKeyword(keyword, current, size));
            case "post":
                return Result.success(postService.searchByKeyword(keyword, current, size));
            case "circle":
                // 单独搜索圈子：返回分页的完整字段 Map
                Page<Circle> circlePage = circleService.page(current, size, null, keyword);
                Page<Map<String, Object>> enrichedPage = new Page<>(circlePage.getCurrent(), circlePage.getSize(), circlePage.getTotal());
                enrichedPage.setRecords(enrichCircles(circlePage.getRecords(), request));
                return Result.success(enrichedPage);
            case "topic":
                // 话题搜索暂无独立服务，返回空结果
                return Result.success(new Page<>(current, size));
            case "all":
            default:
                // 全局搜索：并行查询用户、帖子、圈子，合并返回
                Map<String, Object> allResult = new HashMap<>();
                allResult.put("users", userService.searchByKeyword(keyword, current, size).getRecords());
                allResult.put("posts", postService.searchByKeyword(keyword, current, size).getRecords());
                // 圈子也需要填充完整字段
                allResult.put("circles", enrichCircles(circleService.page(current, size, null, keyword).getRecords(), request));
                return Result.success(allResult);
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

    /**
     * 双通道解析当前登录用户ID
     * @param request HTTP 请求对象
     * @return 当前用户ID，未登录返回 null
     */
    private Long resolveUserId(HttpServletRequest request) {
        try {
            return com.minthive.security.UserContext.getUserId();
        } catch (Exception e) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    return jwtUtil.getUserIdFromToken(authHeader.substring(7));
                } catch (Exception ignored) {
                    return null;
                }
            }
            return null;
        }
    }

    /**
     * 将 Circle 实体列表转换为 Map 列表，补充 joined/ownerName/ownerAvatar/type 等字段
     */
    private List<Map<String, Object>> enrichCircles(List<Circle> circles, HttpServletRequest request) {
        Long currentUserId = resolveUserId(request);
        return circles.stream().map(circle -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", circle.getId());
            map.put("ownerId", circle.getOwnerId());
            if (circle.getOwnerId() != null) {
                User owner = userMapper.selectById(circle.getOwnerId());
                map.put("ownerName", owner != null ? owner.getNickname() : "未知用户");
                map.put("ownerAvatar", owner != null ? owner.getAvatar() : "");
            } else {
                map.put("ownerName", "未知用户");
                map.put("ownerAvatar", "");
            }
            map.put("name", circle.getName());
            map.put("categoryId", circle.getCategoryId());
            map.put("categoryName", circle.getCategoryName());
            map.put("intro", circle.getIntro());
            map.put("avatar", circle.getAvatar());
            map.put("banner", circle.getBanner());
            map.put("type", circle.getType() != null && circle.getType() == 1 ? "private" : "public");
            map.put("memberCount", circle.getMemberCount());
            map.put("postCount", circle.getPostCount() != null ? circle.getPostCount() : 0);
            map.put("status", circle.getStatus());
            map.put("notice", circle.getNotice());
            boolean joined = false;
            if (currentUserId != null) {
                long count = circleUserMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.minthive.entity.CircleUser>()
                        .eq(com.minthive.entity.CircleUser::getCircleId, circle.getId())
                        .eq(com.minthive.entity.CircleUser::getUserId, currentUserId)
                );
                joined = count > 0;
            }
            map.put("joined", joined);
            return map;
        }).toList();
    }
}
