/**
 * 全局类型定义
 * @module types
 */

// ---------- 通用响应 ----------
export interface ApiResponse<T = any> {
  code: number
  msg: string
  data: T
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
  hasMore: boolean
}

// ---------- 用户 ----------
export interface User {
  id: number
  account: string
  nickname: string
  avatar: string
  bio: string
  gender: 0 | 1 | 2
  birthday: string
  interests: string[]
  postCount: number
  followCount: number
  fanCount: number
  registerTime: string
  status: number
  /** 当前登录用户是否已关注该用户（由后端填充） */
  isFollowing?: boolean
}

// ---------- 帖子 ----------
export interface Post {
  id: number
  userId: number
  nickname: string
  avatar: string
  content: string
  images: string[]
  video: string
  topics: string[]
  visibility: 'public' | 'fans' | 'self'
  circleId: number | null
  circleName?: string
  likeCount: number
  commentCount: number
  shareCount: number
  collectCount: number
  liked: boolean
  collected: boolean
  aiGenerated: boolean
  /** 审核状态：0待审 1通过 2驳回 */
  auditStatus?: number
  /** 转发原帖ID（非空时表示这是转发帖） */
  sharePostId: number | null
  /** 原帖完整信息（仅转发帖时有值，由后端填充） */
  originalPost?: Post
  createTime: string
}

// ---------- 评论 ----------
export interface Comment {
  id: number
  postId: number
  userId: number
  nickname: string
  avatar: string
  parentId: number | null
  replyToId?: number
  replyTo?: string
  content: string
  images: string[]
  likeCount: number
  liked: boolean
  aiGenerated: boolean
  createTime: string
  children?: Comment[]
}

// ---------- 圈子分类 ----------
export interface CircleCategory {
  id: number
  name: string
  sort: number
  status: number
}

// ---------- 圈子 ----------
export interface Circle {
  id: number
  ownerId: number
  ownerName: string
  ownerAvatar: string
  name: string
  categoryId: number
  categoryName?: string
  intro: string
  avatar: string
  banner: string
  type: 'public' | 'private'
  memberCount: number
  postCount: number
  notice: string
  joined: boolean
  status: number
}

// ---------- 圈子成员 ----------
export interface CircleMember {
  /** 用户ID */
  userId: number
  /** 昵称 */
  nickname: string
  /** 头像URL */
  avatar: string
  /** 角色:0普通成员 1圈主 */
  role: 0 | 1
  /** 入圈时间 */
  joinTime: string
}

// ---------- 消息 ----------
export interface Message {
  id: number
  fromUserId: number
  fromNickname: string
  fromAvatar: string
  toUserId: number
  content: string
  type: 'text' | 'image' | 'emoji'
  read: boolean
  aiReply: boolean
  createTime: string
}

export interface Notification {
  id: number
  type: 'like' | 'comment' | 'reply' | 'follow' | 'circle' | 'system' | 'report' | 'audit'
  fromUserId: number
  fromNickname: string
  fromAvatar: string
  targetId: number
  content: string
  read: boolean
  createTime: string
}

// ---------- AI ----------
export interface AiPostDraft {
  style: 'simple' | 'vibe' | 'pro'
  styleName: string
  content: string
  topics: string[]
}

export interface AiCommentSuggestion {
  content: string
  tone: string
}
