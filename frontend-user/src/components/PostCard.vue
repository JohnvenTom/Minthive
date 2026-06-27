<template>
  <div
    class="post-card"
    @click="handleClick"
    @contextmenu.prevent="handleContextMenu"
  >
    <!-- 头部：用户信息 -->
    <div class="post-card__header">
      <div class="post-card__avatar" @click.stop="handleClickUser">
        <img :src="post.avatar || defaultAvatar" :alt="post.nickname" />
      </div>
      <div class="post-card__user-info">
        <div class="post-card__nickname">
          <span class="post-card__nickname-text" @click.stop="handleClickUser">{{ post.nickname }}</span>
          <span v-if="post.aiGenerated" class="post-card__ai-badge" title="AI生成内容">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
              <path d="M12 2L14.5 8.5L21 9.5L16.5 14L17.5 21L12 17.5L6.5 21L7.5 14L3 9.5L9.5 8.5L12 2Z" fill="currentColor"/>
            </svg>
          </span>
        </div>
        <div class="post-card__meta">
          <span class="post-card__time">{{ formattedTime }}</span>
          <span v-if="post.circleName" class="post-card__circle-tag">
            <svg width="10" height="10" viewBox="0 0 24 24" fill="none">
              <path d="M12 2L22 8.5V15.5L12 22L2 15.5V8.5L12 2Z" stroke="currentColor" stroke-width="2"/>
            </svg>
            {{ post.circleName }}
          </span>
        </div>
      </div>
    </div>

    <!-- 内容区 -->
    <div class="post-card__body">
      <!-- 转发帖标识 -->
      <div v-if="post.sharePostId" class="post-card__reshare-tag">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none">
          <path d="M4 12v8a2 2 0 002 2h12a2 2 0 002-2v-8M16 6l-4-4-4 4M12 2v13"
            stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        <span>转发自 {{ post.originalPost?.nickname || '原帖' }}</span>
      </div>

      <p class="post-card__content" :class="{ 'is-expanded': expanded }">
        {{ post.content }}
      </p>
      <button
        v-if="post.content && post.content.length > 200 && !expanded"
        class="post-card__expand-btn"
        @click.stop="expanded = true"
      >
        展开全文
      </button>
    </div>

    <!-- 图片网格 -->
    <div
      v-if="imageList.length"
      class="post-card__images"
      :class="`grid-${getImageGridClass(imageList.length)}`"
    >
      <div
        v-for="(img, idx) in imageList"
        :key="idx"
        class="post-card__img-wrap"
        @click.stop="previewImage(idx)"
      >
        <img :src="img" :alt="`图片${idx + 1}`" loading="lazy" />
      </div>
    </div>

    <!-- 视频 -->
    <div v-if="post.video" class="post-card__video" @click.stop>
      <video
        :src="post.video"
        controls
        preload="metadata"
        playsinline
        class="post-card__video-player"
      />
    </div>

    <!-- 底部互动栏 -->
    <div class="post-card__actions">
      <button
        class="post-card__action"
        :class="{ 'is-liked': post.liked }"
        @click.stop="handleLike"
      >
        <span class="post-card__action-icon">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
            <path
              d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"
              :fill="post.liked ? 'currentColor' : 'none'"
              stroke="currentColor"
              stroke-width="1.8"
            />
          </svg>
        </span>
        <span class="post-card__action-count">{{ formatNumber(post.likeCount) }}</span>
        <!-- 点赞 +1 浮动动画 -->
        <Transition name="float-up">
          <span v-if="showLikeAnim" class="post-card__float">+1</span>
        </Transition>
      </button>

      <button class="post-card__action" @click.stop>
        <span class="post-card__action-icon">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
            <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </span>
        <span class="post-card__action-count">{{ formatNumber(post.commentCount) }}</span>
      </button>

      <button
        class="post-card__action"
        :class="{ 'is-collected': post.collected }"
        @click.stop="handleCollect"
      >
        <span class="post-card__action-icon">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
            <path
              d="M19 21l-7-5-7 5V5a2 2 0 012-2h10a2 2 0 012 2z"
              :fill="post.collected ? 'currentColor' : 'none'"
              stroke="currentColor"
              stroke-width="1.8"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>
        </span>
        <span class="post-card__action-count">{{ formatNumber(post.collectCount) }}</span>
        <!-- 收藏 +1 浮动动画 -->
        <Transition name="float-up">
          <span v-if="showCollectAnim" class="post-card__float">+1</span>
        </Transition>
      </button>

      <button class="post-card__action" @click.stop="handleShare">
        <span class="post-card__action-icon">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
            <path d="M4 12v8a2 2 0 002 2h12a2 2 0 002-2v-8M16 6l-4-4-4 4M12 2v13" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </span>
        <span class="post-card__action-count">{{ formatNumber(post.shareCount) }}</span>
      </button>

      <!-- 操作按钮：作者显示编辑/删除/隐藏菜单，其他人显示转发列表 -->
      <button
        v-if="isOwner"
        class="post-card__action post-card__action--dot"
        @click.stop="openOwnerActions"
      >
        <van-icon name="ellipsis" size="14" />
      </button>
      <button
        v-else
        class="post-card__action post-card__action--dot"
        @click.stop="$emit('viewShares', post.id)"
      >
        <van-icon name="ellipsis" size="14" />
      </button>
    </div>

    <!-- 图片预览 -->
    <Teleport to="body">
      <Transition name="preview-fade">
        <div v-if="previewVisible" class="post-card__preview" @click="closePreview">
          <div class="post-card__preview-inner" @click.stop>
            <button class="post-card__preview-close" @click="closePreview">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                <path d="M18 6L6 18M6 6l12 12" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>
            </button>
            <img :src="imageList[previewIndex]" alt="预览" />
            <div class="post-card__preview-counter">
              {{ previewIndex + 1 }} / {{ imageList.length }}
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 长按举报菜单 -->
    <Teleport to="body">
      <Transition name="menu-fade">
        <div v-if="contextMenuVisible" class="post-card__context-menu" :style="contextMenuStyle" @click.stop>
          <button class="post-card__context-item" @click="handleReport">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M12 9v4M12 17h.01M3 4h18l-1.5 16H4.5L3 4z" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
            </svg>
            举报
          </button>
        </div>
      </Transition>

      <!-- 作者操作面板（编辑/隐藏/删除） -->
      <van-action-sheet
        v-model:show="showOwnerActions"
        :actions="ownerActions"
        cancel-text="取消"
        @select="onOwnerActionSelect"
      />
    </Teleport>
  </div>
</template>

<script setup lang="ts">
/**
 * PostCard 动态卡片组件
 * @description 展示帖子内容，包含用户信息、图文/视频、互动栏、AI标记、圈子标签、图片预览和举报菜单
 * @param {Post} post - 帖子数据对象
 * @emits like - 点赞事件，参数为帖子ID
 * @emits collect - 收藏事件，参数为帖子ID
 * @emits share - 转发事件，参数为帖子ID
 * @emits openShare - 打开分享面板事件，参数为帖子ID
 * @emits viewShares - 查看转发链事件，参数为帖子ID
 * @emits click - 卡片点击事件，参数为帖子ID
 * @emits report - 举报事件，参数为帖子ID
 */

import { ref, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import type { Post } from '@/types'
import { formatRelativeTime, formatNumber } from '@/utils/format'
import { showToast } from 'vant'

const props = defineProps<{
  /** 帖子数据 */
  post: Post
  /** 当前用户是否为该帖子所属圈子的圈主 */
  isCircleOwner?: boolean
}>()

const emit = defineEmits<{
  /** 点赞事件 */
  (e: 'like', id: number): void
  /** 收藏事件 */
  (e: 'collect', id: number): void
  /** 转发事件（兼容旧逻辑） */
  (e: 'share', id: number): void
  /** 打开分享面板事件 */
  (e: 'openShare', id: number): void
  /** 查看转发链事件 */
  (e: 'viewShares', id: number): void
  /** 卡片点击事件 */
  (e: 'click', id: number): void
  /** 点击用户（头像/昵称），参数为用户ID */
  (e: 'click-user', userId: number): void
  /** 举报事件 */
  (e: 'report', id: number): void
  /** 编辑帖子事件（仅自己的帖子） */
  (e: 'editPost', id: number): void
  /** 删除帖子事件（仅自己的帖子） */
  (e: 'deletePost', id: number): void
  /** 隐藏/公开帖子事件（仅自己的帖子） */
  (e: 'toggleVisibility', id: number, visibility: number): void
}>()

/** 用户状态（用于判断是否为帖子作者） */
const userStore = useUserStore()

/**
 * 判断当前用户是否为帖子作者
 */
const isPostAuthor = computed(() => {
  return !!userStore.userInfo && userStore.userInfo.id === props.post.userId
})

/**
 * 判断当前用户是否为帖子作者或圈主（可操作该帖子）
 */
const isOwner = computed(() => {
  return isPostAuthor.value || props.isCircleOwner
})

/** 默认头像 */
const defaultAvatar = 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 40"><rect fill="#4ECDC4" width="40" height="40"/><text x="20" y="26" text-anchor="middle" fill="white" font-size="16">U</text></svg>')

/**
 * 解析图片列表（后端 images 字段为 JSON 字符串，需解析为数组）
 * @returns {string[]} 图片 URL 数组
 */
const imageList = computed<string[]>(() => {
  const raw = props.post.images
  if (!raw) return []
  if (Array.isArray(raw)) return raw
  try { return JSON.parse(raw) } catch { return [] }
})

/** 是否展开全文 */
const expanded = ref(false)

/** 图片预览是否可见 */
const previewVisible = ref(false)

/** 当前预览图片索引 */
const previewIndex = ref(0)

/** 右键菜单是否可见 */
const contextMenuVisible = ref(false)

/** 右键菜单位置样式 */
const contextMenuStyle = ref({})

/** 操作面板是否显示（作者操作：编辑/删除/隐藏） */
const showOwnerActions = ref(false)

/**
 * 操作选项列表（根据身份动态生成）
 */
const ownerActions = computed(() => {
  const actions: { name: string; icon: string; color: string }[] = []
  if (isPostAuthor.value) {
    const isHidden = props.post.visibility === 2
    actions.push({ name: '编辑帖子', icon: 'edit', color: '#2D3142' })
    actions.push({ name: isHidden ? '重新公开' : '隐藏帖子', icon: isHidden ? 'eye-o' : 'closed-eye', color: '#2D3142' })
  }
  actions.push({ name: '删除帖子', icon: 'delete-o', color: '#FF6B6B' })
  return actions
})

/** 点赞 +1 动画是否显示 */
const showLikeAnim = ref(false)
/** 收藏 +1 动画是否显示 */
const showCollectAnim = ref(false)

/** 格式化发布时间 */
const formattedTime = computed(() => formatRelativeTime(props.post.createTime))

/**
 * 获取图片网格CSS类名
 * @param {number} count - 图片数量
 * @returns {string} 网格类名（1/2/3/4/6/9）
 */
function getImageGridClass(count: number): string {
  if (count === 1) return '1'
  if (count === 2) return '2'
  if (count === 3) return '3'
  if (count === 4) return '4'
  if (count <= 6) return '6'
  return '9'
}

/**
 * 预览图片
 * @param {number} idx - 图片索引
 */
function previewImage(idx: number): void {
  previewIndex.value = idx
  previewVisible.value = true
}

/** 关闭图片预览 */
function closePreview(): void {
  previewVisible.value = false
}

/**
 * 处理点赞（触发 +1 浮动动画后向父组件派发事件）
 * @description 点击时立即触发 +1 动画效果，同时 emit like 事件由父组件处理接口调用
 */
function handleLike(): void {
  // 未点赞状态下点击时才显示 +1 动画
  if (!props.post.liked) {
    showLikeAnim.value = true
    setTimeout(() => { showLikeAnim.value = false }, 600)
  }
  emit('like', props.post.id)
}

/**
 * 处理收藏（触发 +1 浮动动画后向父组件派发事件）
 * @description 点击时立即触发 +1 动画效果，同时 emit collect 事件由父组件处理接口调用
 */
function handleCollect(): void {
  // 未收藏状态下点击时才显示 +1 动画
  if (!props.post.collected) {
    showCollectAnim.value = true
    setTimeout(() => { showCollectAnim.value = false }, 600)
  }
  emit('collect', props.post.id)
}

/**
 * 处理转发（打开分享面板）
 * @description 派发 openShare 事件，由父组件控制 ShareSheet 弹窗的打开
 */
function handleShare(): void {
  emit('openShare', props.post.id)
}

/**
 * 打开作者操作面板（编辑/删除/隐藏）
 * @description 仅帖子作者可见，点击 ... 按钮时触发
 */
function openOwnerActions(): void {
  showOwnerActions.value = true
}

/**
 * 处理作者操作选择
 *
 * @param {any} action - 选中的操作项
 * @description 根据操作类型派发不同事件：编辑/隐藏或公开/删除
 */
function onOwnerActionSelect(action: any): void {
  const id = props.post.id
  if (action.name === '编辑帖子') {
    emit('editPost', id)
  } else if (action.name === '隐藏帖子') {
    emit('toggleVisibility', id, 2) // 2 = 仅自己(隐藏)
    showToast('已隐藏')
  } else if (action.name === '重新公开') {
    emit('toggleVisibility', id, 0) // 0 = 公开
    showToast('已重新公开')
  } else if (action.name === '删除帖子') {
    emit('deletePost', id)
  }
  showOwnerActions.value = false
}

/** 处理卡片点击 */
function handleClick(): void {
  emit('click', props.post.id)
}

/** 处理头像/昵称点击，跳转用户主页 */
function handleClickUser(): void {
  emit('click-user', props.post.userId)
}

/**
 * 处理右键菜单
 * @param {MouseEvent} event - 鼠标事件
 */
function handleContextMenu(event: MouseEvent): void {
  contextMenuStyle.value = {
    top: `${event.clientY}px`,
    left: `${event.clientX}px`
  }
  contextMenuVisible.value = true
  document.addEventListener('click', closeContextMenu, { once: true })
}

/** 关闭右键菜单 */
function closeContextMenu(): void {
  contextMenuVisible.value = false
}

/** 处理举报 */
function handleReport(): void {
  contextMenuVisible.value = false
  emit('report', props.post.id)
}
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;
@use '@/styles/mixins.scss' as *;

.post-card {
  background: #fff;
  border-radius: $radius-md;
  padding: $space-4;
  box-shadow: $shadow-xs;
  transition: box-shadow $dur-base $ease-out, transform $dur-base $ease-out;
  cursor: pointer;
  position: relative;

  &:hover {
    box-shadow: $shadow-sm;
    transform: translateY(-1px);
  }

  // ---------- 头部 ----------
  &__header {
    display: flex;
    align-items: center;
    gap: $space-3;
    margin-bottom: $space-3;
  }

  &__avatar {
    @include hexagon(42px);
    flex-shrink: 0;
    overflow: hidden;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  &__user-info {
    flex: 1;
    min-width: 0;
  }

  &__nickname {
    font-family: $font-heading;
    font-size: 15px;
    font-weight: 600;
    color: $ink-700;
    display: flex;
    align-items: center;
    gap: $space-1;

    &-text {
      cursor: pointer;
      transition: color $dur-fast ease;

      &:hover {
        color: $mint-600;
      }
    }
  }

  &__ai-badge {
    display: inline-flex;
    align-items: center;
    color: $amber-500;
    animation: hex-breathe 3s ease-in-out infinite;
  }

  &__meta {
    display: flex;
    align-items: center;
    gap: $space-2;
    margin-top: 2px;
  }

  &__time {
    font-size: 12px;
    color: $ink-300;
  }

  &__circle-tag {
    display: inline-flex;
    align-items: center;
    gap: 3px;
    font-size: 11px;
    color: $mint-600;
    background: $mint-50;
    padding: 1px 8px;
    border-radius: $radius-pill;
  }

  // ---------- 内容 ----------
  &__body {
    margin-bottom: $space-3;
  }

  &__content {
    font-size: 14px;
    line-height: 1.7;
    color: $ink-700;
    word-break: break-word;
    @include ellipsis(5);

    &.is-expanded {
      -webkit-line-clamp: unset;
      display: block;
    }
  }

  &__expand-btn {
    font-size: 13px;
    color: $mint-600;
    background: none;
    border: none;
    cursor: pointer;
    padding: 0;
    margin-top: $space-1;

    &:hover {
      color: $mint-700;
    }
  }

  // ---------- 转发帖标识 ----------
  &__reshare-tag {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;
    color: $mint-600;
    background: rgba(78, 205, 196, 0.08);
    padding: 2px 10px;
    border-radius: $radius-pill;
    margin-bottom: $space-2;

    svg {
      flex-shrink: 0;
    }
  }

  // ---------- 图片网格 ----------
  &__images {
    display: grid;
    gap: 4px;
    margin-bottom: $space-3;
    border-radius: $radius-sm;
    overflow: hidden;

    &.grid-1 {
      grid-template-columns: 1fr;
      max-width: 280px;

      .post-card__img-wrap {
        aspect-ratio: auto;
        max-height: 320px;
      }
    }

    &.grid-2 {
      grid-template-columns: 1fr 1fr;
    }

    &.grid-3 {
      grid-template-columns: 1fr 1fr 1fr;
    }

    &.grid-4 {
      grid-template-columns: 1fr 1fr;

      .post-card__img-wrap:nth-child(1) {
        grid-column: 1 / -1;
        aspect-ratio: 2 / 1;
      }
    }

    &.grid-6 {
      grid-template-columns: 1fr 1fr 1fr;
    }

    &.grid-9 {
      grid-template-columns: 1fr 1fr 1fr;
    }
  }

  &__img-wrap {
    aspect-ratio: 1;
    overflow: hidden;
    cursor: zoom-in;
    position: relative;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      transition: transform $dur-base $ease-out;
    }

    &:hover img {
      transform: scale(1.05);
    }
  }

  // ---------- 视频 ----------
  &__video {
    margin-bottom: $space-3;
    border-radius: $radius-sm;
    overflow: hidden;
    background: $ink-900;
  }

  &__video-player {
    width: 100%;
    max-height: 360px;
    display: block;
  }

  // ---------- 互动栏 ----------
  &__actions {
    display: flex;
    align-items: center;
    padding-top: $space-2;
    border-top: 1px solid $ink-50;
  }

  &__action {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
    background: none;
    border: none;
    padding: $space-2 0;
    cursor: pointer;
    color: $ink-300;
    font-size: 13px;
    transition: color $dur-fast ease;
    position: relative; /* 为 +1 浮动动画提供定位基准 */

    &:hover {
      color: $ink-500;
    }

    &.is-liked {
      color: $coral-500;

      .post-card__action-icon {
        animation: like-burst 0.4s $ease-spring;
      }
    }

    &.is-collected {
      color: $rose-500;
    }

    /** 更多按钮（...）— 紧凑无文字 */
    &--dot {
      flex: none;
      padding: $space-2;
      justify-content: center;

      &:hover {
        color: $mint-500;
        background: rgba(78, 205, 196, 0.08);
      }
    }
  }

  // ---------- +1 浮动动画 ----------
  &__float {
    position: absolute;
    top: -8px;
    right: 4px;
    font-size: 11px;
    font-weight: 700;
    color: $coral-500;
    pointer-events: none;
    z-index: 10;
  }

  &__action-icon {
    display: inline-flex;
    align-items: center;
  }

  &__action-count {
    font-family: $font-heading;
    font-size: 12px;
  }

  // ---------- 图片预览 ----------
  &__preview {
    position: fixed;
    inset: 0;
    z-index: 9999;
    background: rgba(26, 29, 46, 0.92);
    @include center;
  }

  &__preview-inner {
    position: relative;
    max-width: 90vw;
    max-height: 90vh;

    img {
      max-width: 90vw;
      max-height: 85vh;
      object-fit: contain;
      border-radius: $radius-sm;
    }
  }

  &__preview-close {
    position: absolute;
    top: -40px;
    right: 0;
    background: rgba(255, 255, 255, 0.15);
    border: none;
    color: #fff;
    width: 36px;
    height: 36px;
    border-radius: 50%;
    cursor: pointer;
    @include center;
    transition: background $dur-fast ease;

    &:hover {
      background: rgba(255, 255, 255, 0.3);
    }
  }

  &__preview-counter {
    text-align: center;
    color: rgba(255, 255, 255, 0.7);
    font-size: 13px;
    margin-top: $space-2;
  }

  // ---------- 右键菜单 ----------
  &__context-menu {
    position: fixed;
    z-index: 10000;
    background: #fff;
    border-radius: $radius-sm;
    box-shadow: $shadow-md;
    padding: $space-1 0;
    animation: scale-in 0.2s $ease-spring;
  }

  &__context-item {
    display: flex;
    align-items: center;
    gap: $space-2;
    width: 100%;
    padding: $space-2 $space-4;
    background: none;
    border: none;
    font-size: 14px;
    color: $ink-700;
    cursor: pointer;
    transition: background $dur-fast ease;

    &:hover {
      background: $ink-50;
      color: $danger;
    }
  }
}

// ---------- 过渡动画 ----------
.preview-fade-enter-active,
.preview-fade-leave-active {
  transition: opacity $dur-base $ease-out;
}
.preview-fade-enter-from,
.preview-fade-leave-to {
  opacity: 0;
}

.menu-fade-enter-active {
  animation: scale-in 0.2s $ease-spring;
}
.menu-fade-leave-active {
  animation: fade-in 0.15s ease reverse;
}

// ---------- +1 浮动动画 ----------
.float-up-enter-active {
  animation: float-up 0.6s ease-out forwards;
}

.float-up-leave-active {
  animation: float-up 0.6s ease-out reverse;
}

@keyframes float-up {
  0% {
    opacity: 1;
    transform: translateY(0) scale(0.5);
  }
  50% {
    opacity: 1;
    transform: translateY(-14px) scale(1.2);
  }
  100% {
    opacity: 0;
    transform: translateY(-24px) scale(1);
  }
}
</style>
