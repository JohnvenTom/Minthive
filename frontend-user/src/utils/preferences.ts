/**
 * 用户本地偏好设置
 * @module utils/preferences
 * @description 持久化设置页的通知与 AI 开关，供聊天未读统计、AI 入口等模块复用。
 *   偏好只保存在本机，属于客户端偏好而非账号级配置。
 */

/** 偏好设置结构 */
export interface UserPreferences {
  /** 评论通知 */
  notifyComment: boolean
  /** 点赞通知 */
  notifyLike: boolean
  /** 私信通知 */
  notifyChat: boolean
  /** 圈子公告通知 */
  notifyCircle: boolean
  /** AI 自动代回复私信 */
  aiAutoReply: boolean
  /** AI 发帖助手 */
  aiPostHelper: boolean
  /** AI 评论建议 */
  aiCommentSuggest: boolean
}

/** localStorage 键名 */
const STORAGE_KEY = 'minthive_prefs'

/** 默认偏好：与设置页初始状态保持一致 */
const DEFAULT_PREFERENCES: UserPreferences = {
  notifyComment: true,
  notifyLike: true,
  notifyChat: true,
  notifyCircle: true,
  aiAutoReply: false,
  aiPostHelper: true,
  aiCommentSuggest: true
}

/**
 * 读取本地偏好
 * @returns {UserPreferences} 合并默认值后的偏好；存储损坏时回退默认值
 * @description 只接受布尔类型字段，避免历史脏数据影响开关状态
 */
export function getPreferences(): UserPreferences {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return { ...DEFAULT_PREFERENCES }
    const parsed = JSON.parse(raw) as Record<string, unknown>
    const merged = { ...DEFAULT_PREFERENCES }
    for (const key of Object.keys(DEFAULT_PREFERENCES) as (keyof UserPreferences)[]) {
      if (typeof parsed[key] === 'boolean') merged[key] = parsed[key] as boolean
    }
    return merged
  } catch {
    return { ...DEFAULT_PREFERENCES }
  }
}

/**
 * 保存偏好
 * @param {Partial<UserPreferences>} patch - 需要更新的字段
 * @returns {UserPreferences} 保存后的完整偏好
 * @description 采用合并写入，未传字段保持原值
 */
export function savePreferences(patch: Partial<UserPreferences>): UserPreferences {
  const next = { ...getPreferences(), ...patch }
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
  } catch {
    // 存储不可用（隐私模式/超额）时仅影响持久化，不阻塞页面操作
  }
  return next
}
