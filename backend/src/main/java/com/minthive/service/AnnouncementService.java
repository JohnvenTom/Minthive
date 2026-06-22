package com.minthive.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.entity.Announcement;

import java.util.List;

/**
 * 系统公告服务接口
 *
 * <p>功能描述：定义系统公告的增删改查及用户端查询方法</p>
 */
public interface AnnouncementService {

    /**
     * 分页查询公告列表（管理端用）
     *
     * @param current 当前页码
     * @param size    每页大小
     * @return 分页结果
     */
    Page<Announcement> page(long current, long size);

    /**
     * 查询所有启用的公告（用户端展示用）
     *
     * @return 启用状态公告列表，按发布时间倒序
     */
    List<Announcement> listActive();

    /**
     * 获取最新一条启用公告（首页横幅用）
     *
     * @return 最新启用的公告，无则返回null
     */
    Announcement getLatestActive();

    /**
     * 新增公告
     *
     * @param announcement 公告实体
     * @return 新增后的公告（含自增ID）
     */
    Announcement add(Announcement announcement);

    /**
     * 更新公告
     *
     * @param announcement 公告实体（必须包含id）
     * @return 更新后的公告
     */
    Announcement update(Announcement announcement);

    /**
     * 根据ID删除公告
     *
     * @param id 公告ID
     */
    void deleteById(Long id);
}
