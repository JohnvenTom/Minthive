/**
 * 帖子相关 API
 * @module api/post
 */

import { request } from './request'
import type { Post, PageResult } from '@/types'

/**
 * 获取首页信息流
 * @param {'recommend' | 'latest' | 'hot'} mode - 推荐模式
 * @param {number} page
 * @param {number} pageSize
 */
export function getFeed(mode: 'recommend' | 'latest' | 'hot' = 'recommend', page = 1, pageSize = 10) {
  return request<PageResult<Post>>({
    url: '/post/feed',
    method: 'get',
    params: { mode, page, pageSize }
  })
}

/**
 * 获取帖子详情
 * @param {number} id
 */
export function getPostDetail(id: number) {
  return request<Post>({ url: `/post/${id}`, method: 'get' })
}

/**
 * 发布帖子
 * @param {object} data
 */
export function createPost(data: {
  content: string
  images?: string[]
  video?: string
  topics?: string[]
  visibility?: 'public' | 'fans' | 'self'
  circleId?: number
  aiGenerated?: boolean
}) {
  return request<Post>({ url: '/post', method: 'post', data })
}

/**
 * 删除帖子
 * @param {number} id
 */
export function deletePost(id: number) {
  return request({ url: `/post/${id}`, method: 'delete' })
}

/**
 * 点赞/取消点赞
 * @param {number} id
 * @param {boolean} liked
 */
export function toggleLike(id: number, liked: boolean) {
  return request({
    url: `/post/${id}/like`,
    method: liked ? 'post' : 'delete'
  })
}

/**
 * 收藏/取消收藏
 * @param {number} id
 * @param {boolean} collected
 */
export function toggleCollect(id: number, collected: boolean) {
  return request({
    url: `/post/${id}/collect`,
    method: collected ? 'post' : 'delete'
  })
}

/**
 * 转发帖子
 * @param {number} id
 */
export function sharePost(id: number) {
  return request({ url: `/post/${id}/share`, method: 'post' })
}

/**
 * 保存草稿
 * @param {object} data
 */
export function saveDraft(data: Partial<Post> & { id?: number }) {
  return request({ url: '/post/draft', method: 'post', data })
}

/**
 * 获取草稿列表
 */
export function getDrafts() {
  return request<Post[]>({ url: '/post/draft', method: 'get' })
}
