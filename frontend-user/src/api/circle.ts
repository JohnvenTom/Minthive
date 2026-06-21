/**
 * 圈子相关 API
 * @module api/circle
 */

import { request } from './request'
import type { Circle, CircleCategory, Post, PageResult } from '@/types'

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
