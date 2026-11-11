package com.minthive.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.common.BusinessException;
import com.minthive.common.Constants;
import com.minthive.common.Result;
import com.minthive.common.ResultCode;
import com.minthive.entity.Circle;
import com.minthive.entity.CircleCategory;
import com.minthive.entity.CircleUser;
import com.minthive.entity.Post;
import com.minthive.entity.User;
import com.minthive.mapper.CircleUserMapper;
import com.minthive.mapper.PostMapper;
import com.minthive.mapper.UserMapper;
import com.minthive.security.UserContext;
import com.minthive.service.CircleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 圈子控制器
 */
@Tag(name = "圈子接口")
@Slf4j
@RestController
@RequestMapping("/api/circle")
public class CircleController {

    private final CircleService circleService;
    private final CircleUserMapper circleUserMapper;
    private final PostMapper postMapper;
    private final UserMapper userMapper;

    /**
     * 构造器注入
     *
     * @param circleService     圈子服务
     * @param circleUserMapper  圈子成员 Mapper（用于查询当前用户是否已加入）
     * @param postMapper        帖子 Mapper（用于查询圈内帖子数）
     * @param userMapper        用户 Mapper（用于查询圈主信息）
     */
    public CircleController(CircleService circleService, CircleUserMapper circleUserMapper,
                            PostMapper postMapper, UserMapper userMapper) {
        this.circleService = circleService;
        this.circleUserMapper = circleUserMapper;
        this.postMapper = postMapper;
        this.userMapper = userMapper;
    }

    /**
     * 创建圈子
     * <p>传入 categoryId（预设分类ID）或 categoryName（自定义分类名称，后端自动创建）</p>
     *
     * @param params 圈子参数（name, categoryId/categoryName, intro, avatar, type）
     * @return 圈子实体
     */
    @Operation(summary = "创建圈子")
    @PostMapping
    public Result<Circle> create(@RequestBody Map<String, Object> params) {
        Long categoryId = null;
        // 优先使用 categoryId
        if (params.get("categoryId") != null) {
            categoryId = Long.valueOf(params.get("categoryId").toString());
        }
        // 如果没有 categoryId 但有 categoryName（自定义），先创建分类
        else if (params.get("categoryName") != null) {
            String categoryName = params.get("categoryName").toString().trim();
            if (!categoryName.isEmpty()) {
                categoryId = circleService.createCategory(categoryName);
            }
        }
        // 构建圈子对象
        Circle circle = new Circle();
        circle.setName((String) params.get("name"));
        circle.setCategoryId(categoryId);
        circle.setIntro((String) params.get("intro"));
        circle.setAvatar((String) params.get("avatar"));
        circle.setBanner((String) params.get("banner"));
        Object typeObj = params.get("type");
        circle.setType(typeObj != null && "private".equals(typeObj.toString()) ? 1 : 0);
        circle.setOwnerId(UserContext.getUserId());
        return Result.success(circleService.create(circle));
    }

    /**
     * 查询圈子详情（含当前用户是否已加入状态）
     *
     * @param id 圈子ID
     * @return 圈子详情（含 joined 字段）
     */
    @Operation(summary = "查询圈子详情")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> get(@PathVariable Long id) {
        Circle circle = circleService.getById(id);
        if (circle.getStatus() != Constants.CIRCLE_STATUS_NORMAL) {
            return Result.error(ResultCode.CIRCLE_NOT_EXISTS);
        }
        Long currentUserId = resolveUserId();
        CircleUser membership = currentUserId == null ? null : circleUserMapper.selectOne(
                new LambdaQueryWrapper<CircleUser>()
                    .eq(CircleUser::getCircleId, id)
                    .eq(CircleUser::getUserId, currentUserId));
        boolean joined = membership != null && Integer.valueOf(1).equals(membership.getAuditStatus());
        boolean pending = membership != null && Integer.valueOf(0).equals(membership.getAuditStatus());
        com.minthive.security.LoginUser viewer = UserContext.get();
        boolean privileged = currentUserId != null && (currentUserId.equals(circle.getOwnerId())
                || (viewer != null && Integer.valueOf(Constants.ROLE_ADMIN).equals(viewer.getRole())));
        boolean privateLocked = Integer.valueOf(Constants.CIRCLE_TYPE_PRIVATE).equals(circle.getType())
                && !joined && !privileged;
        // 私密圈公告和活动量只对正式成员/圈主/管理员可见
        long postCount = 0;
        if (!privateLocked) {
            try {
                postCount = postMapper.selectCount(
                    new LambdaQueryWrapper<Post>()
                        .eq(Post::getCircleId, id)
                        .eq(Post::getAuditStatus, 1)
                );
            } catch (Exception e) {
                log.warn("查询圈子[{}]帖子数异常: {}", id, e.getMessage());
            }
        }
        // 返回 Map 以便附加非实体字段
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("id", circle.getId());
        result.put("ownerId", circle.getOwnerId());
        // 查询圈主昵称和头像
        if (circle.getOwnerId() != null) {
            User owner = userMapper.selectById(circle.getOwnerId());
            result.put("ownerName", owner != null ? owner.getNickname() : "未知用户");
            result.put("ownerAvatar", owner != null ? owner.getAvatar() : "");
        } else {
            result.put("ownerName", "未知用户");
            result.put("ownerAvatar", "");
        }
        result.put("name", circle.getName());
        result.put("categoryId", circle.getCategoryId());
        result.put("categoryName", circle.getCategoryName());
        result.put("intro", circle.getIntro());
        result.put("avatar", circle.getAvatar());
        result.put("banner", circle.getBanner());
        result.put("type", circle.getType() == 0 ? "public" : "private");
        result.put("memberCount", circle.getMemberCount());
        result.put("postCount", postCount);
        result.put("status", circle.getStatus());
        result.put("recommend", circle.getRecommend());
        result.put("notice", privateLocked ? null : circle.getNotice());
        result.put("createTime", circle.getCreateTime());
        result.put("deleted", circle.getDeleted());
        result.put("joined", joined);
        result.put("pending", pending);
        return Result.success(result);
    }

    /**
     * 分页查询圈子
     *
     * @param current    当前页
     * @param size       每页大小
     * @param categoryId 分类ID
     * @param keyword    关键词
     * @return 分页结果
     */
    @Operation(summary = "分页查询圈子")
    @GetMapping("/page")
    public Result<Page<Circle>> page(@RequestParam(defaultValue = "1") long current,
                                     @RequestParam(defaultValue = "10") long size,
                                     @RequestParam(required = false) Long categoryId,
                                     @RequestParam(required = false) String keyword) {
        Page<Circle> result = circleService.page(current, size, categoryId, keyword);
        Long currentUserId = resolveUserId();
        if (currentUserId != null && !result.getRecords().isEmpty()) {
            for (Circle circle : result.getRecords()) {
                long count = circleUserMapper.selectCount(
                    new LambdaQueryWrapper<CircleUser>()
                        .eq(CircleUser::getCircleId, circle.getId())
                        .eq(CircleUser::getUserId, currentUserId)
                        .eq(CircleUser::getAuditStatus, 1)
                );
                circle.setJoined(count > 0);
            }
        } else {
            for (Circle circle : result.getRecords()) {
                circle.setJoined(false);
            }
        }
        return Result.success(result);
    }

    /**
     * 双通道解析当前登录用户ID
     *
     * <p>优先从 UserContext 获取（正常认证接口），失败则从请求头手动解析 Token（公开接口）</p>
     *
     * @param request HTTP 请求对象
     * @return 当前用户ID，未登录返回 null
     */
    private Long resolveUserId() {
        com.minthive.security.LoginUser user = UserContext.get();
        return user == null ? null : user.getUserId();
    }

    /**
     * 加入圈子
     *
     * @param circleId 圈子ID
     * @return 操作结果
     */
    @Operation(summary = "加入圈子")
    @PostMapping("/join/{circleId}")
    public Result<Void> join(@PathVariable Long circleId,
                             @RequestBody(required = false) Map<String, Object> body) {
        String reason = body == null || body.get("reason") == null ? null : body.get("reason").toString();
        circleService.join(circleId, UserContext.getUserId(), reason);
        return Result.success();
    }

    /**
     * 更新圈子信息（仅圈主可操作）
     *
     * @param id     圈子ID
     * @param params 圈子更新参数（name, categoryId/categoryName, intro, avatar, banner, type）
     * @return 更新后的圈子
     */
    @Operation(summary = "更新圈子信息")
    @PutMapping("/{id}")
    public Result<Circle> update(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        Circle existing = circleService.getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "圈子不存在");
        }
        // 校验操作者是否为圈主
        Long currentUserId = UserContext.getUserId();
        if (!existing.getOwnerId().equals(currentUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有圈主才能修改圈子信息");
        }
        // 构建更新对象
        Circle circle = new Circle();
        circle.setId(id);
        if (params.containsKey("name")) circle.setName((String) params.get("name"));
        if (params.containsKey("intro")) circle.setIntro((String) params.get("intro"));
        if (params.containsKey("avatar")) circle.setAvatar((String) params.get("avatar"));
        if (params.containsKey("banner")) circle.setBanner((String) params.get("banner"));
        // 处理分类
        Object categoryIdObj = params.get("categoryId");
        if (categoryIdObj != null) {
            circle.setCategoryId(Long.valueOf(categoryIdObj.toString()));
        } else if (params.containsKey("categoryName")) {
            String categoryName = ((String) params.get("categoryName")).trim();
            if (!categoryName.isEmpty()) {
                circle.setCategoryId(circleService.createCategory(categoryName));
            }
        }
        // 处理类型
        Object typeObj = params.get("type");
        if (typeObj != null) {
            circle.setType("private".equals(typeObj.toString()) ? 1 : 0);
        }
        Circle updated = circleService.update(circle);
        return Result.success(updated);
    }

    /**
     * 退出圈子
     *
     * @param circleId 圈子ID
     * @return 操作结果
     */
    @Operation(summary = "退出圈子")
    @PostMapping("/leave/{circleId}")
    public Result<Void> leave(@PathVariable Long circleId) {
        circleService.leave(circleId, UserContext.getUserId());
        return Result.success();
    }

    /**
     * 重新提交圈子创建（被驳回后重新提交）
     *
     * @param id 圈子ID
     * @return 操作结果
     */
    @Operation(summary = "重新提交圈子创建")
    @PostMapping("/resubmit/{id}")
    public Result<Void> resubmit(@PathVariable Long id) {
        circleService.resubmitCreation(id, UserContext.getUserId());
        return Result.success();
    }

    /**
     * 查询圈内动态（分页）
     *
     * <p>功能描述：获取指定圈子内已审核通过的帖子列表，支持分页</p>
     *
     * @param id      圈子ID（路径参数）
     * @param current 当前页码，默认为1
     * @param size    每页大小，默认为10
     * @return 分页帖子结果
     */
    @Operation(summary = "查询圈内动态")
    @GetMapping("/{id}/posts")
    public Result<Page<Post>> getCirclePosts(@PathVariable Long id,
                                             @RequestParam(defaultValue = "1") long current,
                                             @RequestParam(defaultValue = "10") long size) {
        return Result.success(circleService.getCirclePosts(id, current, size));
    }

    /**
     * 获取圈子分类列表
     *
     * <p>功能描述：返回所有启用的圈子分类（含ID和名称）</p>
     *
     * @return 分类列表
     */
    @Operation(summary = "获取圈子分类列表")
    @GetMapping("/categories")
    public Result<List<CircleCategory>> getCategories() {
        return Result.success(circleService.getCategories());
    }

    /**
     * 获取推荐圈子列表
     *
     * <p>功能描述：返回成员数最多的前10个推荐圈子</p>
     *
     * @return 推荐圈子列表
     */
    @Operation(summary = "推荐圈子")
    @GetMapping("/recommend")
    public Result<List<Circle>> recommend() {
        return Result.success(circleService.recommendCircles());
    }
}
