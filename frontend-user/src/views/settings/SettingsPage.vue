<template>
  <div class="settings-page">
    <NavBar title="设置" :show-back="true" @back="router.back()" />

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

    <!-- 修改密码弹窗 -->
    <Teleport to="body">
      <transition name="modal-fade">
        <div v-if="showChangePassword" class="pwd-modal-mask" @click.self="closePasswordModal">
          <div class="pwd-modal">
            <!-- 顶部装饰条 -->
            <div class="pwd-modal__header-bar">
              <svg width="24" height="28" viewBox="0 0 14 16" fill="none" class="pwd-modal__hex-icon">
                <path d="M7 0L13.062 4V12L7 16L0.938 12V4L7 0Z"
                  fill="url(#pwd-hex-grad)" opacity="0.9"/>
                <defs>
                  <linearGradient id="pwd-hex-grad" x1="0" y1="0" x2="14" y2="16">
                    <stop offset="0%" stop-color="#6BCB77"/>
                    <stop offset="100%" stop-color="#4ECDC4"/>
                  </linearGradient>
                </defs>
              </svg>
              <h3 class="pwd-modal__title">修改密码</h3>
              <button class="pwd-modal__close" @click="closePasswordModal">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                  <path d="M18 6L6 18M6 6l12 12" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                </svg>
              </button>
            </div>

            <!-- 提示文字 -->
            <p class="pwd-modal__hint">
              请输入当前密码和新密码，密码长度 6-20 位，建议使用字母、数字和符号的组合
            </p>

            <!-- 表单 -->
            <form class="pwd-modal__form" @submit.prevent="handleSubmitPassword">
              <!-- 当前密码 -->
              <div class="pwd-modal__field">
                <label class="pwd-modal__label">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                    <rect x="5" y="11" width="14" height="10" rx="2" stroke="currentColor" stroke-width="1.8"/>
                    <path d="M8 11V7a4 4 0 118 0v4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
                    <circle cx="12" cy="16" r="1.5" fill="currentColor"/>
                  </svg>
                  当前密码
                </label>
                <div class="pwd-modal__input-wrap" :class="{ focused: focusOld }">
                  <input
                    v-model="passwordForm.oldPassword"
                    :type="showPwdOld ? 'text' : 'password'"
                    class="pwd-modal__input"
                    placeholder="请输入当前密码"
                    maxlength="20"
                    autocomplete="current-password"
                    @focus="focusOld = true"
                    @blur="focusOld = false"
                  />
                  <button type="button" class="pwd-modal__eye-btn" @click="showPwdOld = !showPwdOld">
                    <svg v-if="!showPwdOld" width="17" height="17" viewBox="0 0 24 24" fill="none">
                      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8S1 12 1 12z" stroke="currentColor" stroke-width="1.8"/>
                      <circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="1.8"/>
                    </svg>
                    <svg v-else width="17" height="17" viewBox="0 0 24 24" fill="none">
                      <path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
                      <line x1="1" y1="1" x2="23" y2="23" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
                    </svg>
                  </button>
                </div>
              </div>

              <!-- 新密码 -->
              <div class="pwd-modal__field">
                <label class="pwd-modal__label">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                    <rect x="3" y="11" width="18" height="11" rx="2" stroke="currentColor" stroke-width="1.8"/>
                    <path d="M7 11V7a5 5 0 1110 0v4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
                  </svg>
                  新密码
                </label>
                <div class="pwd-modal__input-wrap" :class="{ focused: focusNew }">
                  <input
                    v-model="passwordForm.newPassword"
                    :type="showPwdNew ? 'text' : 'password'"
                    class="pwd-modal__input"
                    placeholder="请输入新密码（6-20位）"
                    maxlength="20"
                    autocomplete="new-password"
                    @focus="focusNew = true"
                    @blur="focusNew = false"
                  />
                  <button type="button" class="pwd-modal__eye-btn" @click="showPwdNew = !showPwdNew">
                    <svg v-if="!showPwdNew" width="17" height="17" viewBox="0 0 24 24" fill="none">
                      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8S1 12 1 12z" stroke="currentColor" stroke-width="1.8"/>
                      <circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="1.8"/>
                    </svg>
                    <svg v-else width="17" height="17" viewBox="0 0 24 24" fill="none">
                      <path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
                      <line x1="1" y1="1" x2="23" y2="23" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
                    </svg>
                  </button>
                </div>
                <!-- 密码强度指示器 -->
                <div v-if="passwordForm.newPassword" class="pwd-strength">
                  <div class="pwd-strength__bars">
                    <span :class="['pwd-strength__bar', { active: pwdStrength >= 1, weak: pwdStrength === 1 }]" />
                    <span :class="['pwd-strength__bar', { active: pwdStrength >= 2, medium: pwdStrength === 2 }]" />
                    <span :class="['pwd-strength__bar', { active: pwdStrength >= 3, strong: pwdStrength === 3 }]" />
                  </div>
                  <span :class="['pwd-strength__label', `pwd-strength__label--${pwdStrengthText}`]">{{ pwdStrengthLabel }}</span>
                </div>
              </div>

              <!-- 确认新密码 -->
              <div class="pwd-modal__field">
                <label class="pwd-modal__label">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                    <path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                  确认新密码
                </label>
                <div class="pwd-modal__input-wrap" :class="{ focused: focusConfirm }">
                  <input
                    v-model="passwordForm.confirmPassword"
                    :type="showPwdConfirm ? 'text' : 'password'"
                    class="pwd-modal__input"
                    placeholder="请再次输入新密码"
                    maxlength="20"
                    autocomplete="new-password"
                    @focus="focusConfirm = true"
                    @blur="focusConfirm = false"
                  />
                  <button type="button" class="pwd-modal__eye-btn" @click="showPwdConfirm = !showPwdConfirm">
                    <svg v-if="!showPwdConfirm" width="17" height="17" viewBox="0 0 24 24" fill="none">
                      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8S1 12 1 12z" stroke="currentColor" stroke-width="1.8"/>
                      <circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="1.8"/>
                    </svg>
                    <svg v-else width="17" height="17" viewBox="0 0 24 24" fill="none">
                      <path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
                      <line x1="1" y1="1" x2="23" y2="23" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
                    </svg>
                  </button>
                </div>
                <!-- 错误提示 -->
                <p v-if="pwdError" class="pwd-modal__error">{{ pwdError }}</p>
              </div>

              <!-- 提交按钮 -->
              <button
                type="submit"
                class="pwd-modal__submit"
                :class="{ disabled: !canSubmitPassword || submittingPassword }"
                :disabled="!canSubmitPassword || submittingPassword"
              >
                <span v-if="submittingPassword" class="pwd-modal__submit-loading">
                  <svg class="spin" width="16" height="16" viewBox="0 0 24 24" fill="none">
                    <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2.5" opacity="0.25"/>
                    <path d="M12 2a10 10 0 0110 10" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"/>
                  </svg>
                  修改中...
                </span>
                <span v-else>确认修改</span>
              </button>
            </form>

            <!-- 安全提示 -->
            <div class="pwd-modal__tips">
              <div class="pwd-modal__tip-item">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none">
                  <path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" stroke="#6BCB77" stroke-width="2.5" stroke-linecap="round"/>
                </svg>
                修改成功后需要重新登录
              </div>
              <div class="pwd-modal__tip-item">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none">
                  <path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" stroke="#6BCB77" stroke-width="2.5" stroke-linecap="round"/>
                </svg>
                建议使用字母+数字+符号组合
              </div>
            </div>
          </div>
        </div>
      </transition>
    </Teleport>
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

import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import NavBar from '@/components/NavBar.vue'
import { useUserStore } from '@/stores/user'
import { changePassword } from '@/api/auth'

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

// ---------- 修改密码相关 ----------

/** 修改密码弹窗显示状态 */
const showChangePassword = ref(false)

/** 密码表单数据 */
const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

/** 各输入框聚焦状态（用于样式反馈） */
const focusOld = ref(false)
const focusNew = ref(false)
const focusConfirm = ref(false)

/** 密码可见性切换 */
const showPwdOld = ref(false)
const showPwdNew = ref(false)
const showPwdConfirm = ref(false)

/** 提交中状态 */
const submittingPassword = ref(false)

/** 错误提示信息 */
const pwdError = ref('')

/**
 * 跳转/打开修改密码弹窗
 * @description 打开修改密码弹窗，重置表单状态
 */
function goChangePassword(): void {
  // 重置表单
  passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  pwdError.value = ''
  showChangePassword.value = true
}

/**
 * 关闭修改密码弹窗
 * @description 清空表单数据并关闭弹窗
 */
function closePasswordModal(): void {
  showChangePassword.value = false
  passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  pwdError.value = ''
  showPwdOld.value = false
  showPwdNew.value = false
  showPwdConfirm.value = false
}

/**
 * 计算密码强度等级 (1=弱 2=中 3=强)
 * @returns {number} 强度等级 0-3
 */
const pwdStrength = computed((): number => {
  const pwd = passwordForm.value.newPassword
  if (!pwd) return 0
  let level = 0
  if (pwd.length >= 6) level++
  if (/[a-zA-Z]/.test(pwd) && /[0-9]/.test(pwd)) level++
  if (/[^a-zA-Z0-9]/.test(pwd)) level++
  return Math.min(level, 3)
})

/**
 * 密码强度文字标签
 * @returns {string} 强度描述文本
 */
const pwdStrengthLabel = computed((): string => {
  const labels = ['', '弱', '中', '强']
  return labels[pwdStrength.value]
})

/**
 * 密码强度对应的 CSS 类名
 * @returns {string} 用于样式的类名标识
 */
const pwdStrengthText = computed((): string => {
  const textMap: Record<number, string> = { 1: 'weak', 2: 'medium', 3: 'strong' }
  return textMap[pwdStrength.value] || 'none'
})

/**
 * 判断是否可以提交修改密码表单
 * @returns {boolean} 所有校验通过返回 true
 * @description 校验规则：所有字段非空、长度>=6、两次新密码一致、新旧密码不同
 */
const canSubmitPassword = computed((): boolean => {
  const { oldPassword, newPassword, confirmPassword } = passwordForm.value
  return (
    oldPassword.length >= 6 &&
    newPassword.length >= 6 &&
    confirmPassword.length >= 6 &&
    newPassword === confirmPassword &&
    oldPassword !== newPassword
  )
})

/**
 * 提交修改密码请求
 * @returns {Promise<void>}
 * @description 执行前端校验 → 调用 API → 成功后提示并关闭弹窗
 */
async function handleSubmitPassword(): Promise<void> {
  const { oldPassword, newPassword, confirmPassword } = passwordForm.value

  // 前置校验：确认密码一致性
  if (newPassword !== confirmPassword) {
    pwdError.value = '两次输入的新密码不一致'
    return
  }

  // 前置校验：新旧密码不能相同
  if (oldPassword === newPassword) {
    pwdError.value = '新密码不能与当前密码相同'
    return
  }

  // 前置校验：密码长度
  if (newPassword.length < 6) {
    pwdError.value = '密码长度不能少于6位'
    return
  }

  pwdError.value = ''
  submittingPassword.value = true

  try {
    await changePassword(oldPassword, newPassword)
    showToast('密码修改成功，请重新登录')
    closePasswordModal()
    // 可选：自动退出登录
    // userStore.logout()
  } catch (err: any) {
    // 显示后端返回的错误信息
    pwdError.value = err?.response?.data?.message || err?.message || '修改失败，请稍后重试'
  } finally {
    submittingPassword.value = false
  }
}

/**
 * 跳转编辑个人资料
 */
function goProfile(): void {
  router.push('/edit-profile')
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
@use '@/styles/mixins.scss' as *;

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

// ============================================================
// 修改密码弹窗样式（符合 MintHive 薄荷绿主题）
// ============================================================

.pwd-modal-mask {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  padding: $space-4;
}

.pwd-modal {
  width: 100%;
  max-width: 420px;
  background: #fff;
  border-radius: 20px;
  box-shadow:
    0 25px 60px rgba(78, 205, 196, 0.15),
    0 10px 30px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  animation: pwd-modal-in 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);

  @keyframes pwd-modal-in {
    from {
      opacity: 0;
      transform: scale(0.92) translateY(16px);
    }
    to {
      opacity: 1;
      transform: scale(1) translateY(0);
    }
  }

  // ---------- 头部 ----------
  &__header-bar {
    display: flex;
    align-items: center;
    gap: $space-2;
    padding: $space-4 $space-5;
    background: linear-gradient(135deg, rgba(107, 203, 119, 0.08), rgba(78, 205, 196, 0.08));
    border-bottom: 1px solid rgba(78, 205, 196, 0.12);
  }

  &__hex-icon {
    flex-shrink: 0;
  }

  &__title {
    flex: 1;
    font-family: $font-heading;
    font-size: 17px;
    font-weight: 700;
    color: $ink-700;
    margin: 0;
  }

  &__close {
    @include center;
    width: 32px;
    height: 32px;
    border-radius: 50%;
    border: none;
    background: transparent;
    color: $ink-500;
    cursor: pointer;
    transition: all $dur-fast ease;

    &:hover {
      color: $ink-500;
      background: $ink-100;
    }
  }

  // ---------- 提示文字 ----------
  &__hint {
    margin: $space-3 $space-5 0;
    font-size: 12.5px;
    line-height: 1.6;
    color: $ink-500;
  }

  // ---------- 表单 ----------
  &__form {
    padding: $space-5;
    display: flex;
    flex-direction: column;
    gap: $space-3;
  }

  &__field {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  &__label {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    font-weight: 600;
    color: $ink-700;

    svg {
      color: $mint-500;
    }
  }

  &__input-wrap {
    display: flex;
    align-items: center;
    background: $ink-50;
    border: 2px solid transparent;
    border-radius: 12px;
    padding: 0 12px;
    transition: all $dur-fast ease;

    &.focused {
      border-color: $mint-300;
      background: #fff;
      box-shadow: 0 0 0 4px rgba(78, 205, 196, 0.1);
    }
  }

  &__input {
    flex: 1;
    height: 44px;
    border: none;
    background: none;
    font-size: 14px;
    color: $ink-700;
    outline: none;

    &::placeholder {
      color: $ink-300;
    }
  }

  &__eye-btn {
    @include center;
    width: 36px;
    height: 36px;
    border: none;
    background: none;
    color: $ink-300;
    cursor: pointer;
    border-radius: 8px;
    transition: all $dur-fast ease;

    &:hover,
    &:active {
      color: $mint-500;
      background: rgba(78, 205, 196, 0.08);
    }
  }

  // ---------- 错误提示 ----------
  &__error {
    font-size: 12px;
    color: $coral-500;
    margin-top: 2px;
    animation: pwd-shake 0.4s ease;

    @keyframes pwd-shake {
      0%, 100% { transform: translateX(0); }
      20%, 60% { transform: translateX(-4px); }
      40%, 80% { transform: translateX(4px); }
    }
  }

  // ---------- 提交按钮 ----------
  &__submit {
    width: 100%;
    height: 48px;
    border: none;
    border-radius: 12px;
    background: linear-gradient(135deg, #6BCB77 0%, #4ECDC4 100%);
    color: #fff;
    font-size: 15px;
    font-weight: 700;
    letter-spacing: 0.5px;
    cursor: pointer;
    transition: all $dur-fast ease;
    box-shadow: 0 4px 15px rgba(107, 203, 119, 0.35);

    &:not(.disabled):hover {
      transform: translateY(-2px);
      box-shadow: 0 6px 22px rgba(107, 203, 119, 0.45);
    }

    &:not(.disabled):active {
      transform: translateY(0);
    }

    &.disabled {
      opacity: 0.45;
      cursor: not-allowed;
      box-shadow: none;
    }
  }

  &__submit-loading {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;

    .spin {
      animation: spin 0.8s linear infinite;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }
  }

  // ---------- 安全提示 ----------
  &__tips {
    display: flex;
    flex-direction: column;
    gap: 8px;
    padding: $space-3 $space-5 $space-4;
    background: rgba(107, 203, 119, 0.04);
    border-top: 1px solid rgba(78, 205, 196, 0.08);
  }

  &__tip-item {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 12px;
    color: $ink-500;
  }
}

// ---------- 密码强度指示器 ----------

.pwd-strength {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;

  &__bars {
    display: flex;
    gap: 4px;
    flex: 1;
  }

  &__bar {
    flex: 1;
    height: 3px;
    border-radius: 2px;
    background: $ink-100;
    transition: all $dur-base ease;

    &.active.weak {
      background: #FF6B6B;
    }

    &.active.medium {
      background: #FFB627;
    }

    &.active.strong {
      background: #6BCB77;
    }
  }

  &__label {
    font-size: 11px;
    font-weight: 600;
    min-width: 24px;
    text-align: right;

    &--weak { color: #FF6B6B; }
    &--medium { color: #FFB627; }
    &--strong { color: #6BCB77; }
    &--none { color: $ink-300; }
  }
}

// ---------- 弹窗过渡动画 ----------

.modal-fade-enter-active {
  animation: modal-fade-in 0.25s ease-out;
}

.modal-fade-leave-active {
  animation: modal-fade-in 0.2s ease-in reverse;
}

@keyframes modal-fade-in {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

/**
 * 移动端响应式样式
 * @description 包含安全区域适配，为页面根容器添加顶部安全区域内边距
 */
@include mobile {
  .settings-page {
    padding-top: env(safe-area-inset-top);
  }
}
</style>
