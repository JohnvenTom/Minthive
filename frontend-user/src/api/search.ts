/**
 * 搜索 API
 * @module api/search
 */

import { request } from './request'
import type { User, Post, Circle, PageResult } from '@/types'

/**
 * 全局搜索
 * @param {string} keyword
 * @param {'all' | 'user' | 'post' | 'circle' | 'topic'} type
 * @param {number} page
 */
export function search(keyword: string, type: 'all' | 'user' | 'post' | 'circle' | 'topic' = 'all', page = 1) {
  return request<PageResult<any>>({
    url: '/search',
    method: 'get',
    params: { keyword, type, page }
  })
}

/**
 * 搜索用户
 */
export function searchUsers(keyword: string, page = 1) {
  return request<PageResult<User>>({
    url: '/search/users',
    method: 'get',
    params: { keyword, page }
  })
}

/**
 * 搜索帖子
 */
export function searchPosts(keyword: string, page = 1) {
  return request<PageResult<Post>>({
    url: '/search/posts',
    method: 'get',
    params: { keyword, page }
  })
}

/**
 * 搜索圈子
 */
export function searchCircles(keyword: string, page = 1) {
  return request<PageResult<Circle>>({
    url: '/search/circles',
    method: 'get',
    params: { keyword, page }
  })
}

/**
 * 热门搜索词
 */
export function getHotKeywords() {
  return request<string[]>({ url: '/search/hot', method: 'get' })
}
