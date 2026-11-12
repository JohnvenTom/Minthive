/** 搜索 API：聚合结果与分页结果分别建模。 */
import { request, mappedRequest } from './request'
import { requestUserPage } from './user'
import { requestPostPage } from './post'
import { requestCirclePage } from './circle'
import { normalizeUser, normalizePost, normalizeCircle, type UserResponse, type PostResponse, type CircleResponse } from './normalize'
import type { ApiResponse, User, Post, Circle, PageResult } from '@/types'

export interface SearchResults {
  users: User[]
  posts: Post[]
  circles: Circle[]
}

export type SearchType = 'all' | 'user' | 'post' | 'circle' | 'topic'
type SearchData = SearchResults | PageResult<User> | PageResult<Post> | PageResult<Circle> | PageResult<never>

export function search(keyword: string, type?: 'all', page?: number): Promise<ApiResponse<SearchResults>>
export function search(keyword: string, type: 'user', page?: number): Promise<ApiResponse<PageResult<User>>>
export function search(keyword: string, type: 'post', page?: number): Promise<ApiResponse<PageResult<Post>>>
export function search(keyword: string, type: 'circle', page?: number): Promise<ApiResponse<PageResult<Circle>>>
export function search(keyword: string, type: 'topic', page?: number): Promise<ApiResponse<PageResult<never>>>
export function search(keyword: string, type: SearchType, page?: number): Promise<ApiResponse<SearchData>>
export function search(keyword: string, type: SearchType = 'all', page = 1): Promise<ApiResponse<SearchData>> {
  if (type === 'user') return searchUsers(keyword, page)
  if (type === 'post') return searchPosts(keyword, page)
  if (type === 'circle') return searchCircles(keyword, page)
  const config = { url: '/search', method: 'get', params: { keyword, type, current: page } }
  if (type === 'topic') return request<PageResult<never>>(config)
  return mappedRequest(config, (data: { users: UserResponse[]; posts: PostResponse[]; circles: CircleResponse[] }): SearchResults => ({
    users: data.users.map(normalizeUser),
    posts: data.posts.map(normalizePost),
    circles: data.circles.map(normalizeCircle)
  }))
}

export function searchUsers(keyword: string, page = 1) {
  return requestUserPage({ url: '/search/user', method: 'get', params: { keyword, current: page } })
}

export function searchPosts(keyword: string, page = 1) {
  return requestPostPage({ url: '/search/post', method: 'get', params: { keyword, current: page } })
}

export function searchCircles(keyword: string, page = 1) {
  return requestCirclePage({ url: '/search/circle', method: 'get', params: { keyword, current: page } })
}

export function getHotKeywords() {
  return request<string[]>({ url: '/search/hot', method: 'get' })
}
