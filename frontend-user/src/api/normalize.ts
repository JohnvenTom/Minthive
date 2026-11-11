import type { AiCommentSuggestion, AiPostDraft, Circle, PageResult, Post, User } from '@/types'

/** MyBatis 分页和手工 list 分页统一为页面使用的 PageResult。 */
export function normalizePagination(data: unknown): unknown {
  if (!data || typeof data !== 'object') return data
  if (!('total' in data) || typeof data.total !== 'number') return data
  const list = 'records' in data && Array.isArray(data.records)
    ? data.records
    : 'list' in data && Array.isArray(data.list) ? data.list : undefined
  if (!list) return data
  const page = 'current' in data && typeof data.current === 'number' ? data.current
    : 'page' in data && typeof data.page === 'number' ? data.page : 1
  const pageSize = 'size' in data && typeof data.size === 'number' ? data.size
    : 'pageSize' in data && typeof data.pageSize === 'number' ? data.pageSize : 10
  return { list, total: data.total, page, pageSize, hasMore: page * pageSize < data.total }
}

export function mapPage<T, R>(normalize: (item: T) => R) {
  return (page: PageResult<T>): PageResult<R> => ({ ...page, list: page.list.map(normalize) })
}

/** 数据库中的 JSON 数组和逗号分隔标签只在 API 边界解析。 */
export function stringList(value: string | string[] | null | undefined): string[] {
  if (Array.isArray(value)) return value
  if (!value?.trim()) return []
  if (value.trimStart().startsWith('[')) {
    try {
      const parsed: unknown = JSON.parse(value)
      return Array.isArray(parsed) ? parsed.filter((item): item is string => typeof item === 'string') : []
    } catch {
      return []
    }
  }
  return value.split(',').map(item => item.trim()).filter(Boolean)
}

export type PostResponse = Omit<Post, 'images' | 'topics' | 'aiGenerated' | 'originalPost'> & {
  images: string | null
  tags: string | null
  aiGenerated: 0 | 1 | null
  originalPost?: PostResponse
}

export function normalizePost(post: PostResponse): Post {
  return {
    ...post,
    images: stringList(post.images),
    topics: stringList(post.tags),
    aiGenerated: post.aiGenerated === 1,
    likeCount: post.likeCount ?? 0,
    commentCount: post.commentCount ?? 0,
    shareCount: post.shareCount ?? 0,
    collectCount: post.collectCount ?? 0,
    liked: post.liked ?? false,
    collected: post.collected ?? false,
    originalPost: post.originalPost ? normalizePost(post.originalPost) : undefined
  }
}

export type CircleResponse = Omit<Circle, 'type'> & { type: Circle['type'] | 0 | 1 }

export function normalizeCircle(circle: CircleResponse): Circle {
  return {
    ...circle,
    type: circle.type === 1 || circle.type === 'private' ? 'private' : 'public',
    joined: circle.joined ?? false,
    postCount: circle.postCount ?? 0
  }
}

export type UserResponse = Omit<User, 'interests' | 'birthday'> & {
  interestTags: string | null
  birthday: string | null
}

export function normalizeUser(user: UserResponse): User {
  return {
    ...user,
    interests: stringList(user.interestTags),
    birthday: user.birthday ?? '',
    postCount: user.postCount ?? 0,
    followCount: user.followCount ?? 0,
    fanCount: user.fanCount ?? 0,
    isFollowing: user.isFollowing ?? false
  }
}

export function normalizeAiDrafts(contents: string[]): AiPostDraft[] {
  const styles: AiPostDraft['style'][] = ['simple', 'vibe', 'pro']
  const names = ['简约', '氛围', '干货']
  return contents.map((content, index) => ({
    style: styles[index] ?? 'simple',
    styleName: names[index] ?? `文案${index + 1}`,
    content: content.trim(),
    topics: []
  }))
}

export function normalizeAiComments(contents: string[]): AiCommentSuggestion[] {
  const tones = ['幽默', '共鸣', '提问']
  return contents.map((content, index) => ({ content: content.trim(), tone: tones[index] ?? `建议${index + 1}` }))
}
