import { describe, expect, it } from 'vitest'
import {
  normalizeAiDrafts,
  normalizePagination,
  normalizePost,
  normalizeUser,
  stringList,
  type PostResponse,
  type UserResponse
} from '@/api/normalize'
import type { PageResult } from '@/types'

/**
 * 测试用最小帖子响应，只构造断言涉及的字段，其余通过类型断言忽略
 *
 * @param overrides 需要覆盖的字段
 * @returns 后端 DTO 形式的帖子
 */
function postResponse(overrides: Partial<PostResponse>): PostResponse {
  return overrides as unknown as PostResponse
}

describe('normalizePagination', () => {
  it('把 MyBatis 的 records 分页转成 PageResult', () => {
    const raw = { records: [{ id: 1 }, { id: 2 }], total: 12, current: 2, size: 10 }

    const page = normalizePagination(raw) as PageResult<{ id: number }>

    expect(page.list).toEqual([{ id: 1 }, { id: 2 }])
    expect(page.total).toBe(12)
    expect(page.page).toBe(2)
    expect(page.pageSize).toBe(10)
    expect(page.hasMore).toBe(false)
  })

  it('把手工 list 分页转成 PageResult 并计算 hasMore', () => {
    const raw = { list: [1, 2, 3], total: 30, page: 1, pageSize: 10 }

    const page = normalizePagination(raw) as PageResult<number>

    expect(page.page).toBe(1)
    expect(page.pageSize).toBe(10)
    expect(page.hasMore).toBe(true)
  })

  it('缺少 total 时原样返回，避免误伤非分页响应', () => {
    const raw = { list: [1, 2], page: 1 }

    expect(normalizePagination(raw)).toEqual(raw)
    expect(normalizePagination(null)).toBeNull()
    expect(normalizePagination('text')).toBe('text')
  })
})

describe('stringList', () => {
  it('解析 JSON 数组、逗号分隔与空值', () => {
    expect(stringList('["a","b"]')).toEqual(['a', 'b'])
    expect(stringList(' a , b ,, ')).toEqual(['a', 'b'])
    expect(stringList(['x'])).toEqual(['x'])
    expect(stringList(null)).toEqual([])
  })

  it('JSON 非法时返回空数组而不是抛错', () => {
    expect(stringList('[broken')).toEqual([])
    expect(stringList('[1,2]')).toEqual([])
  })
})

describe('normalizePost', () => {
  it('把图片/标签字符串转数组并补齐计数默认值', () => {
    const post = normalizePost(postResponse({
      id: 7,
      images: '["http://a/1.png"]',
      tags: '旅行,摄影',
      aiGenerated: 1
    }))

    expect(post.images).toEqual(['http://a/1.png'])
    expect(post.topics).toEqual(['旅行', '摄影'])
    expect(post.aiGenerated).toBe(true)
    expect(post.likeCount).toBe(0)
    expect(post.commentCount).toBe(0)
    expect(post.liked).toBe(false)
    expect(post.collected).toBe(false)
  })

  it('递归归一化转发原帖并保留已有计数', () => {
    const post = normalizePost(postResponse({
      id: 8,
      images: null,
      tags: null,
      aiGenerated: 0,
      likeCount: 5,
      originalPost: postResponse({ id: 9, images: '["o.png"]', tags: null, aiGenerated: 0 })
    }))

    expect(post.images).toEqual([])
    expect(post.aiGenerated).toBe(false)
    expect(post.likeCount).toBe(5)
    expect(post.originalPost?.id).toBe(9)
    expect(post.originalPost?.images).toEqual(['o.png'])
  })
})

describe('normalizeAiDrafts', () => {
  it('直接使用后端给出的风格名与正文，不再按下标猜测', () => {
    const drafts = normalizeAiDrafts([
      { style: 'vibe', styleName: '氛围', content: '山风拂面，一切都慢了下来。', topics: ['爬山'] },
      { style: 'simple', styleName: '简约', content: '周末爬山，放松心情。', topics: [] }
    ])

    expect(drafts).toHaveLength(2)
    expect(drafts[0]).toMatchObject({ style: 'vibe', styleName: '氛围', content: '山风拂面，一切都慢了下来。' })
    expect(drafts[0].topics).toEqual(['爬山'])
    expect(drafts[1].styleName).toBe('简约')
  })

  it('丢弃空正文与非法条目，并为缺失风格补齐默认值', () => {
    const drafts = normalizeAiDrafts([
      { content: '   ' },
      { content: '有内容但没有风格' },
      { style: 'pro', content: '干货正文', topics: ['路线', ''] }
    ])

    expect(drafts).toHaveLength(2)
    expect(drafts[0].style).toBe('simple')
    expect(drafts[0].styleName).toBe('简约')
    expect(drafts[1].topics).toEqual(['路线'])
  })

  it('非数组输入返回空列表', () => {
    expect(normalizeAiDrafts(null)).toEqual([])
    expect(normalizeAiDrafts('文本')).toEqual([])
  })
})

describe('normalizeUser', () => {
  it('把 interestTags 转成 interests 并补齐 birthday 与计数', () => {
    const user = normalizeUser({
      id: 1,
      interestTags: '["音乐","徒步"]',
      birthday: null
    } as unknown as UserResponse)

    expect(user.interests).toEqual(['音乐', '徒步'])
    expect(user.birthday).toBe('')
    expect(user.postCount).toBe(0)
    expect(user.followCount).toBe(0)
    expect(user.fanCount).toBe(0)
    expect(user.isFollowing).toBe(false)
  })
})
