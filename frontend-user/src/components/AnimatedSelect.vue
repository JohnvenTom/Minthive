<template>
  <div class="animated-select" ref="selectRef">
    <!-- 触发器：显示当前选中值 -->
    <button
      type="button"
      class="animated-select__trigger"
      :class="{ 'is-open': isOpen, 'is-disabled': disabled, 'has-value': modelValue }"
      :disabled="disabled"
      @click="toggleDropdown"
    >
      <span class="animated-select__value">
        {{ currentLabel || placeholder }}
      </span>
      <!-- 下拉箭头图标（带旋转动画） -->
      <svg
        class="animated-select__arrow"
        :class="{ 'is-rotated': isOpen }"
        width="16"
        height="16"
        viewBox="0 0 24 24"
        fill="none"
      >
        <path
          d="M6 9l6 6 6-6"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        />
      </svg>
    </button>

    <!-- 下拉选项列表（带展开/收起动画） -->
    <Transition name="dropdown">
      <div v-if="isOpen" class="animated-select__dropdown">
        <div class="animated-select__options">
          <button
            v-for="(option, index) in options"
            :key="option.value"
            type="button"
            class="animated-select__option"
            :class="{ 'is-selected': modelValue === option.value }"
            :style="{ animationDelay: `${index * 40}ms` }"
            @click="selectOption(option)"
          >
            <!-- 选中标记 -->
            <span class="animated-select__check" v-if="modelValue === option.value">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                <path
                  d="M20 6L9 17l-5-5"
                  stroke="currentColor"
                  stroke-width="2.5"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
              </svg>
            </span>
            <span class="animated-select__label">{{ option.label }}</span>
          </button>

          <!-- 空状态提示 -->
          <div v-if="options.length === 0" class="animated-select__empty">
            暂无可选成员
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
/**
 * AnimatedSelect 动画下拉选择框组件
 *
 * 功能描述：
 * - 替代原生 select 元素，提供更美观的 UI 和流畅的交互动画
 * - 支持展开/收起过渡动画、选项交错入场动画、箭头旋转动画
 * - 支持点击外部自动关闭、键盘操作、禁用状态
 * - 完全兼容 v-model 双向绑定
 *
 * Props 参数描述：
 * @param {Array<{ value: string | number; label: string }>} options - 选项列表
 * @param {string | number} modelValue - 当前选中值（v-model 绑定）
 * @param {string} placeholder - 未选择时的占位文字，默认 '请选择'
 * @param {boolean} disabled - 是否禁用，默认 false
 *
 * Events 事件描述：
 * @event update:modelValue - 值变化时触发，返回新选中的 value
 *
 * 注意事项：
 * - 组件使用 scoped 样式隔离，不会污染父级样式
 * - 点击组件外部会自动收起下拉列表
 * - 选项数量建议不超过 50 条，过多会影响性能和体验
 */
import { ref, computed, onMounted, onUnmounted } from 'vue'

// ---------- Props / Emits 定义 ----------
interface SelectOption {
  /** 选项值 */
  value: string | number
  /** 选项显示文本 */
  label: string
}

const props = withDefaults(
  defineProps<{
    /** 选项列表 */
    options: SelectOption[]
    /** 当前选中值（v-model） */
    modelValue: string | number | ''
    /** 占位文字 */
    placeholder?: string
    /** 是否禁用 */
    disabled?: boolean
  }>(),
  {
    placeholder: '请选择',
    disabled: false,
  }
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: string | number): void
}>()

// ---------- 响应式状态 ----------
/** 下拉框是否展开 */
const isOpen = ref(false)
/** 组件根元素引用 */
const selectRef = ref<HTMLElement | null>(null)

// ---------- 计算属性 ----------
/**
 * 获取当前选中项的显示文本
 * @returns {string} 选中项 label 或空字符串
 */
const currentLabel = computed(() => {
  if (!props.modelValue) return ''
  const found = props.options.find((o) => o.value === props.modelValue)
  return found?.label || ''
})

// ---------- 方法 ----------
/**
 * 切换下拉框展开/收起状态
 * @returns {void}
 */
function toggleDropdown(): void {
  if (props.disabled) return
  isOpen.value = !isOpen.value
}

/**
 * 选择某个选项并关闭下拉框
 * @param {SelectOption} option - 被选中的选项对象
 * @returns {void}
 */
function selectOption(option: SelectOption): void {
  emit('update:modelValue', option.value)
  isOpen.value = false
}

/**
 * 点击外部区域时关闭下拉框
 * @param {MouseEvent} event - 鼠标事件对象
 * @returns {void}
 */
function handleClickOutside(event: MouseEvent): void {
  if (selectRef.value && !selectRef.value.contains(event.target as Node)) {
    isOpen.value = false
  }
}

// ---------- 生命周期 ----------
onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped lang="scss">
.animated-select {
  position: relative;
  width: 100%;
}

// ---------- 触发器按钮 ----------
.animated-select__trigger {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  height: 44px;
  padding: 0 14px;
  border-radius: 10px;
  border: 1.5px solid #E6E8F0;
  font-size: 14px;
  color: #2D3142;
  background: #fff;
  cursor: pointer;
  transition: all 0.18s cubic-bezier(0.22, 1, 0.36, 1);
  user-select: none;
  outline: none;

  &:hover:not(.is-disabled) {
    border-color: #4ECDC4;
    background: #F8FFFE;
  }

  &.is-open {
    border-color: #4ECDC4;
    box-shadow: 0 0 0 3px rgba(78, 205, 196, 0.12), 0 4px 16px rgba(26, 29, 46, 0.08);
  }

  &.is-disabled {
    opacity: 0.5;
    cursor: not-allowed;
    background: #F5F7FB;
  }

  &.has-value .animated-select__value {
    color: #2D3142;
    font-weight: 500;
  }
}

.animated-select__value {
  flex: 1;
  text-align: left;
  color: #9AA0BC;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

// ---------- 箭头图标（旋转动画） ----------
.animated-select__arrow {
  flex-shrink: 0;
  margin-left: 8px;
  color: #9AA0BC;
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);

  &.is-rotated {
    transform: rotate(180deg);
  }

  .is-open & {
    color: #4ECDC4;
  }
}

// ---------- 下拉面板 ----------
.animated-select__dropdown {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  right: 0;
  z-index: 100;
  background: #fff;
  border-radius: 12px;
  border: 1px solid rgba(26, 29, 46, 0.06);
  box-shadow: 0 8px 28px rgba(26, 29, 46, 0.12), 0 2px 8px rgba(26, 29, 46, 0.06);
  overflow: hidden;
}

.animated-select__options {
  padding: 6px;
  max-height: 240px;
  overflow-y: auto;

  // 自定义滚动条
  &::-webkit-scrollbar {
    width: 4px;
  }
  &::-webkit-scrollbar-track {
    background: transparent;
  }
  &::-webkit-scrollbar-thumb {
    background: #E6E8F0;
    border-radius: 2px;
  }
}

// ---------- 单个选项 ----------
.animated-select__option {
  display: flex;
  align-items: center;
  width: 100%;
  padding: 10px 12px;
  border: none;
  border-radius: 8px;
  background: transparent;
  font-size: 14px;
  color: #2D3142;
  cursor: pointer;
  transition: all 0.15s ease;
  animation: option-in 0.25s cubic-bezier(0.22, 1, 0.36, 1) both;
  outline: none;

  &:hover {
    background: #E8FBF8;
    color: #2A8E87;
  }

  &.is-selected {
    background: linear-gradient(135deg, rgba(78, 205, 196, 0.1) 0%, rgba(107, 203, 119, 0.08) 100%);
    color: #2A8E87;
    font-weight: 500;
  }
}

// 选项入场动画
@keyframes option-in {
  from {
    opacity: 0;
    transform: translateY(-6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

// 选中勾选标记
.animated-select__check {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  margin-right: 8px;
  border-radius: 50%;
  background: linear-gradient(135deg, #4ECDC4 0%, #6BCB77 100%);
  color: #fff;
  flex-shrink: 0;
}

.animated-select__label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

// ---------- 空状态 ----------
.animated-select__empty {
  padding: 20px 12px;
  text-align: center;
  color: #9AA0BC;
  font-size: 13px;
}

// ========== Vue Transition 动画 ==========

// 下拉面板展开/收起动画
.dropdown-enter-active {
  animation: dropdown-in 0.25s cubic-bezier(0.22, 1, 0.36, 1);
}
.dropdown-leave-active {
  animation: dropdown-out 0.18s cubic-bezier(0.55, 0, 1, 0.45);
}

@keyframes dropdown-in {
  from {
    opacity: 0;
    transform: translateY(-8px) scale(0.96);
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
    transform: translateY(-4px) scale(0.98);
  }
}
</style>
