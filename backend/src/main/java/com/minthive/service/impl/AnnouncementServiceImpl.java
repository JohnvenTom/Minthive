package com.minthive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.entity.Announcement;
import com.minthive.mapper.AnnouncementMapper;
import com.minthive.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统公告服务实现类
 *
 * <p>功能描述：实现公告的持久化增删改查，支持管理端分页管理和用户端公开查询</p>
 */
@RequiredArgsConstructor
@Service
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementMapper announcementMapper;

    /**
     * 分页查询公告列表（管理端用）
     *
     * @param current 当前页码
     * @param size    每页大小
     * @return 按发布时间倒序的分页结果
     */
    @Override
    public Page<Announcement> page(long current, long size) {
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<Announcement>()
                .orderByDesc(Announcement::getPublishTime);
        return announcementMapper.selectPage(new Page<>(current, size), wrapper);
    }

    /**
     * 查询所有启用的公告（用户端展示用）
     *
     * @return 启用状态公告列表，按发布时间倒序
     */
    @Override
    public List<Announcement> listActive() {
        return announcementMapper.selectList(
                new LambdaQueryWrapper<Announcement>()
                        .eq(Announcement::getStatus, 1)
                        .orderByDesc(Announcement::getPublishTime)
        );
    }

    /**
     * 获取最新一条启用公告（首页横幅用）
     *
     * @return 最新启用的公告，无则返回null
     */
    @Override
    public Announcement getLatestActive() {
        List<Announcement> list = announcementMapper.selectList(
                new LambdaQueryWrapper<Announcement>()
                        .eq(Announcement::getStatus, 1)
                        .orderByDesc(Announcement::getPublishTime)
                        .last("LIMIT 1")
        );
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 新增公告
     *
     * @param announcement 公告实体（title、content、status）
     * @return 新增后的公告（含自增ID和发布时间）
     */
    @Override
    public Announcement add(Announcement announcement) {
        announcement.setPublishTime(LocalDateTime.now());
        announcementMapper.insert(announcement);
        return announcement;
    }

    /**
     * 更新公告
     *
     * @param announcement 公告实体（必须包含id）
     * @return 更新后的公告
     */
    @Override
    public Announcement update(Announcement announcement) {
        announcementMapper.updateById(announcement);
        return announcement;
    }

    /**
     * 根据ID删除公告
     *
     * @param id 公告ID
     */
    @Override
    public void deleteById(Long id) {
        announcementMapper.deleteById(id);
    }
}
