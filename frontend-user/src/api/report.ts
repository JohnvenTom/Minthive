/**
 * 举报 API
 * @module api/report
 */

import { request } from './request'

/** targetType 枚举值映射（后端用数字） */
const TARGET_TYPE_MAP = {
  post: 1,
  comment: 2,
  message: 3,
  user: 4
} as const

/**
 * 发起举报
 * @param {object} data - 举报参数
 * @param {number} data.targetId - 被举报内容 ID
 * @param {'post' | 'comment' | 'message' | 'user'} data.targetType - 被举报内容类型
 * @param {string} data.type - 举报类型（中文，如"低俗色情"、"广告引流"、"自定义"等）
 * @param {string} [data.reason] - 举报理由（可选）
 */
export function report(data: {
  targetId: number
  targetType: 'post' | 'comment' | 'message' | 'user'
  type: string
  reason?: string
}) {
  return request({
    url: '/report',
    method: 'post',
    data: {
      targetId: data.targetId,
      targetType: TARGET_TYPE_MAP[data.targetType], // 字符串 → 数字映射
      reportType: data.type,   // 中文举报类型 → 后端 reportType
      reason: data.reason || ''
    }
  })
}
