<template>
  <div class="circle-detail">
    <!-- 加载状态 -->
    <LoadingSpinner v-if="loading" />

    <template v-else-if="circle">
      <!-- Banner大图 + 渐变遮罩 -->
      <section class="banner-hero">
        <!-- 有 banner 显示图片，无 banner 用渐变背景兜底 -->
        <img v-if="circle.banner" :src="circle.banner" :alt="circle.name" class="banner-img" />
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
              <span class="tag-category">{{ circle.categoryName || '未分类' }}</span>
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
            <img v-if="circle.ownerAvatar" :src="circle.ownerAvatar" class="owner-avatar" />
            <span class="stat-value owner-name">{{ circle.ownerName || '圈主' }}</span>
            <span class="stat-label">圈主</span>
          </div>
        </div>

        <!-- 加入/退出 按钮 -->
        <div class="action-row">
          <!-- 未加入：显示加入/申请按钮 -->
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

          <!-- 已加入 + 普通成员：显示退出按钮 -->
          <button
            v-else-if="!isOwner"
            class="leave-btn"
            @click="handleLeave"
          >
            已加入 · 退出
          </button>

          <!-- 圈主：显示简洁状态提示 -->
          <div v-else class="owner-badge">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
              <path d="M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2Z" fill="currentColor"/>
            </svg>
            圈主
          </div>
        </div>

        <!-- 圈主管理操作（与上方按钮风格统一） -->
        <div v-if="isOwner" class="admin-actions">
          <button class="admin-action-btn members" @click="goManageMembers">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
              <circle cx="9" cy="7" r="4"/>
              <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
              <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
            </svg>
            管理成员
          </button>
          <button
            class="admin-action-btn transfer"
            :disabled="memberList.length === 0"
            :title="memberList.length === 0 ? '暂无可转让的成员' : ''"
            @click="memberList.length > 0 && (showTransferModal = true)"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
              <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
              <circle cx="8.5" cy="7" r="4"/>
              <line x1="20" y1="8" x2="20" y2="14"/>
              <line x1="23" y1="11" x2="17" y2="11"/>
            </svg>
            转让圈主
          </button>
          <button class="admin-action-btn edit" @click="goEditCircle">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
              <path d="M12 20h9"/>
              <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/>
            </svg>
            修改圈子
          </button>
          <button class="admin-action-btn dissolve" @click="showDissolveConfirm = true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
              <circle cx="12" cy="12" r="10"/>
              <path d="M15 9l-6 6M9 9l6 6"/>
            </svg>
            解散圈子
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
        <PostCard
          :post="pinnedPost"
          @click="goPostDetail(pinnedPost.id)"
          @click-user="router.push(`/profile/${$event}`)"
          @like="onLike"
          @collect="onCollect"
          @openShare="onShare"
          @select="onPostActionSelect"
          @editPost="goPostDetail"
          @deletePost="onDeletePost"
          @toggleVisibility="onToggleVisibility"
        />
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
            @click="goPostDetail(post.id)"
            @click-user="router.push(`/profile/${$event}`)"
            @like="onLike"
            @collect="onCollect"
            @openShare="onShare"
            @select="onPostActionSelect"
            @editPost="goPostDetail"
            @deletePost="onDeletePost"
            @toggleVisibility="onToggleVisibility"
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

    <!-- 转让圈主弹窗 -->
    <transition name="modal-fade">
      <div v-if="showTransferModal" class="modal-mask" @click.self="showTransferModal = false">
        <div class="modal-box">
          <h3 class="modal-title">转让圈主</h3>
          <p class="modal-desc">选择一位成员转让圈主身份，转让后你将变为普通成员</p>
          <AnimatedSelect
            v-model="transferUserId"
            :options="memberList.map(m => ({ value: m.userId, label: m.nickname }))"
            placeholder="请选择要转让的成员"
          />
          <div class="modal-footer">
            <button class="modal-cancel" @click="showTransferModal = false">取消</button>
            <button
              :class="['modal-confirm', { disabled: !transferUserId }]"
              :disabled="!transferUserId || transferring"
              @click="confirmTransfer"
            >
              {{ transferring ? '转让中...' : '确认转让' }}
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- 解散圈子确认弹窗 -->
    <transition name="modal-fade">
      <div v-if="showDissolveConfirm" class="modal-mask" @click.self="showDissolveConfirm = false">
        <div class="modal-box modal-danger">
          <h3 class="modal-title">解散圈子</h3>
          <p class="modal-desc warning-text">此操作不可恢复！解散后圈内所有内容将被永久删除，所有成员自动移除。</p>
          <input
            v-model="dissolveConfirmText"
            type="text"
            class="modal-input"
            placeholder='请输入 "解散" 确认操作'
          />
          <div class="modal-footer">
            <button class="modal-cancel" @click="showDissolveConfirm = false">取消</button>
            <button
              :class="['modal-confirm danger', { disabled: dissolveConfirmText !== '解散' }]"
              :disabled="dissolveConfirmText !== '解散' || dissolving"
              @click="confirmDissolve"
            >
              {{ dissolving ? '解散中...' : '确认解散' }}
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- 分享面板 -->
    <ShareSheet
      v-model:show="showShareSheet"
      :post-id="shareTargetPostId || 0"
      @shared="onShared"
      @click-share="(id: number) => goPostDetail(id)"
      @click-user="router.push(`/profile/${$event}`)"
    />
  </div>
</template>

<script setup lang="ts">
/**
 * 圈子详情页面
 * 展示圈子Banner、信息、公告、圈内动态，支持加入/退出、管理员操作
 */
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCircleDetail, getCirclePosts, joinCircle, leaveCircle, getCircleMembers, transferCircleOwner, updateCircleNotice } from '@/api/circle'
import { toggleLike, toggleCollect, sharePost, deletePost, updatePost as togglePostVisibility } from '@/api/post'
import { showToast, showConfirmDialog } from 'vant'
import { useUserStore } from '@/stores/user'
import type { Circle, Post } from '@/types'
import PostCard from '@/components/PostCard.vue'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import EmptyState from '@/components/EmptyState.vue'
import AnimatedSelect from '@/components/AnimatedSelect.vue'
import ShareSheet from '@/components/ShareSheet.vue'

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
/** 是否显示转让圈主弹窗 */
const showTransferModal = ref(false)
/** 转让目标用户ID */
const transferUserId = ref<number | string>('')
/** 转让提交中 */
const transferring = ref(false)
/** 圈子成员列表（用于转让选择） */
const memberList = ref<{ userId: number; nickname: string }[]>([])
/** 是否显示解散确认弹窗 */
const showDissolveConfirm = ref(false)
/** 解散确认文字输入 */
const dissolveConfirmText = ref('')
/** 解散提交中 */
const dissolving = ref(false)
/** 分享面板显示状态 */
const showShareSheet = ref(false)
/** 分享目标帖子ID */
const shareTargetPostId = ref<number | null>(null)

// ---------- 计算属性 ----------
/** 圈子ID */
const circleId = computed(() => Number(route.params.id))

/** 当前用户是否为圈主（使用 == 宽松比较避免 number/string 类型不匹配） */
const isOwner = computed(() => {
  if (!circle.value || !userStore.userInfo) return false
  return String(circle.value.ownerId) === String(userStore.userInfo.id)
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
  try {
    await updateCircleNotice(circleId.value, editNoticeText.value)
    circle.value.notice = editNoticeText.value
    showNoticeEditor.value = false
  } catch {
    // 请求失败时不关闭弹窗，用户可重试
  }
}

/**
 * 跳转管理成员页面
 */
function goManageMembers(): void {
  router.push(`/circle/${circleId.value}/members`)
}

/**
 * 跳转修改圈子页面
 */
function goEditCircle(): void {
  router.push(`/circle/${circleId.value}/edit`)
}

/**
 * 加载圈子成员列表（用于转让选择，仅取第一页足够）
 * @returns {Promise<void>}
 */
async function loadMemberList(): Promise<void> {
  try {
    const res = await getCircleMembers(circleId.value, { page: 1, pageSize: 100 })
    // 排除当前圈主自己，转让候选只保留普通成员
    const currentUserId = userStore.userInfo?.id
    memberList.value = res.data.list
      .filter(m => String(m.userId) !== String(currentUserId))
      .map(m => ({ userId: m.userId, nickname: m.nickname }))
  } catch {
    memberList.value = []
  }
}

/**
 * 确认转让圈主
 * @returns {Promise<void>}
 */
async function confirmTransfer(): Promise<void> {
  if (!transferUserId.value || transferring.value || !circle.value) return
  transferring.value = true
  try {
    await transferCircleOwner(circleId.value, Number(transferUserId.value))
    showTransferModal.value = false
    // 转让成功后自己变为普通成员，刷新详情以更新 isOwner 与各项管理按钮
    await loadCircleDetail()
    // 重新拉取成员列表（转让候选已变化）
    loadMemberList()
  } catch {
    // 错误由全局拦截器处理
  } finally {
    transferring.value = false
  }
}

/**
 * 确认解散圈子
 * @returns {Promise<void>}
 */
async function confirmDissolve(): Promise<void> {
  if (dissolveConfirmText.value !== '解散' || dissolving.value || !circle.value) return
  dissolving.value = true
  try {
    // TODO: 调用解散圈子 API
    // await dissolveCircle(circleId.value)
    showDissolveConfirm.value = false
    router.push('/circle')
  } catch {
    // 错误由全局拦截器处理
  } finally {
    dissolving.value = false
  }
}

/**
 * 显示圈主信息
 */
function showOwnerInfo(): void {
  if (!circle.value) return
  router.push(`/profile/${circle.value.ownerId}`)
}

/**
 * 返回上一页
 */
function goBack(): void {
  router.back()
}

// ---------- 帖子交互（PostCard 事件处理） ----------

/**
 * 跳转帖子详情
 * @param {number} id - 帖子ID
 */
function goPostDetail(id: number): void {
  router.push(`/post/${id}`)
}

/**
 * 点赞/取消点赞帖子
 * @param {number} postId - 帖子ID
 * @returns {Promise<void>}
 */
async function onLike(postId: number): Promise<void> {
  const post = findPost(postId)
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
 * @returns {Promise<void>}
 */
async function onCollect(postId: number): Promise<void> {
  const post = findPost(postId)
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
 * @param {number} postId - 帖子ID
 */
function onShare(postId: number): void {
  shareTargetPostId.value = postId
  showShareSheet.value = true
}

/**
 * 分享成功回调，更新本地转发计数
 */
function onShared(): void {
  const post = findPost(shareTargetPostId.value!)
  if (post) post.shareCount++
}

/**
 * 删除帖子（从 PostCard emit）
 * @param {number} postId - 帖子ID
 * @returns {Promise<void>}
 */
async function onDeletePost(postId: number): Promise<void> {
  try {
    await showConfirmDialog({ title: '确认删除', message: '删除后不可恢复，确认删除该帖子吗？' })
    await deletePost(postId)
    postList.value = postList.value.filter(p => p.id !== postId)
    if (pinnedPost.value?.id === postId) pinnedPost.value = null
    if (circle.value) circle.value.postCount--
    showToast('已删除')
  } catch {
    // 用户取消或接口失败，静默处理
  }
}

/**
 * 切换帖子可见性（隐藏/公开）
 * @param {number} postId - 帖子ID
 * @param {number} visibility - 目标可见性(0=公开 2=仅自己隐藏)
 * @returns {Promise<void>}
 */
async function onToggleVisibility(postId: number, visibility: number): Promise<void> {
  try {
    await togglePostVisibility(postId, { visibility })
    const post = findPost(postId)
    if (post) post.visibility = visibility
  } catch {
    showToast('操作失败')
  }
}

/**
 * 在帖子列表或置顶帖中查找指定 ID 的帖子
 * @param {number} id - 帖子ID
 * @returns {Post | undefined} 找到的帖子引用
 */
function findPost(id: number): Post | undefined {
  return postList.value.find(p => p.id === id) || (pinnedPost.value?.id === id ? pinnedPost.value : undefined)
}

/**
 * 非作者的操作菜单选择（转发/举报等）
 * @param {any} action - 选中的操作项
 */
function onPostActionSelect(action: any): void {
  // 目前仅处理分享，其他可扩展
  if (action?.value === 'share') {
    onShare(action.postId)
  }
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
    // 圈主预加载成员列表（用于转让）
    if (isOwner.value) {
      loadMemberList()
    }
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
  /* 无 banner 时使用默认渐变背景 */
  background: linear-gradient(135deg, #6BCB77 0%, #4ECDC4 100%);
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

  .owner-avatar {
    width: 28px;
    height: 28px;
    border-radius: 50%;
    object-fit: cover;
    border: 1.5px solid $mint-300;
  }
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

// ---------- 圈主状态徽章 ----------
.owner-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  border-radius: $radius-pill;
  font-size: 12px;
  font-weight: 600;
  color: $amber-600;
  background: linear-gradient(135deg, rgba(255, 182, 39, 0.1), rgba(255, 193, 7, 0.08));
  border: 1px solid rgba(255, 182, 39, 0.25);
  animation: badge-glow 2s ease-in-out infinite alternate;

  svg { color: $amber-500; }
}

@keyframes badge-glow {
  from { box-shadow: 0 0 0 rgba(255, 182, 39, 0); }
  to   { box-shadow: 0 0 8px rgba(255, 182, 39, 0.2); }
}

// ---------- 圈主管理操作（与操作按钮风格统一） ----------
.admin-actions {
  display: flex;
  gap: $space-2;
  margin-top: $space-2;
}

.admin-action-btn {
  flex: 1;
  height: 40px;
  border-radius: $radius-md;
  font-size: 12.5px;
  font-weight: 600;
  @include center;
  gap: 5px;
  transition: all $dur-fast $ease-out;

  // 管理成员 — 紫罗兰（多用户图标）
  &.members {
    color: #8b5cf6;
    background: rgba(139, 92, 246, 0.08);
    border: 1.5px solid rgba(139, 92, 246, 0.22);

    &:hover {
      background: rgba(139, 92, 246, 0.16);
      border-color: rgba(139, 92, 246, 0.38);
    }
  }

  // 转让圈主 — 靛蓝（用户+加号图标）
  &.transfer {
    color: #6366f1;
    background: rgba(99, 102, 241, 0.08);
    border: 1.5px solid rgba(99, 102, 241, 0.22);

    &:hover {
      background: rgba(99, 102, 241, 0.15);
      border-color: rgba(99, 102, 241, 0.38);
    }
  }

  // 修改圈子 — 薄荷绿（画笔图标）
  &.edit {
    color: $mint-700;
    background: rgba(78, 205, 196, 0.08);
    border: 1.5px solid rgba(78, 205, 196, 0.22);

    &:hover {
      background: rgba(78, 205, 196, 0.16);
      border-color: rgba(78, 205, 196, 0.38);
    }
  }

  // 解散圈子 — 珊瑚红（禁止圆圈图标）
  &.dissolve {
    color: $coral-500;
    background: rgba(239, 68, 68, 0.06);
    border: 1.5px solid rgba(239, 68, 68, 0.2);

    &:hover {
      background: rgba(239, 68, 68, 0.12);
      border-color: rgba(239, 68, 68, 0.35);
    }

    &:disabled {
      opacity: 0.35;
      cursor: not-allowed;

      &:hover {
        background: rgba(239, 68, 68, 0.06);
        border-color: rgba(239, 68, 68, 0.2);
      }
    }
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

  &.modal-danger {
    border-top: 4px solid $coral-500;
  }
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

.modal-select {
  width: 100%;
  height: 44px;
  padding: 0 $space-3;
  border-radius: $radius-sm;
  border: 1.5px solid $ink-100;
  font-size: 14px;
  color: $ink-700;
  background: #fff;
  transition: border-color $dur-fast $ease-out;

  &:focus {
    border-color: $mint-500;
    box-shadow: 0 0 0 3px rgba(78, 205, 196, 0.1);
    outline: none;
  }
}

.modal-input {
  width: 100%;
  height: 44px;
  padding: 0 $space-3;
  border-radius: $radius-sm;
  border: 1.5px solid $ink-100;
  font-size: 14px;
  color: $ink-700;
  transition: border-color $dur-fast $ease-out;

  &:focus {
    border-color: $coral-400;
    box-shadow: 0 0 0 3px rgba(255, 107, 107, 0.1);
    outline: none;
  }

  &::placeholder {
    color: $ink-300;
  }
}

.warning-text {
  color: $coral-600;
  font-weight: 500;
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
  /**
   * 移动端安全区域适配
   * @description 为页面根容器添加顶部安全区域内边距，避免内容被刘海屏遮挡
   */
  .circle-detail {
    padding-top: env(safe-area-inset-top);
  }

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

  .owner-actions {
    flex-direction: column;
  }

  .admin-action-btn,
  .owner-action-btn {
    min-width: unset;
  }
}
</style>
