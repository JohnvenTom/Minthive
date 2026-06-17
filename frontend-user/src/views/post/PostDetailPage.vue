<template>
  <div class="post-detail-page">
    <!-- 蜂巢装饰背景 -->
    <div class="hex-bg" />

    <!-- 顶部导航 -->
    <header class="detail-header">
      <button class="back-btn" @click="goBack">
        <van-icon name="arrow-left" size="20" />
      </button>
      <h2 class="header-title">帖子详情</h2>
      <button class="more-btn" @click="showPostActions = true">
        <van-icon name="ellipsis" size="20" />
      </button>
    </header>

    <!-- 加载态 -->
    <div v-if="loading && !post" class="loading-wrap">
      <LoadingSpinner />
    </div>

    <!-- 帖子内容区 -->
    <main v-else-if="post" class="detail-main">
      <!-- 帖子卡片 -->
      <article class="post-content-card glass-card">
        <!-- 作者信息 -->
        <div class="author-row">
          <img :src="post.avatar || '/default-avatar.png'" class="author-avatar" alt="头像" />
          <div class="author-info">
            <span class="author-name">{{ post.nickname }}</span>
            <span class="post-time">{{ formatTime(post.createTime) }}</span>
          </div>
          <span v-if="post.aiGenerated" class="ai-badge">
            <van-icon name="magic-o" />
            AI
          </span>
        </div>

        <!-- 帖子正文 -->
        <div class="post-body">
          <p class="post-text">{{ post.content }}</p>

          <!-- 话题标签 -->
          <div v-if="post.topics?.length" class="topics-row">
            <span v-for="topic in post.topics" :key="topic" class="topic-tag">
              #{{ topic }}
            </span>
          </div>
        </div>

        <!-- 图片展示（大图模式） -->
        <div v-if="post.images?.length" class="post-images">
          <div
            :class="[
              'image-grid',
              `grid-${Math.min(post.images.length, 9)}`
            ]"
          >
            <div
              v-for="(img, idx) in post.images"
              :key="idx"
              class="image-item"
              @click="previewImage(idx)"
            >
              <img :src="img" :alt="`图片${idx + 1}`" loading="lazy" />
            </div>
          </div>
        </div>

        <!-- 视频展示 -->
        <div v-if="post.video" class="post-video">
          <video :src="post.video" controls preload="metadata" class="video-player" />
        </div>

        <!-- 圈子信息 -->
        <div v-if="post.circleName" class="circle-tag">
          <van-icon name="cluster-o" />
          <span>{{ post.circleName }}</span>
        </div>

        <!-- 互动栏 -->
        <div class="interaction-bar">
          <button :class="['action-btn', { liked: post.liked }]" @click="onToggleLike">
            <van-icon :name="post.liked ? 'like' : 'like-o'" />
            <span>{{ post.likeCount || '' }}</span>
          </button>
          <button class="action-btn" @click="scrollToComment">
            <van-icon name="chat-o" />
            <span>{{ post.commentCount || '' }}</span>
          </button>
          <button :class="['action-btn', { collected: post.collected }]" @click="onToggleCollect">
            <van-icon :name="post.collected ? 'star' : 'star-o'" />
            <span>{{ post.collectCount || '' }}</span>
          </button>
          <button class="action-btn" @click="onShare">
            <van-icon name="share-o" />
            <span>{{ post.shareCount || '' }}</span>
          </button>
        </div>
      </article>

      <!-- 评论区 -->
      <section ref="commentSectionRef" class="comment-section">
        <div class="section-header">
          <h3 class="section-title">评论 <span class="count">{{ commentTotal }}</span></h3>
        </div>

        <!-- 评论加载态 -->
        <div v-if="commentLoading && comments.length === 0" class="comment-skeleton">
          <div v-for="i in 3" :key="i" class="skeleton-comment shimmer">
            <div class="skeleton-avatar" />
            <div class="skeleton-content">
              <div class="skeleton-line" />
              <div class="skeleton-line short" />
            </div>
          </div>
        </div>

        <!-- 评论列表 -->
        <div v-else-if="comments.length > 0" class="comment-list">
          <CommentItem
            v-for="comment in comments"
            :key="comment.id"
            :comment="comment"
            @like="onCommentLike"
            @reply="onReply"
            @delete="onCommentDelete"
            @report="onCommentReport"
          />
        </div>

        <!-- 评论空状态 -->
        <EmptyState
          v-else
          title="暂无评论"
          description="快来发表第一条评论吧"
          icon="chat-o"
          :mini="true"
        />

        <!-- 加载更多评论 -->
        <div v-if="comments.length > 0 && commentHasMore" class="load-more-comments">
          <LoadingSpinner v-if="commentLoadingMore" />
          <button v-else class="load-more-btn" @click="loadMoreComments">加载更多评论</button>
        </div>
      </section>
    </main>

    <!-- 底部评论输入栏 -->
    <footer class="comment-input-bar glass-card">
      <div class="input-area">
        <input
          v-model="commentText"
          class="comment-input"
          :placeholder="replyTarget ? `回复 ${replyTarget}...` : '写评论...'"
          @focus="onInputFocus"
          @keyup.enter="onSubmitComment"
        />
        <!-- AI 智能评论按钮 -->
        <button class="ai-comment-btn" :class="{ generating: aiGenerating }" @click="onAiComment">
          <van-icon name="magic-o" />
        </button>
      </div>
      <button class="send-btn" :disabled="!commentText.trim()" @click="onSubmitComment">
        发送
      </button>
    </footer>

    <!-- AI 评论建议面板 -->
    <Transition name="slide-up">
      <div v-if="showAiPanel" class="ai-suggest-panel glass-card">
        <div class="ai-panel-header">
          <span class="ai-panel-title">
            <van-icon name="magic-o" class="ai-icon" />
            AI 智能评论建议
          </span>
          <van-icon name="cross" class="close-icon" @click="showAiPanel = false" />
        </div>
        <div class="ai-suggest-list">
          <div
            v-for="(suggestion, idx) in aiSuggestions"
            :key="idx"
            class="ai-suggest-item"
            @click="selectAiSuggestion(suggestion)"
          >
            <span class="suggest-tone">{{ suggestion.tone }}</span>
            <p class="suggest-content">{{ suggestion.content }}</p>
          </div>
        </div>
      </div>
    </Transition>

    <!-- 举报弹窗 -->
    <ReportDialog
      v-model:show="showReportDialog"
      :target-id="reportTargetId"
      target-type="comment"
      @submitted="onReportSubmitted"
    />

    <!-- 帖子操作弹窗 -->
    <van-action-sheet
      v-model:show="showPostActions"
      :actions="postActions"
      cancel-text="取消"
      @select="onPostActionSelect"
    />

    <!-- 图片预览 -->
    <van-image-preview v-model:show="showPreview" :images="previewImages" :start-position="previewIndex" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import CommentItem from '@/components/CommentItem.vue'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import EmptyState from '@/components/EmptyState.vue'
import ReportDialog from '@/components/ReportDialog.vue'
import { useUserStore } from '@/stores/user'
import { getPostDetail, toggleLike, toggleCollect, sharePost } from '@/api/post'
import { getComments, createComment, toggleCommentLike, deleteComment } from '@/api/comment'
import { aiGenerateComment } from '@/api/ai'
import { report } from '@/api/report'
import { wsClient } from '@/utils/websocket'
import { formatTime } from '@/utils/format'
import type { Post, Comment, AiCommentSuggestion } from '@/types'

// ---------- 路由与Store ----------
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/** 帖子ID */
const postId = computed(() => Number(route.params.id))

// ---------- 帖子数据 ----------
const post = ref<Post | null>(null)
const loading = ref(false)

// ---------- 评论数据 ----------
const comments = ref<Comment[]>([])
const commentTotal = ref(0)
const commentLoading = ref(false)
const commentLoadingMore = ref(false)
const commentHasMore = ref(true)
const commentPage = ref(1)
const commentPageSize = 20

// ---------- 评论输入 ----------
const commentText = ref('')
const replyTarget = ref('')
const replyParentId = ref<number | null>(null)

// ---------- AI 评论 ----------
const showAiPanel = ref(false)
const aiSuggestions = ref<AiCommentSuggestion[]>([])
const aiGenerating = ref(false)

// ---------- 举报 ----------
const showReportDialog = ref(false)
const reportTargetId = ref(0)

// ---------- 图片预览 ----------
const showPreview = ref(false)
const previewImages = ref<string[]>([])
const previewIndex = ref(0)

// ---------- 帖子操作 ----------
const showPostActions = ref(false)
const postActions = [
  { name: '举报帖子', color: '#FF6B6B' },
  { name: '复制链接' }
]

// ---------- DOM引用 ----------
const commentSectionRef = ref<HTMLElement | null>(null)

// ---------- 数据加载 ----------

/**
 * 加载帖子详情
 * @returns {Promise<void>}
 * @description 根据路由参数中的帖子ID请求帖子完整内容
 */
async function fetchPostDetail(): Promise<void> {
  loading.value = true
  try {
    const res = await getPostDetail(postId.value)
    post.value = res.data
  } catch {
    showToast('帖子加载失败')
  } finally {
    loading.value = false
  }
}

/**
 * 加载评论列表
 * @param {boolean} isLoadMore - 是否为加载更多
 * @returns {Promise<void>}
 * @description 请求帖子的一级评论列表，支持分页
 */
async function fetchComments(isLoadMore = false): Promise<void> {
  if (!isLoadMore) {
    commentLoading.value = true
  } else {
    commentLoadingMore.value = true
  }

  try {
    const res = await getComments(postId.value, commentPage.value, commentPageSize)
    const list = res.data?.list || []
    commentTotal.value = res.data?.total || 0

    if (isLoadMore) {
      comments.value.push(...list)
    } else {
      comments.value = list
    }

    commentHasMore.value = res.data?.hasMore ?? false
  } catch {
    showToast('评论加载失败')
  } finally {
    commentLoading.value = false
    commentLoadingMore.value = false
  }
}

/**
 * 加载更多评论
 * @returns {Promise<void>}
 */
async function loadMoreComments(): Promise<void> {
  commentPage.value++
  await fetchComments(true)
}

// ---------- 互动操作 ----------

/**
 * 帖子点赞/取消点赞
 * @returns {Promise<void>}
 */
async function onToggleLike(): Promise<void> {
  if (!post.value) return
  try {
    await toggleLike(post.value.id, !post.value.liked)
    post.value.liked = !post.value.liked
    post.value.likeCount += post.value.liked ? 1 : -1
  } catch {
    showToast('操作失败')
  }
}

/**
 * 帖子收藏/取消收藏
 * @returns {Promise<void>}
 */
async function onToggleCollect(): Promise<void> {
  if (!post.value) return
  try {
    await toggleCollect(post.value.id, !post.value.collected)
    post.value.collected = !post.value.collected
    post.value.collectCount += post.value.collected ? 1 : -1
  } catch {
    showToast('操作失败')
  }
}

/**
 * 分享帖子
 * @returns {Promise<void>}
 */
async function onShare(): Promise<void> {
  if (!post.value) return
  try {
    await sharePost(post.value.id)
    post.value.shareCount++
    showToast('分享成功')
  } catch {
    showToast('分享失败')
  }
}

// ---------- 评论操作 ----------

/**
 * 评论点赞/取消点赞
 * @param {Comment} comment - 目标评论
 * @returns {Promise<void>}
 */
async function onCommentLike(comment: Comment): Promise<void> {
  try {
    await toggleCommentLike(comment.id, !comment.liked)
    comment.liked = !comment.liked
    comment.likeCount += comment.liked ? 1 : -1
  } catch {
    showToast('操作失败')
  }
}

/**
 * 回复评论
 * @param {Comment} comment - 被回复的评论
 * @description 设置回复目标并聚焦输入框
 */
function onReply(comment: Comment): void {
  replyTarget.value = comment.nickname
  replyParentId.value = comment.parentId || comment.id
  commentText.value = `@${comment.nickname} `
}

/**
 * 删除本人评论
 * @param {Comment} comment - 要删除的评论
 * @returns {Promise<void>}
 */
async function onCommentDelete(comment: Comment): Promise<void> {
  try {
    await showConfirmDialog({
      title: '确认删除',
      message: '删除后不可恢复，确认删除该评论吗？'
    })
    await deleteComment(comment.id)
    comments.value = comments.value.filter(c => c.id !== comment.id)
    commentTotal.value--
    if (post.value) post.value.commentCount--
    showToast('已删除')
  } catch (e: any) {
    if (e !== 'cancel') showToast('删除失败')
  }
}

/**
 * 举报评论
 * @param {Comment} comment - 被举报的评论
 */
function onCommentReport(comment: Comment): void {
  reportTargetId.value = comment.id
  showReportDialog.value = true
}

/**
 * 举报提交成功回调
 */
function onReportSubmitted(): void {
  showToast('举报已提交')
}

/**
 * 提交评论
 * @returns {Promise<void>}
 * @description 发送评论或回复，成功后刷新评论列表
 */
async function onSubmitComment(): Promise<void> {
  const content = commentText.value.trim()
  if (!content) return

  try {
    await createComment({
      postId: postId.value,
      parentId: replyParentId.value || undefined,
      content
    })

    commentText.value = ''
    replyTarget.value = ''
    replyParentId.value = null

    // 重新加载评论
    commentPage.value = 1
    await fetchComments()

    if (post.value) post.value.commentCount++
    showToast('评论成功')
  } catch {
    showToast('评论失败')
  }
}

/**
 * 评论输入框聚焦
 */
function onInputFocus(): void {
  // 移动端键盘弹起时滚动到输入框
}

// ---------- AI 智能评论 ----------

/**
 * 请求AI智能评论建议
 * @returns {Promise<void>}
 * @description 调用AI接口生成3条评论建议并展示面板
 */
async function onAiComment(): Promise<void> {
  if (aiGenerating.value) return
  aiGenerating.value = true

  try {
    const res = await aiGenerateComment(postId.value)
    aiSuggestions.value = res.data || []
    showAiPanel.value = true
  } catch {
    showToast('AI 生成失败，请重试')
  } finally {
    aiGenerating.value = false
  }
}

/**
 * 选择AI评论建议
 * @param {AiCommentSuggestion} suggestion - 选中的建议
 * @description 将AI建议内容填入评论输入框
 */
function selectAiSuggestion(suggestion: AiCommentSuggestion): void {
  commentText.value = suggestion.content
  showAiPanel.value = false
}

// ---------- 图片预览 ----------

/**
 * 预览图片
 * @param {number} index - 起始图片索引
 */
function previewImage(index: number): void {
  if (!post.value?.images) return
  previewImages.value = post.value.images
  previewIndex.value = index
  showPreview.value = true
}

// ---------- 帖子操作 ----------

/**
 * 帖子操作选择
 * @param {any} action - 选中的操作项
 */
function onPostActionSelect(action: any): void {
  if (action.name === '举报帖子') {
    reportTargetId.value = postId.value
    showReportDialog.value = true
  } else if (action.name === '复制链接') {
    navigator.clipboard?.writeText(window.location.href)
    showToast('链接已复制')
  }
  showPostActions.value = false
}

// ---------- 导航 ----------

/**
 * 返回上一页
 */
function goBack(): void {
  router.back()
}

/**
 * 滚动到评论区
 */
function scrollToComment(): void {
  commentSectionRef.value?.scrollIntoView({ behavior: 'smooth' })
}

// ---------- WebSocket 实时评论推送 ----------

/**
 * WebSocket 评论推送处理
 * @param {any} data - 推送数据
 * @description 接收实时评论推送并更新评论列表
 */
function onWsComment(data: any): void {
  if (data?.postId === postId.value && data?.comment) {
    const newComment = data.comment as Comment
    comments.value.unshift(newComment)
    commentTotal.value++
    if (post.value) post.value.commentCount++
  }
}

// ---------- 生命周期 ----------
onMounted(() => {
  fetchPostDetail()
  fetchComments()
  wsClient.on('comment', onWsComment)
})

onUnmounted(() => {
  wsClient.off('comment', onWsComment)
})
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;
@use '@/styles/mixins.scss' as *;

.post-detail-page {
  min-height: 100vh;
  background: $ink-50;
  padding-bottom: 80px;
}

// ---------- 顶部导航 ----------
.detail-header {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $space-3 $space-4;
  @include glass(20px, rgba(255, 255, 255, 0.85));

  .header-title {
    font-size: 16px;
    font-weight: 600;
    color: $ink-900;
  }
}

.back-btn,
.more-btn {
  width: 36px;
  height: 36px;
  border-radius: $radius-sm;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $ink-700;
  transition: background $dur-fast $ease-out;

  &:hover {
    background: $ink-50;
  }
}

// ---------- 加载态 ----------
.loading-wrap {
  @include center(column);
  padding-top: 120px;
}

// ---------- 帖子内容卡片 ----------
.post-content-card {
  margin: $space-4;
  padding: $space-5;
  border-radius: $radius-lg;
  box-shadow: $shadow-sm;
  animation: fade-up 0.5s $ease-out both;
}

// ---------- 作者行 ----------
.author-row {
  display: flex;
  align-items: center;
  gap: $space-3;
  margin-bottom: $space-4;
}

.author-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid $mint-300;
}

.author-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.author-name {
  font-size: 15px;
  font-weight: 600;
  color: $ink-900;
}

.post-time {
  font-size: 12px;
  color: $ink-300;
}

.ai-badge {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 2px 8px;
  border-radius: $radius-pill;
  font-size: 11px;
  font-weight: 600;
  color: $amber-600;
  background: rgba(255, 182, 39, 0.12);
  animation: ai-glow 3s ease-in-out infinite;
}

// ---------- 帖子正文 ----------
.post-body {
  margin-bottom: $space-4;
}

.post-text {
  font-size: 15px;
  line-height: 1.75;
  color: $ink-700;
  white-space: pre-wrap;
  word-break: break-word;
}

.topics-row {
  display: flex;
  flex-wrap: wrap;
  gap: $space-2;
  margin-top: $space-3;
}

.topic-tag {
  font-size: 13px;
  color: $mint-600;
  font-weight: 500;
  cursor: pointer;
  transition: color $dur-fast $ease-out;

  &:hover {
    color: $mint-700;
  }
}

// ---------- 图片展示 ----------
.post-images {
  margin-bottom: $space-4;
}

.image-grid {
  display: grid;
  gap: $space-2;
  border-radius: $radius-md;
  overflow: hidden;

  &.grid-1 {
    grid-template-columns: 1fr;
  }
  &.grid-2 {
    grid-template-columns: repeat(2, 1fr);
  }
  &.grid-3 {
    grid-template-columns: repeat(3, 1fr);
  }
  &.grid-4 {
    grid-template-columns: repeat(2, 1fr);
  }
  &.grid-5,
  &.grid-6 {
    grid-template-columns: repeat(3, 1fr);
  }
  &.grid-7,
  &.grid-8,
  &.grid-9 {
    grid-template-columns: repeat(3, 1fr);
  }
}

.image-item {
  aspect-ratio: 1;
  border-radius: $radius-sm;
  overflow: hidden;
  cursor: pointer;
  transition: transform $dur-base $ease-spring;

  &:hover {
    transform: scale(1.02);
  }

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

// ---------- 视频展示 ----------
.post-video {
  margin-bottom: $space-4;
  border-radius: $radius-md;
  overflow: hidden;
}

.video-player {
  width: 100%;
  border-radius: $radius-md;
  background: $ink-900;
}

// ---------- 圈子标签 ----------
.circle-tag {
  display: inline-flex;
  align-items: center;
  gap: $space-1;
  padding: $space-1 $space-3;
  border-radius: $radius-pill;
  font-size: 12px;
  color: $mint-600;
  background: $mint-50;
  margin-bottom: $space-4;
}

// ---------- 互动栏 ----------
.interaction-bar {
  display: flex;
  justify-content: space-around;
  padding-top: $space-4;
  border-top: 1px solid $ink-100;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: $space-1;
  font-size: 13px;
  color: $ink-500;
  padding: $space-2 $space-3;
  border-radius: $radius-sm;
  transition: all $dur-fast $ease-out;

  &:hover {
    background: $ink-50;
  }

  &.liked {
    color: $coral-500;

    :deep(.van-icon) {
      animation: like-burst 0.4s $ease-spring;
    }
  }

  &.collected {
    color: $amber-500;
  }
}

// ---------- 评论区 ----------
.comment-section {
  margin: 0 $space-4;
  padding: $space-4;
  background: white;
  border-radius: $radius-lg;
  box-shadow: $shadow-xs;
  animation: fade-up 0.6s $ease-out both;
  animation-delay: 0.1s;
}

.section-header {
  margin-bottom: $space-4;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: $ink-900;

  .count {
    font-size: 14px;
    color: $ink-300;
    font-weight: 400;
    margin-left: $space-1;
  }
}

// ---------- 评论骨架屏 ----------
.comment-skeleton {
  display: flex;
  flex-direction: column;
  gap: $space-4;
}

.skeleton-comment {
  display: flex;
  gap: $space-3;
  padding: $space-3;
}

.skeleton-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: $ink-100;
  flex-shrink: 0;
}

.skeleton-content {
  flex: 1;
}

.skeleton-line {
  height: 14px;
  border-radius: $radius-xs;
  background: $ink-100;
  margin-bottom: $space-2;

  &.short {
    width: 50%;
  }
}

// ---------- 评论列表 ----------
.comment-list {
  display: flex;
  flex-direction: column;
}

// ---------- 加载更多评论 ----------
.load-more-comments {
  display: flex;
  justify-content: center;
  padding: $space-4 0;
}

.load-more-btn {
  font-size: 13px;
  color: $mint-500;
  padding: $space-2 $space-4;
  border-radius: $radius-pill;
  transition: background $dur-fast $ease-out;

  &:hover {
    background: $mint-50;
  }
}

// ---------- 底部评论输入栏 ----------
.comment-input-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  gap: $space-2;
  padding: $space-3 $space-4;
  box-shadow: 0 -2px 12px rgba(26, 29, 46, 0.06);
}

.input-area {
  flex: 1;
  display: flex;
  align-items: center;
  background: $ink-50;
  border-radius: $radius-pill;
  padding: $space-1 $space-2;
  gap: $space-2;
}

.comment-input {
  flex: 1;
  height: 36px;
  padding: 0 $space-2;
  font-size: 14px;
  background: transparent;
  color: $ink-700;

  &::placeholder {
    color: $ink-300;
  }
}

.ai-comment-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $amber-500;
  background: rgba(255, 182, 39, 0.1);
  transition: all $dur-fast $ease-out;
  flex-shrink: 0;

  &:hover {
    background: rgba(255, 182, 39, 0.2);
    transform: scale(1.1);
  }

  &.generating {
    animation: ai-pulse 1.2s ease-in-out infinite;
  }
}

.send-btn {
  padding: $space-2 $space-5;
  border-radius: $radius-pill;
  font-size: 14px;
  font-weight: 600;
  color: white;
  background: $grad-mint;
  transition: all $dur-fast $ease-out;
  flex-shrink: 0;

  &:hover:not(:disabled) {
    box-shadow: $shadow-glow-mint;
    transform: translateY(-1px);
  }

  &:active:not(:disabled) {
    transform: scale(0.96);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

// ---------- AI 评论建议面板 ----------
.ai-suggest-panel {
  position: fixed;
  bottom: 70px;
  left: $space-4;
  right: $space-4;
  z-index: 200;
  border-radius: $radius-lg;
  box-shadow: $shadow-lg;
  padding: $space-4;
  animation: fade-up 0.4s $ease-spring both;
}

.ai-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: $space-3;
}

.ai-panel-title {
  display: flex;
  align-items: center;
  gap: $space-2;
  font-size: 14px;
  font-weight: 600;
  color: $ink-900;
}

.ai-icon {
  color: $amber-500;
  animation: ai-pulse 2s ease-in-out infinite;
}

.close-icon {
  color: $ink-300;
  cursor: pointer;
  transition: color $dur-fast $ease-out;

  &:hover {
    color: $ink-500;
  }
}

.ai-suggest-list {
  display: flex;
  flex-direction: column;
  gap: $space-3;
}

.ai-suggest-item {
  padding: $space-3;
  border-radius: $radius-md;
  background: $ink-50;
  cursor: pointer;
  transition: all $dur-fast $ease-out;

  &:hover {
    background: $mint-50;
    transform: translateX(4px);
  }

  &:active {
    transform: scale(0.98);
  }
}

.suggest-tone {
  display: inline-block;
  font-size: 11px;
  font-weight: 600;
  color: $amber-600;
  background: rgba(255, 182, 39, 0.12);
  padding: 1px 8px;
  border-radius: $radius-pill;
  margin-bottom: $space-1;
}

.suggest-content {
  font-size: 14px;
  color: $ink-700;
  line-height: 1.6;
}

// ---------- 面板动画 ----------
.slide-up-enter-active {
  animation: fade-up 0.4s $ease-spring both;
}

.slide-up-leave-active {
  animation: fade-up 0.25s $ease-out reverse both;
}
</style>
