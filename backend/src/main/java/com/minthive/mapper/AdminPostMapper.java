package com.minthive.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * 管理后台-内容审核 Mapper
 * <p>提供关联用户表+圈子表查询，补全发布者昵称、头像、圈子名称等字段，对齐前端 PostInfo 类型</p>
 */
@Mapper
public interface AdminPostMapper {

    /**
     * 分页查询待审核帖子（关联用户 + 圈子）
     *
     * @param page    分页参数
     * @param keyword 搜索关键词
     * @return 分页结果
     */
    IPage<Map<String, Object>> selectPendingPostList(
            Page<Map<String, Object>> page,
            @Param("keyword") String keyword);

    /**
     * 分页查询已发布帖子（关联用户 + 圈子）
     *
     * @param page    分页参数
     * @param keyword 搜索关键词
     * @return 分页结果
     */
    IPage<Map<String, Object>> selectPublishedPostList(
            Page<Map<String, Object>> page,
            @Param("keyword") String keyword);
}
