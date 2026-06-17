<template>
  <div class="my-page">
    <!-- 蜂巢装饰背景 -->
    <div class="hex-bg" />

    <!-- 个人信息头部 -->
    <section class="my-hero">
      <div class="hero-inner">
        <!-- 蜂巢装饰 -->
        <div class="hero-deco">
          <div class="deco-hex hex-1 anim-float" />
          <div class="deco-hex hex-2 anim-float" />
        </div>

        <!-- 六边形头像 -->
        <div class="my-avatar-wrap" @click="goProfile">
          <div class="hex-avatar">
            <img :src="userInfo.avatar || defaultAvatar" :alt="userInfo.nickname" />
          </div>
          <div class="avatar-ring" />
        </div>

        <!-- 用户名与简介 -->
        <div class="my-info">
          <h1 class="my-name font-display">{{ userInfo.nickname || '未登录' }}</h1>
          <p v-if="userInfo.bio" class="my-bio">{{ userInfo.bio }}</p>
        </div>

        <!-- 数据统计行 -->
        <div class="my-stats">
          <div class="stat-item" @click="goProfile">
            <span class="stat-num font-heading">{{ formatNumber(userInfo.postCount || 0) }}</span>
            <span class="stat-label">帖子</span>
          </div>
          <div class="stat-divider" />
          <div class="stat-item" @click="goFollowing">
            <span class="stat-num font-heading">{{ formatNumber(userInfo.followCount || 0) }}</span>
            <span class="stat-label">关注</span>
          </div>
          <div class="stat-divider" />
          <div class="stat-item" @click="goFollowers">
            <span class="stat-num font-heading">{{ formatNumber(userInfo.fanCount || 0) }}</span>
            <span class="stat-label">粉丝</span>
          </div>
        </div>
      </div>
    </section>

    <!-- 功能列表 -->
    <section class="menu-section">
      <div class="menu-inner">
        <!-- 内容管理组 -->
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

          <button class="menu-item" @click="goMyCollects">
            <div class="menu-icon collect-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                <path d="M19 21l-7-5-7 5V5a2 2 0 012-2h10a2 2 0 012 2z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
            <span class="menu-label">我的收藏</span>
            <svg class="menu-arrow" width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M9 18l6-6-6-6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>

          <div class="menu-divider" />

          <button class="menu-item" @click="goMyLikes">
            <div class="menu-icon like-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z" stroke="currentColor" stroke-width="2"/>
              </svg>
            </div>
            <span class="menu-label">我的点赞</span>
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
      </div>
    </section>

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
 * 我的页面（移动端个人中心入口）
 * @description 展示个人信息头部、数据统计、功能菜单列表，
 *   支持退出登录确认弹窗，作为移动端底部Tab入口
 */

import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { useUserStore } from '@/stores/user'
import { getUserProfile } from '@/api/user'
import { formatNumber } from '@/utils/format'
import type { User } from '@/types'

// ---------- 路由与Store ----------
const router = useRouter()
const userStore = useUserStore()

// ---------- 默认头像 ----------
const defaultAvatar = 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 40"><rect fill="#4ECDC4" width="40" height="40"/><text x="20" y="26" text-anchor="middle" fill="white" font-size="16">U</text></svg>')

// ---------- 响应式数据 ----------
const userInfo = ref<Partial<User>>({
  nickname: '未登录',
  avatar: defaultAvatar,
  bio: '',
  postCount: 0,
  followCount: 0,
  fanCount: 0
})
const showLogoutConfirm = ref(false)

// ---------- 数据加载 ----------

/**
 * 加载用户资料
 * @returns {Promise<void>}
 * @description 从服务端获取当前用户信息
 */
async function fetchProfile(): Promise<void> {
  try {
    if (!userStore.userInfo) {
      await userStore.fetchUser()
    }
    if (userStore.userInfo) {
      userInfo.value = userStore.userInfo
    }
  } catch {
    // 静默处理
  }
}

// ---------- 交互操作 ----------

/**
 * 退出登录
 * @returns {Promise<void>}
 * @description 调用userStore的logout方法清除登录态并跳转
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

// ---------- 导航 ----------

/**
 * 跳转个人主页
 * @returns {void}
 */
function goProfile(): void {
  if (userStore.userInfo?.id) {
    router.push(`/profile/${userStore.userInfo.id}`)
  }
}

/**
 * 跳转关注列表
 * @returns {void}
 */
function goFollowing(): void {
  if (userStore.userInfo?.id) {
    router.push(`/profile/${userStore.userInfo.id}`)
  }
}

/**
 * 跳转粉丝列表
 * @returns {void}
 */
function goFollowers(): void {
  if (userStore.userInfo?.id) {
    router.push(`/profile/${userStore.userInfo.id}`)
  }
}

/**
 * 跳转我的圈子
 * @returns {void}
 */
function goMyCircles(): void {
  router.push('/circle')
}

/**
 * 跳转我的收藏
 * @returns {void}
 */
function goMyCollects(): void {
  if (userStore.userInfo?.id) {
    router.push(`/profile/${userStore.userInfo.id}`)
  }
}

/**
 * 跳转我的点赞
 * @returns {void}
 */
function goMyLikes(): void {
  if (userStore.userInfo?.id) {
    router.push(`/profile/${userStore.userInfo.id}`)
  }
}

/**
 * 跳转草稿箱
 * @returns {void}
 */
function goDrafts(): void {
  showToast('草稿箱功能开发中')
}

/**
 * 跳转账号设置
 * @returns {void}
 */
function goSettings(): void {
  router.push('/settings')
}

/**
 * 跳转关于页面
 * @returns {void}
 */
function goAbout(): void {
  showToast('MintHive v1.0.0')
}

// ---------- 生命周期 ----------
onMounted(() => {
  fetchProfile()
})
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;
@use '@/styles/mixins.scss' as *;

.my-page {
  min-height: 100vh;
  background: $ink-50;
  position: relative;
  padding-bottom: 100px;
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

// ---------- 个人信息头部 ----------
.my-hero {
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
  padding: $space-8 $space-4 $space-5;
  max-width: 600px;
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
  @include hexagon(36px);
  background: $grad-mint;
  opacity: 0.08;

  &.hex-1 {
    top: 15%;
    right: 10%;
    animation-delay: 0s;
  }

  &.hex-2 {
    bottom: 20%;
    left: 8%;
    width: 24px;
    height: 24px * 1.1547;
    animation-delay: -4s;
  }
}

// ---------- 头像 ----------
.my-avatar-wrap {
  position: relative;
  margin-bottom: $space-3;
  cursor: pointer;
  transition: transform $dur-base $ease-spring;

  &:hover {
    transform: scale(1.05);
  }
}

.hex-avatar {
  @include hexagon(72px);
  overflow: hidden;
  position: relative;
  z-index: 2;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.avatar-ring {
  position: absolute;
  @include hexagon(84px);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: $grad-mint;
  opacity: 0.25;
  z-index: 1;
  animation: hex-breathe 3s ease-in-out infinite;
}

// ---------- 用户信息 ----------
.my-info {
  text-align: center;
  margin-bottom: $space-4;
}

.my-name {
  font-size: 22px;
  font-weight: 700;
  color: $ink-900;
  margin-bottom: $space-1;
}

.my-bio {
  font-size: 13px;
  color: $ink-500;
  line-height: 1.5;
  max-width: 300px;
}

// ---------- 数据统计 ----------
.my-stats {
  display: flex;
  align-items: center;
  gap: $space-6;
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
  font-size: 20px;
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
  height: 24px;
  background: $ink-100;
}

// ---------- 功能列表 ----------
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

  &.circle-icon {
    background: rgba(107, 203, 119, 0.12);
    color: $leaf-500;
  }

  &.collect-icon {
    background: rgba(255, 92, 138, 0.12);
    color: $rose-500;
  }

  &.like-icon {
    background: rgba(255, 107, 107, 0.12);
    color: $coral-500;
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
    background: $ink-100;
    color: $ink-500;

    &:hover {
      background: $ink-50;
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

// ---------- 响应式 ----------
@include mobile {
  .hero-inner {
    padding: $space-6 $space-3 $space-4;
  }

  .hex-avatar {
    @include hexagon(60px);
  }

  .avatar-ring {
    @include hexagon(70px);
  }

  .my-name {
    font-size: 18px;
  }

  .menu-section {
    padding: $space-2 $space-3;
  }
}
</style>
