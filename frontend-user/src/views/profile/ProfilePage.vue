<template>
  <div class="profile-page">
    <!-- 蜂巢装饰背景 -->
    <div class="hex-bg" />

    <!-- 个人信息卡片 -->
    <section class="profile-hero">
      <div class="hero-inner">
        <!-- 蜂巢装饰 -->
        <div class="hero-deco">
          <div class="deco-hex hex-1 anim-float" />
          <div class="deco-hex hex-2 anim-float" />
          <div class="deco-hex hex-3 anim-float" />
        </div>

        <!-- 六边形头像 -->
        <div class="profile-avatar-wrap">
          <div class="hex-avatar">
            <img :src="userInfo.avatar || defaultAvatar" :alt="userInfo.nickname" />
          </div>
          <div class="avatar-ring" />
        </div>

        <!-- 用户名与简介 -->
        <h1 class="profile-name font-display">{{ userInfo.nickname }}</h1>
        <p v-if="userInfo.bio" class="profile-bio">{{ userInfo.bio }}</p>

        <!-- 兴趣标签 -->
        <div v-if="userInfo.interests && userInfo.interests.length" class="interest-tags">
          <span v-for="tag in userInfo.interests" :key="tag" class="interest-tag">
            <svg width="10" height="10" viewBox="0 0 24 24" fill="none">
              <path d="M12 2L22 8.5V15.5L12 22L2 15.5V8.5L12 2Z" stroke="currentColor" stroke-width="2"/>
            </svg>
            {{ tag }}
          </span>
        </div>

        <!-- 数据统计 -->
        <div class="stats-row">
          <div class="stat-item" @click="goStat('posts')">
            <span class="stat-num font-heading">{{ formatNumber(userInfo.postCount || 0) }}</span>
            <span class="stat-label">帖子</span>
          </div>
          <div class="stat-divider" />
          <div class="stat-item" @click="goStat('following')">
            <span class="stat-num font-heading">{{ formatNumber(userInfo.followCount || 0) }}</span>
            <span class="stat-label">关注</span>
          </div>
          <div class="stat-divider" />
          <div class="stat-item" @click="goStat('followers')">
            <span class="stat-num font-heading">{{ formatNumber(userInfo.fanCount || 0) }}</span>
            <span class="stat-label">粉丝</span>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="profile-actions">
          <!-- 自己的主页：编辑资料 -->
          <button v-if="isSelf" class="action-btn edit-btn" @click="goEditProfile">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            编辑资料
          </button>

          <!-- 他人主页：关注/取消关注 -->
          <button
            v-else
            :class="['action-btn', 'follow-btn', { followed: isFollowing }]"
            @click="onToggleFollow"
          >
            <svg v-if="!isFollowing" width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M16 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2M20 8v6M23 11h-6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M20 6L9 17l-5-5" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            {{ isFollowing ? '已关注' : '关注' }}
          </button>

          <!-- 他人主页：私信 -->
          <button v-if="!isSelf" class="action-btn msg-btn" @click="goChat">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            私信
          </button>
        </div>
      </div>
    </section>

    <!-- Tab 切换 -->
    <nav class="content-tabs">
      <div class="tabs-inner">
        <button
          v-for="tab in contentTabs"
          :key="tab.value"
          :class="['tab-btn', { active: activeTab === tab.value }]"
          @click="switchTab(tab.value)"
        >
          <span class="tab-label">{{ tab.label }}</span>
          <span v-if="activeTab === tab.value" class="tab-indicator" />
        </button>
      </div>
    </nav>

    <!-- 内容区域 -->
    <div class="content-area">
      <!-- 加载中 -->
      <div v-if="contentLoading && posts.length === 0" class="loading-wrap">
        <LoadingSpinner />
      </div>

      <!-- 动态列表 -->
      <div v-else-if="posts.length > 0" class="posts-grid">
        <TransitionGroup name="card-anim">
          <PostCard
            v-for="post in posts"
            :key="post.id"
            :post="post"
            class="stagger-item"
            @click="goPostDetail(post.id)"
            @like="onLike"
            @collect="onCollect"
          />
        </TransitionGroup>
      </div>

      <!-- 空状态 -->
      <EmptyState
        v-else-if="!contentLoading"
        :title="emptyTitle"
        :description="emptyDesc"
        :icon="emptyIcon"
      />

      <!-- 加载更多 -->
      <div v-if="posts.length > 0" class="load-more-area">
        <LoadingSpinner v-if="loadingMore" />
        <p v-else-if="!hasMore" class="no-more-text">已经到底啦 ~</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 个人主页
 * @description 展示用户个人信息、数据统计、兴趣标签、动态/收藏/点赞列表，
 *   支持关注/取消关注、编辑资料、私信跳转
 */

import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast } from 'vant'
import PostCard from '@/components/PostCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import { useUserStore } from '@/stores/user'
import { getUserProfile, getUserPosts, getUserCollects, getUserLikes } from '@/api/user'
import { toggleFollow } from '@/api/follow'
import { formatNumber } from '@/utils/format'
import type { User, Post } from '@/types'

// ---------- 路由与Store ----------
const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// ---------- 默认头像 ----------
const defaultAvatar = 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 40"><rect fill="#4ECDC4" width="40" height="40"/><text x="20" y="26" text-anchor="middle" fill="white" font-size="16">U</text></svg>')

// ---------- Tab 配置 ----------
interface ContentTab {
  label: string
  value: 'posts' | 'collects' | 'likes'
}

const contentTabs: ContentTab[] = [
  { label: '动态', value: 'posts' },
  { label: '收藏', value: 'collects' },
  { label: '点赞', value: 'likes' }
]

// ---------- 响应式数据 ----------
const userInfo = ref<Partial<User>>({
  nickname: '用户',
  avatar: defaultAvatar,
  bio: '',
  interests: [],
  postCount: 0,
  followCount: 0,
  fanCount: 0
})
const activeTab = ref<'posts' | 'collects' | 'likes'>('posts')
const posts = ref<Post[]>([])
const contentLoading = ref(false)
const loadingMore = ref(false)
const hasMore = ref(true)
const currentPage = ref(1)
const isFollowing = ref(false)

/** 页面用户ID */
const profileUserId = computed(() => Number(route.params.id) || userStore.userInfo?.id || 0)

/** 是否是自己的主页 */
const isSelf = computed(() => {
  return profileUserId.value === (userStore.userInfo?.id || 0)
})

/** 空状态文案 */
const emptyTitle = computed(() => {
  const map: Record<string, string> = {
    posts: '还没有发布动态',
    collects: '还没有收藏内容',
    likes: '还没有点赞内容'
  }
  return isSelf.value ? map[activeTab.value] : '对方还没有相关内容'
})

const emptyDesc = computed(() => {
  return isSelf.value ? '快去分享你的想法吧' : ''
})

const emptyIcon = computed(() => {
  const map: Record<string, string> = {
    posts: 'notes-o',
    collects: 'star-o',
    likes: 'like-o'
  }
  return map[activeTab.value] || 'notes-o'
})

// ---------- 数据加载 ----------

/**
 * 加载用户资料
 * @returns {Promise<void>}
 * @description 从服务端获取用户信息并更新本地状态
 */
async function fetchUserProfile(): Promise<void> {
  try {
    const res = await getUserProfile(profileUserId.value)
    if (res.data) {
      userInfo.value = res.data
    }
  } catch {
    showToast('加载用户信息失败')
  }
}

/**
 * 加载内容列表
 * @param {boolean} isRefresh - 是否为刷新操作
 * @returns {Promise<void>}
 * @description 根据当前Tab加载动态/收藏/点赞列表
 */
async function fetchContent(isRefresh = false): Promise<void> {
  if (isRefresh) {
    currentPage.value = 1
    hasMore.value = true
  }
  contentLoading.value = true
  try {
    let res: any
    if (activeTab.value === 'posts') {
      res = await getUserPosts(profileUserId.value, currentPage.value, 10)
    } else if (activeTab.value === 'collects') {
      res = await getUserCollects(currentPage.value, 10)
    } else {
      res = await getUserLikes(currentPage.value, 10)
    }

    const list = res.data?.list || []

    // 对 liked/collected 做布尔值兜底，确保 null/undefined → false
    list.forEach((p: any) => {
      p.liked = !!p.liked
      p.collected = !!p.collected
    })

    if (isRefresh || currentPage.value === 1) {
      posts.value = list
    } else {
      posts.value.push(...list)
    }
    hasMore.value = res.data?.hasMore ?? false
  } catch {
    showToast('加载失败')
  } finally {
    contentLoading.value = false
    loadingMore.value = false
  }
}

// ---------- 交互操作 ----------

/**
 * 切换内容Tab
 * @param {'posts' | 'collects' | 'likes'} tab - 目标Tab
 * @returns {void}
 */
function switchTab(tab: 'posts' | 'collects' | 'likes'): void {
  if (activeTab.value === tab) return
  activeTab.value = tab
  posts.value = []
  fetchContent(true)
}

/**
 * 关注/取消关注
 * @returns {Promise<void>}
 * @description 切换关注状态并更新UI
 */
async function onToggleFollow(): Promise<void> {
  try {
    await toggleFollow(profileUserId.value, !isFollowing.value)
    isFollowing.value = !isFollowing.value
    // 更新粉丝数
    if (userInfo.value.fanCount !== undefined) {
      userInfo.value.fanCount += isFollowing.value ? 1 : -1
    }
    showToast(isFollowing.value ? '关注成功' : '已取消关注')
  } catch {
    showToast('操作失败')
  }
}

/**
 * 点赞帖子
 * @param {number} postId - 帖子ID
 * @returns {void}
 */
function onLike(postId: number): void {
  const post = posts.value.find(p => p.id === postId)
  if (post) {
    post.liked = !post.liked
    post.likeCount += post.liked ? 1 : -1
  }
}

/**
 * 收藏帖子
 * @param {number} postId - 帖子ID
 * @returns {void}
 */
function onCollect(postId: number): void {
  const post = posts.value.find(p => p.id === postId)
  if (post) {
    post.collected = !post.collected
    post.collectCount += post.collected ? 1 : -1
  }
}

// ---------- 导航 ----------

/**
 * 跳转帖子详情
 * @param {number} id - 帖子ID
 * @returns {void}
 */
function goPostDetail(id: number): void {
  router.push(`/post/${id}`)
}

/**
 * 跳转编辑资料
 * @returns {void}
 */
function goEditProfile(): void {
  router.push('/settings')
}

/**
 * 跳转私信聊天
 * @returns {void}
 */
function goChat(): void {
  router.push(`/chat/${profileUserId.value}`)
}

/**
 * 跳转统计详情
 * @param {'posts' | 'following' | 'followers'} type - 统计类型
 * @returns {void}
 */
function goStat(type: string): void {
  // 可扩展为跳转关注列表等
}

// ---------- 生命周期 ----------
onMounted(() => {
  fetchUserProfile()
  fetchContent(true)
})

// 监听路由参数变化
watch(() => route.params.id, (newId) => {
  if (newId) {
    posts.value = []
    fetchUserProfile()
    fetchContent(true)
  }
})
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;
@use '@/styles/mixins.scss' as *;

.profile-page {
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

// ---------- 个人信息卡片 ----------
.profile-hero {
  position: relative;
  z-index: 1;
  @include glass(24px, rgba(255, 255, 255, 0.85));
  border-bottom: 1px solid rgba(78, 205, 196, 0.1);
  overflow: hidden;
}

.hero-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: $space-8 $space-4 $space-6;
  max-width: 800px;
  margin: 0 auto;
  position: relative;
}

// ---------- 蜂巢装饰 ----------
.hero-deco {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.deco-hex {
  position: absolute;
  @include hexagon(40px);
  background: $grad-mint;
  opacity: 0.08;

  &.hex-1 {
    top: 10%;
    left: 8%;
    animation-delay: 0s;
  }

  &.hex-2 {
    top: 20%;
    right: 12%;
    width: 28px;
    height: 28px * 1.1547;
    animation-delay: -3s;
  }

  &.hex-3 {
    bottom: 15%;
    left: 15%;
    width: 20px;
    height: 20px * 1.1547;
    animation-delay: -5s;
  }
}

// ---------- 头像 ----------
.profile-avatar-wrap {
  position: relative;
  @include hexagon(92px); /* 与 avatar-ring 同尺寸，作为定位基准 */
  margin-bottom: $space-4;
}

.hex-avatar {
  @include hexagon(80px);
  overflow: hidden;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 2;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.avatar-ring {
  position: absolute;
  @include hexagon(92px);
  top: 0;
  left: 0;
  background: $grad-mint;
  opacity: 0.3;
  z-index: 1;
  animation: hex-breathe 3s ease-in-out infinite;
}

// ---------- 用户名与简介 ----------
.profile-name {
  font-size: 24px;
  font-weight: 700;
  color: $ink-900;
  margin-bottom: $space-1;
  text-align: center;
}

.profile-bio {
  font-size: 14px;
  color: $ink-500;
  text-align: center;
  max-width: 400px;
  line-height: 1.6;
  margin-bottom: $space-3;
}

// ---------- 兴趣标签 ----------
.interest-tags {
  display: flex;
  flex-wrap: wrap;
  gap: $space-2;
  justify-content: center;
  margin-bottom: $space-5;
}

.interest-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: $space-1 $space-3;
  border-radius: $radius-pill;
  background: rgba(78, 205, 196, 0.08);
  color: $mint-600;
  font-size: 12px;
  font-weight: 500;
  transition: all $dur-fast $ease-out;

  &:hover {
    background: rgba(78, 205, 196, 0.15);
    transform: translateY(-1px);
  }

  svg {
    flex-shrink: 0;
  }
}

// ---------- 数据统计 ----------
.stats-row {
  display: flex;
  align-items: center;
  gap: $space-6;
  margin-bottom: $space-5;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  transition: transform $dur-fast $ease-out;

  &:hover {
    transform: scale(1.05);
  }
}

.stat-num {
  font-size: 22px;
  font-weight: 700;
  color: $ink-900;
}

.stat-label {
  font-size: 12px;
  color: $ink-300;
  margin-top: 2px;
}

.stat-divider {
  width: 1px;
  height: 28px;
  background: $ink-100;
}

// ---------- 操作按钮 ----------
.profile-actions {
  display: flex;
  gap: $space-3;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: $space-2;
  padding: $space-2 $space-5;
  border-radius: $radius-pill;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all $dur-fast $ease-out;
  border: none;

  &.edit-btn {
    background: $grad-mint;
    color: #fff;
    box-shadow: $shadow-glow-mint;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 0 32px rgba(78, 205, 196, 0.6);
    }
  }

  &.follow-btn {
    background: $grad-amber;
    color: #fff;
    box-shadow: $shadow-glow-amber;

    &:hover {
      transform: translateY(-2px);
    }

    &.followed {
      background: $ink-100;
      color: $ink-500;
      box-shadow: none;

      &:hover {
        background: $coral-500;
        color: #fff;
      }
    }
  }

  &.msg-btn {
    background: rgba(255, 255, 255, 0.7);
    color: $ink-700;
    border: 1px solid $ink-100;

    &:hover {
      border-color: $mint-500;
      color: $mint-600;
      transform: translateY(-2px);
    }
  }
}

// ---------- Tab 切换 ----------
.content-tabs {
  position: sticky;
  top: 0;
  z-index: 99;
  @include glass(16px, rgba(255, 255, 255, 0.8));
  border-bottom: 1px solid $ink-100;

  .tabs-inner {
    display: flex;
    max-width: 800px;
    margin: 0 auto;
    padding: 0 $space-4;
    gap: $space-8;
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

// ---------- 内容区域 ----------
.content-area {
  position: relative;
  z-index: 1;
  max-width: 800px;
  margin: 0 auto;
  padding: $space-3 $space-4;
}

.loading-wrap {
  @include center;
  padding: $space-12 0;
}

.posts-grid {
  display: flex;
  flex-direction: column;
  gap: $space-3;
}

.stagger-item {
  animation: fade-up 0.5s $ease-out both;

  @for $i from 1 through 20 {
    &:nth-child(#{$i}) {
      animation-delay: #{$i * 0.06}s;
    }
  }
}

.card-anim-enter-active {
  animation: fade-up 0.5s $ease-out both;
}

.card-anim-leave-active {
  animation: scale-in 0.3s $ease-out reverse both;
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
  .hero-inner {
    padding: $space-6 $space-3 $space-4;
  }

  .profile-avatar-wrap {
    @include hexagon(74px); /* 与移动端 avatar-ring 同尺寸 */
  }

  .hex-avatar {
    @include hexagon(64px);
  }

  .avatar-ring {
    @include hexagon(74px);
  }

  .profile-name {
    font-size: 20px;
  }

  .stats-row {
    gap: $space-4;
  }

  .stat-num {
    font-size: 18px;
  }

  .content-area {
    padding: $space-2 $space-3;
  }
}
</style>
