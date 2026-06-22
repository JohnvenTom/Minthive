<template>
  <div class="create-circle-page">
    <!-- 顶部导航 -->
    <header class="create-header">
      <button class="back-btn" @click="goBack">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
          <path d="M15 18L9 12L15 6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </button>
      <h2 class="header-title">创建圈子</h2>
      <div style="width: 36px"></div>
    </header>

    <!-- 表单主体 -->
    <main class="create-main">
      <!-- 圈子名称 -->
      <section class="form-section" :style="{ animationDelay: '0ms' }">
        <div class="float-input-wrap" :class="{ focused: nameFocused, filled: form.name }">
          <input
            ref="nameInputRef"
            v-model="form.name"
            type="text"
            class="float-input"
            maxlength="20"
            placeholder=" "
            @focus="nameFocused = true"
            @blur="nameFocused = false"
          />
          <label class="float-label">圈子名称</label>
          <span class="input-hint">{{ form.name.length }}/20</span>
          <div v-if="errors.name" class="input-error">{{ errors.name }}</div>
        </div>
      </section>

      <!-- 圈子分类 -->
      <section class="form-section" :style="{ animationDelay: '80ms' }">
        <div class="section-label-row">
          <span class="section-label">圈子分类</span>
          <span class="section-required">*</span>
        </div>
        <div v-if="categoriesLoading" class="loading-skeleton">
          <div v-for="i in 4" :key="i" class="skeleton-tag"></div>
        </div>
        <div v-else class="category-tags-wrap">
          <div class="category-tags" ref="categoryTagsRef">
            <div
              class="category-indicator"
              :style="indicatorStyle"
            ></div>
            <button
              v-for="(cat, idx) in categories"
              :key="'preset-' + cat.id"
              :data-cat-id="cat.id"
              :class="['category-tag', { active: form.categoryId === cat.id && !isCustomCategory }]"
              @click="selectPresetCategory(cat, idx)"
            >
              {{ cat.name }}
            </button>
            <!-- 自定义分类输入 -->
            <button
              :class="['category-tag', 'category-tag--custom', { active: isCustomCategory }]"
              @click="focusCustomInput"
            >
              <template v-if="!isCustomCategory">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none">
                  <path d="M12 5v14M5 12h14" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"/>
                </svg>
                自定义
              </template>
              <span v-else>自定义</span>
            </button>
          </div>
          <!-- 自定义输入框（选中自定义时展开） -->
          <Transition name="custom-input-slide">
            <div v-if="showCustomInput" class="custom-category-row">
              <input
                ref="customCategoryRef"
                v-model="customCategoryText"
                type="text"
                class="custom-category-input"
                placeholder="输入自定义分类名称..."
                maxlength="10"
                @blur="onCustomBlur"
                @keyup.enter="confirmCustomCategory"
              />
              <button
                v-if="customCategoryText.trim()"
                class="custom-confirm-btn"
                @click="confirmCustomCategory"
              >
                确定
              </button>
            </div>
          </Transition>
          <div v-if="errors.category" class="input-error">{{ errors.category }}</div>
        </div>
      </section>

      <!-- 圈子简介 -->
      <section class="form-section" :style="{ animationDelay: '160ms' }">
        <div class="float-textarea-wrap" :class="{ focused: introFocused, filled: form.intro }">
          <textarea
            ref="introTextareaRef"
            v-model="form.intro"
            class="float-textarea"
            maxlength="200"
            placeholder=" "
            rows="4"
            @focus="introFocused = true"
            @blur="introFocused = false"
          ></textarea>
          <label class="float-label">圈子简介</label>
          <span class="textarea-hint">{{ form.intro.length }}/200</span>
          <div v-if="errors.intro" class="input-error">{{ errors.intro }}</div>
        </div>
      </section>

      <!-- 头像上传 -->
      <section class="form-section" :style="{ animationDelay: '240ms' }">
        <div class="section-label-row">
          <span class="section-label">圈子头像</span>
          <span class="section-required">*</span>
          <span class="section-hint">圆形头像</span>
        </div>
        <div class="avatar-upload-wrap" :class="{ 'has-avatar': !!form.avatar }">
          <div
            :class="['avatar-preview', { uploading: avatarUploading, uploaded: !!form.avatar }]"
            @click="triggerAvatarInput"
          >
            <div v-if="!form.avatar && !avatarUploading" class="avatar-placeholder">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none">
                <path d="M12 5v14M5 12h14" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
              </svg>
              <span>上传头像</span>
            </div>
            <img v-if="form.avatar && !avatarUploading" :src="form.avatar" alt="头像预览" class="avatar-img" />
            <div v-if="avatarUploading" class="avatar-uploading-overlay">
              <div class="pulse-ring"></div>
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" class="spin-icon">
                <path d="M12 2v4m0 12v4M4.93 4.93l2.83 2.83m8.48 8.48l2.83 2.83M2 12h4m12 0h4M4.93 19.07l2.83-2.83m8.48-8.48l2.83-2.83" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>
            </div>
          </div>
          <!-- remove 按钮放在外层容器，避免被圆形 overflow:hidden 裁剪 -->
          <button v-if="form.avatar && !avatarUploading" class="remove-avatar-btn" @click.stop="removeAvatar">
            <svg width="10" height="10" viewBox="0 0 24 24" fill="none">
              <path d="M18 6L6 18M6 6l12 12" stroke="currentColor" stroke-width="3" stroke-linecap="round"/>
            </svg>
          </button>
          <div v-if="errors.avatar" class="input-error">{{ errors.avatar }}</div>
        </div>
        <input
          ref="avatarInputRef"
          type="file"
          accept="image/jpeg,image/png,image/webp"
          class="hidden-input"
          @change="onAvatarChange"
        />
      </section>

      <!-- 封面图上传 -->
      <section class="form-section" :style="{ animationDelay: '320ms' }">
        <div class="section-label-row">
          <span class="section-label">封面图片</span>
          <span class="section-hint">建议尺寸 1600×900</span>
        </div>
        <div class="banner-upload-wrap">
          <div
            :class="['banner-preview', { uploading: bannerUploading, uploaded: !!form.banner }]"
            @click="triggerBannerInput"
          >
            <div v-if="!form.banner && !bannerUploading" class="banner-placeholder">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none">
                <path d="M12 5v14M5 12h14" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
              </svg>
              <span>上传封面</span>
            </div>
            <img v-if="form.banner && !bannerUploading" :src="form.banner" alt="封面预览" class="banner-img" />
            <div v-if="bannerUploading" class="banner-uploading-overlay">
              <div class="pulse-ring"></div>
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" class="spin-icon">
                <path d="M12 2v4m0 12v4M4.93 4.93l2.83 2.83m8.48 8.48l2.83 2.83M2 12h4m12 0h4M4.93 19.07l2.83-2.83m8.48-8.48l2.83-2.83" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>
            </div>
            <button v-if="form.banner && !bannerUploading" class="remove-banner-btn" @click.stop="removeBanner">
              <svg width="10" height="10" viewBox="0 0 24 24" fill="none">
                <path d="M18 6L6 18M6 6l12 12" stroke="currentColor" stroke-width="3" stroke-linecap="round"/>
              </svg>
            </button>
          </div>
        </div>
        <input
          ref="bannerInputRef"
          type="file"
          accept="image/jpeg,image/png,image/webp"
          class="hidden-input"
          @change="onBannerChange"
        />
      </section>
    </main>

    <!-- 底部操作栏 -->
    <footer class="create-footer">
      <button class="cancel-btn" @click="goBack">取消</button>
      <button
        ref="submitBtnRef"
        :class="['submit-btn', { disabled: !canSubmit || submitting }]"
        :disabled="!canSubmit || submitting"
        @click="onSubmit"
      >
        <span class="btn-content">
          <LoadingSpinner v-if="submitting" :size="16" />
          <span v-else>创建圈子</span>
        </span>
        <span class="btn-ripple" ref="rippleRef"></span>
      </button>
    </footer>

    <!-- 成功庆祝动画容器 -->
    <Transition name="fade">
      <div v-if="showSuccessParticles" class="success-particles-container">
        <div
          v-for="i in 12"
          :key="i"
          class="hex-particle"
          :style="getHexParticleStyle(i)"
        ></div>
      </div>
    </Transition>

    <!-- 图片裁剪弹窗 -->
    <Teleport to="body">
      <Transition name="crop-fade">
        <div v-if="cropVisible" class="crop-modal" @click.self="cancelCrop">
          <div class="crop-dialog">
            <!-- 标题栏 -->
            <div class="crop-header">
              <button class="crop-btn crop-btn-cancel" @click="cancelCrop">取消</button>
              <span class="crop-title">{{ cropType === 'avatar' ? '裁剪头像' : '裁剪封面' }}</span>
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
                :can-scale="false"
                :fixed="cropType === 'avatar'"
                :fixed-number="cropType === 'avatar' ? [1, 1] : [16, 9]"
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
            <p class="crop-hint">{{ cropType === 'avatar' ? '拖动或缩放选择头像区域' : '拖动或缩放选择封面区域' }}</p>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import { VueCropper } from 'vue-cropper'
import 'vue-cropper/dist/index.css'
import { createCircle, getCategories } from '@/api/circle'
import type { CircleCategory } from '@/types'
import { uploadImage } from '@/api/file'

// ---------- 路由实例 ----------
const router = useRouter()

// ---------- 表单数据 ----------
/**
 * 创建圈子表单数据
 * @description 包含圈子名称、分类、简介、头像URL、封面图URL
 */
const form = reactive({
  name: '',
  categoryId: null as number | null,
  categoryName: '',
  intro: '',
  avatar: '',
  banner: ''
})

// ---------- 表单校验错误信息 ----------
/**
 * 表单字段校验错误信息
 * @description 每个字段对应一条错误提示，为空表示校验通过
 */
const errors = reactive({
  name: '',
  category: '',
  intro: '',
  avatar: ''
})

// ---------- 状态变量 ----------
/** 是否正在提交 */
const submitting = ref(false)
/** 分类列表是否加载中 */
const categoriesLoading = ref(false)
/** 头像是否上传中 */
const avatarUploading = ref(false)
/** 封面图是否上传中 */
const bannerUploading = ref(false)
/** 是否显示成功粒子动画 */
const showSuccessParticles = ref(false)

// ---------- 裁剪相关状态（vue-cropper） ----------
/** 裁剪弹窗是否可见 */
const cropVisible = ref(false)
/** 裁剪图片 URL */
const cropImageUrl = ref('')
/** 当前裁剪类型：头像或封面 */
const cropType = ref<'avatar' | 'banner'>('avatar')
/** 裁剪确认中状态 */
const cropConfirming = ref(false)
/** vue-cropper 组件引用 */
const cropperRef = ref<InstanceType<typeof VueCropper> | null>(null)
/** 待裁剪的原始 File 对象 */
const pendingFile = ref<File | null>(null)

// ---------- 本地缓存的文件（提交时才上传） ----------
/** 头像裁剪后的 File 对象 */
const avatarFile = ref<File | null>(null)
/** 封面裁剪后的 File 对象 */
const bannerFile = ref<File | null>(null)

// ---------- 聚焦状态 ----------
/** 名称输入框聚焦状态 */
const nameFocused = ref(false)
/** 简介文本域聚焦状态 */
const introFocused = ref(false)

// ---------- 分类相关 ----------
/** 分类列表数据 */
const categories = ref<CircleCategory[]>([])
/** 分类标签容器引用（用于计算指示器位置） */
const categoryTagsRef = ref<HTMLElement | null>(null)
/** 自定义分类输入框引用 */
const customCategoryRef = ref<HTMLInputElement | null>(null)
/** 自定义分类输入文本 */
const customCategoryText = ref('')
/** 是否显示自定义输入框 */
const showCustomInput = ref(false)
/** 是否当前选中了自定义分类 */
const isCustomCategory = computed(() => {
  return showCustomInput.value && !!customCategoryText.value.trim()
})

/**
 * 分类指示器样式
 * @description 动态计算背景滑块的位置和宽度，实现跟随选中标签滑动效果
 */
const indicatorStyle = computed(() => {
  if (!categoryTagsRef.value || !form.categoryId) return {}
  const tags = categoryTagsRef.value.querySelectorAll('.category-tag')
  let targetTag: Element | null = null
  tags.forEach(tag => {
    const catId = (tag as HTMLElement).dataset?.catId
    if (catId && Number(catId) === form.categoryId) {
      targetTag = tag
    }
  })
  if (!targetTag) return {}
  const el = targetTag as HTMLElement
  return {
    width: `${el.offsetWidth}px`,
    transform: `translateX(${el.offsetLeft}px)`
  }
})

// ---------- DOM 引用 ----------
/** 名称输入框引用 */
const nameInputRef = ref<HTMLInputElement | null>(null)
/** 简介文本域引用 */
const introTextareaRef = ref<HTMLTextAreaElement | null>(null)
/** 头像文件输入框引用 */
const avatarInputRef = ref<HTMLInputElement | null>(null)
/** 封面图文件输入框引用 */
const bannerInputRef = ref<HTMLInputElement | null>(null)
/** 提交按钮引用（用于涟漪效果定位） */
const submitBtnRef = ref<HTMLButtonElement | null>(null)
/** 涟漪元素引用 */
const rippleRef = ref<HTMLSpanElement | null>(null)

// ---------- 计算属性 ----------

/**
 * 判断表单是否可以提交
 * @returns {boolean} 所有必填字段均填写且无校验错误时返回 true
 * @description 校验规则：名称2-20字、简介10-200字、分类已选、头像已上传
 */
const canSubmit = computed(() => {
  const nameLen = form.name.trim().length
  const introLen = form.intro.trim().length
  return nameLen >= 2 && nameLen <= 20 &&
         introLen >= 10 && introLen <= 200 &&
         (!!form.categoryId || !!form.categoryName.trim()) &&
         !!avatarFile.value
})

// ---------- 分类选择 ----------

/**
 * 选择预设圈子分类
 * @param {CircleCategory} cat - 选中的分类对象
 * @param {number} _idx - 分类索引（预留用于动画）
 */
function selectPresetCategory(cat: CircleCategory, _idx: number): void {
  form.categoryId = cat.id
  form.categoryName = ''
  errors.category = ''
  // 切换到预设分类时关闭自定义输入
  showCustomInput.value = false
  customCategoryText.value = ''
}

/**
 * 聚焦自定义分类输入框
 * @description 点击"自定义"标签时展开输入框并自动聚焦
 */
function focusCustomInput(): void {
  showCustomInput.value = true
  // 清除之前选中的预设分类
  if (!customCategoryText.value.trim()) {
    form.categoryId = null
    form.categoryName = ''
  }
  nextTick(() => {
    customCategoryRef.value?.focus()
  })
}

/**
 * 确认自定义分类
 * @description 用户输入完成后点击确定或按回车，将自定义文本设为当前选中分类
 */
function confirmCustomCategory(): void {
  const text = customCategoryText.value.trim()
  if (text) {
    form.categoryName = text
    form.categoryId = null
    errors.category = ''
  }
}

/**
 * 自定义输入框失焦处理
 * @description 失焦时如果已有内容则确认，否则保持展开状态供用户继续输入
 */
function onCustomBlur(): void {
  if (customCategoryText.value.trim()) {
    confirmCustomCategory()
  }
}

// ---------- 头像选择（打开裁剪弹窗） ----------

/**
 * 触发头像文件选择对话框
 */
function triggerAvatarInput(): void {
  avatarInputRef.value?.click()
}

/**
 * 头像文件选择 → 打开裁剪弹窗
 */
async function onAvatarChange(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    showToast('仅支持 JPG/PNG/WebP 格式')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    showToast('图片大小不能超过 5MB')
    return
  }

  pendingFile.value = file
  cropType.value = 'avatar'
  cropImageUrl.value = URL.createObjectURL(file)
  cropVisible.value = true

  if (avatarInputRef.value) avatarInputRef.value.value = ''
}

/** 移除已选择的头像 */
function removeAvatar(): void {
  avatarFile.value = null
  form.avatar = ''
}

// ---------- 封面图选择（打开裁剪弹窗） ----------

/**
 * 触发封面图文件选择对话框
 */
function triggerBannerInput(): void {
  bannerInputRef.value?.click()
}

/**
 * 封面文件选择 → 打开裁剪弹窗
 */
async function onBannerChange(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    showToast('仅支持 JPG/PNG/WebP 格式')
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    showToast('图片大小不能超过 10MB')
    return
  }

  pendingFile.value = file
  cropType.value = 'banner'
  cropImageUrl.value = URL.createObjectURL(file)
  cropVisible.value = true

  if (bannerInputRef.value) bannerInputRef.value.value = ''
}

/** 移除已选择的封面 */
function removeBanner(): void {
  bannerFile.value = null
  form.banner = ''
}

// ========== 裁剪弹窗逻辑（vue-cropper） ==========

/** ESC 键关闭裁剪弹窗 */
function onCropKeydown(e: KeyboardEvent): void {
  if (e.key === 'Escape' && cropVisible.value) cancelCrop()
}

/** 监听裁剪弹窗的开关，管理 ESC 键监听 */
watch(cropVisible, (val) => {
  if (val) {
    window.addEventListener('keydown', onCropKeydown)
  } else {
    window.removeEventListener('keydown', onCropKeydown)
  }
})

/** 取消裁剪 */
function cancelCrop(): void {
  closeCropModal()
}

/**
 * 确认裁剪 — 获取裁剪结果 → 本地缓存 File 对象（不立即上传）
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

    // 2. 将 Blob 转为 File 对象，按类型暂存
    const prefix = cropType.value === 'avatar' ? 'avatar' : 'banner'
    const cropFile = new File([cropBlob], `${prefix}_${Date.now()}.png`, { type: 'image/png' })

    // 3. 用 blob URL 做本地预览，缓存 File 待提交时上传
    const previewUrl = URL.createObjectURL(cropFile)

    if (cropType.value === 'avatar') {
      avatarFile.value = cropFile
      form.avatar = previewUrl
      errors.avatar = ''
    } else {
      bannerFile.value = cropFile
      form.banner = previewUrl
    }

    closeCropModal()
  } catch (err) {
    console.error('[CreateCircle] 裁剪失败:', err)
    showToast('裁剪失败，请重试')
  } finally {
    cropConfirming.value = false
  }
}

/**
 * 关闭裁剪弹窗并释放资源
 * @description 确保关闭后页面可正常滚动（清除所有可能的滚动锁定）
 */
function closeCropModal(): void {
  if (cropImageUrl.value) {
    URL.revokeObjectURL(cropImageUrl.value)
    cropImageUrl.value = ''
  }
  cropVisible.value = false
  pendingFile.value = null
  // 强制恢复页面滚动（防止任何残留的锁定状态）
  document.body.style.overflow = ''
  document.documentElement.style.overflow = ''
}

// ---------- 表单校验 ----------

/**
 * 执行完整的表单校验
 * @returns {boolean} 全部通过返回 true，否则返回 false
 * @description 依次校验名称、分类、简介、头像四个必填字段，将错误信息写入 errors 对象
 */
function validateForm(): boolean {
  let valid = true

  // 名称校验：必填 2-20 字
  const nameTrimmed = form.name.trim()
  if (!nameTrimmed) {
    errors.name = '请输入圈子名称'
    valid = false
  } else if (nameTrimmed.length < 2) {
    errors.name = '名称至少需要 2 个字符'
    valid = false
  } else if (nameTrimmed.length > 20) {
    errors.name = '名称不能超过 20 个字符'
    valid = false
  } else {
    errors.name = ''
  }

  // 分类校验：必选
  if (!form.categoryId && !form.categoryName.trim()) {
    errors.category = '请选择圈子分类'
    valid = false
  } else {
    errors.category = ''
  }

  // 简介校验：必填 10-200 字
  const introTrimmed = form.intro.trim()
  if (!introTrimmed) {
    errors.intro = '请输入圈子简介'
    valid = false
  } else if (introTrimmed.length < 10) {
    errors.intro = '简介至少需要 10 个字符'
    valid = false
  } else if (introTrimmed.length > 200) {
    errors.intro = '简介不能超过 200 个字符'
    valid = false
  } else {
    errors.intro = ''
  }

  // 头像校验：必传
  if (!form.avatar) {
    errors.avatar = '请上传圈子头像'
    valid = false
  } else {
    errors.avatar = ''
  }

  return valid
}

// ---------- 提交 ----------

/**
 * 提交创建圈子表单
 * @returns {Promise<void>}
 * @description 先校验表单，再上传图片到 MinIO，最后调用创建圈子 API
 */
async function onSubmit(): Promise<void> {
  if (!canSubmit.value || submitting.value) return

  // 先做一次完整校验
  if (!validateForm()) {
    showToast('请完善表单信息')
    return
  }

  // 触发提交按钮涟漪效果
  triggerRipple()

  submitting.value = true
  try {
    // 1. 上传头像到 MinIO（裁剪后的文件）
    let avatarUrl = ''
    if (avatarFile.value) {
      const res = await uploadImage(avatarFile.value)
      avatarUrl = typeof res.data === 'string' ? res.data : res.data?.url || ''
      if (!avatarUrl) throw new Error('头像上传失败')
    }

    // 2. 上传封面到 MinIO（如果有）
    let bannerUrl = ''
    if (bannerFile.value) {
      const res = await uploadImage(bannerFile.value)
      bannerUrl = typeof res.data === 'string' ? res.data : res.data?.url || ''
      if (!bannerUrl) throw new Error('封面上传失败')
    }

    // 3. 创建圈子
    await createCircle({
      name: form.name.trim(),
      ...(form.categoryId ? { categoryId: form.categoryId } : { categoryName: form.categoryName.trim() }),
      intro: form.intro.trim(),
      avatar: avatarUrl,
      banner: bannerUrl || undefined,
      type: 'public'
    })

    // 展示成功粒子动画
    showSuccessParticles.value = true
    showToast('圈子创建成功')

    // 2.5 秒后关闭动画并返回
    setTimeout(() => {
      showSuccessParticles.value = false
      router.back()
    }, 2500)
  } catch (err) {
    console.error('[CreateCircle] 创建圈子失败:', err)
    showToast('创建失败，请重试')
  } finally {
    submitting.value = false
  }
}

/**
 * 触发按钮点击涟漪扩散效果
 * @description 在点击位置生成一个圆形涟漪动画
 */
function triggerRipple(): void {
  if (!rippleRef.value || !submitBtnRef.value) return
  const btn = submitBtnRef.value
  const ripple = rippleRef.value
  const rect = btn.getBoundingClientRect()
  const size = Math.max(rect.width, rect.height)
  const x = rect.width / 2
  const y = rect.height / 2

  ripple.style.width = ripple.style.height = `${size}px`
  ripple.style.left = `${x - size / 2}px`
  ripple.style.top = `${y - size / 2}px`
  ripple.classList.add('active')

  setTimeout(() => {
    ripple.classList.remove('active')
  }, 600)
}

// ---------- 加载分类列表 ----------

/**
 * 从后端获取圈子分类列表
 * @returns {Promise<void>}
 * @description 页面挂载时调用 getCategories API 获取可用分类，失败时使用默认分类兜底
 */
async function fetchCategories(): Promise<void> {
  categoriesLoading.value = true
  try {
    const res = await getCategories()
    const list = res.data
    categories.value = Array.isArray(list) && list.length > 0 ? list : []
  } catch (err) {
    // 静默降级：使用默认分类列表，不弹 toast 干扰用户
    console.warn('[CreateCircle] 获取分类失败，使用默认列表:', err)
    categories.value = [
      { id: 1, name: '技术', sort: 1, status: 1 },
      { id: 2, name: '生活', sort: 2, status: 1 },
      { id: 3, name: '娱乐', sort: 3, status: 1 },
      { id: 4, name: '学习', sort: 4, status: 1 },
      { id: 5, name: '运动', sort: 5, status: 1 },
      { id: 6, name: '美食', sort: 6, status: 1 },
      { id: 7, name: '旅行', sort: 7, status: 1 },
      { id: 8, name: '游戏', sort: 8, status: 1 },
      { id: 9, name: '音乐', sort: 9, status: 1 },
      { id: 10, name: '其他', sort: 10, status: 1 }
    ]
  } finally {
    categoriesLoading.value = false
  }
}

// ---------- 导航 ----------

/**
 * 返回上一页
 * @description 如果表单有内容则弹出确认框，否则直接返回
 */
function goBack(): void {
  const hasContent = form.name.trim() || form.intro.trim() || form.avatar || form.banner
  if (hasContent) {
    showConfirmDialog({
      title: '确认离开',
      message: '离开后编辑内容将丢失，确定要离开吗？',
      confirmButtonText: '确认离开',
      cancelButtonText: '继续编辑'
    }).then(() => {
      router.back()
    }).catch(() => {
      // 用户取消，留在当前页面
    })
  } else {
    router.back()
  }
}

// ---------- 六边形粒子样式生成 ----------

/**
 * 生成单个六边形粒子的随机样式
 * @param {number} index - 粒子序号（1-based）
 * @returns {CSSProperties} 包含位置、颜色、延迟、动画时长等内联样式
 * @description 为每个粒子分配随机的起始角度、距离、颜色和动画参数，形成放射状爆发效果
 */
function getHexParticleStyle(index: number): Record<string, string> {
  const colors = ['#4ECDC4', '#6BCB77', '#FFB627', '#FF6B6B']
  const angle = (index - 1) * (360 / 12)
  const distance = 80 + Math.random() * 120
  const delay = Math.random() * 0.3
  const duration = 0.6 + Math.random() * 0.4
  const color = colors[index % colors.length]

  return {
    '--hex-angle': `${angle}deg`,
    '--hex-distance': `${distance}px`,
    '--hex-color': color,
    '--hex-delay': `${delay}s`,
    '--hex-duration': `${duration}s`
  } as Record<string, string>
}

// ---------- 生命周期 ----------
onMounted(async () => {
  await fetchCategories()
})

/** 组件卸载时强制清理所有残留状态（防止 vue-cropper 事件泄漏） */
onUnmounted(() => {
  // 清理裁剪弹窗资源
  if (cropImageUrl.value) URL.revokeObjectURL(cropImageUrl.value)
  // 强制恢复滚动（vue-cropper 可能残留 wheel 监听器）
  document.body.style.overflow = ''
  document.documentElement.style.overflow = ''
})
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;
@use '@/styles/mixins.scss' as *;

// ---------- 局部变量覆盖 ----------
$bg-warm: #FAF9F7;
$bg-card: #FFFFFF;
$border-subtle: #E8E5DF;
$border-hover: #D5D0C8;
$text-primary: #1A1A1A;
$text-secondary: #6B6560;
$text-tertiary: #9B958E;
$accent-mint: $mint-500;
$accent-mint-light: rgba(78, 205, 196, 0.1);
$accent-amber: $amber-500;
$radius-card: 16px;
$radius-btn: 10px;

// ---------- 页面容器 ----------
.create-circle-page {
  min-height: 100vh;
  background: $bg-warm;
  padding-bottom: 88px;
  -webkit-font-smoothing: antialiased;
  /* 确保页面自身可滚动，不依赖 body 层 */
  overflow-y: auto;
}

// ---------- Header ----------
.create-header {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  background: rgba(250, 249, 247, 0.88);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border-bottom: 1px solid $border-subtle;
}

.header-title {
  font-size: 17px;
  font-weight: 600;
  color: $text-primary;
  letter-spacing: -0.01em;
  font-family: $font-heading;
  margin: 0; /* 消除 h2 默认边距，与导航栏对齐 */
}

.back-btn {
  @include center;
  width: 36px;
  height: 36px;
  border-radius: $radius-btn;
  color: $text-secondary;
  transition: all $dur-fast ease;

  &:hover {
    background: rgba(0, 0, 0, 0.04);
    color: $text-primary;
  }
}

// ---------- Main ----------
.create-main {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

// ---------- Form Section（入场动画） ----------
.form-section {
  background: $bg-card;
  border-radius: $radius-card;
  border: 1px solid $border-subtle;
  padding: 20px;
  opacity: 0;
  transform: translateY(16px);
  animation: stagger-reveal 0.45s $ease-out forwards;
  animation-delay: inherit; /* 由 inline style 控制 */

  &:hover {
    border-color: $border-hover;
  }
}

@keyframes stagger-reveal {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

// ---------- Section Label Row ----------
.section-label-row {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 12px;
}

.section-label {
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
  letter-spacing: -0.01em;
  font-family: $font-heading;
}

.section-required {
  font-size: 13px;
  font-weight: 600;
  color: $coral-500;
}

.section-hint {
  margin-left: auto;
  font-size: 12px;
  color: $text-tertiary;
  font-weight: 400;
}

// ---------- Floating Input（浮动标签输入框） ----------
.float-input-wrap {
  position: relative;
  margin-top: 4px;

  .float-input {
    width: 100%;
    padding: 18px 14px 8px;
    font-size: 15px;
    color: $text-primary;
    background: $bg-warm;
    border: 1.5px solid $border-subtle;
    border-radius: $radius-sm;
    outline: none;
    transition: all $dur-base $ease-out;
    font-family: $font-body;

    &::placeholder {
      color: transparent;
    }

    &:focus {
      border-color: $accent-mint;
      box-shadow: 0 0 0 3px $accent-mint-light, 0 0 20px rgba(78, 205, 196, 0.15);
      background: #fff;
    }
  }

  .float-label {
    position: absolute;
    left: 14px;
    top: 50%;
    transform: translateY(-50%);
    font-size: 15px;
    color: $text-tertiary;
    pointer-events: none;
    transition: all $dur-base $ease-spring;
    font-family: $font-body;
  }

  &.focused .float-label,
  &.filled .float-label {
    top: 10px;
    transform: translateY(0) scale(0.82);
    font-size: 11px;
    color: $mint-600;
    font-weight: 600;
    letter-spacing: 0.02em;
  }

  .input-hint {
    position: absolute;
    right: 14px;
    top: 50%;
    transform: translateY(-50%);
    font-size: 12px;
    color: $text-tertiary;
    font-variant-numeric: tabular-nums;
    font-family: $font-heading;
    opacity: 0;
    transition: opacity $dur-fast ease;
  }

  &.focused .input-hint,
  &.filled .input-hint {
    opacity: 1;
  }

  .input-error {
    margin-top: 6px;
    font-size: 12px;
    color: $coral-500;
    font-weight: 500;
    animation: shake 0.35s $ease-spring;
  }
}

// ---------- Floating Textarea（浮动标签文本域） ----------
.float-textarea-wrap {
  position: relative;
  margin-top: 4px;

  .float-textarea {
    width: 100%;
    min-height: 120px;
    padding: 18px 14px 8px;
    font-size: 15px;
    line-height: 1.7;
    color: $text-primary;
    background: $bg-warm;
    border: 1.5px solid $border-subtle;
    border-radius: $radius-sm;
    outline: none;
    resize: vertical;
    transition: all $dur-base $ease-out;
    font-family: $font-body;

    &::placeholder {
      color: transparent;
    }

    &:focus {
      border-color: $accent-mint;
      box-shadow: 0 0 0 3px $accent-mint-light, 0 0 20px rgba(78, 205, 196, 0.15);
      background: #fff;
    }
  }

  .float-label {
    position: absolute;
    left: 14px;
    top: 18px;
    font-size: 15px;
    color: $text-tertiary;
    pointer-events: none;
    transition: all $dur-base $ease-spring;
    font-family: $font-body;
  }

  &.focused .float-label,
  &.filled .float-label {
    top: 10px;
    transform: scale(0.82);
    font-size: 11px;
    color: $mint-600;
    font-weight: 600;
    letter-spacing: 0.02em;
  }

  .textarea-hint {
    position: absolute;
    right: 14px;
    bottom: 10px;
    font-size: 12px;
    color: $text-tertiary;
    font-variant-numeric: tabular-nums;
    font-family: $font-heading;
  }

  .input-error {
    margin-top: 6px;
    font-size: 12px;
    color: $coral-500;
    font-weight: 500;
    animation: shake 0.35s $ease-spring;
  }
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  20% { transform: translateX(-6px); }
  40% { transform: translateX(6px); }
  60% { transform: translateX(-4px); }
  80% { transform: translateX(4px); }
}

// ---------- Category Tags（分类标签选择器） ----------
.category-tags-wrap {
  margin-top: 4px;
}

.loading-skeleton {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.skeleton-tag {
  width: 64px;
  height: 32px;
  border-radius: $radius-pill;
  background: linear-gradient(90deg, $bg-warm 25%, darken($bg-warm, 3%) 50%, $bg-warm 75%);
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.5s infinite;
}

@keyframes skeleton-shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.category-tags {
  display: flex;
  flex-wrap: nowrap;
  gap: 8px;
  position: relative;
  padding: 4px;
  overflow-x: auto;
  scrollbar-width: none; /* Firefox 隐藏滚动条 */
  -ms-overflow-style: none; /* IE/Edge 隐藏滚动条 */
  &::-webkit-scrollbar { display: none; } /* Chrome/Safari 隐藏滚动条 */
}

.category-indicator {
  position: absolute;
  height: calc(100% - 8px);
  top: 4px;
  left: 4px;
  border-radius: $radius-pill;
  background: $accent-mint-light;
  border: 1.5px solid $accent-mint;
  transition: all 0.35s $ease-spring;
  z-index: 0;
  pointer-events: none;
}

.category-tag {
  position: relative;
  z-index: 1;
  padding: 7px 18px;
  border-radius: $radius-pill;
  font-size: 13px;
  font-weight: 500;
  color: $text-secondary;
  background: transparent;
  border: 1.5px solid transparent;
  cursor: pointer;
  transition: all $dur-fast $ease-out;
  white-space: nowrap;
  font-family: $font-heading;

  &:hover:not(.active) {
    color: $text-primary;
    background: rgba(0, 0, 0, 0.03);
  }

  &.active {
    color: $mint-700;
    font-weight: 600;
    // 点击时的弹性缩放效果由 JS 控制或 CSS active 伪类
    &:active {
      animation: spring-bounce 0.4s $ease-spring;
    }
  }
}

@keyframes spring-bounce {
  0% { transform: scale(1); }
  30% { transform: scale(0.92); }
  60% { transform: scale(1.06); }
  80% { transform: scale(0.98); }
  100% { transform: scale(1); }
}

// ---------- 自定义分类输入 ----------
.category-tag--custom {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: $mint-500 !important;
  border: 1.5px dashed $mint-300 !important;

  &:hover:not(.active) {
    background: $accent-mint-light !important;
    border-color: $mint-500 !important;
  }

  &.active {
    color: #fff !important;
    background: $grad-mint !important;
    border-color: transparent !important;
  }
}

.custom-category-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  animation: slide-down 0.25s $ease-spring both;
}

.custom-category-input {
  flex: 1;
  height: 36px;
  padding: 0 14px;
  border: 1.5px solid $border-subtle;
  border-radius: $radius-btn;
  font-size: 13px;
  color: $text-primary;
  background: #fff;
  outline: none;
  transition: all $dur-fast ease;

  &::placeholder {
    color: $text-tertiary;
  }

  &:focus {
    border-color: $accent-mint;
    box-shadow: 0 0 0 3px rgba(78, 205, 196, 0.12);
  }
}

.custom-confirm-btn {
  @include center;
  flex-shrink: 0;
  height: 36px;
  padding: 0 16px;
  border-radius: $radius-btn;
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  background: $grad-mint;
  border: none;
  cursor: pointer;
  transition: all $dur-fast ease;

  &:hover {
    opacity: 0.9;
    transform: translateY(-1px);
  }

  &:active {
    transform: scale(0.96);
  }
}

@keyframes slide-down {
  from {
    opacity: 0;
    max-height: 0;
    margin-top: 0;
  }
  to {
    opacity: 1;
    max-height: 50px;
    margin-top: 10px;
  }
}

/** 自定义输入框展开/收起过渡 */
.custom-input-slide-enter-active {
  transition: all 0.25s $ease-spring;
}
.custom-input-slide-leave-active {
  transition: all 0.15s ease;
}
.custom-input-slide-enter-from,
.custom-input-slide-leave-to {
  opacity: 0;
  max-height: 0;
  margin-top: 0;
}

// ---------- Avatar Upload（头像上传 - 圆形） ----------
.avatar-upload-wrap {
  margin-top: 4px;
  position: relative;
  display: inline-block;
}

.avatar-preview {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  position: relative;
  border: 2.5px dashed $border-hover;
  background: $bg-warm;
  @include center;
  flex-direction: column;
  gap: 6px;
  transition: all $dur-base $ease-spring;

  &:hover {
    border-color: $accent-mint;
    box-shadow: 0 0 0 4px $accent-mint-light;
  }

  &.uploading {
    border-color: $accent-mint;
    border-style: solid;
    animation: pulse-glow 1.5s infinite;
  }

  &.uploaded {
    border-style: solid;
    border-color: $ink-100;
    animation: pop-in 0.4s $ease-spring both;
  }
}

@keyframes pulse-glow {
  0%, 100% { box-shadow: 0 0 0 0 rgba(78, 205, 196, 0.3); }
  50% { box-shadow: 0 0 0 10px rgba(78, 205, 196, 0); }
}

@keyframes pop-in {
  0% { transform: scale(0.85); opacity: 0; }
  60% { transform: scale(1.05); }
  100% { transform: scale(1); opacity: 1; }
}

.avatar-placeholder {
  @include center;
  flex-direction: column;
  gap: 6px;
  color: $text-tertiary;
  font-size: 12px;
  font-family: $font-heading;
  transition: color $dur-fast ease;

  .avatar-preview:hover & {
    color: $accent-mint;
  }
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-uploading-overlay {
  @include center;
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(4px);

  .pulse-ring {
    position: absolute;
    width: 60px;
    height: 60px;
    border-radius: 50%;
    border: 2px solid $accent-mint;
    animation: pulse-ring-expand 1.5s infinite;
  }
}

@keyframes pulse-ring-expand {
  0% { transform: scale(0.6); opacity: 1; }
  100% { transform: scale(1.4); opacity: 0; }
}

.spin-icon {
  color: $accent-mint;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.remove-avatar-btn {
  /* 相对于 .avatar-upload-wrap 定位，避免被圆形 overflow:hidden 裁剪 */
  position: absolute;
  top: -4px;
  right: -4px;
  @include center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: rgba(26, 29, 46, 0.55);
  backdrop-filter: blur(6px);
  color: #fff;
  cursor: pointer;
  transition: all $dur-fast ease;
  z-index: 10;

  &:hover {
    background: $coral-500;
    transform: scale(1.1);
  }
}

.remove-banner-btn {
  position: absolute;
  top: 4px;
  right: 4px;
  @include center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: rgba(26, 29, 46, 0.55);
  backdrop-filter: blur(6px);
  color: #fff;
  cursor: pointer;
  transition: all $dur-fast ease;

  &:hover {
    background: $coral-500;
    transform: scale(1.1);
  }
}

// ---------- Banner Upload（封面图上传 - 矩形） ----------
.banner-upload-wrap {
  margin-top: 4px;
}

.banner-preview {
  width: 100%;
  max-width: 480px;
  aspect-ratio: 16 / 9;
  max-height: 240px;
  border-radius: $radius-md;
  overflow: hidden;
  cursor: pointer;
  position: relative;
  border: 2.5px dashed $border-hover;
  background: $bg-warm;
  @include center;
  flex-direction: column;
  gap: 6px;
  transition: all $dur-base $ease-spring;

  &:hover {
    border-color: $accent-mint;
    box-shadow: 0 0 0 4px $accent-mint-light;
  }

  &.uploading {
    border-color: $accent-mint;
    border-style: solid;
    animation: pulse-glow 1.5s infinite;
  }

  &.uploaded {
    border-style: solid;
    border-color: $ink-100;
    animation: pop-in 0.4s $ease-spring both;
  }
}

.banner-placeholder {
  @include center;
  flex-direction: column;
  gap: 6px;
  color: $text-tertiary;
  font-size: 12px;
  font-family: $font-heading;
  transition: color $dur-fast ease;

  .banner-preview:hover & {
    color: $accent-mint;
  }
}

.banner-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.banner-uploading-overlay {
  @include center;
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(4px);

  .pulse-ring {
    position: absolute;
    width: 60px;
    height: 60px;
    border-radius: 50%;
    border: 2px solid $accent-mint;
    animation: pulse-ring-expand 1.5s infinite;
  }
}

// ---------- 隐藏的文件输入 ----------
.hidden-input {
  display: none;
}

// ---------- Footer ----------
.create-footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  background: rgba(250, 249, 247, 0.9);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border-top: 1px solid $border-subtle;
}

.cancel-btn {
  padding: 10px 24px;
  border-radius: $radius-btn;
  font-size: 14px;
  font-weight: 500;
  color: $text-secondary;
  background: transparent;
  transition: all $dur-fast ease;
  font-family: $font-body;

  &:hover {
    background: rgba(0, 0, 0, 0.04);
    color: $text-primary;
  }
}

.submit-btn {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  padding: 10px 36px;
  border-radius: $radius-btn;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  background: $grad-mint;
  background-size: 200% 100%;
  border: none;
  cursor: pointer;
  box-shadow: 0 2px 12px rgba(78, 205, 196, 0.3);
  transition: all $dur-base $ease-out;
  font-family: $font-heading;

  &:hover:not(.disabled) {
    // 渐变流动效果
    background-position: 100% 0;
    box-shadow: 0 4px 20px rgba(78, 205, 196, 0.4);
    transform: translateY(-1px);
  }

  &:active:not(.disabled) {
    transform: scale(0.97);
  }

  &.disabled {
    opacity: 0.4;
    cursor: not-allowed;
    box-shadow: none;
  }

  .btn-content {
    position: relative;
    z-index: 2;
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .btn-ripple {
    position: absolute;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.35);
    transform: scale(0);
    pointer-events: none;
    z-index: 1;

    &.active {
      animation: ripple-expand 0.6s $ease-out forwards;
    }
  }
}

@keyframes ripple-expand {
  to {
    transform: scale(2.5);
    opacity: 0;
  }
}

// ---------- 成功粒子动画 ----------
.success-particles-container {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 9999;
  @include center;
}

.hex-particle {
  position: absolute;
  width: 14px;
  height: 16px;
  clip-path: polygon(50% 0%, 100% 25%, 100% 75%, 50% 100%, 0% 75%, 0% 25%);
  background: var(--hex-color, $mint-500);
  opacity: 0;
  animation: hex-burst var(--hex-duration, 0.8s) var(--ease-spring) var(--hex-delay, 0s) forwards;

  // 根据角度和距离计算终点位置
  --start-x: 0;
  --start-y: 0;
  --end-x: calc(cos(var(--hex-angle, 0deg)) * var(--hex-distance, 100px));
  --end-y: calc(sin(var(--hex-angle, 0deg)) * var(--hex-distance, 100px));
}

@keyframes hex-burst {
  0% {
    opacity: 1;
    transform: translate(-50%, -50%) scale(0.3) rotate(0deg);
  }
  50% {
    opacity: 1;
    transform: translate(
      calc(-50% + var(--end-x) * 0.6),
      calc(-50% + var(--end-y) * 0.6)
    ) scale(1) rotate(180deg);
  }
  100% {
    opacity: 0;
    transform: translate(
      calc(-50% + var(--end-x)),
      calc(-50% + var(--end-y))
    ) scale(0.5) rotate(360deg);
  }
}

// ---------- Transition ----------
.fade-enter-active {
  animation: fade-in 0.3s ease both;
}
.fade-leave-active {
  animation: fade-in 0.3s ease reverse both;
}

@keyframes fade-in {
  from { opacity: 0; }
  to { opacity: 1; }
}

// ---------- Mobile ----------
@include mobile {
  .create-main {
    padding: 16px;
    gap: 12px;
  }

  .form-section {
    padding: 16px;
  }

  .header-title {
    font-size: 16px;
  }

  .avatar-preview {
    width: 80px;
    height: 80px;
  }

  .banner-preview {
    max-height: 140px;
  }

  .category-tag {
    padding: 6px 14px;
    font-size: 12px;
  }

  .submit-btn {
    padding: 10px 28px;
    font-size: 13px;
  }

  .cancel-btn {
    padding: 10px 18px;
    font-size: 13px;
  }
}

// ========== 裁剪弹窗样式（vue-cropper） ==========
// 与 EditProfilePage 保持一致的写法
.crop-modal {
  position: fixed;
  inset: 0;
  z-index: 10000;
  background: rgba(0, 0, 0, 0.65);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: $space-4;
}

.crop-dialog {
  width: 100%;
  max-width: 480px;
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
  font-family: $font-heading;
}

.crop-btn {
  padding: $space-1 $space-3;
  border: none;
  border-radius: $radius-md;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all $dur-fast ease;
  font-family: $font-heading;

  &-cancel {
    background: transparent;
    color: $ink-300;

    &:hover { color: #fff; }
    &:active { color: #fff; }
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
  height: 360px;
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

// ========== 裁剪弹窗过渡动画 ==========
.crop-fade-enter-active,
.crop-fade-leave-active {
  transition: opacity 0.25s ease;
}
.crop-fade-enter-from,
.crop-fade-leave-to {
  opacity: 0;
}
</style>
