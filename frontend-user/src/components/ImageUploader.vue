<template>
  <div class="image-uploader">
    <!-- 图片网格预览 -->
    <div class="image-uploader__grid">
      <div
        v-for="(item, idx) in fileList"
        :key="idx"
        class="image-uploader__item"
      >
        <img :src="item.url" :alt="`图片${idx + 1}`" class="image-uploader__img" />

        <!-- 上传进度 -->
        <div v-if="item.status === 'uploading'" class="image-uploader__progress">
          <div class="image-uploader__progress-bar" :style="{ width: item.progress + '%' }"></div>
          <span class="image-uploader__progress-text">{{ Math.round(item.progress) }}%</span>
        </div>

        <!-- 删除按钮 -->
        <button class="image-uploader__delete" @click="removeFile(idx)">
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none">
            <path d="M18 6L6 18M6 6l12 12" stroke="currentColor" stroke-width="3" stroke-linecap="round"/>
          </svg>
        </button>
      </div>

      <!-- 添加按钮 -->
      <div
        v-if="fileList.length < maxCount"
        class="image-uploader__add"
        @click="triggerInput"
      >
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
          <path d="M12 5v14M5 12h14" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
        </svg>
        <span class="image-uploader__add-text">{{ fileList.length }}/{{ maxCount }}</span>
      </div>
    </div>

    <!-- 隐藏的文件输入 -->
    <input
      ref="inputRef"
      type="file"
      accept="image/jpeg,image/png,image/webp,image/gif"
      multiple
      class="image-uploader__input"
      @change="handleFileChange"
    />
  </div>
</template>

<script setup lang="ts">
/**
 * ImageUploader 图片上传组件
 * @description 选图时仅做本地预览和压缩，不立即上传。父组件在发布时调用 uploadAll() 批量上传到 MinIO。
 * @param {number} maxCount - 最大上传数量，默认9
 * @emits change - 图片列表变化事件（本地预览阶段发出 File 对象数组，上传完成后发出 URL 数组）
 */

import { ref } from 'vue'
import { showToast } from 'vant'
import { compressImage } from '@/utils/compress'
import { uploadImage } from '@/api/file'

/** 上传文件项类型 */
interface UploadItem {
  /** 本地预览URL */
  url: string
  /** 上传状态 */
  status: 'pending' | 'uploading' | 'done' | 'error'
  /** 上传进度 */
  progress: number
  /** 原始文件（压缩后） */
  file?: File
  /** 服务端返回的URL */
  serverUrl?: string
}

const props = withDefaults(defineProps<{
  /** 最大上传数量 */
  maxCount?: number
}>(), {
  maxCount: 9
})

const emit = defineEmits<{
  /** 图片列表变化事件 */
  (e: 'change', urls: string[]): void
}>()

/** 文件列表 */
const fileList = ref<UploadItem[]>([])

/** 文件输入框引用 */
const inputRef = ref<HTMLInputElement | null>(null)

/** 触发文件选择 */
function triggerInput(): void {
  inputRef.value?.click()
}

/**
 * 处理文件选择变化
 * @param {Event} event - 文件选择事件
 * @description 选图后仅压缩并本地预览，不上传到服务器
 */
async function handleFileChange(event: Event): Promise<void> {
  const target = event.target as HTMLInputElement
  const files = target.files
  if (!files) return

  const remaining = props.maxCount - fileList.value.length
  const toProcess = Array.from(files).slice(0, remaining)

  for (const file of toProcess) {
    try {
      // 压缩图片，保存压缩后的 File 用于后续上传
      const compressed = await compressImage(file)
      const localUrl = URL.createObjectURL(compressed)
      const item: UploadItem = {
        url: localUrl,
        status: 'pending',
        progress: 0,
        file: compressed
      }
      fileList.value.push(item)
    } catch {
      showToast('图片压缩失败')
    }
  }

  // 重置input
  target.value = ''
  emitChange()
}

/**
 * 批量上传所有待上传的图片到 MinIO
 * @returns {Promise<string[]>} 上传成功的图片 URL 数组
 * @throws 上传全部失败时抛出异常
 * @description 由父组件在发布时调用，逐张上传并更新进度
 */
async function uploadAll(): Promise<string[]> {
  const pendingItems = fileList.value.filter(f => f.status === 'pending' || f.status === 'error')
  const urls: string[] = []

  for (const item of pendingItems) {
    if (!item.file) continue

    try {
      item.status = 'uploading'
      item.progress = 30

      // 模拟上传进度
      const progressTimer = setInterval(() => {
        if (item.progress < 90) {
          item.progress += Math.random() * 15
        }
      }, 200)

      const res = await uploadImage(item.file!)
      clearInterval(progressTimer)

      // 后端返回 data 直接是 URL 字符串
      const url = typeof res.data === 'string' ? res.data : res.data?.url || ''
      if (!url) {
        throw new Error('上传成功但未返回URL')
      }

      item.progress = 100
      item.status = 'done'
      item.serverUrl = url
      urls.push(url)
    } catch (err) {
      console.error('[ImageUploader] 上传失败:', err)
      item.status = 'error'
      item.progress = 0
    }
  }

  emitChange()

  // 如果有失败的，提示用户
  const failedCount = fileList.value.filter(f => f.status === 'error').length
  if (failedCount > 0) {
    showToast(`${failedCount}张图片上传失败`)
  }

  return urls
}

/**
 * 移除指定索引的图片
 * @param {number} idx - 图片索引
 */
function removeFile(idx: number): void {
  const item = fileList.value[idx]
  if (item) {
    URL.revokeObjectURL(item.url)
    fileList.value.splice(idx, 1)
    emitChange()
  }
}

/** 触发change事件，发出当前所有已上传成功的URL列表 */
function emitChange(): void {
  const urls = fileList.value
    .filter(f => f.status === 'done' && f.serverUrl)
    .map(f => f.serverUrl!)
  emit('change', urls)
}

/** 暴露方法给父组件调用 */
defineExpose({ uploadAll })
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;
@use '@/styles/mixins.scss' as *;

.image-uploader {
  &__grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: $space-2;
  }

  &__item {
    aspect-ratio: 1;
    border-radius: $radius-sm;
    overflow: hidden;
    position: relative;
    background: $ink-50;
  }

  &__img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  // ---------- 进度条 ----------
  &__progress {
    position: absolute;
    inset: 0;
    background: rgba(26, 29, 46, 0.5);
    @include center;
    flex-direction: column;
    gap: $space-1;
  }

  &__progress-bar {
    width: 60%;
    height: 4px;
    background: rgba(255, 255, 255, 0.3);
    border-radius: 2px;
    overflow: hidden;
    position: relative;

    &::after {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      height: 100%;
      width: 100%;
      background: $grad-mint;
      border-radius: 2px;
    }
  }

  &__progress-text {
    font-size: 11px;
    color: #fff;
    font-family: $font-heading;
  }

  // ---------- 删除按钮 ----------
  &__delete {
    position: absolute;
    top: 4px;
    right: 4px;
    width: 20px;
    height: 20px;
    border-radius: 50%;
    background: rgba(26, 29, 46, 0.6);
    border: none;
    color: #fff;
    cursor: pointer;
    @include center;
    opacity: 0;
    transition: opacity $dur-fast ease;

    .image-uploader__item:hover & {
      opacity: 1;
    }

    &:hover {
      background: $coral-500;
    }
  }

  // ---------- 添加按钮 ----------
  &__add {
    aspect-ratio: 1;
    border: 2px dashed $ink-100;
    border-radius: $radius-sm;
    @include center;
    flex-direction: column;
    gap: $space-1;
    color: $ink-300;
    cursor: pointer;
    transition: all $dur-fast ease;

    &:hover {
      border-color: $mint-500;
      color: $mint-500;
      background: $mint-50;
    }
  }

  &__add-text {
    font-size: 11px;
    font-family: $font-heading;
  }

  // ---------- 隐藏输入 ----------
  &__input {
    display: none;
  }
}
</style>
