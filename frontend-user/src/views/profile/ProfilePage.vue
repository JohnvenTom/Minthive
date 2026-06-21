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
            <span class="stat-num font-heading"><AnimatedNumber :value="userInfo.postCount || 0" /></span>
            <span class="stat-label">帖子</span>
          </div>
          <div class="stat-divider" />
          <div class="stat-item" @click="goStat('following')">
            <span class="stat-num font-heading"><AnimatedNumber :value="userInfo.followCount || 0" /></span>
            <span class="stat-label">关注</span>
          </div>
          <div class="stat-divider" />
          <div class="stat-item" @click="goStat('followers')">
            <span class="stat-num font-heading"><AnimatedNumber :value="userInfo.fanCount || 0" /></span>
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

    <!-- 功能区（仅自己的主页） -->
    <section v-if="isSelf" class="menu-section">
      <div class="menu-inner">
        <!-- 内容切换（横向5个，始终可见） -->
        <div class="tab-switch glass-card">
          <button
            v-for="tab in contentTabs"
            :key="tab.value"
            :class="['tab-chip', { active: showContent && activeTab === tab.value }]"
            @click="toggleContent(tab.value)"
          >
            <svg v-if="tab.value === 'posts'" width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M19 3H5a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2V5a2 2 0 00-2-2zM16 11H8M16 15H8M10 7H6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <svg v-else-if="tab.value === 'collects'" width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M19 21l-7-5-7 5V5a2 2 0 012-2h10a2 2 0 012 2z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <svg v-else-if="tab.value === 'likes'" width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z" stroke="currentColor" stroke-width="2"/>
            </svg>
            <svg v-else-if="tab.value === 'following'" width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M16 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2M20 8v6M23 11h-6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2M9 11a4 4 0 100-8 4 4 0 000 8zM23 21v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            {{ tab.label }}
          </button>
        </div>

        <!-- 菜单列表（未展开内容时显示） -->
        <template v-if="!showContent">
          <div class="menu-group glass-card">
          <button class="menu-item" @click="goMyCircles">
            <div class="menu-icon circle-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                <path d="M12 2L22 8.5V15.5L12 22L2 15.5V8.5L12 2Z" stroke="currentColor" stroke-width="2"/>
              </svg>
            </div>
            <span class="menu-label">我的圈子</span>
            <svg class="menu-arrow" width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M9 18l6-6-6-6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>

          <div class="menu-divider" />

          <button class="menu-item" @click="goDrafts">
            <div class="menu-icon draft-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M14 2v6h6M16 13H8M16 17H8M10 9H8" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
            <span class="menu-label">草稿箱</span>
            <svg class="menu-arrow" width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M9 18l6-6-6-6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
        </div>

        <!-- 设置组 -->
        <div class="menu-group glass-card">
          <button class="menu-item" @click="goSettings">
            <div class="menu-icon settings-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                <circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="2"/>
                <path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-2 2 2 2 0 01-2-2v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83 0 2 2 0 010-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 01-2-2 2 2 0 012-2h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 010-2.83 2 2 0 012.83 0l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 012-2 2 2 0 012 2v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 0 2 2 0 010 2.83l-.06.06a1.65 1.65 0 00-.33 1.82V9a1.65 1.65 0 001.51 1H21a2 2 0 012 2 2 2 0 01-2 2h-.09a1.65 1.65 0 00-1.51 1z" stroke="currentColor" stroke-width="2"/>
              </svg>
            </div>
            <span class="menu-label">账号设置</span>
            <svg class="menu-arrow" width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M9 18l6-6-6-6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>

          <div class="menu-divider" />

          <button class="menu-item" @click="goAbout">
            <div class="menu-icon about-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                <path d="M12 2L14.5 8.5L21 9.5L16.5 14L17.5 21L12 17.5L6.5 21L7.5 14L3 9.5L9.5 8.5L12 2Z" stroke="currentColor" stroke-width="2"/>
              </svg>
            </div>
            <span class="menu-label">关于MintHive</span>
            <svg class="menu-arrow" width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M9 18l6-6-6-6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
        </div>

        <!-- 退出登录 -->
        <button class="logout-btn glass-card" @click="showLogoutConfirm = true">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
            <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4M16 17l5-5-5-5M21 12H9" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          退出登录
        </button>
          </template>

          <!-- 内容列表（展开时显示，替换菜单位置） -->
          <div v-else class="content-panel glass-card">
            <div class="panel-header">
              <button class="back-btn" @click="collapseContent">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                  <path d="M15 18l-6-6 6-6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </button>
              <span class="panel-title">{{ currentTabLabel }}</span>
              <div class="panel-placeholder" />
            </div>

            <div v-if="contentLoading && posts.length === 0 && users.length === 0" class="loading-wrap">
              <LoadingSpinner />
            </div>

            <!-- 关注/粉丝用户列表 -->
            <div v-else-if="(activeTab === 'following' || activeTab === 'followers') && users.length > 0" class="users-list">
              <TransitionGroup name="card-anim">
                <UserCard
                  v-for="user in users"
                  :key="user.id"
                  :user="user"
                  :show-follow="isSelf"
                  class="stagger-item"
                  @click="router.push(`/profile/${user.id}`)"
                  @follow="onUserFollow"
                />
              </TransitionGroup>
            </div>

            <!-- 帖子列表 -->
            <div v-else-if="posts.length > 0" class="posts-grid">
              <TransitionGroup name="card-anim">
                <PostCard
                  v-for="post in posts"
                  :key="post.id"
                  :post="post"
                  class="stagger-item"
                  @click="goPostDetail(post.id)"
                  @click-user="router.push(`/profile/${$event}`)"
                  @like="onLike"
                  @collect="onCollect"
                  @openShare="onOpenShare"
                  @viewShares="onViewShares"
                  @editPost="goPostDetail"
                  @deletePost="onDeletePost"
                  @toggleVisibility="onToggleVisibility"
                />
              </TransitionGroup>
            </div>

            <EmptyState
              v-else-if="!contentLoading"
              :title="emptyTitle"
              :description="emptyDesc"
              :icon="emptyIcon"
            />

            <div v-if="(activeTab === 'following' || activeTab === 'followers') && users.length > 0" class="load-more-area">
              <LoadingSpinner v-if="loadingMore" />
              <p v-else-if="!hasMore" class="no-more-text">已经到底啦 ~</p>
            </div>

            <div v-if="posts.length > 0 && (activeTab !== 'following' && activeTab !== 'followers')" class="load-more-area">
              <LoadingSpinner v-if="loadingMore" />
              <p v-else-if="!hasMore" class="no-more-text">已经到底啦 ~</p>
            </div>
          </div>
      </div>
    </section>

    <!-- 内容区域（非自己的主页，始终显示） -->
    <template v-if="!isSelf">
    <nav class="content-tabs">
      <div class="tabs-inner">
        <!-- 添加Tab切换按钮 -->
        <div class="tab-switch-inline">
          <button
            v-for="tab in contentTabs"
            :key="tab.value"
            :class="['tab-chip-inline', { active: activeTab === tab.value }]"
            @click="switchTab(tab.value)"
          >
            {{ tab.label }}
          </button>
        </div>
      </div>
    </nav>

    <div class="content-area">
      <div v-if="contentLoading && posts.length === 0 && users.length === 0" class="loading-wrap">
        <LoadingSpinner />
      </div>

      <!-- 关注/粉丝用户列表 -->
      <div v-else-if="(activeTab === 'following' || activeTab === 'followers') && users.length > 0" class="users-list">
        <TransitionGroup name="card-anim">
          <UserCard
            v-for="user in users"
            :key="user.id"
            :user="user"
            :show-follow="true"
            class="stagger-item"
            @click="router.push(`/profile/${user.id}`)"
            @follow="onUserFollow"
          />
        </TransitionGroup>
      </div>

      <div v-else-if="posts.length > 0" class="posts-grid">
        <TransitionGroup name="card-anim">
          <PostCard
            v-for="post in posts"
            :key="post.id"
            :post="post"
            class="stagger-item"
            @click="goPostDetail(post.id)"
            @click-user="router.push(`/profile/${$event}`)"
            @like="onLike"
            @collect="onCollect"
            @openShare="onOpenShare"
            @viewShares="onViewShares"
          />
        </TransitionGroup>
      </div>

      <EmptyState
        v-else-if="!contentLoading"
        :title="emptyTitle"
        :description="emptyDesc"
        :icon="emptyIcon"
      />

      <div v-if="(activeTab === 'following' || activeTab === 'followers') && users.length > 0" class="load-more-area">
        <LoadingSpinner v-if="loadingMore" />
        <p v-else-if="!hasMore" class="no-more-text">已经到底啦 ~</p>
      </div>

      <div v-if="posts.length > 0 && (activeTab !== 'following' && activeTab !== 'followers')" class="load-more-area">
        <LoadingSpinner v-if="loadingMore" />
        <p v-else-if="!hasMore" class="no-more-text">已经到底啦 ~</p>
      </div>
    </div>
    </template>

    <!-- 分享面板 -->
    <ShareSheet
      v-model:show="showShareSheet"
      :post-id="shareTargetPostId"
      :post-content="shareTargetContent"
      @shared="onShared"
    />

    <!-- 转发链弹窗 -->
    <ShareChainDialog
      v-model:show="showShareChain"
      :post-id="shareChainPostId"
      @click-share="router.push(`/post/${$event}`)"
      @click-user="router.push(`/profile/${$event}`)"
    />

    <!-- 退出登录确认弹窗 -->
    <Teleport to="body">
      <Transition name="modal-fade">
        <div v-if="showLogoutConfirm" class="modal-overlay" @click="showLogoutConfirm = false">
          <div class="modal-card glass-card" @click.stop>
            <div class="modal-deco">
              <div class="modal-hex anim-breathe" />
            </div>
            <h3 class="modal-title font-heading">确认退出</h3>
            <p class="modal-desc">退出后需要重新登录才能使用MintHive</p>
            <div class="modal-actions">
              <button class="modal-btn cancel" @click="showLogoutConfirm = false">取消</button>
              <button class="modal-btn confirm" @click="onLogout">确认退出</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
/**
 * 个人主页
 * @description 展示用户个人信息、数据统计、兴趣标签、动态/收藏/点赞列表，
 *   支持关注/取消关注、编辑资料、私信跳转
 */

import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import PostCard from '@/components/PostCard.vue'
import UserCard from '@/components/UserCard.vue'
import AnimatedNumber from '@/components/AnimatedNumber.vue'
import EmptyState from '@/components/EmptyState.vue'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import ShareSheet from '@/components/ShareSheet.vue'
import ShareChainDialog from '@/components/ShareChainDialog.vue'
import { useUserStore } from '@/stores/user'
import { getUserProfile, getUserPosts, getUserCollects, getUserLikes } from '@/api/user'
import { toggleFollow, checkFollowStatus, getFollowing, getFollowers } from '@/api/follow'
import { deletePost, togglePostVisibility } from '@/api/post'
import type { User, Post } from '@/types'

// ---------- 路由与Store ----------
const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// ---------- 默认头像 ----------
const defaultAvatar = 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 40"><rect fill="#4ECDC4" width="40" height="40"/><text x="20" y="26" text-anchor="middle" fill="white" font-size="16">U</text></svg>')

// ---------- Tab 配置 ----------
/**
 * 内容Tab项配置
 * @interface ContentTab
 * @property {string} label - 显示标签名称
 * @property {('posts' | 'collects' | 'likes' | 'following' | 'followers')} value - Tab值
 */
interface ContentTab {
  label: string
  value: 'posts' | 'collects' | 'likes' | 'following' | 'followers'
}

const contentTabs: ContentTab[] = [
  { label: '动态', value: 'posts' },
  { label: '收藏', value: 'collects' },
  { label: '点赞', value: 'likes' },
  { label: '关注', value: 'following' },
  { label: '粉丝', value: 'followers' }
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
const activeTab = ref<'posts' | 'collects' | 'likes' | 'following' | 'followers'>('posts')
const posts = ref<Post[]>([])
/** 关注/粉丝用户列表 */
const users = ref<User[]>([])
const contentLoading = ref(false)
const loadingMore = ref(false)
const hasMore = ref(true)
const currentPage = ref(1)
const isFollowing = ref(false)
/** 退出登录确认弹窗是否显示 */
const showLogoutConfirm = ref(false)
/** 是否展开内容面板（true=显示内容列表，false=显示功能菜单） */
const showContent = ref(false)

// ---------- 分享面板 ----------
/** 分享面板是否显示 */
const showShareSheet = ref(false)
/** 分享目标帖子ID */
const shareTargetPostId = ref(0)
/** 分享目标帖子内容（用于生成默认转发文案） */
const shareTargetContent = ref('')

// ---------- 转发链弹窗 ----------
/** 转发链弹窗是否显示 */
const showShareChain = ref(false)
/** 转发链目标帖子ID */
const shareChainPostId = ref(0)

/** 页面用户ID */
const profileUserId = computed(() => Number(route.params.id) || userStore.userInfo?.id || 0)

/** 是否是自己的主页 */
const isSelf = computed(() => {
  return profileUserId.value === (userStore.userInfo?.id || 0)
})

/** 当前选中 Tab 的显示名称 */
const currentTabLabel = computed(() => {
  const tab = contentTabs.find(t => t.value === activeTab.value)
  return tab?.label || '动态'
})

/** 空状态标题 */
const emptyTitle = computed(() => {
  const map: Record<string, string> = {
    posts: '还没有发布动态',
    collects: '还没有收藏内容',
    likes: '还没有点赞内容',
    following: '还没有关注任何人',
    followers: '还没有粉丝'
  }
  return isSelf.value ? map[activeTab.value] : `对方${map[activeTab.value] || '还没有相关内容'}`
})

/** 空状态描述 */
const emptyDesc = computed(() => {
  const descMap: Record<string, string> = {
    posts: '快去分享你的想法吧',
    collects: '浏览动态时点击收藏按钮即可',
    likes: '点赞感兴趣的动态后会在这里显示',
    following: '关注感兴趣的用户，获取他们的最新动态',
    followers: '分享优质内容，吸引更多粉丝关注'
  }
  return descMap[activeTab.value] || ''
})

/** 空状态图标 */
const emptyIcon = computed(() => {
  const map: Record<string, string> = {
    posts: 'notes-o',
    collects: 'star-o',
    likes: 'like-o',
    following: 'friends-o',
    followers: 'users-o'
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
    if (!isSelf.value && userStore.isLoggedIn) {
      const followRes = await checkFollowStatus(profileUserId.value)
      isFollowing.value = !!followRes.data
    } else {
      isFollowing.value = false
    }
  } catch {
    showToast('加载用户信息失败')
  }
}

/**
 * 加载内容列表
 * @param {boolean} isRefresh - 是否为刷新操作
 * @returns {Promise<void>}
 * @description 根据当前Tab加载动态/收藏/点赞/关注/粉丝列表
 */
async function fetchContent(isRefresh = false): Promise<void> {
  if (isRefresh) {
    currentPage.value = 1
    hasMore.value = true
  }
  contentLoading.value = true
  try {
    // 关注/粉丝列表：加载用户数据
    if (activeTab.value === 'following' || activeTab.value === 'followers') {
      let res: any
      if (activeTab.value === 'following') {
        res = await getFollowing(profileUserId.value, currentPage.value, 20)
      } else {
        res = await getFollowers(profileUserId.value, currentPage.value, 20)
      }

      const list = res.data?.list || []

      if (isRefresh || currentPage.value === 1) {
        users.value = list
      } else {
        users.value.push(...list)
      }
      hasMore.value = res.data?.hasMore ?? false
      return
    }

    // 帖子相关列表：加载帖子数据
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
 * 切换内容面板展开/收起
 * @param {'posts' | 'collects' | 'likes' | 'following' | 'followers'} tab - 目标Tab
 * @returns {void}
 * @description 点击tab按钮：如果已展开且是同一个tab则收起，否则展开并加载数据
 */
function toggleContent(tab: 'posts' | 'collects' | 'likes' | 'following' | 'followers'): void {
  // 已展开且点击同一个 → 收起，回到菜单
  if (showContent.value && activeTab.value === tab) {
    showContent.value = false
    return
  }
  // 切换到新tab并展开
  activeTab.value = tab
  posts.value = []
  users.value = []
  showContent.value = true
  fetchContent(true)
}

/**
 * 非自己主页的Tab切换
 * @param {'posts' | 'collects' | 'likes' | 'following' | 'followers'} tab - 目标Tab
 * @returns {void}
 * @description 在他人主页切换不同内容标签
 */
function switchTab(tab: 'posts' | 'collects' | 'likes' | 'following' | 'followers'): void {
  if (activeTab.value === tab) return
  activeTab.value = tab
  posts.value = []
  users.value = []
  fetchContent(true)
}

/**
 * 关注/取消关注用户（从 UserCard emit）
 * @param {{ userId: number, follow: boolean }} payload - 用户ID和关注状态
 * @returns {Promise<void>}
 * @description 在关注/粉丝列表中切换对某用户的关注状态
 */
async function onUserFollow({ userId, follow }: { userId: number; follow: boolean }): Promise<void> {
  try {
    await toggleFollow(userId, follow)
    // 更新本地用户列表中的关注状态
    const user = users.value.find(u => u.id === userId)
    if (user) {
      user.isFollowed = follow
    }
    showToast(follow ? '关注成功' : '已取消关注')
  } catch {
    showToast('操作失败')
  }
}

/**
 * 收起内容面板，回到功能菜单
 * @returns {void}
 */
function collapseContent(): void {
  showContent.value = false
}

/**
 * 关注/取消关注
 * @returns {Promise<void>}
 * @description 切换关注状态并更新UI
 */
async function onToggleFollow(): Promise<void> {
  try {
    const willFollow = !isFollowing.value
    await toggleFollow(profileUserId.value, willFollow)
    isFollowing.value = willFollow
    if (userInfo.value.fanCount !== undefined) {
      userInfo.value.fanCount += willFollow ? 1 : -1
    }
    if (userStore.userInfo) {
      userStore.userInfo.followCount += willFollow ? 1 : -1
    }
    showToast(willFollow ? '关注成功' : '已取消关注')
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

/**
 * 打开分享面板
 * @param {number} postId - 目标帖子ID
 * @description 根据帖子ID查找对应帖子内容，设置目标后打开 ShareSheet
 */
function onOpenShare(postId: number): void {
  const target = posts.value.find(p => p.id === postId)
  shareTargetPostId.value = postId
  shareTargetContent.value = target?.content || ''
  showShareSheet.value = true
}

/**
 * 分享成功回调
 * @description 接收 ShareSheet 的 shared 事件，更新本地转发计数
 */
function onShared(): void {
  const post = posts.value.find(p => p.id === shareTargetPostId.value)
  if (post) post.shareCount++
}

/**
 * 打开转发链弹窗
 * @param {number} postId - 目标帖子ID
 */
function onViewShares(postId: number): void {
  shareChainPostId.value = postId
  showShareChain.value = true
}

/**
 * 删除帖子（从 PostCard emit）
 *
 * @param {number} postId - 帖子ID
 * @description 弹出确认框后调用删除接口，成功后从本地列表移除
 */
async function onDeletePost(postId: number): Promise<void> {
  try {
    await showConfirmDialog({
      title: '确认删除',
      message: '删除后不可恢复，确认删除该帖子吗？'
    })
    await deletePost(postId)
    posts.value = posts.value.filter(p => p.id !== postId)
    showToast('已删除')
  } catch (e: any) {
    if (e !== 'cancel') showToast('删除失败')
  }
}

/**
 * 切换帖子可见性（从 PostCard emit）
 *
 * @param {number} postId - 帖子ID
 * @param {number} visibility - 目标可见性(0=公开 2=隐藏)
 */
async function onToggleVisibility(postId: number, visibility: number): Promise<void> {
  try {
    await togglePostVisibility(postId, visibility)
    const post = posts.value.find(p => p.id === postId)
    if (post) post.visibility = visibility
  } catch {
    showToast('操作失败')
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
  router.push('/edit-profile')
}

/**
 * 跳转私信聊天
 * @returns {void}
 */
function goChat(): void {
  router.push(`/chat/${profileUserId.value}`)
}

/**
 * 跳转统计详情（帖子/关注/粉丝）
 * @param {'posts' | 'following' | 'followers'} type - 统计类型
 * @returns {void}
 * @description 点击统计数字时，展开对应的内容面板并加载数据
 */
function goStat(type: 'posts' | 'following' | 'followers'): void {
  // 如果是自己的主页，使用内容面板切换
  if (isSelf.value) {
    toggleContent(type)
  } else {
    // 他人主页：直接切换tab并加载
    activeTab.value = type
    posts.value = []
    users.value = []
    fetchContent(true)
  }
}

/**
 * 跳转我的圈子
 * @returns {void}
 * @description 导航到圈子广场页面
 */
function goMyCircles(): void {
  router.push('/circle')
}

/**
 * 跳转草稿箱
 * @returns {void}
 * @description 当前版本展示开发中提示
 */
function goDrafts(): void {
  showToast('草稿箱功能开发中')
}

/**
 * 跳转账号设置
 * @returns {void}
 * @description 导航到设置页面
 */
function goSettings(): void {
  router.push('/settings')
}

/**
 * 显示关于信息
 * @returns {void}
 * @description 展示当前应用版本号
 */
function goAbout(): void {
  showToast('MintHive v1.0.0')
}

/**
 * 退出登录
 * @returns {Promise<void>}
 * @description 关闭确认弹窗，调用 userStore.logout 清除登录态
 */
async function onLogout(): Promise<void> {
  showLogoutConfirm.value = false
  try {
    await userStore.logout()
    showToast('已退出登录')
  } catch {
    showToast('退出失败')
  }
}

// ---------- 生命周期 ----------
onMounted(() => {
  fetchUserProfile()
  // 默认展开动态内容（帖子）
  if (isSelf.value) {
    showContent.value = true
  }
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
  line-height: 1;
  height: 1em;
  display: inline-flex;
  align-items: center;
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

// ---------- 内容区域标题栏 ----------
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
    padding: $space-3 $space-4;
  }
}

.current-tab-label {
  font-size: 16px;
  font-weight: 600;
  color: $ink-900;
}

// ---------- 非自己主页的Tab切换 ----------
.tab-switch-inline {
  display: flex;
  gap: $space-2;
  flex-wrap: wrap;
}

.tab-chip-inline {
  padding: $space-1 $space-3;
  border-radius: $radius-pill;
  border: 1px solid $ink-100;
  background: none;
  font-size: 13px;
  font-weight: 500;
  color: $ink-500;
  cursor: pointer;
  transition: all $dur-fast $ease-out;

  &:hover {
    background: rgba(78, 205, 196, 0.06);
    color: $ink-700;
    border-color: $mint-300;
  }

  &.active {
    background: $mint-50;
    color: $mint-700;
    font-weight: 600;
    border-color: $mint-300;
  }
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

.users-list {
  display: flex;
  flex-direction: column;
  gap: $space-2;
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

// ========== 横向内容切换按钮 ==========
.tab-switch {
  display: flex;
  border-radius: $radius-md;
  padding: 4px;
  gap: 0;
}

.tab-chip {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: $space-2 $space-3;
  border-radius: $radius-sm;
  border: none;
  background: none;
  font-size: 13px;
  font-weight: 500;
  color: $ink-500;
  cursor: pointer;
  transition: all $dur-fast $ease-out;

  svg {
    color: inherit;
    opacity: 0.7;
  }

  &:hover {
    background: rgba(78, 205, 196, 0.06);
    color: $ink-700;
  }

  &.active {
    background: $mint-50;
    color: $mint-700;
    font-weight: 600;

    svg {
      opacity: 1;
      color: $mint-600;
    }
  }
}

// ========== 内容面板（展开时替换菜单） ==========
.content-panel {
  border-radius: $radius-md;
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: center;
  gap: $space-2;
  padding: $space-3 $space-4;
  border-bottom: 1px solid $ink-100;
  position: sticky;
  top: 0;
  background: inherit;
  z-index: 1;
}

.back-btn {
  width: 32px;
  height: 32px;
  border-radius: $radius-sm;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  color: $ink-500;
  cursor: pointer;
  flex-shrink: 0;
  transition: all $dur-fast $ease-out;

  &:hover {
    background: rgba(78, 205, 196, 0.08);
    color: $mint-600;
  }
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: $ink-900;
}

.panel-placeholder {
  width: 32px;
  flex-shrink: 0;
}

// ========== 功能菜单（仅自己的主页） ==========
.menu-section {
  position: relative;
  z-index: 1;
  padding: $space-3 $space-4;
}

.menu-inner {
  max-width: 600px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: $space-3;
}

.menu-group {
  border-radius: $radius-md;
  overflow: hidden;
  animation: fade-up 0.5s $ease-out both;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: $space-3;
  width: 100%;
  padding: $space-4;
  background: none;
  border: none;
  cursor: pointer;
  transition: background $dur-fast ease;
  text-align: left;

  &:hover {
    background: rgba(78, 205, 196, 0.04);
  }

  &:active {
    background: rgba(78, 205, 196, 0.08);
  }
}

.menu-icon {
  width: 40px;
  height: 40px;
  border-radius: $radius-sm;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  &.posts-icon {
    background: rgba(78, 205, 196, 0.12);
    color: $mint-500;
  }

  &.collects-icon {
    background: rgba(255, 92, 138, 0.12);
    color: $rose-500;
  }

  &.likes-icon {
    background: rgba(255, 107, 107, 0.12);
    color: $coral-500;
  }

  &.circle-icon {
    background: rgba(107, 203, 119, 0.12);
    color: $leaf-500;
  }

  &.draft-icon {
    background: rgba(78, 205, 196, 0.12);
    color: $mint-500;
  }

  &.settings-icon {
    background: rgba(154, 160, 188, 0.12);
    color: $ink-500;
  }

  &.about-icon {
    background: rgba(255, 182, 39, 0.12);
    color: $amber-500;
  }
}

.menu-label {
  flex: 1;
  font-size: 15px;
  font-weight: 500;
  color: $ink-700;
}

/** 当前选中的内容菜单项 */
.menu-item.is-active {
  .menu-label {
    font-weight: 600;
    color: $mint-600;
  }
}

.menu-active-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: $mint-500;
  flex-shrink: 0;
}

.menu-arrow {
  color: $ink-300;
  flex-shrink: 0;
  transition: transform $dur-fast $ease-out;
}

.menu-item:hover .menu-arrow {
  transform: translateX(3px);
  color: $mint-500;
}

.menu-divider {
  height: 1px;
  background: $ink-50;
  margin: 0 $space-4;
}

// ---------- 退出登录按钮 ----------
.logout-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $space-2;
  width: 100%;
  padding: $space-4;
  border-radius: $radius-md;
  border: none;
  font-size: 15px;
  font-weight: 600;
  color: $coral-500;
  cursor: pointer;
  transition: all $dur-fast $ease-out;
  animation: fade-up 0.5s $ease-out both;
  animation-delay: 0.2s;

  &:hover {
    background: rgba(255, 107, 107, 0.06);
    color: #e55a5a;
  }

  &:active {
    transform: scale(0.98);
  }
}

// ---------- 退出确认弹窗 ----------
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: rgba(26, 29, 46, 0.5);
  @include center;
  padding: $space-4;
}

.modal-card {
  width: 100%;
  max-width: 340px;
  border-radius: $radius-lg;
  padding: $space-8 $space-6 $space-6;
  text-align: center;
  position: relative;
  overflow: hidden;
  animation: scale-in 0.35s $ease-spring both;
  background: linear-gradient(160deg, #2A3D3C 0%, #2D3946 100%);
  border: 1px solid rgba(78, 205, 196, 0.18);
}

.modal-deco {
  position: absolute;
  top: -10px;
  right: -10px;
  pointer-events: none;
}

.modal-hex {
  @include hexagon(60px);
  background: $grad-mint;
  opacity: 0.08;
}

.modal-title {
  font-size: 20px;
  font-weight: 700;
  color: $ink-900;
  margin-bottom: $space-2;
}

.modal-desc {
  font-size: 14px;
  color: $ink-500;
  line-height: 1.6;
  margin-bottom: $space-6;
}

.modal-actions {
  display: flex;
  gap: $space-3;
}

.modal-btn {
  flex: 1;
  padding: $space-3 0;
  border-radius: $radius-pill;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  border: none;
  transition: all $dur-fast $ease-out;

  &.cancel {
    background: $ink-700;
    color: $ink-300;

    &:hover {
      background: $ink-800;
    }
  }

  &.confirm {
    background: $coral-500;
    color: #fff;

    &:hover {
      background: #e55a5a;
      transform: translateY(-1px);
    }
  }
}

// ---------- 弹窗动画 ----------
.modal-fade-enter-active {
  animation: fade-in 0.25s ease both;
}
.modal-fade-leave-active {
  animation: fade-in 0.2s ease reverse both;
}
</style>
