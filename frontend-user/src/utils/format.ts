/**
 * 格式化工具集
 * @module utils/format
 */

import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

/**
 * 格式化时间为相对时间（如"3小时前"）
 * @param {string | number | Date} time - 时间戳或日期字符串
 * @returns {string} 相对时间描述
 */
export function formatRelativeTime(time: string | number | Date): string {
  if (!time) return ''
  const t = dayjs(time)
  const diff = Date.now() - t.valueOf()
  // 7天内显示相对时间，超过则显示日期
  if (diff < 7 * 24 * 3600 * 1000) {
    return t.fromNow()
  }
  return t.format('YYYY-MM-DD')
}

/**
 * 格式化为完整日期时间
 * @param {string | number | Date} time
 * @param {string} fmt - dayjs 格式
 * @returns {string}
 */
export function formatDateTime(time: string | number | Date, fmt = 'YYYY-MM-DD HH:mm'): string {
  if (!time) return ''
  return dayjs(time).format(fmt)
}

/**
 * 数字友好化（1.2w、3.5k）
 * @param {number} num
 * @returns {string}
 */
export function formatNumber(num: number): string {
  if (num === null || num === undefined) return '0'
  if (num >= 10000) return (num / 10000).toFixed(1).replace(/\.0$/, '') + 'w'
  if (num >= 1000) return (num / 1000).toFixed(1).replace(/\.0$/, '') + 'k'
  return String(num)
}

/**
 * 文件大小格式化
 * @param {number} bytes
 * @returns {string}
 */
export function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

/**
 * 截断文本
 * @param {string} text
 * @param {number} max - 最大长度
 * @returns {string}
 */
export function truncate(text: string, max = 100): string {
  if (!text) return ''
  return text.length > max ? text.slice(0, max) + '...' : text
}

/**
 * 高亮关键词
 * @param {string} text - 原文
 * @param {string} keyword - 关键词
 * @returns {string} HTML 字符串
 */
export function highlightKeyword(text: string, keyword: string): string {
  if (!keyword || !text) return text
  const reg = new RegExp(`(${keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi')
  return text.replace(reg, '<span class="hl">$1</span>')
}
