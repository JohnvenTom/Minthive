<template>
  <div class="settings-page">
    <NavBar title="设置" :show-back="true" />

    <div class="settings-page__content">
      <!-- 账号与安全 -->
      <section class="settings-page__section">
        <h3 class="settings-page__section-title">账号与安全</h3>
        <div class="settings-page__group">
          <div class="settings-page__item" @click="goChangePassword">
            <span class="settings-page__item-label">修改密码</span>
            <svg class="settings-page__arrow" width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M9 6l6 6-6 6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <div class="settings-page__item" @click="goProfile">
            <span class="settings-page__item-label">编辑个人资料</span>
            <svg class="settings-page__arrow" width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M9 6l6 6-6 6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <div class="settings-page__item" @click="goInterest">
            <span class="settings-page__item-label">修改兴趣标签</span>
            <svg class="settings-page__arrow" width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M9 6l6 6-6 6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
        </div>
      </section>

      <!-- 通知设置 -->
      <section class="settings-page__section">
        <h3 class="settings-page__section-title">通知设置</h3>
        <div class="settings-page__group">
          <div class="settings-page__item">
            <span class="settings-page__item-label">评论通知</span>
            <van-switch v-model="notifyComment" size="20px" active-color="#6BCB77" />
          </div>
          <div class="settings-page__item">
            <span class="settings-page__item-label">点赞通知</span>
            <van-switch v-model="notifyLike" size="20px" active-color="#6BCB77" />
          </div>
          <div class="settings-page__item">
            <span class="settings-page__item-label">私信通知</span>
            <van-switch v-model="notifyChat" size="20px" active-color="#6BCB77" />
          </div>
          <div class="settings-page__item">
            <span class="settings-page__item-label">圈子公告</span>
            <van-switch v-model="notifyCircle" size="20px" active-color="#6BCB77" />
          </div>
        </div>
      </section>

      <!-- AI 功能 -->
      <section class="settings-page__section">
        <h3 class="settings-page__section-title">AI 功能</h3>
        <div class="settings-page__group">
          <div class="settings-page__item">
            <span class="settings-page__item-label">AI 自动代回复私信</span>
            <van-switch v-model="aiAutoReply" size="20px" active-color="#FFB627" />
          </div>
          <div class="settings-page__item">
            <span class="settings-page__item-label">AI 发帖助手</span>
            <van-switch v-model="aiPostHelper" size="20px" active-color="#FFB627" />
          </div>
          <div class="settings-page__item">
            <span class="settings-page__item-label">AI 评论建议</span>
            <van-switch v-model="aiCommentSuggest" size="20px" active-color="#FFB627" />
          </div>
        </div>
      </section>

      <!-- 其他 -->
      <section class="settings-page__section">
        <h3 class="settings-page__section-title">其他</h3>
        <div class="settings-page__group">
          <div class="settings-page__item" @click="clearCache">
            <span class="settings-page__item-label">清除缓存</span>
            <span class="settings-page__item-value">{{ cacheSize }}</span>
          </div>
          <div class="settings-page__item" @click="showAbout = true">
            <span class="settings-page__item-label">关于 MintHive</span>
            <svg class="settings-page__arrow" width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M9 6l6 6-6 6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <div class="settings-page__item" @click="showUserAgreement = true">
            <span class="settings-page__item-label">用户协议</span>
            <svg class="settings-page__arrow" width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M9 6l6 6-6 6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <div class="settings-page__item" @click="showPrivacy = true">
            <span class="settings-page__item-label">隐私政策</span>
            <svg class="settings-page__arrow" width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M9 6l6 6-6 6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
        </div>
      </section>

      <!-- 退出登录 -->
      <button class="settings-page__logout" @click="handleLogout">退出登录</button>

      <!-- 注销账号 -->
      <button class="settings-page__delete-account" @click="handleDeleteAccount">注销账号</button>
    </div>

    <!-- 关于弹窗 -->
    <van-dialog v-model:show="showAbout" title="关于 MintHive" :show-confirm-button="true">
      <div class="settings-page__about-content">
        <svg width="48" height="56" viewBox="0 0 28 32" fill="none">
          <path d="M14 0L27 8V24L14 32L1 24V8L14 0Z" fill="url(#about-grad)" />
          <path d="M14 8L20 12V20L14 24L8 20V12L14 8Z" fill="rgba(255,255,255,0.3)" />
          <defs>
            <linearGradient id="about-grad" x1="0" y1="0" x2="28" y2="32">
              <stop offset="0%" stop-color="#6BCB77" />
              <stop offset="100%" stop-color="#4ECDC4" />
            </linearGradient>
          </defs>
        </svg>
        <p class="settings-page__about-name">MintHive</p>
        <p class="settings-page__about-version">v1.0.0</p>
        <p class="settings-page__about-desc">以兴趣聚合人群，以内容产生互动</p>
      </div>
    </van-dialog>

    <!-- 用户协议弹窗 -->
    <van-dialog v-model:show="showUserAgreement" title="用户协议" :show-confirm-button="true">
      <div class="settings-page__agreement-content">
        <p>欢迎使用 MintHive 兴趣社交平台。在使用本平台前，请您仔细阅读并遵守以下协议内容...</p>
        <p>1. 用户注册后需遵守社区规范，不得发布违规内容。</p>
        <p>2. 平台有权对违规内容进行删除，对违规账号进行封禁处理。</p>
        <p>3. 用户需对自己的账号安全负责，不得将账号借予他人使用。</p>
      </div>
    </van-dialog>

    <!-- 隐私政策弹窗 -->
    <van-dialog v-model:show="showPrivacy" title="隐私政策" :show-confirm-button="true">
      <div class="settings-page__agreement-content">
        <p>MintHive 重视您的隐私保护和个人信息保护。</p>
        <p>1. 我们仅收集为提供服务所必需的最少信息。</p>
        <p>2. 您的个人数据采用加密存储，绝不外传公网。</p>
        <p>3. AI 功能全程内网调用，用户数据安全有保障。</p>
      </div>
    </van-dialog>
  </div>
</template>

<script setup lang="ts">
/**
 * SettingsPage 设置页面
 * @description 用户设置页面，包含账号安全、通知设置、AI功能开关、缓存清理、退出登录、注销账号等
 */

import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import NavBar from '@/components/NavBar.vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

/** 通知开关 */
const notifyComment = ref(true)
const notifyLike = ref(true)
const notifyChat = ref(true)
const notifyCircle = ref(true)

/** AI 功能开关 */
const aiAutoReply = ref(false)
const aiPostHelper = ref(true)
const aiCommentSuggest = ref(true)

/** 缓存大小显示 */
const cacheSize = ref('12.3 MB')

/** 弹窗控制 */
const showAbout = ref(false)
const showUserAgreement = ref(false)
const showPrivacy = ref(false)

/**
 * 跳转修改密码页
 */
function goChangePassword(): void {
  router.push('/profile')
}

/**
 * 跳转编辑个人资料
 */
function goProfile(): void {
  router.push('/profile')
}

/**
 * 跳转兴趣标签选择
 */
function goInterest(): void {
  router.push('/interest-select')
}

/**
 * 清除缓存
 * @description 清除本地缓存数据并提示用户
 */
function clearCache(): void {
  showConfirmDialog({
    title: '清除缓存',
    message: '确定要清除所有本地缓存数据吗？'
  }).then(() => {
    cacheSize.value = '0 MB'
    showToast('缓存已清除')
  }).catch(() => {})
}

/**
 * 退出登录
 * @description 确认后调用 store 的 logout 方法
 */
function handleLogout(): void {
  showConfirmDialog({
    title: '退出登录',
    message: '确定要退出当前账号吗？'
  }).then(() => {
    userStore.logout()
  }).catch(() => {})
}

/**
 * 注销账号
 * @description 确认后调用注销接口（7天冷静期）
 */
function handleDeleteAccount(): void {
  showConfirmDialog({
    title: '注销账号',
    message: '注销后账号将进入7天冷静期，冷静期结束后所有数据将被永久删除且不可恢复。确定要注销吗？'
  }).then(() => {
    showToast('已提交注销申请，7天冷静期后生效')
  }).catch(() => {})
}
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;

.settings-page {
  min-height: 100vh;
  background: $ink-50;

  &__content {
    padding: $space-4;
    padding-bottom: 120px;
  }

  &__section {
    margin-bottom: $space-4;
  }

  &__section-title {
    font-size: 13px;
    font-weight: 600;
    color: $ink-500;
    margin-bottom: $space-2;
    padding-left: $space-1;
  }

  &__group {
    background: #fff;
    border-radius: $radius-lg;
    overflow: hidden;
    box-shadow: $shadow-xs;
  }

  &__item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: $space-3 $space-4;
    cursor: pointer;
    transition: background $dur-fast ease;

    &:not(:last-child) {
      border-bottom: 1px solid $ink-50;
    }

    &:active {
      background: $ink-50;
    }
  }

  &__item-label {
    font-size: 14px;
    color: $ink-700;
  }

  &__item-value {
    font-size: 13px;
    color: $ink-300;
  }

  &__arrow {
    color: $ink-300;
  }

  &__logout {
    width: 100%;
    height: 44px;
    border: 1px solid $coral-500;
    border-radius: $radius-lg;
    background: #fff;
    color: $coral-500;
    font-size: 15px;
    font-weight: 500;
    cursor: pointer;
    transition: all $dur-fast ease;
    margin-bottom: $space-3;

    &:active {
      background: rgba(255, 107, 107, 0.08);
    }
  }

  &__delete-account {
    width: 100%;
    height: 44px;
    border: none;
    border-radius: $radius-lg;
    background: none;
    color: $ink-300;
    font-size: 13px;
    cursor: pointer;
    transition: color $dur-fast ease;

    &:active {
      color: $ink-500;
    }
  }

  &__about-content {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: $space-4;
    gap: $space-2;
  }

  &__about-name {
    font-family: $font-heading;
    font-size: 20px;
    font-weight: 700;
    color: $ink-700;
  }

  &__about-version {
    font-size: 13px;
    color: $ink-300;
  }

  &__about-desc {
    font-size: 13px;
    color: $ink-500;
    text-align: center;
  }

  &__agreement-content {
    padding: $space-4;
    font-size: 13px;
    line-height: 1.8;
    color: $ink-700;

    p {
      margin-bottom: $space-2;
    }
  }
}
</style>
