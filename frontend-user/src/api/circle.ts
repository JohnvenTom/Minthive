/**
 * 圈子相关 API
 * @module api/circle
 */

import { request } from './request'
import type { Circle, Post, PageResult } from '@/types'

/**
 * 圈子广场列表
 * @param {object} params - keyword, category, page, pageSize
 */
export function getCircles(params: {
  keyword?: string
  category?: string
  page?: number
  pageSize?: number
}) {
  return request<PageResult<Circle>>({
    url: '/circle/list',
    method: 'get',
    params
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
  return request({ url: `/circle/${id}/join`, method: 'post', data: { reason } })
}

/**
 * 退出圈子
 * @param {number} id
 */
export function leaveCircle(id: number) {
  return request({ url: `/circle/${id}/leave`, method: 'post' })
}

/**
 * 申请创建圈子
 * @param {object} data
 */
export function createCircle(data: {
  name: string
  category: string
  intro: string
  avatar?: string
  banner?: string
  type: 'public' | 'private'
}) {
  return request({ url: '/circle', method: 'post', data })
}

/**
 * 获取圈子分类
 */
export function getCategories() {
  return request<string[]>({ url: '/circle/categories', method: 'get' })
}

/**
 * 推荐圈子
 */
export function getRecommendCircles() {
  return request<Circle[]>({ url: '/circle/recommend', method: 'get' })
}
