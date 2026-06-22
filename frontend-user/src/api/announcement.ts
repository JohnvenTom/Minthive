/**
 * 系统公告相关 API
 * @module api/announcement
 * @description 用户端公开接口，无需登录即可访问
 */

import { request } from './request'

/** 系统公告 */
export interface Announcement {
  id: number
  title: string
  content: string
  status: number
  publishTime: string
}

/**
 * 获取最新一条启用公告（首页横幅用）
 * @returns {Promise<Announcement>} 最新启用的公告
 */
export function getLatestAnnouncement() {
  return request<Announcement>({
    url: '/announcement/latest',
    method: 'get'
  })
}

/**
 * 获取所有启用公告列表（消息中心公告页签用）
 * @returns {Promise<Announcement[]>} 启用状态公告列表
 */
export function getAnnouncementList() {
  return request<Announcement[]>({
    url: '/announcement/list',
    method: 'get'
  })
}
