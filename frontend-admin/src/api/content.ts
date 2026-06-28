import { get, post } from './request'
import type { PageQuery, PageResult } from './types'

/**
 * 内容审核 API
 * 功能：待审核列表、通过驳回、已发布管理
 */

/** 内容审核查询参数 */
export interface ContentQuery extends PageQuery {
  status?: string
  type?: string
}

/** 帖子内容 */
export interface PostInfo {
  postId: number
  userId: number
  nickname: string
  avatar: string
  content: string
  images: string[]
  video?: string
  status: string
  auditReason?: string
  likeCount: number
  commentCount: number
  circleId?: number
  circleName?: string
  aiGenerated: boolean
  createTime: string
}

/**
 * 查询待审核内容列表
 */
export function getPendingList(params: ContentQuery) {
  return get<PageResult<PostInfo>>('/content/pending', params)
}

/**
 * 查询已发布内容列表
 */
export function getPublishedList(params: ContentQuery) {
  return get<PageResult<PostInfo>>('/content/published', params)
}

/**
 * 审核通过
 * @param postId 帖子ID
 */
export function approvePost(postId: number) {
  return post('/content/approve', { postId })
}

/**
 * 审核驳回
 * @param postId 帖子ID
 * @param reason 驳回原因
 */
export function rejectPost(postId: number, reason: string) {
  return post('/content/reject', { postId, reason })
}

/**
 * 删除已发布内容
 * @param postId 帖子ID
 */
export function deletePost(postId: number) {
  return del('/content/delete', { postId })
}


