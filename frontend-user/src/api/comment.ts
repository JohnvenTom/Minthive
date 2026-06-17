/**
 * 评论相关 API
 * @module api/comment
 */

import { request } from './request'
import type { Comment, PageResult } from '@/types'

/**
 * 获取帖子评论列表
 * @param {number} postId
 * @param {number} page
 * @param {number} pageSize
 */
export function getComments(postId: number, page = 1, pageSize = 20) {
  return request<PageResult<Comment>>({
    url: `/comment/list`,
    method: 'get',
    params: { postId, page, pageSize }
  })
}

/**
 * 发表评论
 * @param {object} data
 */
export function createComment(data: {
  postId: number
  parentId?: number
  content: string
  images?: string[]
  aiGenerated?: boolean
}) {
  return request<Comment>({ url: '/comment', method: 'post', data })
}

/**
 * 删除评论
 * @param {number} id
 */
export function deleteComment(id: number) {
  return request({ url: `/comment/${id}`, method: 'delete' })
}

/**
 * 评论点赞
 * @param {number} id
 * @param {boolean} liked
 */
export function toggleCommentLike(id: number, liked: boolean) {
  return request({
    url: `/comment/${id}/like`,
    method: liked ? 'post' : 'delete'
  })
}
