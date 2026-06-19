<template>
  <Teleport to="body">
    <Transition name="chain-dialog-fade">
      <div v-if="show" class="chain-overlay" @click="close">
        <div class="chain-dialog" @click.stop>
          <!-- 标题栏 -->
          <header class="chain-header">
            <h3 class="chain-title">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" class="chain-title__icon">
                <path d="M4 12v8a2 2 0 002 2h12a2 2 0 002-2v-8M16 6l-4-4-4 4M12 2v13"
                  stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              转发列表
            </h3>
            <button class="chain-close" @click="close">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                <path d="M18 6L6 18M6 6l12 12" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>
            </button>
          </header>

          <!-- 内容区 -->
          <div class="chain-body">
            <!-- 加载态 -->
            <div v-if="loading && list.length === 0" class="chain-loading">
              <van-loading size="24px" color="#4ECDC4">加载中...</van-loading>
            </div>

            <!-- 转发列表 -->
            <div v-else-if="list.length > 0" class="chain-list">
              <div
                v-for="item in list"
                :key="item.id"
                class="chain-item"
                @click="$emit('clickShare', item.id)"
              >
                <!-- 用户头像 + 信息 -->
                <div class="chain-item__user" @click.stop="$emit('clickUser', item.userId)">
                  <img
                    :src="item.avatar || defaultAvatar"
                    :alt="item.nickname"
                    class="chain-item__avatar"
                  />
                  <div class="chain-item__info">
                    <span class="chain-item__name">{{ item.nickname || '用户' }}</span>
                    <span class="chain-item__time">{{ formatRelativeTime(item.createTime) }}</span>
                  </div>
                </div>

                <!-- 转发文案 -->
                <p v-if="item.content" class="chain-item__content">
                  {{ item.content }}
                </p>
                <p v-else class="chain-item__content chain-item__content--empty">
                  （无附加文案）
                </p>
              </div>

              <!-- 加载更多 -->
              <div v-if="hasMore" class="chain-more">
                <button
                  v-if="!loadingMore"
                  class="chain-more__btn"
                  @click="loadMore"
                >
                  加载更多
                </button>
                <van-loading v-else size="20px" color="#4ECDC4" />
              </div>
            </div>

            <!-- 空状态 -->
            <div v-else class="chain-empty">
              <van-empty
                image="search"
                description="暂无转发"
                :image-size="100"
              />
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
/**
 * ShareChainDialog 转发链弹窗组件
 *
 * @description 展示某个帖子被哪些用户转发过的完整列表，支持分页加载。
 *   每条转发记录显示转发者头像、昵称、时间和转发文案，
 *   点击用户头像可跳转主页，点击卡片可跳转到转发帖详情。
 *
 * @prop {boolean} show - 控制弹窗显示/隐藏（v-model 双向绑定）
 * @prop {number} postId - 原帖ID，用于查询转发列表
 *
 * @emits update:show - 更新显示状态
 * @emits clickShare - 点击转发帖项，参数为转发帖ID
 * @emits clickUser - 点击用户头像，参数为用户ID
 */
import { ref, watch } from 'vue'
import { getShareChain } from '@/api/post'
import { formatRelativeTime } from '@/utils/format'
import type { Post } from '@/types'

const props = defineProps<{
  /** 是否显示弹窗（v-model） */
  show: boolean
  /** 原帖ID */
  postId: number
}>()

const emit = defineEmits<{
  /** 更新显示状态 */
  (e: 'update:show', value: boolean): void
  /** 点击转发帖项 */
  (e: 'clickShare', sharePostId: number): void
  /** 点击用户头像 */
  (e: 'clickUser', userId: number): void
}>()

// ---------- 状态 ----------

/** 默认头像（SVG Data URI） */
const defaultAvatar = 'data:image/svg+xml,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 40"><rect fill="#4ECDC4" width="40" height="40"/><text x="20" y="26" text-anchor="middle" fill="white" font-size="16">U</text></svg>'
)

/** 转发列表数据 */
const list = ref<Post[]>([])

/** 是否正在加载首屏 */
const loading = ref(false)

/** 是否正在加载更多 */
const loadingMore = ref(false)

/** 当前页码 */
const currentPage = ref(1)

/** 每页大小 */
const pageSize = 20

/** 是否还有更多数据 */
const hasMore = ref(true)

// ---------- 方法 ----------

/**
 * 关闭弹窗
 * @description 重置内部状态并通知父组件关闭
 */
function close(): void {
  emit('update:show', false)
}

/**
 * 加载转发列表数据
 *
 * @param {boolean} isLoadMore - 是否为加载更多操作
 * @returns {Promise<void>}
 * @description 调用 getShareChain 接口获取分页转发数据，加载更多时追加到现有列表
 */
async function fetchData(isLoadMore = false): Promise<void> {
  if (isLoadMore) {
    loadingMore.value = true
  } else {
    loading.value = true
    currentPage.value = 1
    hasMore.value = true
  }

  try {
    const res = await getShareChain(props.postId, currentPage.value, pageSize)
    const pageData = res.data || {}
    const newList: Post[] = pageData.records || []

    // 对 liked/collected 做布尔值兜底
    newList.forEach(p => {
      p.liked = !!p.liked
      p.collected = !!p.collected
    })

    if (isLoadMore) {
      list.value.push(...newList)
    } else {
      list.value = newList
    }

    // 判断是否还有更多
    const total = pageData.total ?? 0
    hasMore.value = (currentPage.value * pageSize) < total
  } catch {
    // 静默失败，不弹 toast（避免干扰用户浏览）
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

/**
 * 加载下一页数据
 * @description 页码自增后请求接口追加数据
 */
function loadMore(): void {
  if (loadingMore.value || !hasMore.value) return
  currentPage.value++
  fetchData(true)
}

// ---------- 监听 ----------

/**
 * 监听弹窗打开状态自动加载数据
 * @description 每次打开时重新拉取第一页数据；关闭时清空列表释放内存
 */
watch(() => props.show, (val) => {
  if (val) {
    fetchData()
  } else {
    list.value = []
    currentPage.value = 1
  }
})
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;
@use '@/styles/mixins.scss' as *;

// ========== 遮罩层 + 弹窗容器 ==========
.chain-overlay {
  position: fixed;
  inset: 0;
  z-index: 1050;
  background: rgba(26, 29, 46, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: $space-4;
}

.chain-dialog {
  width: 100%;
  max-width: 480px;
  max-height: 75vh;
  background: #fff;
  border-radius: $radius-lg;
  box-shadow: $shadow-lg;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: scale-in 0.28s $ease-spring both;

  @include mobile {
    max-height: 85vh;
    border-radius: $radius-lg $radius-lg 0 0;
    margin-top: auto;
    animation: slide-up 0.32s $ease-spring both;
  }
}

// ========== 标题栏 ==========
.chain-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $space-5 $space-5 $space-3;
  border-bottom: 1px solid $ink-100;
  flex-shrink: 0;
}

.chain-title {
  font-size: 16px;
  font-weight: 600;
  color: $ink-900;
  margin: 0;
  display: flex;
  align-items: center;
  gap: $space-2;

  &__icon {
    color: $mint-500;
    flex-shrink: 0;
  }
}

.chain-close {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: none;
  border: none;
  color: $ink-500;
  cursor: pointer;
  transition: all $dur-fast ease;

  &:hover {
    background: $ink-50;
    color: $ink-700;
  }
}

// ========== 内容区 ==========
.chain-body {
  flex: 1;
  overflow-y: auto;
  padding: $space-3 $space-4 $space-5;

  &::-webkit-scrollbar {
    width: 4px;
  }

  &::-webkit-scrollbar-thumb {
    background: $ink-100;
    border-radius: 2px;
  }
}

// ========== 加载态 ==========
.chain-loading {
  display: flex;
  justify-content: center;
  padding: $space-12 0;
}

// ========== 转发列表 ==========
.chain-list {
  display: flex;
  flex-direction: column;
  gap: $space-2;
}

.chain-item {
  padding: $space-3 $space-4;
  background: $ink-50;
  border-radius: $radius-md;
  cursor: pointer;
  transition: all $dur-fast ease;

  &:hover {
    background: $mint-50;
    transform: translateX(2px);
  }

  &:active {
    transform: scale(0.99);
  }
}

.chain-item__user {
  display: flex;
  align-items: center;
  gap: $space-3;
  margin-bottom: $space-2;
}

.chain-item__avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
  border: 2px solid $mint-100;
}

.chain-item__info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chain-item__name {
  font-size: 14px;
  font-weight: 600;
  color: $ink-900;
}

.chain-item__time {
  font-size: 11px;
  color: $ink-500;
  margin-top: 1px;
}

.chain-item__content {
  font-size: 13px;
  line-height: 1.6;
  color: $ink-700;
  margin: 0;
  word-break: break-word;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;

  &--empty {
    color: $ink-500;
    font-style: italic;
  }
}

// ========== 加载更多 ==========
.chain-more {
  display: flex;
  justify-content: center;
  padding: $space-4 0 $space-2;
}

.chain-more__btn {
  font-size: 13px;
  color: $mint-500;
  background: none;
  border: none;
  cursor: pointer;
  padding: $space-2 $space-4;
  border-radius: $radius-pill;
  transition: all $dur-fast ease;

  &:hover {
    background: $mint-50;
    color: $mint-700;
  }

  &:active {
    transform: scale(0.96);
  }
}

// ========== 空状态 ==========
.chain-empty {
  padding: $space-10 0 $space-6;
}

// ========== 过渡动画 ==========
.chain-dialog-fade-enter-active {
  transition: opacity $dur-base $ease-out;
}
.chain-dialog-fade-leave-active {
  transition: opacity 0.2s ease-in;
}
.chain-dialog-fade-enter-from,
.chain-dialog-fade-leave-to {
  opacity: 0;
}

@keyframes scale-in {
  from {
    opacity: 0;
    transform: scale(0.92);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes slide-up {
  from {
    transform: translateY(100%);
  }
  to {
    transform: translateY(0);
  }
}
</style>
