<template>
  <div class="home-page">
    <!-- 蜂巢装饰背景 -->
    <div class="hex-bg" />

    <!-- 顶部导航栏 -->
    <header class="home-header">
      <div class="header-inner">
        <h1 class="logo gradient-text font-display">PulseSocial</h1>
        <div class="header-actions">
          <van-icon name="search" size="22" class="action-icon" @click="onSearch" />
          <van-icon name="chat-o" size="22" class="action-icon" :badge="unreadCount || ''" @click="onMessage" />
        </div>
      </div>
    </header>

    <!-- Tab 切换栏 -->
    <nav class="feed-tabs">
      <div class="tabs-inner">
        <button
          v-for="tab in tabs"
          :key="tab.value"
          :class="['tab-btn', { active: activeTab === tab.value }]"
          @click="switchTab(tab.value)"
        >
          <span class="tab-label">{{ tab.label }}</span>
          <span v-if="activeTab === tab.value" class="tab-indicator" />
        </button>
      </div>
    </nav>

    <!-- 系统公告横幅 -->
    <Transition name="slide-down">
      <div
        v-if="bannerVisible && latestAnnouncement"
        class="announcement-banner glass-card"
        @click="goAnnouncementList"
      >
        <svg class="announcement-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9M13.73 21a2 2 0 01-3.46 0" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        <span class="announcement-title">{{ latestAnnouncement.title }}</span>
        <van-icon name="cross" class="announcement-close" @click.stop="dismissAnnouncement" />
      </div>
    </Transition>

    <!-- 首页轮播 -->
    <BannerSwiper />

    <!-- 下拉刷新 -->
    <van-pull-refresh
      v-model="refreshing"
      @refresh="onRefresh"
      :success-text="'刷新成功'"
      :pull-distance="60"
    >
      <!-- 骨架屏 -->
      <div v-if="loading && posts.length === 0" class="skeleton-grid">
        <div v-for="i in 6" :key="i" class="skeleton-card shimmer">
          <div class="skeleton-img" />
          <div class="skeleton-text" />
          <div class="skeleton-text short" />
        </div>
      </div>

      <!-- 瀑布流信息流 -->
      <div v-else-if="posts.length > 0" class="waterfall-container">
        <div class="waterfall-column left-column">
          <TransitionGroup name="card-anim">
            <PostCard
              v-for="post in leftPosts"
              :key="post.id"
              :post="post"
              class="stagger-item"
              @click="goDetail(post.id)"
              @click-user="goProfile"
              @like="onLike"
              @collect="onCollect"
              @openShare="onOpenShare"
              @viewShares="onViewShares"
              @editPost="goDetail"
              @deletePost="onDeletePost"
              @toggleVisibility="onToggleVisibility"
            />
          </TransitionGroup>
        </div>
        <div class="waterfall-column right-column">
          <TransitionGroup name="card-anim">
            <PostCard
              v-for="post in rightPosts"
              :key="post.id"
              :post="post"
              class="stagger-item"
              @click="goDetail(post.id)"
              @click-user="goProfile"
              @like="onLike"
              @collect="onCollect"
              @openShare="onOpenShare"
              @viewShares="onViewShares"
              @editPost="goDetail"
              @deletePost="onDeletePost"
              @toggleVisibility="onToggleVisibility"
            />
          </TransitionGroup>
        </div>
      </div>

      <!-- 空状态 -->
      <EmptyState
        v-else-if="!loading"
        title="还没有帖子哦"
        description="快去发布第一条动态吧"
        icon="notes-o"
      />
    </van-pull-refresh>

    <!-- 上拉加载更多 -->
    <div v-if="posts.length > 0" class="load-more-area">
      <LoadingSpinner v-if="loadingMore" />
      <p v-else-if="!hasMore" class="no-more-text">已经到底啦 ~</p>
    </div>

    <!-- 新评论实时提醒浮窗 -->
    <Transition name="slide-up">
      <div v-if="newCommentTip" class="realtime-tip glass-card" @click="onRealtimeTipClick">
        <van-icon name="chat-o" class="tip-icon" />
        <span class="tip-text">{{ newCommentTip }}</span>
        <van-icon name="cross" class="tip-close" @click.stop="newCommentTip = ''" />
      </div>
    </Transition>

    <!-- 发帖 FAB -->
    <button class="fab-create glass-card" @click="goCreate">
      <van-icon name="edit" size="24" />
    </button>

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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import PostCard from '@/components/PostCard.vue'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import EmptyState from '@/components/EmptyState.vue'
import ShareSheet from '@/components/ShareSheet.vue'
import ShareChainDialog from '@/components/ShareChainDialog.vue'
import BannerSwiper from '@/components/BannerSwiper.vue'
import { useUserStore } from '@/stores/user'
import { useChatStore } from '@/stores/chat'
import { getFeed, toggleLike, toggleCollect, deletePost, togglePostVisibility } from '@/api/post'
import { wsClient } from '@/utils/websocket'
import { getLatestAnnouncement } from '@/api/announcement'
import type { Announcement } from '@/api/announcement'
import type { Post } from '@/types'

// ---------- 路由与Store ----------
const router = useRouter()
const userStore = useUserStore()
const chatStore = useChatStore()

// ---------- Tab 配置 ----------
interface TabItem {
  label: string
  value: 'recommend' | 'latest' | 'hot'
}

const tabs: TabItem[] = [
  { label: '推荐', value: 'recommend' },
  { label: '最新', value: 'latest' },
  { label: '最热', value: 'hot' }
]

// ---------- 响应式数据 ----------
const activeTab = ref<'recommend' | 'latest' | 'hot'>('recommend')
const posts = ref<Post[]>([])
const loading = ref(false)
const loadingMore = ref(false)
const refreshing = ref(false)
const hasMore = ref(true)
const currentPage = ref(1)
const pageSize = 10
const newCommentTip = ref('')

// ---------- 系统公告横幅 ----------
const latestAnnouncement = ref<Announcement | null>(null)
const bannerVisible = ref(true)

async function fetchLatestAnnouncement(): Promise<void> {
  try {
    const res = await getLatestAnnouncement()
    const announcement: Announcement = res.data
    const dismissedKey = `announcement_dismissed_${announcement.id}`
    const isDismissed = localStorage.getItem(dismissedKey) === 'true'
    if (announcement && !isDismissed) {
      latestAnnouncement.value = announcement
      bannerVisible.value = true
    }
  } catch {}
}

function dismissAnnouncement(): void {
  if (latestAnnouncement.value) {
    const dismissedKey = `announcement_dismissed_${latestAnnouncement.value.id}`
    localStorage.setItem(dismissedKey, 'true')
    bannerVisible.value = false
  }
}

function goAnnouncementList(): void {
  router.push('/chat?tab=announce')
}

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

// ---------- 计算属性 ----------
/** 未读消息数 */
const unreadCount = computed(() => chatStore.unreadCount || 0)

/** 瀑布流左列帖子 */
const leftPosts = computed(() =>
  posts.value.filter((_, i) => i % 2 === 0)
)

/** 瀑布流右列帖子 */
const rightPosts = computed(() =>
  posts.value.filter((_, i) => i % 2 !== 0)
)

// ---------- 数据加载 ----------

/**
 * 加载信息流数据
 * @param {boolean} isRefresh - 是否为下拉刷新
 * @returns {Promise<void>}
 * @description 根据当前Tab模式请求信息流，支持首次加载和刷新
 */
async function fetchFeed(isRefresh = false): Promise<void> {
  if (isRefresh) {
    currentPage.value = 1
    hasMore.value = true
  }

  try {
    const res = await getFeed(activeTab.value, currentPage.value, pageSize)
    const pageData = res.data || {}
    const list: Post[] = pageData.records || []

    // 对 liked/collected 做布尔值兜底，确保 null/undefined → false
    // 避免后续 !post.liked 切换时出现 !!undefined = true 的异常行为
    list.forEach(p => {
      p.liked = !!p.liked
      p.collected = !!p.collected
    })

    if (isRefresh || currentPage.value === 1) {
      posts.value = list
    } else {
      posts.value.push(...list)
    }

    // 根据分页元数据判断是否还有更多
    const total = pageData.total ?? 0
    hasMore.value = (currentPage.value * pageSize) < total
  } catch (err: any) {
    showToast('加载失败，请重试')
  } finally {
    loading.value = false
    refreshing.value = false
    loadingMore.value = false
  }
}

/**
 * 切换Tab
 * @param {'recommend' | 'latest' | 'hot'} tab - 目标Tab
 * @returns {void}
 * @description 切换信息流模式并重新加载数据
 */
function switchTab(tab: 'recommend' | 'latest' | 'hot'): void {
  if (activeTab.value === tab) return
  activeTab.value = tab
  loading.value = true
  posts.value = []
  fetchFeed(true)
}

/**
 * 下拉刷新回调
 * @returns {void}
 */
function onRefresh(): void {
  fetchFeed(true)
}

/**
 * 滚动到底部加载下一页
 * @returns {Promise<void>}
 * @description 检测滚动到底部触发加载下一页数据
 */
async function onLoadMore(): Promise<void> {
  if (loading.value || loadingMore.value || !hasMore.value) return
  loadingMore.value = true
  currentPage.value++
  await fetchFeed()
}

/**
 * 点赞/取消点赞
 * @param {number} postId - 帖子ID（由PostCard emit传出）
 * @returns {Promise<void>}
 */
async function onLike(postId: number): Promise<void> {
  const post = posts.value.find(p => p.id === postId)
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
 * 收藏/取消收藏
 * @param {number} postId - 帖子ID（由PostCard emit传出）
 * @returns {Promise<void>}
 */
async function onCollect(postId: number): Promise<void> {
  const post = posts.value.find(p => p.id === postId)
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
 * @description 调用接口切换可见性，更新本地数据
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
 */
function goDetail(id: number): void {
  router.push(`/post/${id}`)
}

/**
 * 跳转用户主页
 * @param {number} userId - 用户ID（由PostCard emit传出）
 */
function goProfile(userId: number): void {
  router.push(`/profile/${userId}`)
}

/**
 * 跳转发帖页
 */
function goCreate(): void {
  router.push('/post/create')
}

/**
 * 跳转搜索页
 */
function onSearch(): void {
  router.push('/search')
}

/**
 * 跳转消息页
 */
function onMessage(): void {
  router.push('/chat')
}

// ---------- WebSocket 实时推送 ----------

/**
 * WebSocket 新评论推送处理
 * @param {any} data - 推送数据
 * @description 接收实时评论推送并显示浮窗提醒
 */
function onWsComment(data: any): void {
  if (data?.nickname && data?.content) {
    newCommentTip.value = `${data.nickname} 评论了: ${data.content.slice(0, 20)}...`
    setTimeout(() => {
      newCommentTip.value = ''
    }, 5000)
  }
}

/**
 * 点击实时提醒浮窗
 * @description 跳转到对应帖子详情
 */
function onRealtimeTipClick(): void {
  newCommentTip.value = ''
}

// ---------- 无限滚动 ----------

/**
 * 滚动事件处理
 * @description 检测滚动到底部触发加载更多
 */
function onScroll(): void {
  const scrollTop = window.scrollY || document.documentElement.scrollTop
  const windowHeight = window.innerHeight
  const docHeight = document.documentElement.scrollHeight
  if (scrollTop + windowHeight >= docHeight - 200) {
    onLoadMore()
  }
}

// ---------- 生命周期 ----------
onMounted(() => {
  loading.value = true
  fetchFeed()
  fetchLatestAnnouncement()
  wsClient.on('comment', onWsComment)
  window.addEventListener('scroll', onScroll, { passive: true })
})

onUnmounted(() => {
  wsClient.off('comment', onWsComment)
  window.removeEventListener('scroll', onScroll)
})
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;
@use '@/styles/mixins.scss' as *;

.home-page {
  min-height: 100vh;
  background: $ink-50;
  position: relative;
  padding-bottom: 80px;
  overscroll-behavior: contain;
}

// ---------- 顶部导航 ----------
.home-header {
  position: sticky;
  top: 0;
  z-index: 100;
  @include glass(20px, rgba(255, 255, 255, 0.82));

  .header-inner {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: $space-3 $space-4;
    max-width: 1200px;
    margin: 0 auto;
  }

  .logo {
    font-size: 22px;
    letter-spacing: -0.5px;
  }

  .header-actions {
    display: flex;
    gap: $space-4;
    align-items: center;
  }

  .action-icon {
    color: $ink-700;
    transition: color $dur-fast $ease-out;
    cursor: pointer;

    &:hover {
      color: $mint-500;
    }
  }
}

// ---------- Tab 切换 ----------
.feed-tabs {
  position: sticky;
  top: 52px;
  z-index: 99;
  @include glass(16px, rgba(255, 255, 255, 0.78));
  border-bottom: 1px solid $ink-100;

  .tabs-inner {
    display: flex;
    max-width: 1200px;
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

// ---------- 系统公告横幅 ----------
.announcement-banner {
  display: flex;
  align-items: center;
  gap: $space-2;
  height: 40px;
  margin: $space-2 $space-4 0;
  padding: 0 $space-3;
  border-radius: $radius-md;
  cursor: pointer;
  transition: transform $dur-fast $ease-out, box-shadow $dur-fast $ease-out;
  @include glass(12px, rgba(78, 205, 196, 0.08));
  &:hover { transform: translateY(-1px); box-shadow: $shadow-md; }
  &:active { transform: scale(0.98); }
  .announcement-icon { width: 18px; height: 18px; color: $mint-500; flex-shrink: 0; }
  .announcement-title { flex: 1; font-size: 13px; font-weight: 500; color: $ink-700; @include ellipsis-1; }
  .announcement-close { color: $ink-300; font-size: 14px; flex-shrink: 0; padding: $space-1; transition: color $dur-fast $ease-out; &:hover { color: $ink-500; } }
}
.slide-down-enter-active { animation: fade-up 0.4s $ease-spring both; }
.slide-down-leave-active { animation: fade-up 0.3s $ease-out reverse both; }

// ---------- 骨架屏 ----------
.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: $space-3;
  padding: $space-4;
  max-width: 1200px;
  margin: 0 auto;
}

.skeleton-card {
  border-radius: $radius-md;
  overflow: hidden;
  background: white;
  padding: $space-3;
}

.skeleton-img {
  width: 100%;
  height: 160px;
  border-radius: $radius-sm;
  background: $ink-100;
}

.skeleton-text {
  height: 14px;
  margin-top: $space-3;
  border-radius: $radius-xs;
  background: $ink-100;

  &.short {
    width: 60%;
  }
}

// ---------- 瀑布流 ----------
.waterfall-container {
  display: flex;
  gap: $space-3;
  padding: $space-4;
  max-width: 1200px;
  margin: 0 auto;
  align-items: flex-start;

  @include mobile {
    padding: $space-3;
    gap: $space-2;
  }
}

.waterfall-column {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: $space-3;

  @include mobile {
    gap: $space-2;
  }
}

// ---------- 卡片动画 ----------
.card-anim-enter-active {
  animation: fade-up 0.5s $ease-out both;
}

.card-anim-leave-active {
  animation: scale-in 0.3s $ease-out reverse both;
}

.stagger-item {
  animation: fade-up 0.6s $ease-out both;

  @for $i from 1 through 20 {
    &:nth-child(#{$i}) {
      animation-delay: #{$i * 0.06}s;
    }
  }
}

// ---------- 加载更多 ----------
.load-more-area {
  display: flex;
  justify-content: center;
  padding: $space-6 0;
}

.no-more-text {
  font-size: 13px;
  color: $ink-300;
}

// ---------- 实时提醒浮窗 ----------
.realtime-tip {
  position: fixed;
  top: 120px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 200;
  display: flex;
  align-items: center;
  gap: $space-2;
  padding: $space-2 $space-4;
  border-radius: $radius-pill;
  box-shadow: $shadow-md;
  max-width: 90vw;
  cursor: pointer;
  animation: fade-up 0.4s $ease-spring both;

  .tip-icon {
    color: $mint-500;
    flex-shrink: 0;
  }

  .tip-text {
    font-size: 13px;
    color: $ink-700;
    @include ellipsis-1;
  }

  .tip-close {
    color: $ink-300;
    flex-shrink: 0;
    margin-left: $space-1;
  }
}

.slide-up-enter-active {
  animation: fade-up 0.4s $ease-spring both;
}

.slide-up-leave-active {
  animation: fade-up 0.3s $ease-out reverse both;
}

// ---------- 发帖 FAB ----------
.fab-create {
  position: fixed;
  right: $space-6;
  bottom: 100px;
  z-index: 150;
  width: 56px;
  height: 56px;
  border-radius: $radius-lg;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  background: $grad-mint;
  box-shadow: $shadow-glow-mint;
  transition: transform $dur-base $ease-spring, box-shadow $dur-base $ease-out;
  animation: ai-glow 3s ease-in-out infinite;

  &:hover {
    transform: scale(1.08);
    box-shadow: 0 0 32px rgba(78, 205, 196, 0.6);
  }

  &:active {
    transform: scale(0.95);
  }

  @include mobile {
    right: $space-4;
    bottom: 80px;
    width: 50px;
    height: 50px;
  }
}

/**
 * 移动端安全区域适配
 * @description 为页面根容器添加顶部安全区域内边距，避免内容被刘海屏遮挡
 */
@include mobile {
  .home-page {
    padding-top: env(safe-area-inset-top);
  }
}
</style>
