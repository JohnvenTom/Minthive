<template>
  <div class="circle-detail">
    <!-- 加载状态 -->
    <LoadingSpinner v-if="loading" />

    <template v-else-if="circle">
      <!-- Banner大图 + 渐变遮罩 -->
      <section class="banner-hero">
        <img :src="circle.banner" :alt="circle.name" class="banner-img" />
        <div class="banner-gradient" />

        <!-- 返回按钮 -->
        <button class="back-btn" @click="goBack">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" width="22" height="22">
            <path d="M15 18l-6-6 6-6" />
          </svg>
        </button>

        <!-- 圈子信息浮层 -->
        <div class="hero-info">
          <div class="hero-hex-avatar">
            <img :src="circle.avatar" :alt="circle.name" />
          </div>
          <div class="hero-meta">
            <h1 class="hero-name">{{ circle.name }}</h1>
            <div class="hero-tags">
              <span class="tag-category">{{ circle.category }}</span>
              <span :class="['tag-type', circle.type]">
                {{ circle.type === 'public' ? '公开圈' : '私密圈' }}
              </span>
            </div>
          </div>
        </div>
      </section>

      <!-- 圈子信息卡片 -->
      <section class="info-card">
        <p class="circle-intro">{{ circle.intro }}</p>

        <!-- 数据统计 -->
        <div class="stats-row">
          <div class="stat-item">
            <span class="stat-value">{{ formatCount(circle.memberCount) }}</span>
            <span class="stat-label">成员</span>
          </div>
          <div class="stat-divider" />
          <div class="stat-item">
            <span class="stat-value">{{ formatCount(circle.postCount) }}</span>
            <span class="stat-label">帖子</span>
          </div>
          <div class="stat-divider" />
          <div class="stat-item" @click="showOwnerInfo">
            <span class="stat-value owner-name">{{ circle.ownerName }}</span>
            <span class="stat-label">圈主</span>
          </div>
        </div>

        <!-- 加入/退出按钮 -->
        <div class="action-row">
          <button
            v-if="!circle.joined"
            :class="['join-btn', { 'join-public': circle.type === 'public' }]"
            @click="handleJoin"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
              <path d="M12 5v14M5 12h14" />
            </svg>
            {{ circle.type === 'public' ? '加入圈子' : '申请加入' }}
          </button>
          <button
            v-else
            class="leave-btn"
            @click="handleLeave"
          >
            已加入 · 退出
          </button>
        </div>
      </section>

      <!-- 圈内公告 -->
      <section v-if="circle.notice" class="notice-section">
        <div class="section-head">
          <h3 class="section-title">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
              <path d="M22 17H2a3 3 0 0 0 3-3V9a7 7 0 0 1 14 0v5a3 3 0 0 0 3 3zm-8.27 4a2 2 0 0 1-3.46 0" />
            </svg>
            圈内公告
          </h3>
          <button
            v-if="isOwner"
            class="edit-notice-btn"
            @click="showNoticeEditor = true"
          >
            编辑
          </button>
        </div>
        <div class="notice-content">
          {{ circle.notice }}
        </div>
      </section>

      <!-- 管理员操作区 -->
      <section v-if="isOwner" class="admin-section">
        <h3 class="section-title">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
            <path d="M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z" />
            <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1Z" />
          </svg>
          管理操作
        </h3>
        <div class="admin-actions">
          <button class="admin-action-btn" @click="goManageMembers">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="20" height="20">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
              <circle cx="9" cy="7" r="4" />
              <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
              <path d="M16 3.13a4 4 0 0 1 0 7.75" />
            </svg>
            管理成员
          </button>
          <button class="admin-action-btn" @click="showNoticeEditor = true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="20" height="20">
              <path d="M22 17H2a3 3 0 0 0 3-3V9a7 7 0 0 1 14 0v5a3 3 0 0 0 3 3zm-8.27 4a2 2 0 0 1-3.46 0" />
            </svg>
            发布公告
          </button>
          <button class="admin-action-btn" @click="handlePinPost">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="20" height="20">
              <path d="m15 4.5-3 3L7.5 6l-3 3L9 12l-1.5 4.5L12 15l4.5 3-1.5-4.5L21 9l-3-3L12 7.5l3-3z" />
            </svg>
            置顶帖子
          </button>
        </div>
      </section>

      <!-- 置顶帖子 -->
      <section v-if="pinnedPost" class="pinned-section">
        <div class="section-head">
          <h3 class="section-title pinned">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
              <path d="m15 4.5-3 3L7.5 6l-3 3L9 12l-1.5 4.5L12 15l4.5 3-1.5-4.5L21 9l-3-3L12 7.5l3-3z" />
            </svg>
            置顶
          </h3>
        </div>
        <PostCard :post="pinnedPost" />
      </section>

      <!-- 圈内信息流 -->
      <section class="feed-section">
        <div class="section-head">
          <h3 class="section-title">
            <span class="hex-dot" />
            圈内动态
          </h3>
          <span class="post-count">{{ circle.postCount }} 篇帖子</span>
        </div>

        <LoadingSpinner v-if="loadingPosts && !postList.length" />

        <EmptyState
          v-else-if="!loadingPosts && !postList.length"
          description="圈内暂无动态，快来发第一篇吧~"
        />

        <div v-else class="post-list">
          <PostCard
            v-for="post in postList"
            :key="post.id"
            :post="post"
          />
        </div>

        <!-- 加载更多 -->
        <div v-if="postList.length" class="load-more-area">
          <LoadingSpinner v-if="loadingMorePosts" size="small" />
          <p v-else-if="!hasMorePosts" class="no-more">— 没有更多了 —</p>
          <button v-else class="load-more-btn" @click="loadMorePosts">加载更多</button>
        </div>
      </section>
    </template>

    <!-- 圈子不存在 -->
    <EmptyState
      v-else
      description="圈子不存在或已被解散"
      icon="error"
    />

    <!-- 私密圈申请弹窗 -->
    <transition name="modal-fade">
      <div v-if="showApplyModal" class="modal-mask" @click.self="showApplyModal = false">
        <div class="modal-box">
          <h3 class="modal-title">申请加入私密圈</h3>
          <p class="modal-desc">请填写申请理由，圈主审核通过后即可加入</p>
          <textarea
            v-model="applyReason"
            class="modal-textarea"
            placeholder="说说你为什么想加入这个圈子..."
            maxlength="200"
          />
          <div class="modal-footer">
            <button class="modal-cancel" @click="showApplyModal = false">取消</button>
            <button
              :class="['modal-confirm', { disabled: !applyReason.trim() }]"
              :disabled="!applyReason.trim() || applying"
              @click="submitApply"
            >
              {{ applying ? '提交中...' : '提交申请' }}
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- 公告编辑弹窗 -->
    <transition name="modal-fade">
      <div v-if="showNoticeEditor" class="modal-mask" @click.self="showNoticeEditor = false">
        <div class="modal-box">
          <h3 class="modal-title">编辑公告</h3>
          <textarea
            v-model="editNoticeText"
            class="modal-textarea"
            placeholder="输入圈子公告..."
            maxlength="500"
          />
          <div class="modal-footer">
            <button class="modal-cancel" @click="showNoticeEditor = false">取消</button>
            <button class="modal-confirm" @click="saveNotice">保存</button>
          </div>
        </div>
      </div>
    </transition>

    <!-- 退出确认弹窗 -->
    <transition name="modal-fade">
      <div v-if="showLeaveConfirm" class="modal-mask" @click.self="showLeaveConfirm = false">
        <div class="modal-box">
          <h3 class="modal-title">确认退出圈子</h3>
          <p class="modal-desc">退出后你将无法查看圈内动态，确定要退出吗？</p>
          <div class="modal-footer">
            <button class="modal-cancel" @click="showLeaveConfirm = false">再想想</button>
            <button class="modal-confirm danger" @click="confirmLeave">确认退出</button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
/**
 * 圈子详情页面
 * 展示圈子Banner、信息、公告、圈内动态，支持加入/退出、管理员操作
 */
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCircleDetail, getCirclePosts, joinCircle, leaveCircle } from '@/api/circle'
import { getPostDetail } from '@/api/post'
import { useUserStore } from '@/stores/user'
import type { Circle, Post } from '@/types'
import PostCard from '@/components/PostCard.vue'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// ---------- 响应式状态 ----------
/** 圈子详情数据 */
const circle = ref<Circle | null>(null)
/** 帖子列表 */
const postList = ref<Post[]>([])
/** 置顶帖子 */
const pinnedPost = ref<Post | null>(null)
/** 帖子当前页码 */
const postPage = ref(1)
/** 帖子每页数量 */
const postPageSize = 10
/** 是否有更多帖子 */
const hasMorePosts = ref(true)
/** 圈子详情加载中 */
const loading = ref(true)
/** 帖子加载中 */
const loadingPosts = ref(false)
/** 加载更多帖子中 */
const loadingMorePosts = ref(false)
/** 是否显示申请弹窗 */
const showApplyModal = ref(false)
/** 申请理由 */
const applyReason = ref('')
/** 申请提交中 */
const applying = ref(false)
/** 是否显示退出确认弹窗 */
const showLeaveConfirm = ref(false)
/** 是否显示公告编辑弹窗 */
const showNoticeEditor = ref(false)
/** 编辑公告文本 */
const editNoticeText = ref('')

// ---------- 计算属性 ----------
/** 圈子ID */
const circleId = computed(() => Number(route.params.id))

/** 当前用户是否为圈主 */
const isOwner = computed(() => {
  if (!circle.value || !userStore.user) return false
  return circle.value.ownerId === userStore.user.id
})

// ---------- 方法 ----------

/**
 * 加载圈子详情
 * @returns {Promise<void>}
 */
async function loadCircleDetail(): Promise<void> {
  loading.value = true
  try {
    const res = await getCircleDetail(circleId.value)
    circle.value = res.data
    editNoticeText.value = circle.value?.notice || ''
  } catch {
    circle.value = null
  } finally {
    loading.value = false
  }
}

/**
 * 加载圈内帖子列表
 * @param {boolean} append - 是否追加模式
 * @returns {Promise<void>}
 */
async function loadPosts(append = false): Promise<void> {
  if (append) {
    loadingMorePosts.value = true
  } else {
    loadingPosts.value = true
  }
  try {
    const res = await getCirclePosts(circleId.value, postPage.value, postPageSize)
    const data = res.data
    if (append) {
      postList.value.push(...data.list)
    } else {
      postList.value = data.list
    }
    hasMorePosts.value = data.hasMore
  } catch {
    if (!append) postList.value = []
  } finally {
    loadingPosts.value = false
    loadingMorePosts.value = false
  }
}

/**
 * 加载置顶帖子
 * @returns {Promise<void>}
 */
async function loadPinnedPost(): Promise<void> {
  // 置顶帖子ID暂存于circle.notice字段后的标记中
  // 此处为示例逻辑，实际由后端提供pinnedPostId
  try {
    // TODO: 替换为实际置顶帖子API
    // const res = await getPinnedPost(circleId.value)
    // pinnedPost.value = res.data
    pinnedPost.value = null
  } catch {
    pinnedPost.value = null
  }
}

/**
 * 处理加入圈子
 */
function handleJoin(): void {
  if (!circle.value) return
  if (circle.value.type === 'private') {
    showApplyModal.value = true
    applyReason.value = ''
  } else {
    doJoin()
  }
}

/**
 * 执行加入圈子（公开圈）
 * @returns {Promise<void>}
 */
async function doJoin(): Promise<void> {
  if (!circle.value) return
  try {
    await joinCircle(circleId.value)
    circle.value.joined = true
    circle.value.memberCount++
  } catch {
    // 错误由全局拦截器处理
  }
}

/**
 * 提交私密圈申请
 * @returns {Promise<void>}
 */
async function submitApply(): Promise<void> {
  if (!applyReason.value.trim() || applying.value) return
  applying.value = true
  try {
    await joinCircle(circleId.value, applyReason.value.trim())
    showApplyModal.value = false
    // 私密圈需等待审核，不立即更新joined状态
  } catch {
    // 错误由全局拦截器处理
  } finally {
    applying.value = false
  }
}

/**
 * 处理退出圈子
 */
function handleLeave(): void {
  showLeaveConfirm.value = true
}

/**
 * 确认退出圈子
 * @returns {Promise<void>}
 */
async function confirmLeave(): Promise<void> {
  if (!circle.value) return
  try {
    await leaveCircle(circleId.value)
    circle.value.joined = false
    circle.value.memberCount--
    showLeaveConfirm.value = false
  } catch {
    // 错误由全局拦截器处理
  }
}

/**
 * 加载更多帖子
 * @returns {Promise<void>}
 */
async function loadMorePosts(): Promise<void> {
  if (loadingMorePosts.value || !hasMorePosts.value) return
  postPage.value++
  await loadPosts(true)
}

/**
 * 保存公告
 * @returns {Promise<void>}
 */
async function saveNotice(): Promise<void> {
  if (!circle.value) return
  // TODO: 调用更新公告API
  circle.value.notice = editNoticeText.value
  showNoticeEditor.value = false
}

/**
 * 置顶帖子操作
 */
function handlePinPost(): void {
  // TODO: 实现置顶帖子选择弹窗
}

/**
 * 跳转管理成员页面
 */
function goManageMembers(): void {
  router.push(`/circle/${circleId.value}/members`)
}

/**
 * 显示圈主信息
 */
function showOwnerInfo(): void {
  if (!circle.value) return
  router.push(`/user/${circle.value.ownerId}`)
}

/**
 * 返回上一页
 */
function goBack(): void {
  router.back()
}

/**
 * 格式化数字显示
 * @param {number} count - 原始数量
 * @returns {string} 格式化后的字符串
 */
function formatCount(count: number): string {
  if (count >= 10000) return (count / 10000).toFixed(1) + 'w'
  if (count >= 1000) return (count / 1000).toFixed(1) + 'k'
  return String(count)
}

/**
 * 处理滚动加载
 */
function handleScroll(): void {
  const scrollTop = window.scrollY || document.documentElement.scrollTop
  const scrollHeight = document.documentElement.scrollHeight
  const clientHeight = document.documentElement.clientHeight

  if (scrollHeight - scrollTop - clientHeight < 200 && hasMorePosts.value && !loadingMorePosts.value && !loadingPosts.value) {
    loadMorePosts()
  }
}

// ---------- 生命周期 ----------
onMounted(async () => {
  await loadCircleDetail()
  if (circle.value) {
    loadPosts()
    loadPinnedPost()
  }
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;
@use '@/styles/mixins.scss' as *;

// ---------- 页面容器 ----------
.circle-detail {
  min-height: 100vh;
  background: $ink-50;
  padding-bottom: 80px;
}

// ---------- Banner大图 ----------
.banner-hero {
  position: relative;
  width: 100%;
  aspect-ratio: 16 / 9;
  overflow: hidden;
  background: $ink-800;
}

.banner-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.banner-gradient {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    to top,
    rgba(26, 29, 46, 0.95) 0%,
    rgba(26, 29, 46, 0.6) 35%,
    rgba(26, 29, 46, 0.15) 70%,
    transparent 100%
  );
}

.back-btn {
  position: absolute;
  top: $space-4;
  left: $space-4;
  z-index: 10;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  @include glass(12px, rgba(26, 29, 46, 0.5));
  color: #fff;
  @include center;
  transition: all $dur-fast $ease-out;

  &:hover {
    background: rgba(26, 29, 46, 0.7);
    transform: scale(1.05);
  }
}

.hero-info {
  position: absolute;
  bottom: $space-5;
  left: $space-5;
  right: $space-5;
  display: flex;
  align-items: center;
  gap: $space-4;
}

.hero-hex-avatar {
  @include hexagon(64px);
  overflow: hidden;
  flex-shrink: 0;
  border: 3px solid $mint-500;
  box-shadow: $shadow-glow-mint;
  animation: hex-breathe 4s ease-in-out infinite;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.hero-meta {
  flex: 1;
  min-width: 0;
}

.hero-name {
  font-family: $font-heading;
  font-size: 24px;
  font-weight: 700;
  color: #fff;
  @include ellipsis-1;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

.hero-tags {
  display: flex;
  gap: $space-2;
  margin-top: $space-1;
}

.tag-category {
  padding: 2px $space-2;
  border-radius: $radius-xs;
  font-size: 11px;
  font-weight: 600;
  color: $mint-500;
  background: rgba(78, 205, 196, 0.2);
  border: 1px solid rgba(78, 205, 196, 0.3);
}

.tag-type {
  padding: 2px $space-2;
  border-radius: $radius-xs;
  font-size: 11px;
  font-weight: 600;

  &.public {
    color: $leaf-500;
    background: rgba(107, 203, 119, 0.2);
    border: 1px solid rgba(107, 203, 119, 0.3);
  }

  &.private {
    color: $amber-500;
    background: rgba(255, 182, 39, 0.2);
    border: 1px solid rgba(255, 182, 39, 0.3);
  }
}

// ---------- 信息卡片 ----------
.info-card {
  margin: -$space-4 $space-4 0;
  position: relative;
  z-index: 5;
  padding: $space-5;
  background: #fff;
  border-radius: $radius-lg;
  box-shadow: $shadow-md;
}

.circle-intro {
  font-size: 14px;
  color: $ink-500;
  line-height: 1.7;
  @include ellipsis(3);
  margin-bottom: $space-4;
}

.stats-row {
  display: flex;
  align-items: center;
  justify-content: space-around;
  padding: $space-3 0;
  border-top: 1px solid $ink-100;
  border-bottom: 1px solid $ink-100;
}

.stat-item {
  @include center(column);
  gap: 2px;
  cursor: default;
}

.stat-value {
  font-family: $font-heading;
  font-size: 20px;
  font-weight: 700;
  color: $ink-900;

  &.owner-name {
    font-size: 14px;
    color: $mint-600;
    cursor: pointer;

    &:hover {
      text-decoration: underline;
    }
  }
}

.stat-label {
  font-size: 12px;
  color: $ink-300;
}

.stat-divider {
  width: 1px;
  height: 28px;
  background: $ink-100;
}

.action-row {
  margin-top: $space-4;
}

.join-btn {
  width: 100%;
  height: 44px;
  border-radius: $radius-md;
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  background: $grad-mint;
  @include center;
  gap: $space-2;
  box-shadow: $shadow-sm;
  transition: all $dur-base $ease-spring;

  &:hover {
    transform: translateY(-1px);
    box-shadow: $shadow-md, $shadow-glow-mint;
  }

  &:active {
    transform: scale(0.98);
  }

  &.join-public {
    background: $grad-mint;
  }
}

.leave-btn {
  width: 100%;
  height: 44px;
  border-radius: $radius-md;
  font-size: 14px;
  font-weight: 500;
  color: $ink-500;
  background: $ink-50;
  border: 1.5px solid $ink-100;
  transition: all $dur-fast $ease-out;

  &:hover {
    color: $coral-500;
    border-color: $coral-500;
    background: rgba(255, 107, 107, 0.05);
  }
}

// ---------- 公告区域 ----------
.notice-section {
  margin: $space-4;
  padding: $space-4;
  background: #fff;
  border-radius: $radius-md;
  box-shadow: $shadow-xs;
  border-left: 4px solid $amber-500;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: $space-3;
}

.section-title {
  font-family: $font-heading;
  font-size: 16px;
  font-weight: 700;
  color: $ink-900;
  display: flex;
  align-items: center;
  gap: $space-2;

  &.pinned {
    color: $amber-600;
  }

  .hex-dot {
    width: 10px;
    height: 10px;
    background: $mint-500;
    clip-path: polygon(50% 0%, 100% 25%, 100% 75%, 50% 100%, 0% 75%, 0% 25%);
  }
}

.edit-notice-btn {
  font-size: 13px;
  color: $mint-500;
  font-weight: 500;
  transition: color $dur-fast $ease-out;

  &:hover {
    color: $mint-600;
  }
}

.notice-content {
  font-size: 14px;
  color: $ink-700;
  line-height: 1.7;
  white-space: pre-wrap;
}

// ---------- 管理员操作区 ----------
.admin-section {
  margin: $space-4;
  padding: $space-4;
  background: #fff;
  border-radius: $radius-md;
  box-shadow: $shadow-xs;
  border-left: 4px solid $mint-500;
}

.admin-actions {
  display: flex;
  gap: $space-3;
  flex-wrap: wrap;
}

.admin-action-btn {
  flex: 1;
  min-width: 100px;
  padding: $space-3 $space-4;
  border-radius: $radius-sm;
  font-size: 13px;
  font-weight: 500;
  color: $ink-700;
  background: $ink-50;
  border: 1.5px solid $ink-100;
  @include center;
  gap: $space-2;
  transition: all $dur-fast $ease-out;

  &:hover {
    color: $mint-600;
    border-color: $mint-300;
    background: rgba(78, 205, 196, 0.05);
  }
}

// ---------- 置顶帖子 ----------
.pinned-section {
  margin: $space-4;
  padding: $space-4;
  background: #fff;
  border-radius: $radius-md;
  box-shadow: $shadow-xs;
  border-left: 4px solid $amber-500;
}

// ---------- 信息流 ----------
.feed-section {
  margin: $space-4;
}

.post-count {
  font-size: 13px;
  color: $ink-300;
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: $space-3;
}

.load-more-area {
  @include center(column);
  padding: $space-6 0;
  min-height: 60px;
}

.no-more {
  font-size: 13px;
  color: $ink-300;
}

.load-more-btn {
  padding: $space-2 $space-6;
  border-radius: $radius-pill;
  font-size: 13px;
  font-weight: 500;
  color: $mint-600;
  background: rgba(78, 205, 196, 0.08);
  border: 1.5px solid rgba(78, 205, 196, 0.3);
  transition: all $dur-fast $ease-out;

  &:hover {
    background: rgba(78, 205, 196, 0.15);
    border-color: $mint-500;
  }
}

// ---------- 弹窗通用 ----------
.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(26, 29, 46, 0.6);
  @include center;
  padding: $space-4;
  backdrop-filter: blur(4px);
}

.modal-box {
  width: 100%;
  max-width: 400px;
  background: #fff;
  border-radius: $radius-lg;
  padding: $space-6;
  box-shadow: $shadow-lg;
  animation: scale-in 0.3s $ease-spring both;
}

.modal-title {
  font-family: $font-heading;
  font-size: 18px;
  font-weight: 700;
  color: $ink-900;
  margin-bottom: $space-2;
}

.modal-desc {
  font-size: 14px;
  color: $ink-500;
  margin-bottom: $space-4;
  line-height: 1.6;
}

.modal-textarea {
  width: 100%;
  min-height: 100px;
  padding: $space-3;
  border-radius: $radius-sm;
  border: 1.5px solid $ink-100;
  font-size: 14px;
  color: $ink-700;
  resize: vertical;
  transition: border-color $dur-fast $ease-out;

  &:focus {
    border-color: $mint-500;
    box-shadow: 0 0 0 3px rgba(78, 205, 196, 0.1);
  }

  &::placeholder {
    color: $ink-300;
  }
}

.modal-footer {
  display: flex;
  gap: $space-3;
  margin-top: $space-5;
}

.modal-cancel {
  flex: 1;
  height: 42px;
  border-radius: $radius-sm;
  font-size: 14px;
  font-weight: 500;
  color: $ink-500;
  background: $ink-50;
  border: 1.5px solid $ink-100;
  transition: all $dur-fast $ease-out;

  &:hover {
    background: $ink-100;
  }
}

.modal-confirm {
  flex: 1;
  height: 42px;
  border-radius: $radius-sm;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  background: $grad-mint;
  transition: all $dur-fast $ease-out;

  &:hover {
    box-shadow: $shadow-glow-mint;
  }

  &.disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  &.danger {
    background: $coral-500;

    &:hover {
      box-shadow: 0 0 20px rgba(255, 107, 107, 0.4);
    }
  }
}

// ---------- 弹窗过渡 ----------
.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: all $dur-base $ease-out;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;

  .modal-box {
    transform: scale(0.9);
  }
}

// ---------- 响应式 ----------
@include mobile {
  .banner-hero {
    aspect-ratio: 16 / 10;
  }

  .hero-hex-avatar {
    @include hexagon(52px);
  }

  .hero-name {
    font-size: 20px;
  }

  .info-card {
    margin: -$space-3 $space-3 0;
    padding: $space-4;
  }

  .notice-section,
  .admin-section,
  .pinned-section,
  .feed-section {
    margin: $space-3;
  }

  .admin-actions {
    flex-direction: column;
  }

  .admin-action-btn {
    min-width: unset;
  }
}
</style>
