<template>
  <div class="search-page">
    <!-- 蜂巢装饰背景 -->
    <div class="hex-bg" />

    <!-- 顶部搜索栏 -->
    <header class="search-header">
      <div class="header-inner">
        <button class="back-btn" @click="goBack">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <path d="M15 18l-6-6 6-6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
        <div class="search-input-wrap">
          <svg class="search-icon" width="18" height="18" viewBox="0 0 24 24" fill="none">
            <circle cx="11" cy="11" r="8" stroke="currentColor" stroke-width="2"/>
            <path d="M21 21l-4.35-4.35" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
          <input
            ref="searchInputRef"
            v-model="keyword"
            type="text"
            class="search-input"
            placeholder="搜索用户、帖子、圈子..."
            @keydown.enter="onSearch"
          />
          <button v-if="keyword" class="clear-btn" @click="clearKeyword">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
              <circle cx="12" cy="12" r="10" fill="currentColor" opacity="0.2"/>
              <path d="M15 9l-6 6M9 9l6 6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </button>
        </div>
        <button class="search-action-btn" @click="onSearch">搜索</button>
      </div>
    </header>

    <!-- 搜索前：历史 + 热门 -->
    <div v-if="!hasSearched" class="pre-search">
      <!-- 搜索历史 -->
      <section v-if="searchHistory.length > 0" class="history-section">
        <div class="section-header">
          <h3 class="section-title font-heading">搜索历史</h3>
          <button class="clear-history-btn" @click="clearHistory">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M3 6h18M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6M8 6V4a2 2 0 012-2h4a2 2 0 012 2v2" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            清空
          </button>
        </div>
        <div class="history-tags">
          <button
            v-for="(item, idx) in searchHistory"
            :key="idx"
            class="history-tag glass-card"
            @click="quickSearch(item)"
          >
            {{ item }}
          </button>
        </div>
      </section>

      <!-- 热门搜索 -->
      <section v-if="hotKeywords.length > 0" class="hot-section">
        <div class="section-header">
          <h3 class="section-title font-heading">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M12 2L14.5 8.5L21 9.5L16.5 14L17.5 21L12 17.5L6.5 21L7.5 14L3 9.5L9.5 8.5L12 2Z" fill="currentColor"/>
            </svg>
            热门搜索
          </h3>
        </div>
        <div class="hot-list">
          <button
            v-for="(word, idx) in hotKeywords"
            :key="idx"
            class="hot-item"
            @click="quickSearch(word)"
          >
            <span :class="['hot-rank', { top: idx < 3 }]">{{ idx + 1 }}</span>
            <span class="hot-word">{{ word }}</span>
          </button>
        </div>
      </section>
    </div>

    <!-- 搜索结果 -->
    <div v-else class="search-results">
      <!-- 结果Tab -->
      <nav class="result-tabs">
        <div class="tabs-inner">
          <button
            v-for="tab in resultTabs"
            :key="tab.value"
            :class="['tab-btn', { active: activeResultTab === tab.value }]"
            @click="switchResultTab(tab.value)"
          >
            <span class="tab-label">{{ tab.label }}</span>
            <span v-if="activeResultTab === tab.value" class="tab-indicator" />
          </button>
        </div>
      </nav>

      <!-- 加载中 -->
      <div v-if="loading && results.length === 0" class="loading-wrap">
        <LoadingSpinner />
      </div>

      <!-- 全部结果 -->
      <div v-else-if="activeResultTab === 'all' && results.length > 0" class="result-list">
        <!-- 用户结果 -->
        <div v-if="userResults.length > 0" class="result-group">
          <h4 class="group-title font-heading">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
              <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2M12 11a4 4 0 100-8 4 4 0 000 8z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            用户
          </h4>
          <UserCard
            v-for="user in userResults.slice(0, 3)"
            :key="user.id"
            :user="user"
            class="result-item"
            @click="onUserClick"
            @follow="onUserFollow"
          />
          <button
            v-if="userResults.length > 3"
            class="view-more-btn"
            @click="switchResultTab('user')"
          >
            查看更多用户
          </button>
        </div>

        <!-- 帖子结果 -->
        <div v-if="postResults.length > 0" class="result-group">
          <h4 class="group-title font-heading">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
              <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M14 2v6h6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            帖子
          </h4>
          <PostCard
            v-for="post in postResults.slice(0, 3)"
            :key="post.id"
            :post="post"
            class="result-item"
            @click="goPostDetail(post.id)"
            @click-user="router.push(`/profile/${$event}`)"
            @like="onPostLike"
            @collect="onPostCollect"
            @openShare="onPostShare"
            @viewShares="onViewShares"
            @select="onPostActionSelect"
          />
          <button
            v-if="postResults.length > 3"
            class="view-more-btn"
            @click="switchResultTab('post')"
          >
            查看更多帖子
          </button>
        </div>

        <!-- 圈子结果 -->
        <div v-if="circleResults.length > 0" class="result-group">
          <h4 class="group-title font-heading">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
              <path d="M12 2L22 8.5V15.5L12 22L2 15.5V8.5L12 2Z" stroke="currentColor" stroke-width="2"/>
            </svg>
            圈子
          </h4>
          <CircleCard
            v-for="circle in circleResults.slice(0, 3)"
            :key="circle.id"
            :circle="circle"
            class="result-item"
            @click="onCircleClick"
          />
          <button
            v-if="circleResults.length > 3"
            class="view-more-btn"
            @click="switchResultTab('circle')"
          >
            查看更多圈子
          </button>
        </div>
      </div>

      <!-- 分类结果 -->
      <div v-else-if="activeResultTab !== 'all' && results.length > 0" class="result-list">
        <template v-if="activeResultTab === 'user'">
          <UserCard
            v-for="user in userResults"
            :key="user.id"
            :user="user"
            class="result-item"
            @click="onUserClick"
            @follow="onUserFollow"
          />
        </template>
        <template v-else-if="activeResultTab === 'post'">
          <PostCard
            v-for="post in postResults"
            :key="post.id"
            :post="post"
            class="result-item"
            @click="goPostDetail(post.id)"
            @click-user="router.push(`/profile/${$event}`)"
            @like="onPostLike"
            @collect="onPostCollect"
            @openShare="onPostShare"
            @viewShares="onViewShares"
            @select="onPostActionSelect"
          />
        </template>
        <template v-else-if="activeResultTab === 'circle'">
          <CircleCard
            v-for="circle in circleResults"
            :key="circle.id"
            :circle="circle"
            class="result-item"
            @click="onCircleClick"
          />
        </template>
      </div>

      <!-- 空结果 -->
      <EmptyState
        v-else-if="!loading"
        title="未找到相关内容"
        description="换个关键词试试"
        icon="search"
      />

      <!-- 加载更多 -->
      <div v-if="results.length > 0" class="load-more-area">
        <LoadingSpinner v-if="loadingMore" />
        <p v-else-if="!hasMore" class="no-more-text">没有更多结果</p>
      </div>
    </div>
  </div>

  <!-- 分享面板 -->
  <ShareSheet
    v-model:show="showShareSheet"
    :post-id="sharePostId"
    @shared="onShared"
  />

  <!-- 转发链弹窗 -->
  <ShareChainDialog
    v-model:show="showShareChain"
    :post-id="shareChainPostId"
    @click-share="router.push(`/post/${$event}`)"
    @click-user="router.push(`/profile/${$event}`)"
  />
</template>

<script setup lang="ts">
/**
 * 搜索页面
 * @description 支持全局搜索，包含搜索历史（本地存储）、热门搜索词、
 *   搜索结果Tab分类（全部/用户/帖子/圈子），清空搜索历史
 */

import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import PostCard from '@/components/PostCard.vue'
import UserCard from '@/components/UserCard.vue'
import CircleCard from '@/components/CircleCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import ShareSheet from '@/components/ShareSheet.vue'
import ShareChainDialog from '@/components/ShareChainDialog.vue'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import { search, searchUsers, searchPosts, searchCircles, getHotKeywords } from '@/api/search'
import { toggleFollow } from '@/api/follow'
import { toggleLike, toggleCollect } from '@/api/post'
import type { User, Post, Circle } from '@/types'

// ---------- 路由 ----------
const router = useRouter()

// ---------- Tab 配置 ----------
interface ResultTab {
  label: string
  value: 'all' | 'user' | 'post' | 'circle'
}

const resultTabs: ResultTab[] = [
  { label: '全部', value: 'all' },
  { label: '用户', value: 'user' },
  { label: '帖子', value: 'post' },
  { label: '圈子', value: 'circle' }
]

// ---------- 常量 ----------
const HISTORY_KEY = 'minthive_search_history'
const MAX_HISTORY = 10

// ---------- 响应式数据 ----------
const keyword = ref('')
const hasSearched = ref(false)
const loading = ref(false)
const loadingMore = ref(false)
const hasMore = ref(true)
const currentPage = ref(1)
const activeResultTab = ref<'all' | 'user' | 'post' | 'circle'>('all')
const searchHistory = ref<string[]>([])
const hotKeywords = ref<string[]>([])

/** 搜索结果 */
const results = ref<any[]>([])
const userResults = ref<User[]>([])
const postResults = ref<Post[]>([])
const circleResults = ref<Circle[]>([])

/** DOM引用 */
const searchInputRef = ref<HTMLInputElement | null>(null)

/** 分享面板状态 */
const showShareSheet = ref(false)
const sharePostId = ref(0)
/** 转发链弹窗状态 */
const showShareChain = ref(false)
const shareChainPostId = ref(0)

// ---------- 搜索历史管理 ----------

/**
 * 从本地存储加载搜索历史
 * @returns {string[]} 搜索历史列表
 */
function loadHistory(): string[] {
  try {
    const data = localStorage.getItem(HISTORY_KEY)
    return data ? JSON.parse(data) : []
  } catch {
    return []
  }
}

/**
 * 保存搜索历史到本地存储
 * @param {string[]} history - 搜索历史列表
 * @returns {void}
 */
function saveHistory(history: string[]): void {
  localStorage.setItem(HISTORY_KEY, JSON.stringify(history))
}

/**
 * 添加搜索关键词到历史
 * @param {string} word - 搜索关键词
 * @returns {void}
 * @description 去重并限制最多10条，最新的排在前面
 */
function addHistory(word: string): void {
  if (!word.trim()) return
  const history = searchHistory.value.filter(h => h !== word.trim())
  history.unshift(word.trim())
  searchHistory.value = history.slice(0, MAX_HISTORY)
  saveHistory(searchHistory.value)
}

/**
 * 清空搜索历史
 * @returns {void}
 */
function clearHistory(): void {
  searchHistory.value = []
  localStorage.removeItem(HISTORY_KEY)
  showToast('已清空搜索历史')
}

// ---------- 数据加载 ----------

/**
 * 加载热门搜索词
 * @returns {Promise<void>}
 * @description 从服务端获取热门搜索关键词
 */
async function fetchHotKeywords(): Promise<void> {
  try {
    const res = await getHotKeywords()
    hotKeywords.value = res.data || []
  } catch {
    // 静默处理
  }
}

/**
 * 执行搜索
 * @returns {Promise<void>}
 * @description 根据当前关键词和结果Tab类型执行搜索
 */
async function onSearch(): Promise<void> {
  const kw = keyword.value.trim()
  if (!kw) return

  hasSearched.value = true
  loading.value = true
  currentPage.value = 1
  hasMore.value = true
  addHistory(kw)

  try {
    if (activeResultTab.value === 'all') {
      const res = await search(kw, 'all', 1)
      const data = res.data
      userResults.value = data?.users || []
      postResults.value = data?.posts || []
      circleResults.value = data?.circles || []
      results.value = [...userResults.value, ...postResults.value, ...circleResults.value]
    } else if (activeResultTab.value === 'user') {
      const res = await searchUsers(kw, 1)
      userResults.value = res.data?.list || []
      results.value = userResults.value
    } else if (activeResultTab.value === 'post') {
      const res = await searchPosts(kw, 1)
      postResults.value = res.data?.list || []
      results.value = postResults.value
    } else if (activeResultTab.value === 'circle') {
      const res = await searchCircles(kw, 1)
      circleResults.value = res.data?.list || []
      results.value = circleResults.value
    }

    hasMore.value = results.value.length >= 10
  } catch {
    showToast('搜索失败')
  } finally {
    loading.value = false
  }
}

/**
 * 快捷搜索
 * @param {string} word - 搜索关键词
 * @returns {void}
 * @description 点击历史或热门标签直接搜索
 */
function quickSearch(word: string): void {
  keyword.value = word
  onSearch()
}

/**
 * 清空搜索关键词
 * @returns {void}
 */
function clearKeyword(): void {
  keyword.value = ''
  hasSearched.value = false
  results.value = []
  userResults.value = []
  postResults.value = []
  circleResults.value = []
  searchInputRef.value?.focus()
}

/**
 * 切换结果Tab
 * @param {'all' | 'user' | 'post' | 'circle'} tab - 目标Tab
 * @returns {void}
 */
function switchResultTab(tab: 'all' | 'user' | 'post' | 'circle'): void {
  if (activeResultTab.value === tab) return
  activeResultTab.value = tab
  onSearch()
}

/**
 * 跳转帖子详情
 * @param {number} id - 帖子ID
 * @returns {void}
 */
function goPostDetail(id: number): void {
  router.push(`/post/${id}`)
}

/**
 * 返回上一页
 * @returns {void}
 */
function goBack(): void {
  router.back()
}

// ---------- 用户交互 ----------

/**
 * 点击用户卡片，跳转个人主页
 * @param {number} userId - 用户ID
 */
function onUserClick(userId: number): void {
  router.push(`/profile/${userId}`)
}

/**
 * 关注/取关用户
 * @param {number} userId - 用户ID
 * @param {boolean} willFollow - true=关注, false=取关
 * @returns {Promise<void>}
 */
async function onUserFollow(userId: number, willFollow: boolean): Promise<void> {
  const user = userResults.value.find(u => u.id === userId)
  if (!user) return
  try {
    await toggleFollow(userId, willFollow)
    user.isFollowing = willFollow
    user.followerCount += willFollow ? 1 : -1
    showToast(willFollow ? '已关注' : '已取消关注')
  } catch {
    showToast('操作失败')
  }
}

/**
 * 点击圈子卡片，跳转圈子详情
 * @param {number} circleId - 圈子ID
 */
function onCircleClick(circleId: number): void {
  router.push(`/circle/${circleId}`)
}

// ---------- 帖子交互 ----------

/**
 * 点赞/取消点赞帖子
 * @param {number} postId - 帖子ID
 */
async function onPostLike(postId: number): Promise<void> {
  const post = postResults.value.find(p => p.id === postId)
  if (!post) return
  try {
    await toggleLike(postId, !post.liked)
    post.liked = !post.liked
    post.likeCount += post.liked ? 1 : -1
  } catch {
    showToast('操作失败')
  }
}

/**
 * 收藏/取消收藏帖子
 * @param {number} postId - 帖子ID
 */
async function onPostCollect(postId: number): Promise<void> {
  const post = postResults.value.find(p => p.id === postId)
  if (!post) return
  try {
    await toggleCollect(postId, !post.collected)
    post.collected = !post.collected
    post.collectCount += post.collected ? 1 : -1
  } catch {
    showToast('操作失败')
  }
}

/**
 * 转发/分享帖子 - 打开分享面板
 * @param {number} postId - 帖子ID
 */
function onPostShare(postId: number): void {
  sharePostId.value = postId
  showShareSheet.value = true
}

/**
 * 查看转发链弹窗
 * @param {number} postId - 帖子ID
 */
function onViewShares(postId: number): void {
  shareChainPostId.value = postId
  showShareChain.value = true
}

/**
 * 分享成功回调
 * @returns {void}
 */
function onShared(): void {
  const post = postResults.value.find(p => p.id === sharePostId.value)
  if (post) post.shareCount++
}

/**
 * 非作者的操作菜单选择（转发等）
 * @param {any} action - 选中的操作项
 */
function onPostActionSelect(action: any): void {
  if (action?.value === 'share') {
    onPostShare(action.postId)
  }
}

// ---------- 生命周期 ----------
onMounted(() => {
  searchHistory.value = loadHistory()
  fetchHotKeywords()
  // 自动聚焦搜索框
  setTimeout(() => {
    searchInputRef.value?.focus()
  }, 300)
})
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;
@use '@/styles/mixins.scss' as *;

.search-page {
  min-height: 100vh;
  background: $ink-50;
  position: relative;
  padding-bottom: 80px;
}

// ---------- 蜂巢装饰背景 ----------
.hex-bg {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  @include honeycomb-bg(rgba(78, 205, 196, 0.04), 32px);
  pointer-events: none;
  z-index: 0;
}

// ---------- 顶部搜索栏 ----------
.search-header {
  position: sticky;
  top: 0;
  z-index: 100;
  @include glass(20px, rgba(255, 255, 255, 0.88));
  border-bottom: 1px solid rgba(78, 205, 196, 0.1);

  .header-inner {
    display: flex;
    align-items: center;
    gap: $space-2;
    padding: $space-3 $space-4;
    max-width: 800px;
    margin: 0 auto;
  }

  .back-btn {
    background: none;
    border: none;
    color: $ink-700;
    cursor: pointer;
    padding: $space-1;
    border-radius: $radius-xs;
    flex-shrink: 0;
    transition: background $dur-fast ease;

    &:hover {
      background: $ink-50;
    }
  }
}

.search-input-wrap {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: $space-3;
  color: $ink-300;
  pointer-events: none;
}

.search-input {
  width: 100%;
  padding: $space-2 $space-3 $space-2 40px;
  border: 1px solid $ink-100;
  border-radius: $radius-pill;
  font-size: 14px;
  color: $ink-700;
  background: rgba(255, 255, 255, 0.6);
  outline: none;
  transition: border-color $dur-fast ease, box-shadow $dur-fast ease;

  &::placeholder {
    color: $ink-300;
  }

  &:focus {
    border-color: $mint-500;
    box-shadow: 0 0 0 3px rgba(78, 205, 196, 0.15);
  }
}

.clear-btn {
  position: absolute;
  right: $space-2;
  background: none;
  border: none;
  color: $ink-300;
  cursor: pointer;
  padding: $space-1;
  border-radius: 50%;
  transition: color $dur-fast ease;

  &:hover {
    color: $ink-500;
  }
}

.search-action-btn {
  padding: $space-2 $space-4;
  border-radius: $radius-pill;
  border: none;
  background: $grad-mint;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  flex-shrink: 0;
  transition: all $dur-fast $ease-out;

  &:hover {
    box-shadow: $shadow-glow-mint;
    transform: translateY(-1px);
  }
}

// ---------- 搜索前内容 ----------
.pre-search {
  position: relative;
  z-index: 1;
  max-width: 800px;
  margin: 0 auto;
  padding: $space-4;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: $space-3;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: $ink-900;
  display: flex;
  align-items: center;
  gap: $space-2;

  svg {
    color: $amber-500;
  }
}

.clear-history-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  background: none;
  border: none;
  color: $ink-300;
  font-size: 12px;
  cursor: pointer;
  padding: $space-1 $space-2;
  border-radius: $radius-xs;
  transition: color $dur-fast ease;

  &:hover {
    color: $coral-500;
  }
}

// ---------- 搜索历史 ----------
.history-section {
  margin-bottom: $space-6;
}

.history-tags {
  display: flex;
  flex-wrap: wrap;
  gap: $space-2;
}

.history-tag {
  padding: $space-1 $space-3;
  border-radius: $radius-pill;
  font-size: 13px;
  color: $ink-500;
  cursor: pointer;
  transition: all $dur-fast $ease-out;
  border: none;
  background: rgba(255, 255, 255, 0.7);

  &:hover {
    color: $mint-600;
    background: rgba(78, 205, 196, 0.08);
    transform: translateY(-1px);
  }
}

// ---------- 热门搜索 ----------
.hot-section {
  margin-bottom: $space-6;
}

.hot-list {
  display: flex;
  flex-direction: column;
  gap: 0;
  background: rgba(255, 255, 255, 0.6);
  border-radius: $radius-md;
  overflow: hidden;
}

.hot-item {
  display: flex;
  align-items: center;
  gap: $space-3;
  padding: $space-3 $space-4;
  background: none;
  border: none;
  cursor: pointer;
  transition: background $dur-fast ease;
  text-align: left;

  &:hover {
    background: rgba(78, 205, 196, 0.04);
  }

  &:not(:last-child) {
    border-bottom: 1px solid $ink-50;
  }
}

.hot-rank {
  width: 22px;
  height: 22px;
  border-radius: $radius-xs;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  font-family: $font-heading;
  color: $ink-300;
  background: $ink-50;
  flex-shrink: 0;

  &.top {
    background: $grad-amber;
    color: #fff;
  }
}

.hot-word {
  font-size: 14px;
  color: $ink-700;
  @include ellipsis-1;
}

// ---------- 搜索结果 ----------
.search-results {
  position: relative;
  z-index: 1;
}

// ---------- 结果Tab ----------
.result-tabs {
  position: sticky;
  top: 60px;
  z-index: 99;
  @include glass(16px, rgba(255, 255, 255, 0.8));
  border-bottom: 1px solid $ink-100;

  .tabs-inner {
    display: flex;
    max-width: 800px;
    margin: 0 auto;
    padding: 0 $space-4;
    gap: $space-6;
  }
}

.tab-btn {
  position: relative;
  padding: $space-3 0;
  font-size: 15px;
  font-weight: 500;
  color: $ink-500;
  background: none;
  border: none;
  cursor: pointer;
  transition: color $dur-fast $ease-out;

  &.active {
    color: $ink-900;

    .tab-label {
      font-weight: 600;
    }
  }

  &:hover {
    color: $mint-600;
  }
}

.tab-indicator {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 24px;
  height: 3px;
  border-radius: 2px;
  background: $grad-mint;
  animation: scale-in 0.3s $ease-spring both;
}

// ---------- 结果列表 ----------
.result-list {
  max-width: 800px;
  margin: 0 auto;
  padding: $space-3 $space-4;
}

.result-group {
  margin-bottom: $space-5;

  &:last-child {
    margin-bottom: 0;
  }
}

.group-title {
  display: flex;
  align-items: center;
  gap: $space-2;
  font-size: 15px;
  font-weight: 600;
  color: $ink-700;
  margin-bottom: $space-3;
  padding-bottom: $space-2;
  border-bottom: 1px solid $ink-50;

  svg {
    color: $mint-500;
  }
}

.result-item {
  margin-bottom: $space-2;
  animation: fade-up 0.4s $ease-out both;

  @for $i from 1 through 10 {
    &:nth-child(#{$i}) {
      animation-delay: #{$i * 0.05}s;
    }
  }
}

.view-more-btn {
  display: block;
  width: 100%;
  padding: $space-3 0;
  background: none;
  border: none;
  font-size: 13px;
  color: $mint-600;
  cursor: pointer;
  text-align: center;
  transition: color $dur-fast ease;

  &:hover {
    color: $mint-700;
  }
}

.loading-wrap {
  @include center;
  padding: $space-12 0;
}

.load-more-area {
  display: flex;
  justify-content: center;
  padding: $space-6 0;
}

.no-more-text {
  font-size: 13px;
  color: $ink-300;
}

// ---------- 响应式 ----------
@include mobile {
  /**
   * 移动端安全区域适配
   * @description 为页面根容器添加顶部安全区域内边距，避免内容被刘海屏遮挡
   */
  .search-page {
    padding-top: env(safe-area-inset-top);
  }

  .search-header .header-inner {
    padding: $space-2 $space-3;
  }

  .pre-search {
    padding: $space-3;
  }

  .result-list {
    padding: $space-2 $space-3;
  }
}
</style>
