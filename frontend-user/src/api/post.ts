/**
 * 帖子相关 API
 * @module api/post
 */

import { request, mappedRequest } from './request'
import type { AxiosRequestConfig } from 'axios'
import type { Post } from '@/types'
import { mapPage, normalizePost, type PostResponse } from './normalize'

export function requestPost(config: AxiosRequestConfig) {
  return mappedRequest(config, normalizePost)
}

export function requestPostPage(config: AxiosRequestConfig) {
  return mappedRequest(config, mapPage(normalizePost))
}

/** 写入接口绑定后端 Post 实体，不使用已转换的视图模型。 */
export interface PostInput {
  content: string
  images?: string
  video?: string
  tags?: string
  visibility?: Post['visibility']
  circleId?: number
  aiGenerated?: boolean
}

/**
 * 获取首页信息流
 * @param {'recommend' | 'latest' | 'hot'} mode - 推荐模式
 * @param {number} current 当前页码
 * @param {number} size 每页大小
 */
export function getFeed(mode: 'recommend' | 'latest' | 'hot' = 'recommend', current = 1, size = 10) {
  return requestPostPage({
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
  return requestPost({ url: `/post/${id}`, method: 'get' })
}

/**
 * 发布帖子
 * @param {object} data
 * @param {string} data.content - 文案内容
 * @param {string} [data.images] - 图片地址(JSON数组字符串)
 * @param {string} [data.video] - 视频地址
 * @param {string} [data.tags] - 话题标签(逗号分隔字符串，对应后端 tags 字段)
 * @param {number} [data.visibility] - 可见范围:0公开 1仅粉丝 2仅自己
 * @param {number} [data.circleId] - 圈子ID
 * @param {boolean} [data.aiGenerated] - 是否AI生成
 */
export function createPost(data: PostInput) {
  return requestPost({
    url: '/post', method: 'post',
    data: { ...data, aiGenerated: data.aiGenerated === undefined ? undefined : Number(data.aiGenerated) }
  })
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
export function updatePost(id: number, data: { content?: string; images?: string; visibility?: Post['visibility'] }) {
  return requestPost({ url: `/post/${id}`, method: 'put', data })
}

/**
 * 切换帖子可见性
 *
 * @param {number} id - 帖子ID
 * @param {number} visibility - 目标可见性(0=公开 1=仅粉丝 2=隐藏)
 * @returns {Promise<Post>} 更新后的帖子
 */
export function togglePostVisibility(id: number, visibility: Post['visibility']) {
  return requestPost({ url: `/post/${id}/visibility`, method: 'patch', params: { visibility } })
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
  return requestPost({
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
  return requestPostPage({
    url: `/post/${id}/shares`,
    method: 'get',
    params: { current, size }
  })
}

/**
 * 保存草稿
 * @param {object} data
 */
export function saveDraft(data: Partial<PostInput> & { id?: number }) {
  return requestPost({
    url: '/post/draft', method: 'post',
    data: { ...data, aiGenerated: data.aiGenerated === undefined ? undefined : Number(data.aiGenerated) }
  })
}

/**
 * 获取草稿列表
 */
export function getDrafts() {
  return mappedRequest({ url: '/post/draft', method: 'get' }, (posts: PostResponse[]) => posts.map(normalizePost))
}
