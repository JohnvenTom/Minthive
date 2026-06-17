import { get, post, put } from './request'
import type { PageQuery, PageResult } from './types'

/**
 * 圈子管理 API
 * 功能：创建申请审核、编辑、下架、转让圈主、分类、推荐位
 */

/** 圈子信息 */
export interface CircleInfo {
  circleId: number
  ownerId: number
  ownerName: string
  name: string
  category: string
  intro: string
  type: 'PUBLIC' | 'PRIVATE'
  memberCount: number
  postCount: number
  status: string
  recommended: boolean
  icon?: string
  createTime: string
}

/** 圈子申请 */
export interface CircleApply {
  applyId: number
  userId: number
  nickname: string
  name: string
  category: string
  intro: string
  applyTime: string
  status: string
}

/**
 * 查询圈子列表
 */
export function getCircleList(params: PageQuery & { status?: string; category?: string }) {
  return get<PageResult<CircleInfo>>('/circle/list', params)
}

/**
 * 查询创建申请列表
 */
export function getCircleApplyList(params: PageQuery) {
  return get<PageResult<CircleApply>>('/circle/apply-list', params)
}

/**
 * 审核通过圈子创建申请
 */
export function approveCircleApply(applyId: number) {
  return post('/circle/apply/approve', { applyId })
}

/**
 * 驳回圈子创建申请
 */
export function rejectCircleApply(applyId: number, reason: string) {
  return post('/circle/apply/reject', { applyId, reason })
}

/**
 * 编辑圈子信息
 */
export function updateCircle(data: Partial<CircleInfo> & { circleId: number }) {
  return put('/circle/update', data)
}

/**
 * 下架圈子
 */
export function offlineCircle(circleId: number, reason: string) {
  return post('/circle/offline', { circleId, reason })
}

/**
 * 转让圈主
 */
export function transferOwner(circleId: number, newOwnerId: number) {
  return post('/circle/transfer', { circleId, newOwnerId })
}

/**
 * 设置推荐位
 */
export function setRecommend(circleId: number, recommended: boolean) {
  return post('/circle/recommend', { circleId, recommended })
}

/**
 * 获取圈子分类列表
 */
export function getCircleCategories() {
  return get<string[]>('/circle/categories')
}
