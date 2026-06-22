/**
 * 圈子相关 API
 * @module api/circle
 */

import { request } from './request'
import type { Circle, CircleCategory, CircleMember, Post, PageResult } from '@/types'

/**
 * 圈子广场列表
 * @param {object} params - keyword, categoryId, page, pageSize
 */
export function getCircles(params: {
  keyword?: string
  categoryId?: number
  page?: number
  pageSize?: number
}) {
  return request<PageResult<Circle>>({
    url: '/circle/page',
    method: 'get',
    params: { current: params.page, size: params.pageSize, categoryId: params.categoryId, keyword: params.keyword }
  })
}

/**
 * 圈子详情
 * @param {number} id
 */
export function getCircleDetail(id: number) {
  return request<Circle>({ url: `/circle/${id}`, method: 'get' })
}

/**
 * 圈内动态
 * @param {number} id
 * @param {number} page
 */
export function getCirclePosts(id: number, page = 1, pageSize = 10) {
  return request<PageResult<Post>>({
    url: `/circle/${id}/posts`,
    method: 'get',
    params: { page, pageSize }
  })
}

/**
 * 加入圈子
 * @param {number} id
 * @param {string} reason - 私密圈申请理由
 */
export function joinCircle(id: number, reason?: string) {
  return request({ url: `/circle/join/${id}`, method: 'post', data: { reason } })
}

/**
 * 退出圈子
 * @param {number} id
 */
export function leaveCircle(id: number) {
  return request({ url: `/circle/leave/${id}`, method: 'post' })
}

/**
 * 申请创建圈子
 * @param {object} data - categoryId(预设分类ID) 或 categoryName(自定义分类名称)
 */
export function createCircle(data: {
  name: string
  categoryId?: number
  categoryName?: string
  intro: string
  avatar?: string
  banner?: string
  type: 'public' | 'private'
}) {
  return request({ url: '/circle', method: 'post', data })
}

/**
 * 更新圈子信息（仅圈主可操作）
 * @param {number} id - 圈子ID
 * @param {object} data - 可更新的字段（name, categoryId, intro, avatar, banner, type）
 */
export function updateCircle(id: number, data: {
  name?: string
  categoryId?: number
  categoryName?: string
  intro?: string
  avatar?: string
  banner?: string
  type?: 'public' | 'private'
}) {
  return request<Circle>({ url: `/circle/${id}`, method: 'put', data })
}

/**
 * 获取圈子分类列表（含ID和名称）
 */
export function getCategories() {
  return request<CircleCategory[]>({ url: '/circle/categories', method: 'get' })
}

/**
 * 推荐圈子
 */
export function getRecommendCircles() {
  return request<Circle[]>({ url: '/circle/recommend', method: 'get' })
}

/**
 * 圈子正式成员列表（分页，圈主权限）
 * @param {number} circleId - 圈子ID
 * @param {object} params - page, pageSize, keyword
 */
export function getCircleMembers(circleId: number, params: {
  page?: number
  pageSize?: number
  keyword?: string
}) {
  return request<{ list: CircleMember[]; total: number; page: number; pageSize: number }>({
    url: '/circle-admin/members',
    method: 'get',
    params: { circleId, current: params.page, size: params.pageSize, keyword: params.keyword }
  })
}

/**
 * 移出圈子成员（圈主权限，不能移除圈主）
 * @param {number} circleId - 圈子ID
 * @param {number} userId - 被移除的用户ID
 */
export function removeCircleMember(circleId: number, userId: number) {
  return request({
    url: '/circle-admin/member',
    method: 'delete',
    params: { circleId, userId }
  })
}

/**
 * 转让圈主（圈主权限）
 * @param {number} circleId - 圈子ID
 * @param {number} newOwnerId - 新圈主用户ID
 */
export function transferCircleOwner(circleId: number, newOwnerId: number) {
  return request({
    url: '/circle-admin/transfer',
    method: 'post',
    data: { circleId, newOwnerId }
  })
}

/**
 * 发布/更新圈子公告（圈主权限）
 * @param {number} circleId - 圈子ID
 * @param {string} notice - 公告内容
 */
export function updateCircleNotice(circleId: number, notice: string) {
  return request({
    url: '/circle-admin/notice',
    method: 'put',
    params: { circleId, notice }
  })
}
