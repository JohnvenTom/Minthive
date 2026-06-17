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
  replyTo?: string
  content: string
  images: string[]
  likeCount: number
  liked: boolean
  aiGenerated: boolean
  createTime: string
  children?: Comment[]
}

// ---------- 圈子 ----------
export interface Circle {
  id: number
  ownerId: number
  ownerName: string
  name: string
  category: string
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
  type: 'like' | 'comment' | 'follow' | 'circle' | 'system'
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
