package com.minthive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minthive.entity.NotificationRead;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 通知已读记录 Mapper 接口
 *
 * <p>功能描述：提供 notification_read 表的基础 CRUD 与幂等写入能力</p>
 */
@Mapper
public interface NotificationReadMapper extends BaseMapper<NotificationRead> {

    /**
     * 幂等写入一条已读记录
     *
     * @param userId 接收通知的用户ID
     * @param type   通知类型（like/comment/follow）
     * @param refId  业务记录ID
     * @return 影响行数，重复标记时返回 0
     * @note 依赖 uk_user_type_ref 唯一键，并发重复标记不会报错也不会产生脏数据
     */
    @Insert("INSERT IGNORE INTO notification_read(user_id, type, ref_id, read_time) "
            + "VALUES(#{userId}, #{type}, #{refId}, NOW())")
    int insertIgnore(@Param("userId") Long userId, @Param("type") String type, @Param("refId") Long refId);
}
