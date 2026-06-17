/**
 * 关注关系 API
 * @module api/follow
 */

import { request } from './request'
import type { User, PageResult } from '@/types'

/**
 * 关注/取消关注
 * @param {number} userId
 * @param {boolean} follow
 */
export function toggleFollow(userId: number, follow: boolean) {
  return request({
    url: `/follow/${userId}`,
    method: follow ? 'post' : 'delete'
  })
}

/**
 * 关注列表
 * @param {number} userId
 */
export function getFollowing(userId: number, page = 1, pageSize = 20) {
  return request<PageResult<User>>({
    url: `/follow/${userId}/following`,
    method: 'get',
    params: { page, pageSize }
  })
}

/**
 * 粉丝列表
 * @param {number} userId
 */
export function getFollowers(userId: number, page = 1, pageSize = 20) {
  return request<PageResult<User>>({
    url: `/follow/${userId}/followers`,
    method: 'get',
    params: { page, pageSize }
  })
}

/**
 * AI 推荐好友
 */
export function getRecommendUsers() {
  return request<User[]>({ url: '/follow/recommend', method: 'get' })
}
