/**
 * 文件上传 API
 * @module api/file
 */

import { request } from './request'

/**
 * 上传图片
 * @param {File} file
 * @returns {Promise<{url: string}>} 图片访问 URL
 */
export function uploadImage(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request<{ url: string }>({
    url: '/file/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 上传视频
 * @param {File} file
 */
export function uploadVideo(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request<{ url: string }>({
    url: '/file/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000
  })
}
