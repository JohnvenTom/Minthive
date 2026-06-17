package com.minthive.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * 管理后台-用户管理 Mapper
 * <p>提供关联查询，补全发帖数/关注数/粉丝数/最后登录时间等字段，对齐前端 UserInfo 类型</p>
 */
@Mapper
public interface AdminUserMapper {

    /**
     * 分页查询用户列表（含统计子查询）
     * <p>返回字段与前端 UserInfo 类型完全对齐</p>
     *
     * @param page    分页参数
     * @param keyword 搜索关键词（匹配账号或昵称）
     * @param status  账号状态（0封禁 1正常）
     * @return 分页结果（每条记录为 Map）
     */
    IPage<Map<String, Object>> selectUserListWithStats(
            Page<Map<String, Object>> page,
            @Param("keyword") String keyword,
            @Param("status") Integer status);
}
