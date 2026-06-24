<template>
  <div class="members-page page-enter">
    <!-- 顶部导航栏 -->
    <header class="nav-bar">
      <button class="back-btn" @click="goBack">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" width="22" height="22">
          <path d="M15 18l-6-6 6-6" />
        </svg>
      </button>
      <h1 class="nav-title">成员管理</h1>
      <span class="member-count">{{ total }} 人</span>
    </header>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
        <circle cx="11" cy="11" r="8" />
        <path d="M21 21l-4.35-4.35" />
      </svg>
      <input
        v-model="keyword"
        class="search-input"
        type="text"
        placeholder="搜索成员昵称"
        @keyup.enter="handleSearch"
      />
      <button v-if="keyword" class="clear-btn" @click="clearSearch">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
          <path d="M18 6L6 18M6 6l12 12" />
        </svg>
      </button>
    </div>

    <!-- 成员列表 -->
    <LoadingSpinner v-if="loading && !memberList.length" />

    <EmptyState
      v-else-if="!loading && !memberList.length"
      :description="keyword ? '没有找到匹配的成员' : '圈子暂无其他成员'"
    />

    <div v-else class="member-list">
      <div v-for="(member, index) in memberList" :key="member.userId" class="member-card stagger-item" :style="{ '--stagger-delay': `${index * 60}ms` }">
        <img
          :src="member.avatar"
          :alt="member.nickname"
          class="member-avatar"
          @click="goProfile(member.userId)"
        />
        <div class="member-info" @click="goProfile(member.userId)">
          <div class="member-name-row">
            <span class="member-name">{{ member.nickname }}</span>
            <span v-if="member.role === 1" class="role-badge owner">圈主</span>
            <span v-else class="role-badge normal">成员</span>
          </div>
          <span class="member-join-time">入圈 · {{ formatTime(member.joinTime) }}</span>
        </div>

        <!-- 操作区：圈主不显示操作按钮 -->
        <div v-if="member.role !== 1" class="member-actions">
          <button class="action-btn transfer" @click="openTransfer(member)">转让</button>
          <button class="action-btn remove" @click="openRemove(member)">移除</button>
        </div>
      </div>

      <!-- 加载更多 -->
      <div class="load-more-area">
        <LoadingSpinner v-if="loadingMore" size="small" />
        <p v-else-if="!hasMore && memberList.length" class="no-more">— 没有更多了 —</p>
        <button v-else-if="hasMore" class="load-more-btn" @click="loadMore">加载更多</button>
      </div>
    </div>

    <!-- 移除确认弹窗 -->
    <transition name="modal-fade">
      <div v-if="showRemoveModal" class="modal-mask" @click.self="showRemoveModal = false">
        <div class="modal-box modal-danger">
          <h3 class="modal-title">移除成员</h3>
          <p class="modal-desc">
            确认将「{{ targetMember?.nickname }}」移出圈子？移除后对方将不再是本圈成员，可重新申请加入。
          </p>
          <div class="modal-footer">
            <button class="modal-cancel" @click="showRemoveModal = false">取消</button>
            <button
              :class="['modal-confirm danger', { disabled: removing }]"
              :disabled="removing"
              @click="confirmRemove"
            >
              {{ removing ? '移除中...' : '确认移除' }}
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- 转让圈主弹窗 -->
    <transition name="modal-fade">
      <div v-if="showTransferModal" class="modal-mask" @click.self="showTransferModal = false">
        <div class="modal-box modal-danger">
          <h3 class="modal-title">转让圈主</h3>
          <p class="modal-desc warning-text">
            确认将圈主转让给「{{ targetMember?.nickname }}」？转让后你将变为普通成员，失去全部管理权限。
          </p>
          <div class="modal-footer">
            <button class="modal-cancel" @click="showTransferModal = false">取消</button>
            <button
              :class="['modal-confirm warning', { disabled: transferring }]"
              :disabled="transferring"
              @click="confirmTransfer"
            >
              {{ transferring ? '转让中...' : '确认转让' }}
            </button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
/**
 * CircleMembersPage 圈子成员管理页
 * 功能：成员列表（分页+搜索）、移除成员、转让圈主
 * 权限：仅圈主可访问（入口由详情页控制；后端接口亦做圈主校验）
 */
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCircleMembers, removeCircleMember, transferCircleOwner } from '@/api/circle'
import type { CircleMember } from '@/types'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
const router = useRouter()

// ---------- 响应式状态 ----------
/** 圈子ID */
const circleId = computed(() => Number(route.params.id))
/** 成员列表 */
const memberList = ref<CircleMember[]>([])
/** 成员总数 */
const total = ref(0)
/** 当前页码 */
const page = ref(1)
/** 每页数量 */
const pageSize = 20
/** 是否有更多 */
const hasMore = ref(true)
/** 搜索关键词 */
const keyword = ref('')
/** 首次加载中 */
const loading = ref(true)
/** 加载更多中 */
const loadingMore = ref(false)
/** 是否显示移除弹窗 */
const showRemoveModal = ref(false)
/** 是否显示转让弹窗 */
const showTransferModal = ref(false)
/** 当前操作的目标成员 */
const targetMember = ref<CircleMember | null>(null)
/** 移除提交中 */
const removing = ref(false)
/** 转让提交中 */
const transferring = ref(false)

// ---------- 方法 ----------
/**
 * 加载成员列表
 * @param {boolean} append - 是否追加（加载更多）
 */
async function loadMembers(append = false): Promise<void> {
  if (append) {
    loadingMore.value = true
  } else {
    loading.value = true
  }
  try {
    const res = await getCircleMembers(circleId.value, {
      page: page.value,
      pageSize,
      keyword: keyword.value.trim() || undefined
    })
    const data = res.data
    if (append) {
      memberList.value.push(...data.list)
    } else {
      memberList.value = data.list
    }
    total.value = data.total
    // 是否还有更多：已加载条数 < 总数
    hasMore.value = memberList.value.length < data.total
  } catch {
    if (!append) memberList.value = []
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

/**
 * 搜索（重置分页）
 */
function handleSearch(): void {
  page.value = 1
  loadMembers()
}

/**
 * 清除搜索
 */
function clearSearch(): void {
  keyword.value = ''
  handleSearch()
}

/**
 * 加载更多
 */
function loadMore(): void {
  if (loadingMore.value || !hasMore.value) return
  page.value++
  loadMembers(true)
}

/**
 * 打开移除弹窗
 */
function openRemove(member: CircleMember): void {
  targetMember.value = member
  showRemoveModal.value = true
}

/**
 * 确认移除成员
 */
async function confirmRemove(): Promise<void> {
  if (!targetMember.value || removing.value) return
  removing.value = true
  try {
    await removeCircleMember(circleId.value, targetMember.value.userId)
    // 本地剔除并更新计数
    memberList.value = memberList.value.filter(m => m.userId !== targetMember.value!.userId)
    total.value = Math.max(0, total.value - 1)
    showRemoveModal.value = false
  } catch {
    // 错误由全局拦截器处理
  } finally {
    removing.value = false
  }
}

/**
 * 打开转让弹窗
 */
function openTransfer(member: CircleMember): void {
  targetMember.value = member
  showTransferModal.value = true
}

/**
 * 确认转让圈主
 */
async function confirmTransfer(): Promise<void> {
  if (!targetMember.value || transferring.value) return
  transferring.value = true
  try {
    await transferCircleOwner(circleId.value, targetMember.value.userId)
    // 本地交换角色：新圈主置 1，其余（含原圈主自己）置 0
    const newOwnerId = targetMember.value.userId
    memberList.value = memberList.value.map(m => ({
      ...m,
      role: (m.userId === newOwnerId ? 1 : 0) as 0 | 1
    }))
    // 按圈主在前重新排序，保持视觉一致
    memberList.value.sort((a, b) => b.role - a.role)
    showTransferModal.value = false
  } catch {
    // 错误由全局拦截器处理
  } finally {
    transferring.value = false
  }
}

/**
 * 跳转用户主页
 */
function goProfile(userId: number): void {
  router.push(`/profile/${userId}`)
}

/**
 * 返回上一页
 */
function goBack(): void {
  router.back()
}

/**
 * 格式化入圈时间
 * @param {string} time - ISO 时间字符串
 * @returns {string} YYYY-MM-DD
 */
function formatTime(time: string): string {
  if (!time) return ''
  return time.replace('T', ' ').slice(0, 10)
}

// ---------- 生命周期 ----------
onMounted(() => {
  loadMembers()
})
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;
@use '@/styles/mixins.scss' as *;

.members-page {
  min-height: 100vh;
  background: $ink-50;
  padding-bottom: 40px;
  animation: page-enter 0.5s $ease-out both;
}

// ---------- 页面整体入场动画 ----------
@keyframes page-enter {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

// ---------- 顶部导航 ----------
.nav-bar {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  gap: $space-3;
  height: 56px;
  padding: 0 $space-4;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid $border-subtle;
}

.back-btn {
  @include center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  color: $ink-700;
  transition: all $dur-fast $ease-out;

  &:active {
    background: $ink-100;
  }
}

.nav-title {
  flex: 1;
  font-family: $font-heading;
  font-size: 17px;
  font-weight: 700;
  color: $ink-900;
}

.member-count {
  font-size: 13px;
  color: $ink-300;
}

// ---------- 搜索栏 ----------
.search-bar {
  display: flex;
  align-items: center;
  gap: $space-2;
  margin: $space-4;
  padding: 0 $space-4;
  height: 44px;
  background: #fff;
  border-radius: $radius-pill;
  border: 1.5px solid $ink-100;
  transition: border-color $dur-fast $ease-out;

  &:focus-within {
    border-color: $mint-500;
    box-shadow: 0 0 0 3px rgba(78, 205, 196, 0.1);
  }
}

.search-icon {
  color: $ink-300;
  flex-shrink: 0;
}

.search-input {
  flex: 1;
  height: 100%;
  border: none;
  outline: none;
  font-size: 14px;
  color: $ink-700;
  background: transparent;

  &::placeholder {
    color: $ink-300;
  }
}

.clear-btn {
  @include center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  color: $ink-300;
  background: $ink-100;
  flex-shrink: 0;
  transition: all $dur-fast $ease-out;

  &:active {
    background: $ink-300;
    color: #fff;
  }
}

// ---------- 成员列表 ----------
.member-list {
  margin: 0 $space-4;
  display: flex;
  flex-direction: column;
  gap: $space-2;
}

.member-card {
  display: flex;
  align-items: center;
  gap: $space-3;
  padding: $space-3;
  background: #fff;
  border-radius: $radius-md;
  box-shadow: $shadow-xs;
  transition: transform $dur-fast $ease-out;

  &:active {
    transform: scale(0.99);
  }
}

.member-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
  border: 1.5px solid $mint-300;
  flex-shrink: 0;
  cursor: pointer;
}

.member-info {
  flex: 1;
  min-width: 0;
  cursor: pointer;
}

.member-name-row {
  display: flex;
  align-items: center;
  gap: $space-2;
}

.member-name {
  font-size: 15px;
  font-weight: 600;
  color: $ink-900;
  @include ellipsis-1;
}

.role-badge {
  flex-shrink: 0;
  padding: 1px 8px;
  border-radius: $radius-xs;
  font-size: 11px;
  font-weight: 600;

  &.owner {
    color: $amber-600;
    background: rgba(255, 182, 39, 0.15);
    border: 1px solid rgba(255, 182, 39, 0.3);
  }

  &.normal {
    color: $mint-600;
    background: rgba(78, 205, 196, 0.1);
    border: 1px solid rgba(78, 205, 196, 0.25);
  }
}

.member-join-time {
  display: block;
  margin-top: 2px;
  font-size: 12px;
  color: $ink-300;
}

// ---------- 操作按钮 ----------
.member-actions {
  display: flex;
  gap: $space-2;
  flex-shrink: 0;
}

.action-btn {
  padding: 6px 14px;
  border-radius: $radius-sm;
  font-size: 13px;
  font-weight: 600;
  transition: all $dur-fast $ease-out;

  &.transfer {
    color: $amber-600;
    background: rgba(255, 182, 39, 0.08);
    border: 1.5px solid rgba(255, 182, 39, 0.25);

    &:active {
      background: rgba(255, 182, 39, 0.18);
    }
  }

  &.remove {
    color: $coral-500;
    background: rgba(255, 107, 107, 0.06);
    border: 1.5px solid rgba(255, 107, 107, 0.22);

    &:active {
      background: rgba(255, 107, 107, 0.15);
    }
  }
}

// ---------- 加载更多 ----------
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

  &:active {
    background: rgba(78, 205, 196, 0.15);
  }
}

// ---------- 弹窗 ----------
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

@keyframes scale-in {
  from { transform: scale(0.9); opacity: 0; }
  to   { transform: scale(1); opacity: 1; }
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

  &:active {
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
  transition: all $dur-fast $ease-out;

  &.danger {
    background: $coral-500;
  }

  &.warning {
    background: $amber-500;
  }

  &.disabled {
    opacity: 0.5;
    cursor: not-allowed;
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
  .members-page {
    padding-top: env(safe-area-inset-top);
  }

  .member-actions {
    flex-direction: column;
    gap: $space-1;
  }

  .action-btn {
    padding: 4px 10px;
    font-size: 12px;
  }
}
</style>
