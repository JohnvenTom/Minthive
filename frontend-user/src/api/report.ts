/**
 * 举报 API
 * @module api/report
 */

import { request } from './request'

/**
 * 发起举报
 * @param {object} data
 * @param {number} data.targetId - 被举报内容 ID
 * @param {'post' | 'comment' | 'message' | 'user'} data.targetType
 * @param {'porn' | 'ad' | 'attack' | 'illegal' | 'copy'} data.type - 举报类型
 * @param {string} data.reason - 举报理由
 */
export function report(data: {
  targetId: number
  targetType: 'post' | 'comment' | 'message' | 'user'
  type: 'porn' | 'ad' | 'attack' | 'illegal' | 'copy'
  reason: string
}) {
  return request({ url: '/report', method: 'post', data })
}
