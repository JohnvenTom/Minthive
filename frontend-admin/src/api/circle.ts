import { get, post, put } from './request'
import type { PageQuery, PageResult } from './types'

/**
 * 圈子管理 API
 * 功能：创建申请审核、编辑、下架、转让圈主、分类、推荐位
 */

/** 圈子分类 */
export interface CircleCategory {
  id: number
  name: string
  sort: number
  status: number
}

/** 圈子信息 */
export interface CircleInfo {
  circleId: number
  ownerId: number
  ownerName: string
  name: string
  categoryId: number
  categoryName: string
  intro: string
  type: 'PUBLIC' | 'PRIVATE'
  memberCount: number
  postCount: number
  status: string
  recommended: boolean
  icon?: string
  createTime: string
}

/** 圈子创建申请 */
export interface CircleCreationApply {
  applyId: number
  ownerId: number
  ownerName: string
  name: string
  categoryId: number
  categoryName: string
  intro: string
  avatar: string
  type: 'PUBLIC' | 'PRIVATE'
  applyTime: string
  status: 'PENDING' | 'REJECTED'
}

/**
 * 查询圈子列表
 */
export function getCircleList(params: PageQuery & { status?: string | number; categoryId?: number }) {
  return get<PageResult<CircleInfo>>('/circle/list', params)
}

/**
 * 查询圈子创建申请列表
 */
export function getCircleCreationApplyList(params: PageQuery) {
  return get<PageResult<CircleCreationApply>>('/circle/creation-apply-list', params)
}

/**
 * 审批通过圈子创建
 */
export function approveCircleCreation(circleId: number) {
  return post('/circle/creation-approve', { circleId })
}

/**
 * 驳回圈子创建
 */
export function rejectCircleCreation(circleId: number, reason: string) {
  return post('/circle/creation-reject', { circleId, reason })
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
  return get<CircleCategory[]>('/circle/categories')
}
