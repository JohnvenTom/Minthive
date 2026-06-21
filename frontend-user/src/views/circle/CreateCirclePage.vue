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
        </div        >
        <div v-else class="category-tags-wrap">
          <div class="category-tags" ref="categoryTagsRef">
            <div
              class="category-indicator"
              :style="indicatorStyle"
            ></div>
            <button
              v-for="(cat, idx) in categories"
              :key="cat"
              :class="['category-tag', { active: form.category === cat }]"
              @click="selectCategory(cat, idx)"
            >
              {{ cat }}
            </button>
          </div>
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
        <div class="avatar-upload-wrap">
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
            <button v-if="form.avatar && !avatarUploading" class="remove-avatar-btn" @click.stop="removeAvatar">
              <svg width="10" height="10" viewBox="0 0 24 24" fill="none">
                <path d="M18 6L6 18M6 6l12 12" stroke="currentColor" stroke-width="3" stroke-linecap="round"/>
              </svg>
            </button>
          </div>
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
          <span class="section-hint">建议尺寸 1200×400</span>
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import { compressImage } from '@/utils/compress'
import { createCircle, getCategories } from '@/api/circle'
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
  category: '',
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

// ---------- 聚焦状态 ----------
/** 名称输入框聚焦状态 */
const nameFocused = ref(false)
/** 简介文本域聚焦状态 */
const introFocused = ref(false)

// ---------- 分类相关 ----------
/** 分类列表数据 */
const categories = ref<string[]>([])
/** 分类标签容器引用（用于计算指示器位置） */
const categoryTagsRef = ref<HTMLElement | null>(null)

/**
 * 分类指示器样式
 * @description 动态计算背景滑块的位置和宽度，实现跟随选中标签滑动效果
 */
const indicatorStyle = computed(() => {
  if (!categoryTagsRef.value || !form.category) return {}
  const tags = categoryTagsRef.value.querySelectorAll('.category-tag')
  let targetTag: Element | null = null
  tags.forEach(tag => {
    if ((tag as HTMLElement).textContent?.trim() === form.category) {
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
         !!form.category &&
         !!form.avatar
})

// ---------- 分类选择 ----------

/**
 * 选择圈子分类
 * @param {string} category - 选中的分类名称
 * @param {number} _idx - 分类索引（预留用于动画）
 * @description 更新选中分类，清除该字段的校验错误
 */
function selectCategory(category: string, _idx: number): void {
  form.category = category
  errors.category = ''
}

// ---------- 图片上传通用方法 ----------

/**
 * 压缩并上传图片文件到服务器
 * @param {File} file - 原始图片文件
 * @param {Function} onProgress - 上传进度回调（可选）
 * @returns {Promise<string>} 上传成功后返回的图片 URL
 * @throws 上传失败时抛出异常
 * @description 先调用 compressImage 压缩图片，再调用 uploadImage API 上传至 MinIO
 */
async function compressAndUpload(file: File, onProgress?: (pct: number) => void): Promise<string> {
  // 压缩图片
  const compressed = await compressImage(file)
  onProgress?.(30)

  // 上传到 MinIO
  const res = await uploadImage(compressed)
  onProgress?.(100)

  // 解析后端返回的 URL
  const url = typeof res.data === 'string' ? res.data : res.data?.url || ''
  if (!url) {
    throw new Error('上传成功但未返回URL')
  }
  return url
}

// ---------- 头像上传 ----------

/**
 * 触发头像文件选择对话框
 */
function triggerAvatarInput(): void {
  avatarInputRef.value?.click()
}

/**
 * 头像文件选择变更处理
 * @param {Event} event - 文件选择事件对象
 * @returns {Promise<void>}
 * @description 校验文件类型和大小后，压缩并上传头像，更新表单数据和预览
 */
async function onAvatarChange(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  // 文件类型校验
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    showToast('仅支持 JPG/PNG/WebP 格式')
    return
  }

  // 文件大小限制（最大 5MB）
  if (file.size > 5 * 1024 * 1024) {
    showToast('图片大小不能超过 5MB')
    return
  }

  avatarUploading.value = true
  errors.avatar = ''

  try {
    const url = await compressAndUpload(file, (pct) => {
      // 进度回调，可用于扩展 UI 反馈
    })
    form.avatar = url
    showToast('头像上传成功')
  } catch (err) {
    console.error('[CreateCircle] 头像上传失败:', err)
    showToast('头像上传失败，请重试')
  } finally {
    avatarUploading.value = false
    // 重置 input 以允许重复选择同一文件
    if (avatarInputRef.value) {
      avatarInputRef.value.value = ''
    }
  }
}

/**
 * 移除已上传的头像
 * @description 清空头像 URL 和本地预览
 */
function removeAvatar(): void {
  form.avatar = ''
}

// ---------- 封面图上传 ----------

/**
 * 触发封面图文件选择对话框
 */
function triggerBannerInput(): void {
  bannerInputRef.value?.click()
}

/**
 * 封面图文件选择变更处理
 * @param {Event} event - 文件选择事件对象
 * @returns {Promise<void>}
 * @description 校验文件类型和大小后，压缩并上传封面图，更新表单数据和预览
 */
async function onBannerChange(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  // 文件类型校验
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    showToast('仅支持 JPG/PNG/WebP 格式')
    return
  }

  // 文件大小限制（最大 10MB）
  if (file.size > 10 * 1024 * 1024) {
    showToast('图片大小不能超过 10MB')
    return
  }

  bannerUploading.value = true

  try {
    const url = await compressAndUpload(file)
    form.banner = url
    showToast('封面上传成功')
  } catch (err) {
    console.error('[CreateCircle] 封面上传失败:', err)
    showToast('封面上传失败，请重试')
  } finally {
    bannerUploading.value = false
    if (bannerInputRef.value) {
      bannerInputRef.value.value = ''
    }
  }
}

/**
 * 移除已上传的封面图
 * @description 清空封面图 URL 和本地预览
 */
function removeBanner(): void {
  form.banner = ''
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
  if (!form.category) {
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
 * @description 先执行表单校验，校验通过后调用 createCircle API，成功后展示粒子动画并返回上一页
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
    await createCircle({
      name: form.name.trim(),
      category: form.category,
      intro: form.intro.trim(),
      avatar: form.avatar,
      banner: form.banner || undefined,
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
    categories.value = ['技术', '生活', '娱乐', '学习', '运动', '美食', '旅行', '游戏', '音乐', '其他']
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
  flex-wrap: wrap;
  gap: 8px;
  position: relative;
  padding: 4px;
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

// ---------- Avatar Upload（头像上传 - 圆形） ----------
.avatar-upload-wrap {
  margin-top: 4px;
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

.remove-avatar-btn,
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
  aspect-ratio: 3 / 1;
  max-height: 180px;
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
</style>
