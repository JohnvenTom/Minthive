package com.minthive.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * 圈子成员管理 Mapper
 *
 * <p>功能描述：提供圈子正式成员的分页查询（关联 user 表补全昵称/头像），供圈主管理使用</p>
 */
@Mapper
public interface CircleMemberMapper {

    /**
     * 分页查询圈子正式成员（audit_status=1）
     *
     * <p>返回字段：userId, nickname, avatar, role(0普通/1圈主), joinTime</p>
     * <p>排序：圈主置顶，其余按入圈时间倒序</p>
     *
     * @param page    分页参数
     * @param circleId 圈子ID
     * @param keyword  昵称关键词（可空）
     * @return 分页成员列表
     */
    IPage<Map<String, Object>> selectMembers(
            Page<Map<String, Object>> page,
            @Param("circleId") Long circleId,
            @Param("keyword") String keyword);
}
