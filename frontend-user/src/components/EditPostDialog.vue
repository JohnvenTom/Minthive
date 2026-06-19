<template>
  <Teleport to="body">
    <Transition name="edit-dialog-fade">
      <div v-if="show" class="edit-overlay" @click="close">
        <div class="edit-dialog" @click.stop>
          <!-- 标题栏 -->
          <header class="edit-header">
            <h4 class="edit-title">编辑帖子</h4>
            <button class="edit-close" @click="close">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                <path d="M18 6L6 18M6 6l12 12" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>
            </button>
          </header>

          <!-- 编辑表单 -->
          <div class="edit-body">
            <textarea
              ref="textareaRef"
              v-model="editContent"
              class="edit-textarea"
              placeholder="说说你的想法..."
              rows="5"
              maxlength="2000"
            />
            <div class="edit-footer">
              <span class="edit-count">{{ editContent.length }}/2000</span>
              <div class="edit-actions">
                <button class="edit-btn edit-btn--cancel" @click="close">取消</button>
                <button
                  class="edit-btn edit-btn--save"
                  :class="{ 'is-loading': submitting }"
                  :disabled="submitting || !editContent.trim()"
                  @click="handleSave"
                >
                  {{ submitting ? '保存中...' : '保存' }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
/**
 * EditPostDialog 编辑帖子弹窗组件
 *
 * @description 用于编辑已发布帖子的文案内容，支持字数统计和提交状态管理。
 *   保存成功后通知父组件刷新帖子数据
 *
 * @prop {boolean} show - 控制弹窗显示/隐藏（v-model 双向绑定）
 * @prop {number} postId - 要编辑的帖子ID
 * @prop {string} content - 当前帖子内容（用于初始化编辑框）
 *
 * @emits update:show - 更新显示状态
 * @emits saved - 编辑保存成功回调，参数为更新后的帖子数据
 */
import { ref, watch, nextTick } from 'vue'
import { updatePost } from '@/api/post'
import { showToast } from 'vant'

const props = defineProps<{
  /** 是否显示弹窗（v-model） */
  show: boolean
  /** 帖子ID */
  postId: number
  /** 当前帖子内容 */
  content: string
}>()

const emit = defineEmits<{
  /** 更新显示状态 */
  (e: 'update:show', value: boolean): void
  /** 保存成功回调 */
  (e: 'saved', post: any): void
}>()

// ---------- 状态 ----------

/** 编辑中的文案内容 */
const editContent = ref('')

/** 是否正在提交 */
const submitting = ref(false)

/** textarea DOM 引用 */
const textareaRef = ref<HTMLTextAreaElement | null>(null)

// ---------- 方法 ----------

/**
 * 关闭弹窗
 */
function close(): void {
  emit('update:show', false)
}

/**
 * 保存编辑内容
 *
 * @description 调用 updatePost 接口更新帖子文案，成功后关闭弹窗并通知父组件
 * @returns {Promise<void>}
 */
async function handleSave(): Promise<void> {
  if (submitting.value || !editContent.value.trim()) return

  submitting.value = true
  try {
    const res = await updatePost(props.postId, { content: editContent.value.trim() })
    showToast('修改成功')
    close()
    emit('saved', res.data)
  } catch {
    showToast('修改失败，请重试')
  } finally {
    submitting.value = false
  }
}

// ---------- 监听 ----------

/**
 * 弹窗打开时初始化内容并聚焦输入框
 */
watch(() => props.show, (val) => {
  if (val) {
    editContent.value = props.content || ''
    nextTick(() => {
      textareaRef.value?.focus()
    })
  }
})
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;
@use '@/styles/mixins' as *;

// ========== 遮罩层 ==========
.edit-overlay {
  position: fixed;
  inset: 0;
  z-index: 1200;
  background: rgba(26, 29, 46, 0.55);
  @include center;
}

// ========== 弹窗主体 ==========
.edit-dialog {
  width: 88%;
  max-width: 480px;
  background: #fff;
  border-radius: $radius-lg;
  box-shadow: $shadow-lg;
  animation: scale-in 0.28s $ease-spring both;
}

// ========== 标题栏 ==========
.edit-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $space-5 $space-5 $space-3;
  border-bottom: 1px solid $ink-100;
}

.edit-title {
  font-size: 17px;
  font-weight: 600;
  color: $ink-900;
  margin: 0;
}

.edit-close {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: none;
  border: none;
  color: $ink-500;
  cursor: pointer;
  transition: all $dur-fast ease;

  &:hover {
    background: $ink-50;
    color: $ink-700;
  }
}

// ========== 内容区 ==========
.edit-body {
  padding: $space-4 $space-5 $space-5;
}

.edit-textarea {
  width: 100%;
  min-height: 120px;
  padding: $space-3;
  font-size: 15px;
  line-height: 1.7;
  color: $ink-900;
  background: $ink-50;
  border: 1.5px solid transparent;
  border-radius: $radius-sm;
  resize: vertical;
  outline: none;
  transition: border-color $dur-fast ease;
  font-family: inherit;

  &:focus {
    border-color: $mint-500;
    background: #fff;
  }

  &::placeholder {
    color: $ink-300;
  }
}

// ========== 底部操作栏 ==========
.edit-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: $space-3;
}

.edit-count {
  font-size: 12px;
  color: $ink-500;
}

.edit-actions {
  display: flex;
  gap: $space-2;
}

.edit-btn {
  padding: $space-2 $space-5;
  font-size: 14px;
  font-weight: 600;
  border-radius: $radius-pill;
  border: none;
  cursor: pointer;
  transition: all $dur-fast ease;

  &--cancel {
    color: $ink-700;
    background: $ink-50;

    &:hover {
      background: $ink-100;
      color: $ink-900;
    }
  }

  &--save {
    color: #fff;
    background: $grad-mint;

    &:hover:not(:disabled) {
      box-shadow: $shadow-glow-mint;
      transform: translateY(-1px);
    }

    &:active:not(:disabled) {
      transform: scale(0.96);
    }

    &:disabled {
      opacity: 0.45;
      cursor: not-allowed;
    }

    &.is-loading {
      opacity: 0.8;
    }
  }
}

// ========== 动画 ==========
.edit-dialog-fade-enter-active,
.edit-dialog-fade-leave-active {
  transition: opacity 0.25s ease;
}

.edit-dialog-fade-enter-from,
.edit-dialog-fade-leave-to {
  opacity: 0;
}
</style>
