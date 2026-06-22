package com.minthive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minthive.entity.Announcement;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统公告 Mapper 接口
 *
 * <p>功能描述：提供 announcement 表的基础 CRUD 操作</p>
 */
@Mapper
public interface AnnouncementMapper extends BaseMapper<Announcement> {
}
