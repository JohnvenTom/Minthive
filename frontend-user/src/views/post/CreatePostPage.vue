<template>
  <div class="create-post-page">
    <!-- 顶部导航 -->
    <header class="create-header">
      <button class="back-btn" @click="goBack">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
          <path d="M15 18L9 12L15 6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </button>
      <h2 class="header-title">发布动态</h2>
      <button class="draft-btn" @click="onSaveDraft">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
          <path d="M14 2H6C5.46957 2 4.96086 2.21071 4.58579 2.58579C4.21071 2.96086 4 3.46957 4 4V20C4 20.5304 4.21071 21.0391 4.58579 21.4142C4.96086 21.7893 5.46957 22 6 22H18C18.5304 22 19.0391 21.7893 19.4142 21.4142C19.7893 21.0391 20 20.5304 20 20V8L14 2Z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
          <path d="M14 2V8H20M16 13H8M16 17H8M10 9H8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        <span>草稿</span>
      </button>
    </header>

    <!-- 表单主体 -->
    <main class="create-main">
      <!-- 文案输入区 -->
      <section class="content-section">
        <textarea
          ref="textareaRef"
          v-model="form.content"
          class="content-textarea"
          placeholder="分享你的想法..."
          maxlength="2000"
          rows="8"
          @input="onContentInput"
        />
        <div class="textarea-footer">
          <div class="ai-toolbar-inline">
            <button class="ai-chip" @click="onAiGenerate">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                <path d="M12 2L14.5 8.5L21 9.5L16.5 14L17.5 21L12 17.5L6.5 21L7.5 14L3 9.5L9.5 8.5L12 2Z" fill="currentColor"/>
              </svg>
              AI 生成
            </button>
            <button class="ai-chip" :disabled="!form?.content?.trim()" @click="onAiPolish">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                <path d="M9.5 2A2.5 2.5 0 0 1 12 4.5V5H4.5A2.5 2.5 0 0 1 7 2.5H9.5ZM12 7H4V20C4 21.1 4.9 22 6 22H14C15.1 22 16 21.1 16 20V7H12ZM14.5 2H12V5H14.5A2.5 2.5 0 0 0 17 2.5V2H14.5Z" fill="currentColor"/>
              </svg>
              润色
            </button>
            <button class="ai-chip" :disabled="!form?.content?.trim()" @click="onAiCheck">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                <path d="M12 22C17.5228 22 22 17.5228 22 12C22 6.47715 17.5228 2 12 2C6.47715 2 2 6.47715 2 12C2 17.5228 6.47715 22 12 22Z" stroke="currentColor" stroke-width="2"/>
                <path d="M8 12L11 15L16 9" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              预检
            </button>
          </div>
          <span :class="['char-count', { warning: charCount > 1800 }]">
            {{ charCount }}/2000
          </span>
        </div>
      </section>

      <!-- 附件区 -->
      <section class="section-card">
        <div class="section-row">
          <div class="section-icon-wrap --image">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
              <rect x="3" y="3" width="18" height="18" rx="3" stroke="currentColor" stroke-width="1.5"/>
              <circle cx="8.5" cy="8.5" r="1.5" fill="currentColor"/>
              <path d="M21 15L16 10L5 21" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <div class="section-body">
            <div class="section-label-row">
              <span class="section-label">图片</span>
              <span class="section-hint">最多9张</span>
            </div>
            <ImageUploader
              ref="imageUploaderRef"
              :max-count="9"
              @change="onImageChange"
            />
          </div>
        </div>
      </section>

      <section class="section-card">
        <div class="section-row">
          <div class="section-icon-wrap --video">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
              <path d="M15 10L21 6V18L15 14V10Z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
              <rect x="3" y="6" width="12" height="12" rx="2" stroke="currentColor" stroke-width="1.5"/>
            </svg>
          </div>
          <div class="section-body">
            <div class="section-label-row">
              <span class="section-label">视频</span>
              <span class="section-hint">≤60秒</span>
            </div>
            <div v-if="!videoUrl" class="video-upload-area" @click="onVideoSelect">
              <div class="upload-dashed">
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none">
                  <path d="M12 5V19M5 12H19" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                </svg>
                <span>添加视频</span>
              </div>
            </div>
            <div v-else class="video-preview">
              <video :src="videoUrl" class="preview-video" controls />
              <button class="remove-video-btn" @click="onRemoveVideo">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none">
                  <path d="M18 6L6 18M6 6l12 12" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"/>
                </svg>
              </button>
            </div>
          </div>
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
      <section class="section-card">
        <div class="section-row">
          <div class="section-icon-wrap --topic">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
              <path d="M4 9C4 6.79086 5.79086 5 8 5H16C18.2091 5 20 6.79086 20 9V15C20 17.2091 18.2091 19 16 19H8C5.79086 19 4 17.2091 4 15V9Z" stroke="currentColor" stroke-width="1.5"/>
              <path d="M9 9H15M9 13H13" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
          </div>
          <div class="section-body">
            <div class="section-label-row">
              <span class="section-label">话题</span>
              <span class="section-hint">最多5个</span>
            </div>
            <div class="topic-list">
              <span v-for="(topic, idx) in form.topics" :key="idx" class="topic-chip">
                #{{ topic }}
                <button class="chip-remove" @click="removeTopic(idx)">
                  <svg width="10" height="10" viewBox="0 0 24 24" fill="none">
                    <path d="M18 6L6 18M6 6l12 12" stroke="currentColor" stroke-width="3" stroke-linecap="round"/>
                  </svg>
                </button>
              </span>
              <input
                v-model="topicInput"
                class="topic-input"
                placeholder="输入话题，回车添加"
                @keyup.enter="addTopic"
              />
            </div>
          </div>
        </div>
      </section>

      <!-- 可见范围 -->
      <section class="section-card">
        <div class="section-row">
          <div class="section-icon-wrap --visibility">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
              <path d="M1 12S5 4 12 4S23 12 23 12S19 20 12 20S1 12 1 12Z" stroke="currentColor" stroke-width="1.5"/>
              <circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="1.5"/>
            </svg>
          </div>
          <div class="section-body">
            <span class="section-label">可见范围</span>
            <div class="visibility-options">
              <button
                v-for="opt in visibilityOptions"
                :key="opt.value"
                :class="['visibility-btn', { active: form.visibility === opt.value }]"
                @click="form.visibility = opt.value"
              >
                {{ opt.label }}
              </button>
            </div>
          </div>
        </div>
      </section>

      <!-- 选择圈子 -->
      <section class="section-card">
        <div class="section-row">
          <div class="section-icon-wrap --circle">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
              <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="1.5"/>
              <circle cx="12" cy="9" r="3" stroke="currentColor" stroke-width="1.5"/>
              <path d="M6.168 19C6.168 16.5 8.5 15 12 15C15.5 15 17.832 16.5 17.832 19" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
          </div>
          <div class="section-body">
            <span class="section-label">选择圈子</span>
            <div class="circle-select-wrap" ref="circleSelectRef">
              <div
                :class="['circle-select-trigger', { 'is-open': circleDropdownOpen }]"
                @click="toggleCircleDropdown"
              >
                <span :class="['trigger-text', { placeholder: !form.circleId }]">
                  {{ selectedCircleName || '不选择圈子' }}
                </span>
                <svg
                  :class="['select-arrow', { 'arrow-rotate': circleDropdownOpen }]"
                  width="16"
                  height="16"
                  viewBox="0 0 24 24"
                  fill="none"
                >
                  <path d="M6 9L12 15L18 9" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </div>
              <Transition name="dropdown">
                <div v-if="circleDropdownOpen" class="circle-dropdown">
                  <div
                    :class="['circle-option', { 'is-selected': !form.circleId }]"
                    @click="selectCircle(undefined)"
                  >
                    <span class="option-label">不选择圈子</span>
                    <svg v-if="!form.circleId" class="option-check" width="14" height="14" viewBox="0 0 24 24" fill="none">
                      <path d="M8 12L11 15L16 9" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                  </div>
                  <div
                    v-for="circle in circleList"
                    :key="circle.id"
                    :class="['circle-option', { 'is-selected': form.circleId === circle.id }]"
                    @click="selectCircle(circle.id)"
                  >
                    <span class="option-label">{{ circle.name }}</span>
                    <svg v-if="form.circleId === circle.id" class="option-check" width="14" height="14" viewBox="0 0 24 24" fill="none">
                      <path d="M8 12L11 15L16 9" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                  </div>
                </div>
              </Transition>
            </div>
          </div>
        </div>
      </section>
    </main>

    <!-- 底部操作栏 -->
    <footer class="create-footer">
      <button class="cancel-btn" @click="goBack">取消</button>
      <button
        class="publish-btn"
        :disabled="!canPublish || publishing"
        @click="onPublish"
      >
        <LoadingSpinner v-if="publishing" :size="16" />
        <span v-else>发布动态</span>
      </button>
    </footer>

    <!-- AI 文案生成面板 -->
    <Transition name="slide-up">
      <div v-if="showAiPanel" class="ai-generate-panel">
        <div class="ai-panel-header">
          <span class="ai-panel-title">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
              <path d="M12 2L14.5 8.5L21 9.5L16.5 14L17.5 21L12 17.5L6.5 21L7.5 14L3 9.5L9.5 8.5L12 2Z" fill="#FFB627"/>
            </svg>
            AI 文案生成
          </span>
          <button class="close-btn" @click="showAiPanel = false">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
              <path d="M18 6L6 18M6 6l12 12" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </button>
        </div>

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
            <LoadingSpinner v-if="aiGenerating" :size="14" />
            <span v-else>生成</span>
          </button>
        </div>

        <div v-if="aiDrafts.length > 0" class="ai-drafts">
          <div
            v-for="(draft, idx) in aiDrafts"
            :key="idx"
            :class="['ai-draft-card', { selected: selectedDraftIdx === idx }]"
            @click="selectDraft(idx)"
          >
            <div class="draft-header">
              <span class="draft-style-badge">{{ draft.styleName || `文案 ${idx + 1}` }}</span>
              <div v-if="selectedDraftIdx === idx" class="draft-check">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                  <path d="M8 12L11 15L16 9" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </div>
            </div>
            <p class="draft-content">{{ draft.content }}</p>
            <div v-if="draft.topics?.length" class="draft-topics">
              <span v-for="t in draft.topics" :key="t" class="draft-topic">#{{ t }}</span>
            </div>
          </div>
        </div>

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
      teleport="body"
      class="check-dialog"
    >
      <div class="check-result-content">
        <div v-if="!checkResult.violated" class="check-pass">
          <div class="result-icon --pass">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none">
              <path d="M8 12L11 15L16 9" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <p>内容合规，可以放心发布</p>
        </div>
        <div v-else class="check-fail">
          <div class="result-icon --fail">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none">
              <path d="M12 9V13M12 17H12.01M12 22C17.5228 22 22 17.5228 22 12C22 6.47715 17.5228 2 12 2C6.47715 2 2 6.47715 2 12C2 17.5228 6.47715 22 12 22Z" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </div>
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
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog, closeToast } from 'vant'
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

const videoUrl = ref('')
const topicInput = ref('')
const publishing = ref(false)

// ---------- 圈子列表 ----------
const circleList = ref<Circle[]>([])
const circleDropdownOpen = ref(false)
const circleSelectRef = ref<HTMLElement | null>(null)

/** 当前选中的圈子名称 */
const selectedCircleName = computed(() => {
  if (!form.value.circleId) return ''
  const found = circleList.value.find((c) => c.id === form.value.circleId)
  return found?.name || ''
})

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
const imageUploaderRef = ref<InstanceType<typeof ImageUploader> | null>(null)

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
 * 图片列表变化回调
 * @param {string[]} urls - 当前所有已上传成功的图片URL列表
 * @description 用 ImageUploader 的 change 事件同步图片URL到表单
 */
function onImageChange(urls: string[]): void {
  form.value.images = urls
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
    const url = typeof res.data === 'string' ? res.data : res.data?.url || ''
    videoUrl.value = url
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
    // 后端返回的是字符串数组（每条文案一个字符串），需转换为 AiPostDraft 对象格式
    const raw = res.data
    if (Array.isArray(raw) && typeof raw[0] === 'string') {
      aiDrafts.value = (raw as string[]).map((text, idx) => ({
        style: ['simple', 'vibe', 'pro'][idx] as 'simple' | 'vibe' | 'pro',
        styleName: ['简约', '氛围', '干货'][idx],
        content: text.trim(),
        topics: []
      }))
    } else {
      aiDrafts.value = raw || []
    }
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
    closeToast()
    checkResult.value = res.data || { violated: false }
    showCheckResult.value = true
  } catch {
    closeToast()
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
      images: form.value.images.length > 0 ? JSON.stringify(form.value.images) : undefined,
      video: form.value.video || undefined,
      tags: form.value.topics.length > 0 ? form.value.topics.join(',') : undefined,
      visibility: { public: 0, fans: 1, self: 2 }[form.value.visibility] ?? 0,
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
 * @description 先上传图片到 MinIO，再调用创建帖子接口，成功后跳转首页
 */
async function onPublish(): Promise<void> {
  if (!canPublish.value || publishing.value) return

  publishing.value = true
  try {
    // 先上传图片到 MinIO
    let imageUrls: string[] = form.value.images
    if (imageUploaderRef.value) {
      showToast({ message: '图片上传中...', duration: 0, forbidClick: true })
      imageUrls = await imageUploaderRef.value.uploadAll()
      form.value.images = imageUrls
    }

    const postData = {
      content: form.value.content,
      images: imageUrls.length > 0 ? JSON.stringify(imageUrls) : undefined,
      video: form.value.video || undefined,
      tags: form.value.topics.length > 0 ? form.value.topics.join(',') : undefined,
      visibility: { public: 0, fans: 1, self: 2 }[form.value.visibility] ?? 0,
      circleId: form.value.circleId
    }
    await createPost(postData)

    showToast('发布成功')
    router.replace('/')
  } catch {
    showToast('发布失败，请重试')
  } finally {
    publishing.value = false
  }
}

// ---------- 圈子下拉选择 ----------

/**
 * 切换圈子下拉框展开/收起状态
 */
function toggleCircleDropdown(): void {
  circleDropdownOpen.value = !circleDropdownOpen.value
}

/**
 * 选择圈子
 * @param {number | undefined} circleId - 选中的圈子ID，undefined表示不选
 */
function selectCircle(circleId: number | undefined): void {
  form.value.circleId = circleId
  circleDropdownOpen.value = false
}

/**
 * 点击外部关闭圈子下拉框
 * @param {MouseEvent} event - 鼠标点击事件
 */
function handleClickOutside(event: MouseEvent): void {
  if (circleSelectRef.value && !circleSelectRef.value.contains(event.target as Node)) {
    circleDropdownOpen.value = false
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
  document.addEventListener('click', handleClickOutside)
})

/**
 * 组件卸载时移除全局事件监听
 */
onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;
@use '@/styles/mixins.scss' as *;

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
$accent-amber-light: rgba(255, 182, 39, 0.1);
$radius-card: 16px;
$radius-btn: 10px;

.create-post-page {
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
}

.back-btn {
  @include center;
  width: 36px;
  height: 36px;
  border-radius: $radius-btn;
  color: $text-secondary;
  transition: all 0.2s ease;

  &:hover {
    background: rgba(0, 0, 0, 0.04);
    color: $text-primary;
  }
}

.draft-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  color: $accent-mint;
  padding: 6px 14px;
  border-radius: $radius-btn;
  transition: all 0.2s ease;

  &:hover {
    background: $accent-mint-light;
  }
}

// ---------- Main ----------
.create-main {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

// ---------- Content Section ----------
.content-section {
  background: $bg-card;
  border-radius: $radius-card;
  border: 1px solid $border-subtle;
  padding: 20px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;

  &:focus-within {
    border-color: $accent-mint;
    box-shadow: 0 0 0 3px $accent-mint-light;
  }
}

.content-textarea {
  width: 100%;
  min-height: 200px;
  font-size: 15px;
  line-height: 1.8;
  color: $text-primary;
  resize: vertical;
  background: transparent;
  border: none;
  outline: none;
  font-family: inherit;

  &::placeholder {
    color: $text-tertiary;
  }
}

.textarea-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid $border-subtle;
}

.ai-toolbar-inline {
  display: flex;
  gap: 6px;
}

.ai-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
  color: $accent-amber;
  background: $accent-amber-light;
  border: 1px solid rgba(255, 182, 39, 0.15);
  transition: all 0.2s ease;

  &:hover:not(:disabled) {
    background: rgba(255, 182, 39, 0.18);
    border-color: rgba(255, 182, 39, 0.3);
    transform: translateY(-1px);
  }

  &:active:not(:disabled) {
    transform: scale(0.96);
  }

  &:disabled {
    opacity: 0.35;
    cursor: not-allowed;
  }
}

.char-count {
  font-size: 12px;
  font-weight: 500;
  color: $text-tertiary;
  font-variant-numeric: tabular-nums;

  &.warning {
    color: $coral-500;
  }
}

// ---------- Section Card ----------
.section-card {
  background: $bg-card;
  border-radius: $radius-card;
  border: 1px solid $border-subtle;
  transition: border-color 0.2s ease;

  &:hover {
    border-color: $border-hover;
  }
}

.section-row {
  display: flex;
  gap: 16px;
  padding: 16px 20px;
}

.section-icon-wrap {
  @include center;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  flex-shrink: 0;
  margin-top: 2px;

  &.--image {
    background: rgba(78, 205, 196, 0.08);
    color: $mint-600;
  }
  &.--video {
    background: rgba(77, 163, 255, 0.08);
    color: $sky-500;
  }
  &.--topic {
    background: rgba(107, 203, 119, 0.08);
    color: $leaf-600;
  }
  &.--visibility {
    background: rgba(255, 182, 39, 0.08);
    color: $amber-600;
  }
  &.--circle {
    background: rgba(255, 92, 138, 0.08);
    color: $rose-500;
  }
}

.section-body {
  flex: 1;
  min-width: 0;
}

.section-label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.section-label {
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
  letter-spacing: -0.01em;
}

.section-hint {
  font-size: 12px;
  color: $text-tertiary;
  font-weight: 400;
}

// ---------- Video Upload ----------
.video-upload-area {
  cursor: pointer;
}

.upload-dashed {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 28px;
  border: 1.5px dashed $border-hover;
  border-radius: 12px;
  color: $text-tertiary;
  font-size: 13px;
  transition: all 0.2s ease;

  &:hover {
    border-color: $accent-mint;
    color: $accent-mint;
    background: $accent-mint-light;
  }
}

.video-preview {
  position: relative;
  border-radius: 12px;
  overflow: hidden;
}

.preview-video {
  width: 100%;
  border-radius: 12px;
  background: #000;
}

.remove-video-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  @include center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(8px);
  color: #fff;
  transition: background 0.2s ease;

  &:hover {
    background: rgba(0, 0, 0, 0.8);
  }
}

.hidden-input {
  display: none;
}

// ---------- Topic Tags ----------
.topic-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.topic-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 500;
  color: $mint-600;
  background: $accent-mint-light;
  border: 1px solid rgba(78, 205, 196, 0.15);
  transition: all 0.2s ease;
}

.chip-remove {
  @include center;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  color: $text-tertiary;
  cursor: pointer;
  transition: all 0.15s ease;

  &:hover {
    background: rgba(255, 107, 107, 0.15);
    color: $coral-500;
  }
}

.topic-input {
  flex: 1;
  min-width: 100px;
  padding: 5px 0;
  font-size: 13px;
  color: $text-primary;
  background: transparent;
  border: none;
  border-bottom: 1px solid $border-subtle;
  outline: none;
  transition: border-color 0.2s ease;

  &:focus {
    border-color: $accent-mint;
  }

  &::placeholder {
    color: $text-tertiary;
  }
}

// ---------- Visibility ----------
.visibility-options {
  display: flex;
  gap: 8px;
  margin-top: 4px;
}

.visibility-btn {
  flex: 1;
  padding: 10px;
  border-radius: $radius-btn;
  font-size: 13px;
  font-weight: 500;
  color: $text-secondary;
  background: $bg-warm;
  border: 1.5px solid transparent;
  transition: all 0.2s ease;

  &:hover {
    color: $text-primary;
    background: darken($bg-warm, 2%);
  }

  &.active {
    color: $mint-600;
    background: $accent-mint-light;
    border-color: $accent-mint;
    font-weight: 600;
  }
}

// ---------- Circle Select ----------
.circle-select-wrap {
  position: relative;
  margin-top: 4px;
}

.circle-select-trigger {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  min-height: 42px;
  padding: 0 14px;
  font-size: 14px;
  color: $text-primary;
  background: $bg-warm;
  border-radius: $radius-btn;
  border: 1.5px solid $border-subtle;
  cursor: pointer;
  outline: none;
  transition: all 0.25s cubic-bezier(0.22, 1, 0.36, 1);

  &:hover {
    border-color: $border-hover;
    background: lighten($bg-warm, 1%);
  }

  &.is-open {
    border-color: $accent-mint;
    box-shadow: 0 0 0 3px $accent-mint-light, 0 4px 12px rgba(78, 205, 196, 0.1);
  }
}

.trigger-text {
  flex: 1;
  text-align: left;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;

  &.placeholder {
    color: $text-tertiary;
  }
}

.select-arrow {
  flex-shrink: 0;
  margin-left: 8px;
  color: $text-tertiary;
  transition: transform 0.3s cubic-bezier(0.22, 1, 0.36, 1);

  &.arrow-rotate {
    transform: translateY(-50%) rotate(180deg);
  }
}

/* 下拉面板 - 向上展开 */
.circle-dropdown {
  position: absolute;
  bottom: calc(100% + 6px);
  left: 0;
  right: 0;
  z-index: 50;
  background: $bg-card;
  border-radius: $radius-btn;
  border: 1.5px solid $border-subtle;
  box-shadow: 0 -8px 32px rgba(26, 29, 46, 0.12), 0 -2px 8px rgba(26, 29, 46, 0.06);
  overflow: hidden;
  max-height: 220px;
  overflow-y: auto;
  @include custom-scrollbar(4px);
}

.circle-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 11px 14px;
  cursor: pointer;
  transition: all 0.15s ease;

  &:hover {
    background: $accent-mint-light;
  }

  &.is-selected {
    .option-label {
      color: $mint-600;
      font-weight: 600;
    }

    .option-check {
      color: $accent-mint;
    }
  }
}

.option-label {
  font-size: 14px;
  color: $text-primary;
  transition: all 0.15s ease;
}

.option-check {
  flex-shrink: 0;
  color: transparent;
  transition: color 0.2s ease;
}

/* 下拉展开/收起动画 */
.dropdown-enter-active {
  animation: dropdown-in 0.25s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.dropdown-leave-active {
  animation: dropdown-out 0.18s cubic-bezier(0.55, 0, 1, 0.45) both;
}

@keyframes dropdown-in {
  from {
    opacity: 0;
    transform: translateY(6px) scale(0.97);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes dropdown-out {
  from {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
  to {
    opacity: 0;
    transform: translateY(4px) scale(0.98);
  }
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
  transition: all 0.2s ease;

  &:hover {
    background: rgba(0, 0, 0, 0.04);
    color: $text-primary;
  }
}

.publish-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 32px;
  border-radius: $radius-btn;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  background: $grad-mint;
  box-shadow: 0 2px 8px rgba(78, 205, 196, 0.25);
  transition: all 0.2s ease;

  &:hover:not(:disabled) {
    box-shadow: 0 4px 16px rgba(78, 205, 196, 0.35);
    transform: translateY(-1px);
  }

  &:active:not(:disabled) {
    transform: scale(0.97);
  }

  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
    box-shadow: none;
  }
}

// ---------- AI Generate Panel ----------
.ai-generate-panel {
  position: fixed;
  bottom: 70px;
  left: 20px;
  right: 20px;
  z-index: 200;
  border-radius: $radius-card;
  border: 1px solid $border-subtle;
  background: $bg-card;
  box-shadow: 0 16px 48px rgba(26, 29, 46, 0.12);
  padding: 20px;
  animation: fade-up 0.35s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.ai-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.ai-panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: $text-primary;
}

.close-btn {
  @include center;
  width: 32px;
  height: 32px;
  border-radius: $radius-btn;
  color: $text-tertiary;
  transition: all 0.2s ease;

  &:hover {
    background: rgba(0, 0, 0, 0.04);
    color: $text-primary;
  }
}

.keyword-input-area {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

.keyword-input {
  flex: 1;
  padding: 10px 16px;
  font-size: 14px;
  color: $text-primary;
  background: $bg-warm;
  border-radius: $radius-btn;
  border: 1px solid $border-subtle;
  outline: none;
  transition: all 0.2s ease;

  &:focus {
    border-color: $accent-mint;
    box-shadow: 0 0 0 3px $accent-mint-light;
    background: $bg-card;
  }

  &::placeholder {
    color: $text-tertiary;
  }
}

.generate-btn {
  @include center;
  gap: 6px;
  padding: 10px 20px;
  border-radius: $radius-btn;
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  background: $grad-amber;
  flex-shrink: 0;
  transition: all 0.2s ease;

  &:hover:not(:disabled) {
    box-shadow: 0 4px 12px rgba(255, 182, 39, 0.3);
    transform: translateY(-1px);
  }

  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }
}

.ai-drafts {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 14px;
  max-height: 240px;
  overflow-y: auto;
  @include custom-scrollbar(4px);
}

.ai-draft-card {
  padding: 14px;
  border-radius: 12px;
  background: $bg-warm;
  border: 1.5px solid transparent;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: darken($bg-warm, 1%);
    border-color: $border-hover;
  }

  &.selected {
    border-color: $accent-mint;
    background: $accent-mint-light;
  }
}

.draft-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.draft-style-badge {
  font-size: 11px;
  font-weight: 600;
  color: $amber-600;
  background: $accent-amber-light;
  padding: 2px 10px;
  border-radius: 999px;
  letter-spacing: 0.02em;
}

.draft-check {
  @include center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: $accent-mint;
  color: #fff;
}

.draft-content {
  font-size: 14px;
  color: $text-primary;
  line-height: 1.65;
  @include ellipsis(3);
}

.draft-topics {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.draft-topic {
  font-size: 12px;
  color: $mint-600;
}

.use-draft-btn {
  width: 100%;
  padding: 12px;
  border-radius: $radius-btn;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  background: $grad-mint;
  transition: all 0.2s ease;

  &:hover {
    box-shadow: 0 4px 16px rgba(78, 205, 196, 0.3);
    transform: translateY(-1px);
  }

  &:active {
    transform: scale(0.98);
  }
}

// ---------- Check Result Dialog ----------
.check-result-content {
  padding: 24px 0;
  text-align: center;
}

.result-icon {
  @include center;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  margin: 0 auto 14px;

  &.--pass {
    background: $accent-mint-light;
    color: $mint-600;
  }
  &.--fail {
    background: rgba(255, 107, 107, 0.1);
    color: $coral-500;
  }
}

.check-pass,
.check-fail {
  p {
    font-size: 15px;
    color: $text-primary;
    font-weight: 500;
  }
}

.check-fail .fail-detail {
  font-size: 13px;
  color: $text-secondary;
  font-weight: 400;
  margin-top: 8px;
  line-height: 1.6;
}

// ---------- Animations ----------
@keyframes fade-up {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.slide-up-enter-active {
  animation: fade-up 0.35s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.slide-up-leave-active {
  animation: fade-up 0.2s ease reverse both;
}

// ---------- Mobile ----------
@include mobile {
  /**
   * 移动端安全区域适配
   * @description 为页面根容器添加顶部安全区域内边距，避免内容被刘海屏遮挡
   */
  .create-post-page {
    padding-top: env(safe-area-inset-top);
  }

  .create-main {
    padding: 16px;
    gap: 10px;
  }

  .content-section {
    padding: 16px;
  }

  .section-row {
    padding: 14px 16px;
    gap: 12px;
  }

  .section-icon-wrap {
    width: 36px;
    height: 36px;
    border-radius: 10px;
  }

  .ai-chip {
    padding: 4px 10px;
    font-size: 11px;
  }
}
</style>

<style lang="scss">
@use '@/styles/variables.scss' as *;

.check-dialog.van-dialog {
  --van-dialog-background: #FFFFFF;
  --van-dialog-header-color: #1A1A1A;
  --van-dialog-message-color: #6B6560;
  --van-dialog-confirm-button-color: #{$mint-500};
  --van-dialog-cancel-button-color: #9B958E;
}
</style>
