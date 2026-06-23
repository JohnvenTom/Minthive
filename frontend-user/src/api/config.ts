import { request } from './request'

/**
 * 首页轮播图数据结构
 */
export interface Banner {
  id: number
  title: string
  imageUrl: string
  linkUrl: string
  sort: number
  status: string
}

/**
 * 获取首页轮播图列表（仅返回启用状态，按排序升序）
 *
 * @returns {Promise<Banner[]>} 启用状态的轮播图数组
 */
export function getBanners() {
  return request<Banner[]>({ url: '/config/banners', method: 'get' })
}
