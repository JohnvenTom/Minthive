<template>
  <div class="create-post-page">
    <!-- 蜂巢装饰背景 -->
    <div class="hex-bg" />

    <!-- 顶部导航 -->
    <header class="create-header">
      <button class="back-btn" @click="goBack">
        <van-icon name="arrow-left" size="20" />
      </button>
      <h2 class="header-title">发布动态</h2>
      <div class="header-right">
        <button class="draft-btn" @click="onSaveDraft">
          <van-icon name="description" size="16" />
          草稿
        </button>
      </div>
    </header>

    <!-- 表单主体 -->
    <main class="create-main">
      <!-- 文案输入区 -->
      <section class="content-section glass-card">
        <textarea
          ref="textareaRef"
          v-model="form.content"
          class="content-textarea"
          placeholder="分享你的想法..."
          maxlength="2000"
          rows="6"
          @input="onContentInput"
        />
        <div class="textarea-footer">
          <span :class="['char-count', { warning: charCount > 1800 }]">
            {{ charCount }}/2000
          </span>
        </div>
      </section>

      <!-- AI 辅助工具栏 -->
      <section class="ai-toolbar">
        <button class="ai-tool-btn" @click="onAiGenerate">
          <van-icon name="magic-o" />
          <span>AI 生成文案</span>
        </button>
        <button class="ai-tool-btn" :disabled="!form.content.trim()" @click="onAiPolish">
          <van-icon name="brush-o" />
          <span>AI 润色</span>
        </button>
        <button class="ai-tool-btn" :disabled="!form.content.trim()" @click="onAiCheck">
          <van-icon name="shield-o" />
          <span>AI 预检</span>
        </button>
      </section>

      <!-- 图片上传区 -->
      <section class="upload-section glass-card">
        <h3 class="section-label">图片</h3>
        <ImageUploader
          v-model="imageList"
          :max-count="9"
          @upload="onImageUpload"
          @delete="onImageDelete"
        />
      </section>

      <!-- 视频上传区 -->
      <section class="upload-section glass-card">
        <h3 class="section-label">视频 <span class="sub-label">≤60秒</span></h3>
        <div v-if="!videoUrl" class="video-upload-area" @click="onVideoSelect">
          <van-icon name="video-o" size="32" class="upload-icon" />
          <span class="upload-text">点击上传视频</span>
        </div>
        <div v-else class="video-preview">
          <video :src="videoUrl" class="preview-video" controls />
          <button class="remove-video-btn" @click="onRemoveVideo">
            <van-icon name="cross" />
          </button>
        </div>
        <input
          ref="videoInputRef"
          type="file"
          accept="video/*"
          class="hidden-input"
          @change="onVideoChange"
        />
      </section>

      <!-- 话题标签 -->
      <section class="topic-section glass-card">
        <h3 class="section-label">话题标签</h3>
        <div class="topic-list">
          <span v-for="(topic, idx) in form.topics" :key="idx" class="topic-chip">
            #{{ topic }}
            <van-icon name="cross" class="chip-remove" @click="removeTopic(idx)" />
          </span>
          <div class="topic-input-wrap">
            <input
              v-model="topicInput"
              class="topic-input"
              placeholder="输入话题，回车添加"
              @keyup.enter="addTopic"
            />
          </div>
        </div>
      </section>

      <!-- 可见范围 -->
      <section class="option-section glass-card">
        <h3 class="section-label">可见范围</h3>
        <div class="visibility-options">
          <button
            v-for="opt in visibilityOptions"
            :key="opt.value"
            :class="['visibility-btn', { active: form.visibility === opt.value }]"
            @click="form.visibility = opt.value"
          >
            <van-icon :name="opt.icon" />
            <span>{{ opt.label }}</span>
          </button>
        </div>
      </section>

      <!-- 选择圈子 -->
      <section class="option-section glass-card">
        <h3 class="section-label">选择圈子</h3>
        <div class="circle-select-wrap">
          <select v-model="form.circleId" class="circle-select">
            <option :value="undefined">不选择圈子</option>
            <option v-for="circle in circleList" :key="circle.id" :value="circle.id">
              {{ circle.name }}
            </option>
          </select>
          <van-icon name="arrow-down" class="select-arrow" />
        </div>
      </section>
    </main>

    <!-- 底部操作栏 -->
    <footer class="create-footer glass-card">
      <button class="cancel-btn" @click="goBack">取消</button>
      <button
        class="publish-btn"
        :disabled="!canPublish || publishing"
        @click="onPublish"
      >
        <LoadingSpinner v-if="publishing" :size="18" />
        <span v-else>发布</span>
      </button>
    </footer>

    <!-- AI 文案生成面板 -->
    <Transition name="slide-up">
      <div v-if="showAiPanel" class="ai-generate-panel glass-card">
        <div class="ai-panel-header">
          <span class="ai-panel-title">
            <van-icon name="magic-o" class="ai-icon" />
            AI 文案生成
          </span>
          <van-icon name="cross" class="close-icon" @click="showAiPanel = false" />
        </div>

        <!-- 关键词输入 -->
        <div class="keyword-input-area">
          <input
            v-model="aiKeyword"
            class="keyword-input"
            placeholder="输入关键词，如：旅行、美食、日常..."
            @keyup.enter="onAiGenerateSubmit"
          />
          <button
            class="generate-btn"
            :disabled="!aiKeyword.trim() || aiGenerating"
            @click="onAiGenerateSubmit"
          >
            <LoadingSpinner v-if="aiGenerating" :size="16" />
            <span v-else>生成</span>
          </button>
        </div>

        <!-- 生成结果 -->
        <div v-if="aiDrafts.length > 0" class="ai-drafts">
          <div
            v-for="(draft, idx) in aiDrafts"
            :key="idx"
            :class="['ai-draft-card', { selected: selectedDraftIdx === idx }]"
            @click="selectDraft(idx)"
          >
            <div class="draft-header">
              <span class="draft-style-badge">{{ draft.styleName }}</span>
              <van-icon
                v-if="selectedDraftIdx === idx"
                name="success"
                class="draft-check"
              />
            </div>
            <p class="draft-content">{{ draft.content }}</p>
            <div v-if="draft.topics?.length" class="draft-topics">
              <span v-for="t in draft.topics" :key="t" class="draft-topic">#{{ t }}</span>
            </div>
          </div>
        </div>

        <!-- 使用选中文案 -->
        <button
          v-if="selectedDraftIdx !== null"
          class="use-draft-btn"
          @click="applyDraft"
        >
          使用此文案
        </button>
      </div>
    </Transition>

    <!-- AI 预检结果弹窗 -->
    <van-dialog
      v-model:show="showCheckResult"
      title="AI 内容预检结果"
      :show-confirm-button="true"
      confirm-button-text="知道了"
    >
      <div class="check-result-content">
        <div v-if="!checkResult.violated" class="check-pass">
          <van-icon name="passed" size="48" class="pass-icon" />
          <p>内容合规，可以放心发布！</p>
        </div>
        <div v-else class="check-fail">
          <van-icon name="warning-o" size="48" class="fail-icon" />
          <p>检测到可能违规的内容</p>
          <p class="fail-detail">
            类型：{{ checkResult.type || '未知' }}
            <br />
            等级：{{ checkResult.level || '未知' }}
          </p>
        </div>
      </div>
    </van-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import ImageUploader from '@/components/ImageUploader.vue'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import { useUserStore } from '@/stores/user'
import { createPost, saveDraft } from '@/api/post'
import { aiGeneratePost, aiPolishPost, aiCheckContent } from '@/api/ai'
import { uploadImage, uploadVideo } from '@/api/file'
import { getCircles } from '@/api/circle'
import type { Circle, AiPostDraft } from '@/types'

// ---------- 路由与Store ----------
const router = useRouter()
const userStore = useUserStore()

// ---------- 表单数据 ----------
const form = ref({
  content: '',
  images: [] as string[],
  video: '',
  topics: [] as string[],
  visibility: 'public' as 'public' | 'fans' | 'self',
  circleId: undefined as number | undefined
})

const imageList = ref<string[]>([])
const videoUrl = ref('')
const topicInput = ref('')
const publishing = ref(false)

// ---------- 圈子列表 ----------
const circleList = ref<Circle[]>([])

// ---------- 可见范围选项 ----------
const visibilityOptions = [
  { label: '公开', value: 'public' as const, icon: 'eye-o' },
  { label: '仅粉丝', value: 'fans' as const, icon: 'friends-o' },
  { label: '仅自己', value: 'self' as const, icon: 'lock' }
]

// ---------- 计算属性 ----------
/** 字数统计 */
const charCount = computed(() => form.value.content.length)

/** 是否可发布 */
const canPublish = computed(() => form.value.content.trim().length > 0)

// ---------- DOM引用 ----------
const textareaRef = ref<HTMLTextAreaElement | null>(null)
const videoInputRef = ref<HTMLInputElement | null>(null)

// ---------- AI 文案生成 ----------
const showAiPanel = ref(false)
const aiKeyword = ref('')
const aiGenerating = ref(false)
const aiDrafts = ref<AiPostDraft[]>([])
const selectedDraftIdx = ref<number | null>(null)

// ---------- AI 预检 ----------
const showCheckResult = ref(false)
const checkResult = ref<{ violated: boolean; type?: string; level?: string }>({
  violated: false
})

// ---------- 内容输入 ----------

/**
 * 内容输入回调
 * @description 更新字数统计
 */
function onContentInput(): void {
  // 字数由v-model自动更新，charCount计算属性自动响应
}

// ---------- 话题标签 ----------

/**
 * 添加话题标签
 * @description 将输入框中的话题添加到列表，去重处理
 */
function addTopic(): void {
  const topic = topicInput.value.trim()
  if (!topic) return
  if (form.value.topics.includes(topic)) {
    showToast('话题已存在')
    return
  }
  if (form.value.topics.length >= 5) {
    showToast('最多添加5个话题')
    return
  }
  form.value.topics.push(topic)
  topicInput.value = ''
}

/**
 * 移除话题标签
 * @param {number} idx - 要移除的话题索引
 */
function removeTopic(idx: number): void {
  form.value.topics.splice(idx, 1)
}

// ---------- 图片上传 ----------

/**
 * 图片上传回调
 * @param {string} url - 上传成功后的图片URL
 * @description 将上传成功的图片URL添加到表单
 */
function onImageUpload(url: string): void {
  form.value.images.push(url)
}

/**
 * 图片删除回调
 * @param {number} idx - 被删除图片的索引
 */
function onImageDelete(idx: number): void {
  form.value.images.splice(idx, 1)
}

// ---------- 视频上传 ----------

/**
 * 触发视频文件选择
 */
function onVideoSelect(): void {
  videoInputRef.value?.click()
}

/**
 * 视频文件选择变更
 * @param {Event} event - 文件选择事件
 * @returns {Promise<void>}
 * @description 校验视频时长≤60秒后上传
 */
async function onVideoChange(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  // 校验视频时长
  const duration = await getVideoDuration(file)
  if (duration > 60) {
    showToast('视频时长不能超过60秒')
    return
  }

  try {
    showToast({ message: '视频上传中...', duration: 0, forbidClick: true })
    const res = await uploadVideo(file)
    videoUrl.value = res.data?.url || ''
    form.value.video = videoUrl.value
    showToast('上传成功')
  } catch {
    showToast('视频上传失败')
  }
}

/**
 * 获取视频时长
 * @param {File} file - 视频文件
 * @returns {Promise<number>} 视频时长（秒）
 */
function getVideoDuration(file: File): Promise<number> {
  return new Promise((resolve) => {
    const video = document.createElement('video')
    video.preload = 'metadata'
    video.onloadedmetadata = () => {
      resolve(video.duration)
      URL.revokeObjectURL(video.src)
    }
    video.onerror = () => resolve(0)
    video.src = URL.createObjectURL(file)
  })
}

/**
 * 移除已上传视频
 */
function onRemoveVideo(): void {
  videoUrl.value = ''
  form.value.video = ''
  if (videoInputRef.value) {
    videoInputRef.value.value = ''
  }
}

// ---------- AI 文案生成 ----------

/**
 * 打开AI文案生成面板
 */
function onAiGenerate(): void {
  showAiPanel.value = true
  aiDrafts.value = []
  selectedDraftIdx.value = null
}

/**
 * 提交AI文案生成请求
 * @returns {Promise<void>}
 * @description 根据关键词调用AI接口生成3版文案
 */
async function onAiGenerateSubmit(): Promise<void> {
  const keyword = aiKeyword.value.trim()
  if (!keyword) return

  aiGenerating.value = true
  try {
    const res = await aiGeneratePost(keyword)
    aiDrafts.value = res.data || []
  } catch {
    showToast('AI 生成失败，请重试')
  } finally {
    aiGenerating.value = false
  }
}

/**
 * 选择AI生成的文案
 * @param {number} idx - 选中文案的索引
 */
function selectDraft(idx: number): void {
  selectedDraftIdx.value = selectedDraftIdx.value === idx ? null : idx
}

/**
 * 应用选中的AI文案到输入框
 * @description 将选中的AI文案内容填入textarea，同时添加话题标签
 */
function applyDraft(): void {
  if (selectedDraftIdx.value === null) return
  const draft = aiDrafts.value[selectedDraftIdx.value]
  form.value.content = draft.content

  // 合并话题
  if (draft.topics?.length) {
    draft.topics.forEach((t) => {
      if (!form.value.topics.includes(t)) {
        form.value.topics.push(t)
      }
    })
  }

  showAiPanel.value = false
  showToast('文案已应用')
}

// ---------- AI 润色 ----------

/**
 * AI文案润色
 * @returns {Promise<void>}
 * @description 调用AI接口对当前文案进行润色优化
 */
async function onAiPolish(): Promise<void> {
  const content = form.value.content.trim()
  if (!content) return

  try {
    showToast({ message: 'AI 润色中...', duration: 0, forbidClick: true })
    const res = await aiPolishPost(content)
    form.value.content = res.data?.content || content

    // 合并AI推荐的话题
    if (res.data?.topics?.length) {
      res.data.topics.forEach((t: string) => {
        if (!form.value.topics.includes(t)) {
          form.value.topics.push(t)
        }
      })
    }

    showToast('润色完成')
  } catch {
    showToast('润色失败，请重试')
  }
}

// ---------- AI 预检 ----------

/**
 * AI内容预检
 * @returns {Promise<void>}
 * @description 调用AI接口检测内容是否违规
 */
async function onAiCheck(): Promise<void> {
  const content = form.value.content.trim()
  if (!content) return

  try {
    showToast({ message: 'AI 检测中...', duration: 0, forbidClick: true })
    const res = await aiCheckContent(content)
    checkResult.value = res.data || { violated: false }
    showCheckResult.value = true
  } catch {
    showToast('检测失败，请重试')
  }
}

// ---------- 草稿保存 ----------

/**
 * 保存草稿
 * @returns {Promise<void>}
 * @description 将当前编辑内容保存为草稿
 */
async function onSaveDraft(): Promise<void> {
  try {
    await saveDraft({
      content: form.value.content,
      images: form.value.images,
      video: form.value.video || undefined,
      topics: form.value.topics,
      visibility: form.value.visibility,
      circleId: form.value.circleId
    })
    showToast('草稿已保存')
  } catch {
    showToast('保存失败')
  }
}

// ---------- 发布 ----------

/**
 * 发布帖子
 * @returns {Promise<void>}
 * @description 校验表单后调用创建帖子接口，成功后跳转首页
 */
async function onPublish(): Promise<void> {
  if (!canPublish.value || publishing.value) return

  publishing.value = true
  try {
    await createPost({
      content: form.value.content,
      images: form.value.images.length > 0 ? form.value.images : undefined,
      video: form.value.video || undefined,
      topics: form.value.topics.length > 0 ? form.value.topics : undefined,
      visibility: form.value.visibility,
      circleId: form.value.circleId
    })

    showToast('发布成功')
    router.replace('/')
  } catch {
    showToast('发布失败，请重试')
  } finally {
    publishing.value = false
  }
}

// ---------- 圈子列表 ----------

/**
 * 加载圈子列表
 * @returns {Promise<void>}
 * @description 获取用户已加入的圈子列表供选择
 */
async function fetchCircles(): Promise<void> {
  try {
    const res = await getCircles({ page: 1, pageSize: 100 })
    circleList.value = res.data?.list || []
  } catch {
    // 静默失败，圈子选择非必须
  }
}

// ---------- 导航 ----------

/**
 * 返回上一页
 */
function goBack(): void {
  if (form.value.content.trim() || form.value.images.length > 0) {
    showConfirmDialog({
      title: '确认离开',
      message: '离开后编辑内容将丢失，是否保存草稿？',
      confirmButtonText: '保存草稿',
      cancelButtonText: '直接离开'
    })
      .then(() => {
        onSaveDraft()
        router.back()
      })
      .catch(() => {
        router.back()
      })
  } else {
    router.back()
  }
}

// ---------- 生命周期 ----------
onMounted(() => {
  fetchCircles()
})
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;
@use '@/styles/mixins.scss' as *;

.create-post-page {
  min-height: 100vh;
  background: $ink-50;
  padding-bottom: 90px;
}

// ---------- 顶部导航 ----------
.create-header {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $space-3 $space-4;
  @include glass(20px, rgba(255, 255, 255, 0.85));

  .header-title {
    font-size: 16px;
    font-weight: 600;
    color: $ink-900;
  }
}

.back-btn {
  width: 36px;
  height: 36px;
  border-radius: $radius-sm;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $ink-700;
  transition: background $dur-fast $ease-out;

  &:hover {
    background: $ink-50;
  }
}

.draft-btn {
  display: flex;
  align-items: center;
  gap: $space-1;
  font-size: 13px;
  color: $mint-600;
  padding: $space-1 $space-3;
  border-radius: $radius-pill;
  transition: background $dur-fast $ease-out;

  &:hover {
    background: $mint-50;
  }
}

// ---------- 表单主体 ----------
.create-main {
  padding: $space-4;
  display: flex;
  flex-direction: column;
  gap: $space-4;
}

// ---------- 文案输入区 ----------
.content-section {
  padding: $space-4;
  border-radius: $radius-lg;
  box-shadow: $shadow-xs;
  animation: fade-up 0.4s $ease-out both;
}

.content-textarea {
  width: 100%;
  min-height: 160px;
  font-size: 15px;
  line-height: 1.75;
  color: $ink-700;
  resize: vertical;
  background: transparent;

  &::placeholder {
    color: $ink-300;
  }
}

.textarea-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: $space-2;
}

.char-count {
  font-size: 12px;
  color: $ink-300;

  &.warning {
    color: $coral-500;
    font-weight: 600;
  }
}

// ---------- AI 辅助工具栏 ----------
.ai-toolbar {
  display: flex;
  gap: $space-2;
  animation: fade-up 0.5s $ease-out both;
  animation-delay: 0.05s;
}

.ai-tool-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $space-1;
  padding: $space-2 $space-3;
  border-radius: $radius-pill;
  font-size: 12px;
  font-weight: 500;
  color: $amber-600;
  background: rgba(255, 182, 39, 0.08);
  border: 1px solid rgba(255, 182, 39, 0.2);
  transition: all $dur-fast $ease-out;

  &:hover:not(:disabled) {
    background: rgba(255, 182, 39, 0.15);
    transform: translateY(-1px);
    box-shadow: 0 2px 8px rgba(255, 182, 39, 0.2);
  }

  &:active:not(:disabled) {
    transform: scale(0.97);
  }

  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }

  :deep(.van-icon) {
    animation: ai-pulse 3s ease-in-out infinite;
  }
}

// ---------- 上传区域 ----------
.upload-section {
  padding: $space-4;
  border-radius: $radius-lg;
  box-shadow: $shadow-xs;
  animation: fade-up 0.5s $ease-out both;
  animation-delay: 0.1s;
}

.section-label {
  font-size: 14px;
  font-weight: 600;
  color: $ink-900;
  margin-bottom: $space-3;

  .sub-label {
    font-size: 12px;
    font-weight: 400;
    color: $ink-300;
    margin-left: $space-1;
  }
}

.video-upload-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: $space-2;
  padding: $space-8 $space-4;
  border: 2px dashed $ink-100;
  border-radius: $radius-md;
  cursor: pointer;
  transition: all $dur-fast $ease-out;

  &:hover {
    border-color: $mint-300;
    background: $mint-50;
  }

  .upload-icon {
    color: $ink-300;
  }

  .upload-text {
    font-size: 13px;
    color: $ink-300;
  }
}

.video-preview {
  position: relative;
  border-radius: $radius-md;
  overflow: hidden;
}

.preview-video {
  width: 100%;
  border-radius: $radius-md;
  background: $ink-900;
}

.remove-video-btn {
  position: absolute;
  top: $space-2;
  right: $space-2;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  font-size: 14px;
  transition: background $dur-fast $ease-out;

  &:hover {
    background: rgba(0, 0, 0, 0.8);
  }
}

.hidden-input {
  display: none;
}

// ---------- 话题标签 ----------
.topic-section {
  padding: $space-4;
  border-radius: $radius-lg;
  box-shadow: $shadow-xs;
  animation: fade-up 0.5s $ease-out both;
  animation-delay: 0.15s;
}

.topic-list {
  display: flex;
  flex-wrap: wrap;
  gap: $space-2;
  align-items: center;
}

.topic-chip {
  display: inline-flex;
  align-items: center;
  gap: $space-1;
  padding: $space-1 $space-3;
  border-radius: $radius-pill;
  font-size: 13px;
  color: $mint-600;
  background: $mint-50;
  transition: all $dur-fast $ease-out;

  .chip-remove {
    font-size: 12px;
    color: $ink-300;
    cursor: pointer;

    &:hover {
      color: $coral-500;
    }
  }
}

.topic-input-wrap {
  flex: 1;
  min-width: 120px;
}

.topic-input {
  width: 100%;
  padding: $space-1 $space-2;
  font-size: 13px;
  color: $ink-700;
  border-bottom: 1px solid $ink-100;
  transition: border-color $dur-fast $ease-out;

  &:focus {
    border-color: $mint-500;
  }

  &::placeholder {
    color: $ink-300;
  }
}

// ---------- 选项区域 ----------
.option-section {
  padding: $space-4;
  border-radius: $radius-lg;
  box-shadow: $shadow-xs;
  animation: fade-up 0.5s $ease-out both;
  animation-delay: 0.2s;
}

.visibility-options {
  display: flex;
  gap: $space-2;
}

.visibility-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $space-2;
  padding: $space-3;
  border-radius: $radius-md;
  font-size: 13px;
  color: $ink-500;
  background: $ink-50;
  border: 2px solid transparent;
  transition: all $dur-fast $ease-out;

  &:hover {
    color: $ink-700;
    background: $ink-100;
  }

  &.active {
    color: $mint-600;
    background: $mint-50;
    border-color: $mint-500;
    font-weight: 600;
  }
}

// ---------- 圈子选择 ----------
.circle-select-wrap {
  position: relative;
}

.circle-select {
  width: 100%;
  padding: $space-3 $space-4;
  padding-right: $space-8;
  font-size: 14px;
  color: $ink-700;
  background: $ink-50;
  border-radius: $radius-md;
  appearance: none;
  cursor: pointer;
  transition: border-color $dur-fast $ease-out;

  &:focus {
    outline: 2px solid $mint-500;
  }
}

.select-arrow {
  position: absolute;
  right: $space-4;
  top: 50%;
  transform: translateY(-50%);
  color: $ink-300;
  pointer-events: none;
}

// ---------- 底部操作栏 ----------
.create-footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $space-3 $space-4;
  box-shadow: 0 -2px 12px rgba(26, 29, 46, 0.06);
}

.cancel-btn {
  padding: $space-2 $space-6;
  border-radius: $radius-pill;
  font-size: 14px;
  color: $ink-500;
  background: $ink-50;
  transition: all $dur-fast $ease-out;

  &:hover {
    background: $ink-100;
  }
}

.publish-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $space-2;
  padding: $space-2 $space-8;
  border-radius: $radius-pill;
  font-size: 14px;
  font-weight: 600;
  color: white;
  background: $grad-mint;
  transition: all $dur-fast $ease-out;

  &:hover:not(:disabled) {
    box-shadow: $shadow-glow-mint;
    transform: translateY(-1px);
  }

  &:active:not(:disabled) {
    transform: scale(0.96);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

// ---------- AI 生成面板 ----------
.ai-generate-panel {
  position: fixed;
  bottom: 70px;
  left: $space-4;
  right: $space-4;
  z-index: 200;
  border-radius: $radius-lg;
  box-shadow: $shadow-lg;
  padding: $space-4;
  animation: fade-up 0.4s $ease-spring both;
}

.ai-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: $space-4;
}

.ai-panel-title {
  display: flex;
  align-items: center;
  gap: $space-2;
  font-size: 14px;
  font-weight: 600;
  color: $ink-900;
}

.ai-icon {
  color: $amber-500;
  animation: ai-pulse 2s ease-in-out infinite;
}

.close-icon {
  color: $ink-300;
  cursor: pointer;
  transition: color $dur-fast $ease-out;

  &:hover {
    color: $ink-500;
  }
}

.keyword-input-area {
  display: flex;
  gap: $space-2;
  margin-bottom: $space-4;
}

.keyword-input {
  flex: 1;
  padding: $space-2 $space-3;
  font-size: 14px;
  color: $ink-700;
  background: $ink-50;
  border-radius: $radius-pill;
  transition: all $dur-fast $ease-out;

  &:focus {
    outline: 2px solid $mint-500;
    background: white;
  }

  &::placeholder {
    color: $ink-300;
  }
}

.generate-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: $space-2 $space-4;
  border-radius: $radius-pill;
  font-size: 13px;
  font-weight: 600;
  color: white;
  background: $grad-amber;
  transition: all $dur-fast $ease-out;
  flex-shrink: 0;

  &:hover:not(:disabled) {
    box-shadow: $shadow-glow-amber;
    transform: translateY(-1px);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.ai-drafts {
  display: flex;
  flex-direction: column;
  gap: $space-3;
  margin-bottom: $space-3;
}

.ai-draft-card {
  padding: $space-3;
  border-radius: $radius-md;
  background: $ink-50;
  border: 2px solid transparent;
  cursor: pointer;
  transition: all $dur-fast $ease-out;

  &:hover {
    background: $mint-50;
    transform: translateX(4px);
  }

  &.selected {
    border-color: $mint-500;
    background: $mint-50;
    box-shadow: $shadow-glow-mint;
  }
}

.draft-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: $space-2;
}

.draft-style-badge {
  display: inline-block;
  font-size: 11px;
  font-weight: 600;
  color: $amber-600;
  background: rgba(255, 182, 39, 0.12);
  padding: 1px 8px;
  border-radius: $radius-pill;
}

.draft-check {
  color: $mint-500;
  font-size: 18px;
}

.draft-content {
  font-size: 14px;
  color: $ink-700;
  line-height: 1.6;
  @include ellipsis(3);
}

.draft-topics {
  display: flex;
  flex-wrap: wrap;
  gap: $space-1;
  margin-top: $space-2;
}

.draft-topic {
  font-size: 12px;
  color: $mint-600;
}

.use-draft-btn {
  width: 100%;
  padding: $space-3;
  border-radius: $radius-md;
  font-size: 14px;
  font-weight: 600;
  color: white;
  background: $grad-mint;
  transition: all $dur-fast $ease-out;

  &:hover {
    box-shadow: $shadow-glow-mint;
    transform: translateY(-1px);
  }

  &:active {
    transform: scale(0.98);
  }
}

// ---------- 预检结果弹窗 ----------
.check-result-content {
  padding: $space-4 0;
  text-align: center;
}

.check-pass {
  .pass-icon {
    color: $mint-500;
    margin-bottom: $space-3;
  }

  p {
    font-size: 15px;
    color: $ink-700;
  }
}

.check-fail {
  .fail-icon {
    color: $coral-500;
    margin-bottom: $space-3;
  }

  p {
    font-size: 15px;
    color: $ink-700;
  }

  .fail-detail {
    font-size: 13px;
    color: $ink-500;
    margin-top: $space-2;
    line-height: 1.6;
  }
}

// ---------- 面板动画 ----------
.slide-up-enter-active {
  animation: fade-up 0.4s $ease-spring both;
}

.slide-up-leave-active {
  animation: fade-up 0.25s $ease-out reverse both;
}
</style>
