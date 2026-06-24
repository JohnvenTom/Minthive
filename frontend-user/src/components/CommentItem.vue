<template>
  <div class="comment-item" :class="{ 'is-ai': comment.aiGenerated, 'is-child': isChild }">
    <div class="comment-item__main">
      <!-- 头像 -->
      <div class="comment-item__avatar" @click="handleClickUser">
        <img :src="comment.avatar || defaultAvatar" :alt="comment.nickname" />
      </div>

      <div class="comment-item__body">
        <!-- 昵称 + AI标记 -->
        <div class="comment-item__header">
          <span class="comment-item__nickname" @click="handleClickUser">{{ comment.nickname }}</span>
          <span v-if="comment.aiGenerated" class="comment-item__ai-tag">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none">
              <path d="M12 2L14.5 8.5L21 9.5L16.5 14L17.5 21L12 17.5L6.5 21L7.5 14L3 9.5L9.5 8.5L12 2Z" fill="currentColor"/>
            </svg>
            AI
          </span>
          <span v-if="comment.replyTo" class="comment-item__reply-to">
            回复 <span class="comment-item__reply-name">@{{ comment.replyTo }}</span>
          </span>
        </div>

        <!-- 内容 -->
        <div class="comment-item__content">{{ comment.content }}</div>

        <!-- 评论图片 -->
        <div v-if="comment.images && comment.images.length" class="comment-item__images">
          <img
            v-for="(img, idx) in comment.images"
            :key="idx"
            :src="img"
            :alt="`评论图片${idx + 1}`"
            class="comment-item__img"
          />
        </div>

        <!-- 底部操作 -->
        <div class="comment-item__footer">
          <span class="comment-item__time">{{ formattedTime }}</span>
          <div v-if="!isChild" class="comment-item__ops">
            <button
              class="comment-item__op"
              :class="{ 'is-liked': comment.liked }"
              @click="handleLike"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                <path
                  d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"
                  :fill="comment.liked ? 'currentColor' : 'none'"
                  stroke="currentColor"
                  stroke-width="2"
                />
              </svg>
              <span v-if="comment.likeCount">{{ formatNumber(comment.likeCount) }}</span>
            </button>
            <button class="comment-item__op" @click="handleReply">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              回复
            </button>
          </div>
          <div v-else class="comment-item__ops">
            <button class="comment-item__op" @click="handleReply">回复</button>
          </div>

          <!-- 更多操作（三点菜单） -->
          <div class="comment-item__more" ref="moreRef">
            <button
              class="comment-item__more-btn"
              :class="{ 'is-active': showMenu }"
              @click.stop="toggleMenu"
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                <circle cx="12" cy="5" r="2"/>
                <circle cx="12" cy="12" r="2"/>
                <circle cx="12" cy="19" r="2"/>
              </svg>
            </button>
            <Transition name="menu-dropdown">
              <div v-if="showMenu" class="comment-item__dropdown">
                <!-- 菜单顶部装饰条 -->
                <div class="comment-item__dropdown-bar"></div>
                <!-- 自己的评论：显示删除 -->
                <button
                  v-if="isOwner"
                  class="comment-item__dropdown-item is-danger"
                  @click="handleDeleteClick"
                >
                  <span class="item-icon">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <polyline points="3 6 5 6 21 6"/>
                      <path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/>
                    </svg>
                  </span>
                  <span class="item-text">删除</span>
                </button>
                <!-- 非自己的评论：显示举报 -->
                <button
                  v-else
                  class="comment-item__dropdown-item is-report"
                  @click="handleReportClick"
                >
                  <span class="item-icon">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/>
                      <line x1="12" y1="9" x2="12" y2="13"/>
                      <line x1="12" y1="17" x2="12.01" y2="17"/>
                    </svg>
                  </span>
                  <span class="item-text">举报</span>
                </button>
              </div>
            </Transition>
          </div>
        </div>
      </div>
    </div>

    <!-- 楼中楼回复 -->
    <div v-if="comment.children && comment.children.length" class="comment-item__children">
      <CommentItem
        v-for="child in comment.children"
        :key="child.id"
        :comment="child"
        :is-child="true"
        @reply="(c: Comment) => emit('reply', c)"
        @like="(c: Comment) => emit('like', c)"
        @delete="(id: number) => emit('delete', id)"
      />
    </div>

    <!-- 举报弹窗 -->
    <ReportDialog
      v-model:visible="showReportDialog"
      :target-id="comment.id"
      target-type="comment"
      @success="handleReportSuccess"
    />
  </div>
</template>

<script setup lang="ts">
/**
 * CommentItem 评论项组件
 * @description 展示单条评论，支持楼中楼递归、AI标记、点赞动画、回复操作，
 *   以及评论的删除（自己的评论）和举报（他人的评论）功能
 * @param {Comment} comment - 评论数据对象
 * @param {boolean} isChild - 是否为子评论（楼中楼）
 * @emits reply - 回复事件，参数为评论对象
 * @emits like - 点赞事件，参数为评论对象
 * @emits delete - 删除事件，参数为评论ID
 * @emits click-user - 点击用户头像/昵称事件，参数为用户ID
 */

import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Comment } from '@/types'
import { formatRelativeTime, formatNumber } from '@/utils/format'
import { useUserStore } from '@/stores/user'
import { deleteComment } from '@/api/comment'
import ReportDialog from './ReportDialog.vue'

const props = defineProps<{
  /** 评论数据 */
  comment: Comment
  /** 是否为子评论（楼中楼） */
  isChild?: boolean
}>()

const emit = defineEmits<{
  (e: 'reply', comment: Comment): void
  (e: 'like', comment: Comment): void
  (e: 'delete', id: number): void
  (e: 'click-user', userId: number): void
}>()

/** 用户状态管理实例 */
const userStore = useUserStore()

/** 默认头像 */
const defaultAvatar = 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 40"><rect fill="#4ECDC4" width="40" height="40"/><text x="20" y="26" text-anchor="middle" fill="white" font-size="16">U</text></svg>')

/** 格式化评论时间 */
const formattedTime = computed(() => formatRelativeTime(props.comment.createTime))

/**
 * 判断当前评论是否属于登录用户
 * @returns {boolean} 评论作者ID与当前用户ID一致时返回true
 */
const isOwner = computed(() => {
  return !!userStore.userInfo && userStore.userInfo.id === props.comment.userId
})

// ============================================================
// 三点菜单状态与交互
// ============================================================

/** 菜单是否展开 */
const showMenu = ref(false)

/** 菜单按钮容器引用（用于点击外部关闭） */
const moreRef = ref<HTMLElement | null>(null)

/** 举报弹窗是否显示 */
const showReportDialog = ref(false)

/**
 * 切换菜单显示状态
 * @description 点击三点按钮时切换下拉菜单的显隐
 * @returns {void}
 */
function toggleMenu(): void {
  showMenu.value = !showMenu.value
}

/**
 * 关闭菜单
 * @description 用于点击外部区域时收起下拉菜单
 * @returns {void}
 */
function closeMenu(): void {
  showMenu.value = false
}

/**
 * 全局点击事件处理
 * @description 点击菜单外部区域时自动关闭菜单
 * @param {MouseEvent} e - 鼠标事件
 * @returns {void}
 */
function handleClickOutside(e: MouseEvent): void {
  if (moreRef.value && !moreRef.value.contains(e.target as Node)) {
    closeMenu()
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

// ============================================================
// 删除评论
// ============================================================

/**
 * 处理删除按钮点击
 * @description 弹出确认对话框，确认后调用删除API并向上emit delete事件
 * @returns {Promise<void>}
 * @throws 删除失败时通过ElMessage提示用户
 */
async function handleDeleteClick(): Promise<void> {
  closeMenu()
  try {
    await ElMessageBox.confirm(
      '确定要删除这条评论吗？删除后子评论也将不可见。',
      '删除评论',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
    )
    await deleteComment(props.comment.id)
    ElMessage.success('评论已删除')
    emit('delete', props.comment.id)
  } catch (error: unknown) {
    // 用户取消操作不提示错误
    if (error !== 'cancel') {
      const msg = error instanceof Error ? error.message : '删除失败，请稍后重试'
      ElMessage.error(msg)
    }
  }
}

// ============================================================
// 举报评论
// ============================================================

/**
 * 处理举报按钮点击
 * @description 关闭菜单并打开举报弹窗
 * @returns {void}
 */
function handleReportClick(): void {
  closeMenu()
  showReportDialog.value = true
}

/**
 * 举报成功回调
 * @description 举报提交成功后提示用户
 * @returns {void}
 */
function handleReportSuccess(): void {
  ElMessage.success('举报已提交，我们会尽快处理')
}

// ============================================================
// 原有交互方法
// ============================================================

/** 处理点赞 */
function handleLike(): void {
  emit('like', props.comment)
}

/** 处理回复 */
function handleReply(): void {
  emit('reply', props.comment)
}

/** 处理头像/昵称点击，跳转用户主页 */
function handleClickUser(): void {
  emit('click-user', props.comment.userId)
}
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;
@use '@/styles/mixins.scss' as *;

.comment-item {
  position: relative;

  &.is-ai {
    .comment-item__main {
      background: linear-gradient(135deg, rgba(78, 205, 196, 0.04), rgba(255, 182, 39, 0.04));
      border-radius: $radius-sm;
      padding: $space-2 $space-3;
      margin: -$space-2 -$space-3;
    }
  }

  &__main {
    display: flex;
    gap: $space-3;
  }

  &__avatar {
    @include hexagon(36px);
    flex-shrink: 0;
    overflow: hidden;
    cursor: pointer;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  &__body {
    flex: 1;
    min-width: 0;
  }

  &__header {
    display: flex;
    align-items: center;
    gap: $space-1;
    flex-wrap: wrap;
  }

  &__nickname {
    font-size: 13px;
    font-weight: 600;
    color: $ink-700;
    cursor: pointer;
    transition: color $dur-fast ease;

    &:hover {
      color: $mint-600;
    }
  }

  &__ai-tag {
    display: inline-flex;
    align-items: center;
    gap: 2px;
    font-size: 10px;
    font-weight: 600;
    color: $amber-500;
    background: rgba(255, 182, 39, 0.1);
    padding: 1px 6px;
    border-radius: $radius-pill;
  }

  &__reply-to {
    font-size: 12px;
    color: $ink-300;
  }

  &__reply-name {
    color: $mint-600;
  }

  &__content {
    font-size: 14px;
    line-height: 1.6;
    color: $ink-700;
    margin-top: $space-1;
    word-break: break-word;
  }

  &__images {
    display: flex;
    gap: $space-1;
    margin-top: $space-2;
    flex-wrap: wrap;
  }

  &__img {
    width: 80px;
    height: 80px;
    object-fit: cover;
    border-radius: $radius-xs;
    cursor: zoom-in;
  }

  &__footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: $space-2;

    // 子评论（楼中楼）：改用固定间距，避免时间与回复按钮挤在一起
    .is-child & {
      justify-content: flex-start;
      gap: $space-6;
    }
  }

  &__time {
    font-size: 11px;
    color: $ink-300;
  }

  &__ops {
    display: flex;
    align-items: center;
    gap: $space-3;
  }

  &__op {
    display: inline-flex;
    align-items: center;
    gap: 3px;
    background: none;
    border: none;
    font-size: 12px;
    color: $ink-300;
    cursor: pointer;
    padding: 0;
    transition: color $dur-fast ease;

    &:hover {
      color: $ink-500;
    }

    &.is-liked {
      color: $coral-500;

      svg {
        animation: like-burst 0.4s $ease-spring;
      }
    }
  }

  // ---------- 更多操作（三点菜单） ----------
  &__more {
    position: relative;
    flex-shrink: 0;
    margin-left: auto;
  }

  &__more-btn {
    @include center;
    width: 30px;
    height: 30px;
    border-radius: $radius-sm;
    background: none;
    border: none;
    color: $ink-300;
    cursor: pointer;
    padding: 0;
    transition: all 0.3s $ease-spring;

    &:hover {
      color: $mint-500;
      background: rgba(78, 205, 196, 0.08);
      transform: rotate(15deg);
    }

    &.is-active {
      color: $mint-500;
      background: rgba(78, 205, 196, 0.12);
      transform: rotate(90deg);
    }
  }

  // 下拉面板 - 浅色毛玻璃风格
  &__dropdown {
    position: absolute;
    top: calc(100% + 8px);
    right: -6px;
    min-width: 140px;
    @include glass(20px, rgba(255, 255, 255, 0.92));
    border-radius: $radius-sm;
    border: 1px solid $ink-100;
    box-shadow:
      $shadow-lg,
      0 0 0 1px rgba(78, 205, 196, 0.06);
    padding: $space-1 0;
    z-index: 60;
    overflow: hidden;

    // 三角箭头
    &::before {
      content: '';
      position: absolute;
      top: -5px;
      right: 14px;
      width: 10px;
      height: 10px;
      background: rgba(255, 255, 255, 0.92);
      backdrop-filter: blur(20px) saturate(180%);
      transform: rotate(45deg);
      border-top: 1px solid $ink-100;
      border-left: 1px solid $ink-100;
    }
  }

  // 顶部装饰条（薄荷绿渐变）
  &__dropdown-bar {
    height: 2px;
    margin: 0 $space-2 $space-1;
    border-radius: 1px;
    background: $grad-mint;
    box-shadow: 0 0 8px rgba(78, 205, 196, 0.3);
  }

  // 菜单项
  &__dropdown-item {
    display: flex;
    align-items: center;
    gap: $space-2;
    width: 100%;
    padding: 8px $space-3;
    font-size: 13px;
    font-weight: 500;
    color: $ink-700;
    background: none;
    border: none;
    cursor: pointer;
    text-align: left;
    transition: all $dur-fast $ease-out;
    position: relative;
    overflow: hidden;

    // 左侧高亮条
    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 50%;
      transform: translateY(-50%) scaleY(0);
      width: 3px;
      height: 60%;
      border-radius: 0 2px 2px 0;
      background: $grad-mint;
      transition: transform 0.25s $ease-spring;
    }

    .item-icon {
      @include center;
      width: 22px;
      height: 22px;
      flex-shrink: 0;
      border-radius: $radius-xs;
      background: $ink-50;
      transition: all $dur-fast ease;

      svg {
        width: 14px;
        height: 14px;
      }
    }

    .item-text {
      line-height: 1.3;
    }

    &:hover {
      background: $mint-50;
      color: $mint-700;

      .item-icon {
        background: rgba(78, 205, 196, 0.15);
        color: $mint-600;
      }

      &::before {
        transform: translateY(-50%) scaleY(1);
      }
    }

    &:active {
      transform: scale(0.97);
    }

    // 删除 - 珊瑚红主题
    &.is-danger {
      .item-icon {
        background: rgba(239, 68, 68, 0.08);
        color: $coral-500;
      }

      &:hover {
        background: rgba(239, 68, 68, 0.06);
        color: $coral-600;

        .item-icon {
          background: rgba(239, 68, 68, 0.15);
          color: $coral-500;
          animation: shake-icon 0.4s ease;
        }

        &::before {
          background: linear-gradient(180deg, $coral-500, $coral-600);
        }
      }
    }

    // 举报 - 琥珀主题
    &.is-report {
      .item-icon {
        background: rgba(255, 182, 39, 0.1);
        color: $amber-600;
      }

      &:hover {
        background: rgba(255, 182, 39, 0.08);
        color: $amber-700;

        .item-icon {
          background: rgba(255, 182, 39, 0.18);
          color: $amber-600;
        }

        &::before {
          background: linear-gradient(180deg, $amber-500, $amber-600);
        }
      }
    }
  }

  // ---------- 楼中楼 ----------
  &__children {
    margin-left: 48px;
    margin-top: $space-2;
    padding-left: $space-3;
    border-left: 2px solid $ink-50;
    display: flex;
    flex-direction: column;
    gap: $space-3;

    .comment-item__main {
      // 子评论稍小
      .comment-item__avatar {
        @include hexagon(28px);
      }
    }
  }
}

// ---------- 菜单下拉动画（弹性弹出） ----------
.menu-dropdown-enter-active {
  animation: menu-pop-in 0.35s $ease-spring both;
}
.menu-dropdown-leave-active {
  animation: menu-pop-out 0.2s ease both;
}

@keyframes menu-pop-in {
  from {
    opacity: 0;
    transform: translateY(-8px) scale(0.92);
    filter: blur(4px);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
    filter: blur(0);
  }
}

@keyframes menu-pop-out {
  from {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
  to {
    opacity: 0;
    transform: translateY(-6px) scale(0.95);
  }
}

// ---------- 删除图标抖动动画 ----------
@keyframes shake-icon {
  0%, 100% { transform: translateX(0) rotate(0); }
  20% { transform: translateX(-3px) rotate(-8deg); }
  40% { transform: translateX(3px) rotate(8deg); }
  60% { transform: translateX(-2px) rotate(-4deg); }
  80% { transform: translateX(2px) rotate(4deg); }
}
</style>
