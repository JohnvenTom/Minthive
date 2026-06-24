<template>
  <div class="edit-profile-page">
    <NavBar title="编辑资料" :show-back="true" @back="router.back()" />

    <div class="edit-profile-content">
      <!-- 头像区域 -->
      <section class="section avatar-section">
        <div class="avatar-wrapper" @click="triggerAvatarInput">
          <img :src="form.avatar || defaultAvatar" class="avatar-img" alt="头像" />
          <div class="avatar-overlay">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
              <path d="M23 19a2 2 0 01-2 2H3a2 2 0 01-2-2V8a2 2 0 012-2h4l2-3h6l2 3h4a2 2 0 012 2z"
                stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <circle cx="12" cy="13" r="4" stroke="#fff" stroke-width="2"/>
            </svg>
            <span class="avatar-tip">更换</span>
          </div>
          <!-- 上传进度 -->
          <div v-if="avatarUploading" class="avatar-progress">
            <div class="avatar-progress-bar" :style="{ width: avatarProgress + '%' }" />
          </div>
        </div>
        <p class="avatar-hint">点击更换头像</p>
        <input ref="avatarInputRef" type="file" accept="image/jpeg,image/png,image/webp,image/gif"
               class="hidden-input" @change="handleAvatarChange" />
      </section>

      <!-- 昵称 -->
      <section class="section form-section">
        <label class="field-label">昵称</label>
        <div class="field-input-wrap">
          <input v-model="form.nickname" class="field-input" placeholder="输入你的昵称"
                 maxlength="16" @blur="trimNickname" />
          <span class="field-count">{{ form.nickname.length }}/16</span>
        </div>
      </section>

      <!-- 个人简介 -->
      <section class="section form-section">
        <label class="field-label">个人简介</label>
        <div class="field-textarea-wrap">
          <textarea v-model="form.bio" class="field-textarea" placeholder="介绍一下自己吧..."
                    maxlength="100" rows="4" @blur="trimBio" />
          <span class="field-count textarea-count">{{ form.bio.length }}/100</span>
        </div>
      </section>

      <!-- 兴趣标签入口 -->
      <section class="section link-section" @click="goInterestSelect">
        <div class="link-left">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" class="link-icon">
            <path d="M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2Z"
                  stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <span class="link-text">兴趣标签</span>
        </div>
        <div class="link-right">
          <span v-if="interestPreview" class="link-value">{{ interestPreview }}</span>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" class="arrow-icon">
            <path d="M9 6l6 6-6 6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
      </section>

      <!-- 保存按钮 -->
      <button class="save-btn" :class="{ saving }" :disabled="saving || !hasChanges" @click="onSave">
        {{ saving ? '保存中...' : '保存修改' }}
      </button>
    </div>

    <!-- 头像裁剪弹窗 -->
    <Teleport to="body">
      <Transition name="crop-fade">
        <div v-if="cropVisible" class="crop-modal" @click.self="cancelCrop">
          <div class="crop-dialog">
            <!-- 标题栏 -->
            <div class="crop-header">
              <button class="crop-btn crop-btn-cancel" @click="cancelCrop">取消</button>
              <span class="crop-title">裁剪头像</span>
              <button class="crop-btn crop-btn-confirm" :disabled="cropConfirming" @click="confirmCrop">
                {{ cropConfirming ? '处理中...' : '确定' }}
              </button>
            </div>
            <!-- 裁剪区域 -->
            <div class="crop-body">
              <VueCropper
                ref="cropperRef"
                :img="cropImageUrl"
                :output-size="1"
                :output-type="'png'"
                :info="true"
                :full="false"
                :can-move="true"
                :can-scale="true"
                :fixed="true"
                :fixed-number="[1, 1]"
                :fixed-box="false"
                :original="false"
                :auto-crop="true"
                :auto-crop-width="200"
                :auto-crop-height="200"
                :center-box="true"
                :high="true"
                mode="contain"
              />
            </div>
            <!-- 提示文字 -->
            <p class="crop-hint">拖动或缩放选择头像区域</p>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
/**
 * EditProfilePage 编辑个人资料页面
 * @description 支持更换头像（含裁剪）、修改昵称、编辑个人简介、跳转兴趣标签选择，
 *   头像上传流程：选图 → 裁剪(1:1) → 压缩 → 上传MinIO → 预览
 *   保存后同步更新 userStore 和后端数据
 */

import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { VueCropper } from 'vue-cropper'
import 'vue-cropper/dist/index.css'
import NavBar from '@/components/NavBar.vue'
import { useUserStore } from '@/stores/user'
import { updateProfile } from '@/api/user'
import { compressImage } from '@/utils/compress'
import { uploadImage } from '@/api/file'

const router = useRouter()
const userStore = useUserStore()

/** 默认头像 */
const defaultAvatar = 'https://api.dicebear.com/7.x/initials/svg?seed=MH&backgroundColor=4ECDC4'

/** 表单数据 */
const form = ref({
  nickname: '',
  bio: '',
  avatar: ''
})

/** 初始值（用于判断是否有变更） */
const initialValues = ref({ nickname: '', bio: '', avatar: '' })

/** 头像上传状态 */
const avatarUploading = ref(false)
const avatarProgress = ref(0)
const avatarInputRef = ref<HTMLInputElement | null>(null)

/** 裁剪相关状态 */
const cropVisible = ref(false)
const cropImageUrl = ref('')
const cropConfirming = ref(false)
const cropperRef = ref<InstanceType<typeof VueCropper> | null>(null)

/** 待裁剪的原始 File 对象 */
const pendingFile = ref<File | null>(null)

/** 裁剪后待上传的 File 对象（保存时才会上传到 MinIO） */
const pendingCropFile = ref<File | null>(null)

/** 保存中状态 */
const saving = ref(false)

/** 是否有变更 */
const hasChanges = computed(() => {
  return (
    form.value.nickname !== initialValues.value.nickname ||
    form.value.bio !== initialValues.value.bio ||
    form.value.avatar !== initialValues.value.avatar
  )
})

/** 兴趣标签预览文字 */
const interestPreview = computed(() => {
  const tags = userStore.userInfo?.interestTags
  if (!tags) return ''
  try {
    const arr = JSON.parse(tags)
    return Array.isArray(arr) ? arr.slice(0, 3).join(' / ') + (arr.length > 3 ? '...' : '') : ''
  } catch {
    return tags.length > 15 ? tags.slice(0, 15) + '...' : tags
  }
})

/**
 * 初始化表单数据
 * @description 从 userStore 加载当前用户信息到表单
 */
function initForm(): void {
  const user = userStore.userInfo
  if (!user) return

  form.value = {
    nickname: user.nickname || '',
    bio: user.bio || '',
    avatar: user.avatar || ''
  }
  initialValues.value = { ...form.value }
}

/**
 * 触发头像文件选择
 * @description 点击隐藏的 input 元素打开文件选择器
 */
function triggerAvatarInput(): void {
  avatarInputRef.value?.click()
}

/**
 * 处理头像文件选择 — 打开裁剪弹窗
 * @param {Event} event - 文件选择事件
 * @description 选择图片后不直接上传，而是打开裁剪弹窗让用户框选头像区域
 */
async function handleAvatarChange(event: Event): Promise<void> {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  // 校验文件类型
  if (!['image/jpeg', 'image/png', 'image/webp', 'image/gif'].includes(file.type)) {
    showToast('请选择 JPG / PNG / WebP / GIF 格式的图片')
    target.value = ''
    return
  }

  // 校验文件大小（5MB）
  if (file.size > 5 * 1024 * 1024) {
    showToast('图片大小不能超过 5MB')
    target.value = ''
    return
  }

  // 保存原始文件，显示裁剪弹窗
  pendingFile.value = file
  cropImageUrl.value = URL.createObjectURL(file)
  cropVisible.value = true

  // 重置 input，允许重复选同一张图
  target.value = ''
}

/**
 * 确认裁剪 — 获取裁剪结果 → 本地预览（不立即上传）
 * @description 调用 vue-cropper 的 getCropBlob 获取裁剪后的 Blob，
 *   仅用 blob URL 做本地预览，将 File 对象暂存到 pendingCropFile，
 *   等用户点击"保存"时再上传到 MinIO
 */
async function confirmCrop(): Promise<void> {
  if (!cropperRef.value || !pendingFile.value) return

  cropConfirming.value = true
  try {
    // 1. 获取裁剪后的 Blob（PNG 格式）
    const cropBlob: Blob = await new Promise((resolve, reject) => {
      cropperRef.value!.getCropBlob((blob: Blob | null) => {
        if (blob) resolve(blob)
        else reject(new Error('裁剪失败'))
      })
    })

    // 2. 将 Blob 转为 File 对象，暂存待上传
    const cropFile = new File([cropBlob], `avatar_${Date.now()}.png`, { type: 'image/png' })
    pendingCropFile.value = cropFile

    // 3. 本地预览（用 blob URL 显示，不传到服务端）
    form.value.avatar = URL.createObjectURL(cropFile)

    // 关闭裁剪弹窗
    closeCropModal()
  } catch {
    showToast('头像处理失败')
  } finally {
    cropConfirming.value = false
  }
}

/**
 * 取消裁剪
 * @description 关闭裁剪弹窗，释放资源，恢复之前的头像状态
 */
function cancelCrop(): void {
  closeCropModal()
}

/**
 * 关闭裁剪弹窗并释放资源
 * @description 释放 createObjectURL 创建的临时 URL，重置裁剪相关状态
 */
function closeCropModal(): void {
  if (cropImageUrl.value) {
    URL.revokeObjectURL(cropImageUrl.value)
    cropImageUrl.value = ''
  }
  cropVisible.value = false
  pendingFile.value = null
}

/**
 * 昵称输入框失焦时去除首尾空格
 */
function trimNickname(): void {
  form.value.nickname = form.value.nickname.trim()
}

/**
 * 简介输入框失焦时去除首尾空格
 */
function trimBio(): void {
  form.value.bio = form.value.bio.trim()
}

/**
 * 跳转兴趣标签选择页
 * @description 导航到兴趣选择页面，返回后重新加载用户信息
 */
function goInterestSelect(): void {
  router.push('/interest-select')
}

/**
 * 保存个人资料
 * @description 如果有待上传的裁剪头像（pendingCropFile），先压缩并上传到 MinIO，
 *   然后将昵称、简介、头像 URL 一次性通过 updateProfile 接口提交到后端，
 *   成功后更新本地 store 并提示用户
 */
async function onSave(): Promise<void> {
  if (!hasChanges.value) return

  // 校验
  if (!form.value.nickname.trim()) {
    showToast('昵称不能为空')
    return
  }

  saving.value = true
  try {
    let avatarUrl = form.value.avatar || ''

    // 如果有裁剪后待上传的头像文件，先上传到 MinIO
    if (pendingCropFile.value) {
      avatarUploading.value = true
      avatarProgress.value = 30

      // 压缩图片
      const compressed = await compressImage(pendingCropFile.value)
      avatarProgress.value = 60

      // 上传到 MinIO，获取远程 URL
      const res = await uploadImage(compressed)
      avatarProgress.value = 100

      // 用 MinIO 远程 URL 替换 blob 预览地址
      avatarUrl = res.data
      form.value.avatar = avatarUrl

      // 释放 blob 资源
      if (form.value.avatar.startsWith('blob:')) {
        URL.revokeObjectURL(form.value.avatar)
      }
      pendingCropFile.value = null
      avatarUploading.value = false
      setTimeout(() => { avatarProgress.value = 0 }, 300)
    }

    // 判断头像是否为新上传的远程 URL（非 blob URL 且非默认头像）
    const isNewAvatar = avatarUrl !== initialValues.value.avatar &&
                        !avatarUrl.startsWith('blob:') &&
                        !avatarUrl.includes('dicebear.com')

    // 一次性更新：昵称 + 简介 + 头像（如有变更）
    await updateProfile({
      nickname: form.value.nickname,
      bio: form.value.bio,
      ...(isNewAvatar ? { avatar: avatarUrl } : {})
    })

    // 刷新 store 数据
    await userStore.fetchUser()

    // 更新初始值
    initialValues.value = { ...form.value }

    showToast('资料已更新')

    // 稍后自动返回
    setTimeout(() => router.back(), 800)
  } catch (e: any) {
    showToast(e?.message || '保存失败')
  } finally {
    saving.value = false
    avatarUploading.value = false
  }
}

onMounted(initForm)
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;
@use '@/styles/mixins.scss' as *;

.edit-profile-page {
  min-height: 100vh;
  background: $ink-50;
}

.edit-profile-content {
  padding: $space-4;
  padding-bottom: 120px;
}

// ---------- 区块通用 ----------
.section {
  margin-bottom: $space-5;
}

// ---------- 头像区域 ----------
.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: $space-8 0 $space-4;
}

.avatar-wrapper {
  position: relative;
  width: 96px;
  height: 96px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  border: 3px solid rgba(78, 205, 196, 0.25);
  transition: border-color $dur-base ease;

  &:hover {
    border-color: $mint-500;

    .avatar-overlay {
      opacity: 1;
    }
  }
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  background: rgba(26, 29, 46, 0.55);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  opacity: 0;
  transition: opacity $dur-fast ease;
}

.avatar-tip {
  font-size: 11px;
  color: #fff;
  font-weight: 500;
}

.avatar-progress {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: rgba(26, 29, 46, 0.3);
}

.avatar-progress-bar {
  height: 100%;
  background: $grad-mint;
  transition: width 0.2s linear;
}

.avatar-hint {
  margin-top: $space-2;
  font-size: 12px;
  color: $ink-300;
}

.hidden-input {
  display: none;
}

// ---------- 表单字段 ----------
.form-section {
  background: #fff;
  border-radius: $radius-lg;
  padding: $space-4;
  box-shadow: $shadow-xs;
}

.field-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: $ink-500;
  margin-bottom: $space-2;
}

.field-input-wrap {
  position: relative;
}

.field-input {
  width: 100%;
  height: 44px;
  border: 1px solid $ink-100;
  border-radius: $radius-md;
  padding: 0 $space-10 0 $space-3;
  font-size: 15px;
  color: $ink-700;
  outline: none;
  transition: border-color $dur-fast ease;

  &::placeholder {
    color: $ink-300;
  }

  &:focus {
    border-color: $mint-500;
  }
}

.field-textarea-wrap {
  position: relative;
}

.field-textarea {
  width: 100%;
  min-height: 88px;
  border: 1px solid $ink-100;
  border-radius: $radius-md;
  padding: $space-3;
  padding-right: $space-10;
  font-size: 14px;
  color: $ink-700;
  line-height: 1.6;
  outline: none;
  resize: vertical;
  transition: border-color $dur-fast ease;
  font-family: inherit;

  &::placeholder {
    color: $ink-300;
  }

  &:focus {
    border-color: $mint-500;
  }
}

.field-count {
  position: absolute;
  right: $space-3;
  font-size: 11px;
  color: $ink-300;
  top: 50%;
  transform: translateY(-50%);
  pointer-events: none;

  &.textarea-count {
    bottom: $space-2;
    top: auto;
    transform: none;
  }
}

// ---------- 链接式入口 ----------
.link-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-radius: $radius-lg;
  padding: $space-4;
  box-shadow: $shadow-xs;
  cursor: pointer;
  transition: background $dur-fast ease;

  &:active {
    background: $ink-50;
  }
}

.link-left {
  display: flex;
  align-items: center;
  gap: $space-3;
}

.link-icon {
  color: $amber-500;
  flex-shrink: 0;
}

.link-text {
  font-size: 15px;
  color: $ink-700;
  font-weight: 500;
}

.link-right {
  display: flex;
  align-items: center;
  gap: $space-2;
}

.link-value {
  font-size: 13px;
  color: $ink-300;
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.arrow-icon {
  color: $ink-300;
  flex-shrink: 0;
}

// ---------- 保存按钮 ----------
.save-btn {
  width: 100%;
  height: 48px;
  border: none;
  border-radius: $radius-lg;
  background: $grad-mint;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all $dur-base ease;
  box-shadow: $shadow-glow-mint;

  &:hover:not(:disabled) {
    transform: translateY(-1px);
    box-shadow: 0 8px 28px rgba(78, 205, 196, 0.35);
  }

  &:active:not(:disabled) {
    transform: translateY(0);
  }

  &:disabled {
    opacity: 0.45;
    cursor: not-allowed;
    box-shadow: none;
  }

  &.saving {
    opacity: 0.75;
  }
}

// ========== 头像裁剪弹窗 ==========
.crop-modal {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: rgba(0, 0, 0, 0.65);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: $space-4;
}

.crop-dialog {
  width: 100%;
  max-width: 420px;
  max-height: 90vh;
  background: linear-gradient(160deg, #2A3D3C 0%, #2D3946 100%);
  border: 1px solid rgba(78, 205, 196, 0.18);
  border-radius: $radius-xl;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
}

.crop-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $space-3 $space-4;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  flex-shrink: 0;
}

.crop-title {
  font-size: 15px;
  font-weight: 600;
  color: #fff;
}

.crop-btn {
  padding: $space-1 $space-3;
  border: none;
  border-radius: $radius-md;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all $dur-fast ease;

  &-cancel {
    background: transparent;
    color: $ink-300;

    &:active {
      color: #fff;
    }
  }

  &-confirm {
    background: $grad-mint;
    color: #fff;
    min-width: 56px;

    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    &:active:not(:disabled) {
      opacity: 0.85;
    }
  }
}

.crop-body {
  position: relative;
  width: 100%;
  height: 320px;
  background: #000;
  overflow: hidden;

  /* 覆盖 vue-cropper 默认样式以适配暗色主题 */
  :deep(.cropper-view-box) {
    outline: 2px dashed $mint-500;
    outline-color: rgba(78, 205, 196, 0.6);
  }

  :deep(.cropper-face) {
    background: rgba(78, 205, 196, 0.08);
  }

  :deep(.cropper-line) {
    background-color: $mint-500;
  }

  :deep(.cropper-point) {
    background-color: $mint-500;
    width: 10px;
    height: 10px;
  }

  :deep(.cropper-dash) {
    border-color: rgba(78, 205, 196, 0.5);
  }

  :deep(.cropper-modal) {
    background: rgba(0, 0, 0, 0.55);
  }
}

.crop-hint {
  text-align: center;
  font-size: 12px;
  color: $ink-300;
  padding: $space-2 0 $space-3;
  flex-shrink: 0;
}

// ========== 过渡动画 ==========
.crop-fade-enter-from,
.crop-fade-leave-to {
  opacity: 0;
}

/**
 * 移动端响应式样式
 * @description 包含安全区域适配，为页面根容器添加顶部安全区域内边距
 */
@include mobile {
  .edit-profile-page {
    padding-top: env(safe-area-inset-top);
  }
}
</style>
