/**
 * 文件上传 API
 * @module api/file
 */

import { request } from './request'

/**
 * 上传图片
 * @param {File} file - 图片文件
 * @returns {Promise<string>} 图片访问 URL（后端 data 直接返回 URL 字符串）
 * @note 不手动设置 Content-Type，由浏览器自动生成含 boundary 的 multipart 头
 */
export function uploadImage(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request<string>({
    url: '/file/upload',
    method: 'post',
    data: formData
  })
}

/**
 * 上传视频
 * @param {File} file - 视频文件
 * @returns {Promise<string>} 视频访问 URL（后端 data 直接返回 URL 字符串）
 * @note 不手动设置 Content-Type，由浏览器自动生成含 boundary 的 multipart 头
 */
export function uploadVideo(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request<string>({
    url: '/file/upload',
    method: 'post',
    data: formData,
    timeout: 60000
  })
}

/**
 * 删除已上传的文件
 * @param {string} url - 文件访问 URL
 */
export function deleteFile(url: string) {
  return request<void>({
    url: '/file/delete',
    method: 'delete',
    params: { url }
  })
}
