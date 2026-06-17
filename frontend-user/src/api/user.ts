/**
 * 用户相关 API
 * @module api/user
 */

import { request } from './request'
import type { User, Post, PageResult } from '@/types'

/**
 * 获取用户主页信息
 * @param {number} userId
 */
export function getUserProfile(userId: number) {
  return request<User>({ url: `/user/${userId}`, method: 'get' })
}

/**
 * 更新个人资料
 * @param {Partial<User>} data
 */
export function updateProfile(data: Partial<User>) {
  return request<User>({ url: '/user/profile', method: 'put', data })
}

/**
 * 更新头像
 * @param {string} avatar - 头像 URL
 */
export function updateAvatar(avatar: string) {
  return request({ url: '/user/avatar', method: 'put', data: { avatar } })
}

/**
 * 更新兴趣标签
 * @param {string[]} interests
 */
export function updateInterests(interests: string[]) {
  return request({ url: '/user/interests', method: 'put', data: { interests } })
}

/**
 * 获取用户发布的动态
 * @param {number} userId
 * @param {number} page
 * @param {number} pageSize
 */
export function getUserPosts(userId: number, page = 1, pageSize = 10) {
  return request<PageResult<Post>>({
    url: `/user/${userId}/posts`,
    method: 'get',
    params: { page, pageSize }
  })
}

/**
 * 获取用户收藏的动态
 */
export function getUserCollects(page = 1, pageSize = 10) {
  return request<PageResult<Post>>({
    url: '/user/collects',
    method: 'get',
    params: { page, pageSize }
  })
}

/**
 * 获取用户点赞的动态
 */
export function getUserLikes(page = 1, pageSize = 10) {
  return request<PageResult<Post>>({
    url: '/user/likes',
    method: 'get',
    params: { page, pageSize }
  })
}

/**
 * 账号注销（7天冷静期）
 */
export function deactivateAccount() {
  return request({ url: '/user/deactivate', method: 'post' })
}
