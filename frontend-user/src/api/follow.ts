/**
 * 关注关系 API
 * @module api/follow
 */

import { request, mappedRequest } from './request'
import { normalizeUser, type UserResponse } from './normalize'
import { requestUserPage } from './user'

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
 * 查询是否已关注
 * @param {number} userId
 */
export function checkFollowStatus(userId: number) {
  return request<boolean>({ url: `/follow/${userId}`, method: 'get' })
}

/**
 * 关注列表
 * @param {number} userId
 */
export function getFollowing(userId: number, page = 1, pageSize = 20) {
  return requestUserPage({
    url: `/follow/${userId}/following`,
    method: 'get',
    params: { current: page, size: pageSize }
  })
}

/**
 * 粉丝列表
 * @param {number} userId
 */
export function getFollowers(userId: number, page = 1, pageSize = 20) {
  return requestUserPage({
    url: `/follow/${userId}/followers`,
    method: 'get',
    params: { current: page, size: pageSize }
  })
}

/**
 * AI 推荐好友
 */
export function getRecommendUsers() {
  return mappedRequest({ url: '/follow/recommend', method: 'get' }, (users: UserResponse[]) => users.map(normalizeUser))
}
