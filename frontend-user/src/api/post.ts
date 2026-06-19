/**
 * 帖子相关 API
 * @module api/post
 */

import { request } from './request'
import type { Post, PageResult } from '@/types'

/**
 * 获取首页信息流
 * @param {'recommend' | 'latest' | 'hot'} mode - 推荐模式
 * @param {number} current 当前页码
 * @param {number} size 每页大小
 */
export function getFeed(mode: 'recommend' | 'latest' | 'hot' = 'recommend', current = 1, size = 10) {
  return request<PageResult<Post>>({
    url: '/post/feed',
    method: 'get',
    params: { sortType: mode, current, size }
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
 * 编辑帖子（修改内容、图片、可见性）
 *
 * @param {number} id - 帖子ID
 * @param {object} data - 更新字段
 * @param {string} [data.content] - 新的文案内容
 * @param {string} [data.images] - 新的图片地址(JSON数组字符串)
 * @param {number} [data.visibility] - 可见性(0=公开 1=仅粉丝 2=仅自己)
 * @returns {Promise<Post>} 更新后的帖子
 */
export function updatePost(id: number, data: { content?: string; images?: string; visibility?: number }) {
  return request<Post>({ url: `/post/${id}`, method: 'put', data })
}

/**
 * 切换帖子可见性
 *
 * @param {number} id - 帖子ID
 * @param {number} visibility - 目标可见性(0=公开 1=仅粉丝 2=隐藏)
 * @returns {Promise<Post>} 更新后的帖子
 */
export function togglePostVisibility(id: number, visibility: number) {
  return request<Post>({ url: `/post/${id}/visibility`, method: 'patch', params: { visibility } })
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
 *
 * @param {number} id - 原帖ID
 * @param {string} [content] - 用户填写的转发文案（可选，不传则由后端生成默认文案）
 * @returns {Promise<Post>} 创建的转发帖子
 */
export function sharePost(id: number, content?: string) {
  return request<Post>({
    url: `/post/${id}/share`,
    method: 'post',
    data: content ? { content } : undefined
  })
}

/**
 * 获取帖子转发列表（转发链）
 *
 * @param {number} id - 原帖ID
 * @param {number} current - 页码（默认1）
 * @param {number} size - 每页大小（默认20）
 * @returns {Promise<PageResult<Post>>} 分页的转发帖子列表（含转发者昵称、头像）
 */
export function getShareChain(id: number, current = 1, size = 20) {
  return request<PageResult<Post>>({
    url: `/post/${id}/shares`,
    method: 'get',
    params: { current, size }
  })
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
