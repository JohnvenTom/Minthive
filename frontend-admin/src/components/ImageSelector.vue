<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { VueCropper } from 'vue-cropper'
import 'vue-cropper/dist/index.css'
import { uploadFile } from '@/api/file'

defineProps<{
  modelValue?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', val: string): void
}>()

const cropVisible = ref(false)
const cropImageUrl = ref('')
const cropConfirming = ref(false)
const cropperRef = ref<InstanceType<typeof VueCropper> | null>(null)
const pendingFile = ref<File | null>(null)

const hiddenInput = ref<HTMLInputElement>()

function clearUrl() {
  emit('update:modelValue', '')
}

function triggerFileSelect() {
  hiddenInput.value?.click()
}

function onFileSelected(e: Event) {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  pendingFile.value = file
  cropImageUrl.value = URL.createObjectURL(file)
  cropVisible.value = true
  target.value = ''
}

async function confirmCrop() {
  if (!cropperRef.value || !pendingFile.value) return
  cropConfirming.value = true
  try {
    const cropBlob: Blob = await new Promise((resolve, reject) => {
      cropperRef.value!.getCropBlob((blob: Blob | null) => {
        if (blob) resolve(blob)
        else reject(new Error('裁剪失败'))
      })
    })
    const cropFile = new File([cropBlob], `banner_${Date.now()}.png`, { type: 'image/png' })
    const url = await uploadFile(cropFile)
    emit('update:modelValue', url)
    ElMessage.success('上传成功')
    cropVisible.value = false
  } catch (e) {
    ElMessage.error('裁剪失败，请重试')
  } finally {
    cropConfirming.value = false
  }
}

function cancelCrop() {
  if (cropImageUrl.value) {
    URL.revokeObjectURL(cropImageUrl.value)
    cropImageUrl.value = ''
  }
  cropVisible.value = false
  pendingFile.value = null
}

</script>

<template>
  <input
    ref="hiddenInput"
    type="file"
    accept="image/*"
    style="display:none"
    @change="onFileSelected"
  />

  <div class="image-selector-trigger">
    <div class="selector-preview" @click="triggerFileSelect">
      <img v-if="modelValue" :src="modelValue!" class="preview-img" />
      <div v-else class="preview-placeholder">
        <el-icon><Plus /></el-icon>
        <span>选择图片</span>
      </div>
    </div>
    <div class="selector-input">
      <el-input :model-value="modelValue ?? ''" placeholder="图片 URL" @input="emit('update:modelValue', $event)" />
      <el-button type="primary" @click="triggerFileSelect">选择</el-button>
      <el-button v-if="modelValue" type="danger" :icon="'Delete'" @click="clearUrl" />
    </div>
  </div>

  <!-- 裁剪弹窗 -->
  <Transition name="crop-fade">
    <div v-if="cropVisible" class="crop-modal" @click.self="cancelCrop">
      <div class="crop-dialog">
        <div class="crop-header">
          <button type="button" class="crop-btn crop-btn-cancel" @click="cancelCrop">取消</button>
          <span class="crop-title">裁剪图片</span>
          <button type="button" class="crop-btn crop-btn-confirm" :disabled="cropConfirming" @click="confirmCrop">
            {{ cropConfirming ? '处理中...' : '确定' }}
          </button>
        </div>
        <div class="crop-body">
          <VueCropper
            v-if="cropImageUrl"
            ref="cropperRef"
            :img="cropImageUrl"
            :key="cropImageUrl"
            :output-size="1"
            :output-type="'png'"
            :info="true"
            :full="false"
            :can-move="true"
            :can-scale="false"
            :fixed="true"
            :fixed-number="[2, 1]"
            :fixed-box="false"
            :original="false"
            :auto-crop="true"
            :auto-crop-width="400"
            :auto-crop-height="200"
            :center-box="true"
            :high="true"
          />
        </div>
        <p class="crop-hint">拖动或缩放选择图片区域</p>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.image-selector-trigger {
  display: flex;
  gap: 12px;
  align-items: center;
}
.selector-preview {
  width: 120px;
  height: 68px;
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
  flex-shrink: 0;
  transition: border-color 0.2s;
}
.selector-preview:hover {
  border-color: var(--el-color-primary);
}
.preview-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.preview-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  gap: 4px;
}
.selector-input {
  display: flex;
  gap: 8px;
  flex: 1;
}
.crop-modal {
  position: fixed;
  inset: 0;
  z-index: 3100;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.5);
}
.crop-dialog {
  width: 520px;
  max-height: 80vh;
  background: var(--el-bg-color);
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0,0,0,0.2);
}
.crop-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border-bottom: 1px solid var(--el-border-color);
}
.crop-title {
  font-size: 15px;
  font-weight: 600;
}
.crop-btn {
  border: none;
  background: none;
  font-size: 14px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
}
.crop-btn-cancel {
  color: var(--el-text-color-secondary);
}
.crop-btn-confirm {
  color: var(--el-color-primary);
  font-weight: 600;
}
.crop-btn-confirm:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.crop-body {
  position: relative;
  width: 100%;
  height: 360px;
  background: #000;
  overflow: hidden;
}
.crop-hint {
  text-align: center;
  padding: 10px;
  margin: 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  border-top: 1px solid var(--el-border-color);
}
.crop-fade-enter-active,
.crop-fade-leave-active {
  transition: opacity 0.2s ease;
}
.crop-fade-enter-from,
.crop-fade-leave-to {
  opacity: 0;
}
</style>
