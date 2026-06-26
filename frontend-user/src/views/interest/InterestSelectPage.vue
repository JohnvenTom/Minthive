<template>
  <div class="interest-page">
    <!-- 主题切换按钮 -->
    <button
      class="theme-toggle"
      :class="{ 'theme-toggle--light': appStore.theme === 'light' }"
      @click="appStore.setTheme()"
      :aria-label="appStore.theme === 'dark' ? '切换到浅色模式' : '切换到深色模式'"
    >
      <svg v-if="appStore.theme === 'dark'" key="sun" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <circle cx="12" cy="12" r="5" />
        <path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42" />
      </svg>
      <svg v-else key="moon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M21 12.79A9 9 0 1111.21 3 7 7 0 0021 12.79z" />
      </svg>
    </button>
    <!-- 蜂巢粒子背景 -->
    <div class="interest-page__bg">
      <canvas ref="particleCanvas" class="interest-page__canvas"></canvas>
      <div class="interest-page__hex-grid"></div>
      <div class="interest-page__glow interest-page__glow--mint"></div>
      <div class="interest-page__glow interest-page__glow--amber"></div>
    </div>

    <div class="interest-page__content">
      <!-- 进度指示 -->
      <div class="progress-bar">
        <div class="progress-bar__step progress-bar__step--active">
          <span class="progress-bar__dot">1</span>
          <span class="progress-bar__label">选择兴趣</span>
        </div>
        <div class="progress-bar__line progress-bar__line--done"></div>
        <div class="progress-bar__step">
          <span class="progress-bar__dot">2</span>
          <span class="progress-bar__label">完善资料</span>
        </div>
      </div>

      <!-- 标题区 -->
      <header class="interest-header">
        <h1 class="interest-header__title">选择你感兴趣的领域</h1>
        <p class="interest-header__subtitle">
          至少选择 <span class="interest-header__min">3</span> 个，最多
          <span class="interest-header__max">8</span> 个，我们将为你推荐相关内容
        </p>
      </header>

      <!-- 已选计数器 -->
      <div class="interest-counter">
        <div class="interest-counter__bar">
          <div
            class="interest-counter__fill"
            :style="{ width: `${(selectedInterests.length / 8) * 100}%` }"
          ></div>
        </div>
        <span class="interest-counter__text">
          已选 <strong>{{ selectedInterests.length }}</strong> / 8
        </span>
      </div>

      <!-- 兴趣标签网格 -->
      <div class="interest-grid">
        <button
          v-for="item in interestList"
          :key="item.key"
          :class="[
            'interest-tag',
            {
              'interest-tag--selected': selectedInterests.includes(item.key),
              'interest-tag--disabled':
                !selectedInterests.includes(item.key) && selectedInterests.length >= 8
            }
          ]"
          @click="toggleInterest(item.key)"
        >
          <el-icon class="interest-tag__icon" :size="28">
            <component :is="item.icon" />
          </el-icon>
          <span class="interest-tag__text">{{ item.label }}</span>
          <span v-if="selectedInterests.includes(item.key)" class="interest-tag__check">
            <svg viewBox="0 0 16 16" fill="none">
              <path
                d="M3 8.5L6.5 12L13 4"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </span>
        </button>
      </div>

      <!-- 底部操作区 -->
      <div class="interest-actions">
        <button class="interest-actions__skip" @click="handleSkip">跳过</button>
        <button
          :class="[
            'interest-actions__done',
            { 'interest-actions__done--active': selectedInterests.length >= 3 }
          ]"
          :disabled="selectedInterests.length < 3 || submitLoading"
          @click="handleSubmit"
        >
          <span v-if="submitLoading" class="interest-actions__loading">
            <svg viewBox="0 0 24 24" fill="none">
              <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2" opacity="0.3" />
              <path d="M12 2a10 10 0 019.5 7" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                <animateTransform
                  attributeName="transform"
                  type="rotate"
                  from="0 12 12"
                  to="360 12 12"
                  dur="0.8s"
                  repeatCount="indefinite"
                />
              </path>
            </svg>
          </span>
          <span v-else>完成选择</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, type Component } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Monitor, VideoCamera, Reading, Camera, Cherry, Headset,
  Bowl, Promotion, Soccer, Notebook, Microphone, PriceTag,
  Coin, MagicStick, Brush, Money, FirstAidKit, Sunny,
  School, EditPen, Edit, Star, Van, Iphone
} from '@element-plus/icons-vue'
import { updateInterests } from '@/api/user'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'

// ============================================================
// 路由与Store
// ============================================================
const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()

// ============================================================
// 状态定义
// ============================================================
const selectedInterests = ref<string[]>([])
const submitLoading = ref(false)

// ============================================================
// 粒子画布
// ============================================================
const particleCanvas = ref<HTMLCanvasElement | null>(null)
let animationFrameId: number | null = null

// ============================================================
// 兴趣标签列表
// ============================================================
interface InterestItem {
  key: string
  label: string
  icon: Component
}

const interestList: InterestItem[] = [
  { key: 'gaming', label: '游戏', icon: Monitor },
  { key: 'movie', label: '影视', icon: VideoCamera },
  { key: 'outdoor', label: '户外', icon: MagicStick },
  { key: 'reading', label: '读书', icon: Reading },
  { key: 'photography', label: '摄影', icon: Camera },
  { key: 'anime', label: '二次元', icon: Cherry },
  { key: 'music', label: '音乐', icon: Headset },
  { key: 'food', label: '美食', icon: Bowl },
  { key: 'travel', label: '旅行', icon: Promotion },
  { key: 'craft', label: '手工', icon: Brush },
  { key: 'sports', label: '运动', icon: Soccer },
  { key: 'tech', label: '科技', icon: Notebook },
  { key: 'pet', label: '宠物', icon: FirstAidKit },
  { key: 'fashion', label: '时尚', icon: PriceTag },
  { key: 'art', label: '艺术', icon: Coin },
  { key: 'dance', label: '舞蹈', icon: Microphone },
  { key: 'finance', label: '理财', icon: Money },
  { key: 'health', label: '养生', icon: Sunny },
  { key: 'education', label: '教育', icon: School },
  { key: 'design', label: '设计', icon: EditPen },
  { key: 'writing', label: '写作', icon: Edit },
  { key: 'astrology', label: '星座', icon: Star },
  { key: 'cars', label: '汽车', icon: Van },
  { key: 'digital', label: '数码', icon: Iphone }
]

/**
 * 切换兴趣标签选中状态
 * @param {string} key - 兴趣标签的唯一标识
 * @returns {void}
 * @note 最多选择8个，超出时忽略操作；已选中的再次点击则取消
 */
function toggleInterest(key: string): void {
  const index = selectedInterests.value.indexOf(key)
  if (index > -1) {
    selectedInterests.value.splice(index, 1)
  } else {
    if (selectedInterests.value.length >= 8) {
      ElMessage.warning('最多只能选择8个兴趣标签')
      return
    }
    selectedInterests.value.push(key)
  }
}

/**
 * 提交兴趣标签选择
 * @description 调用API更新用户兴趣标签，成功后跳转首页
 * @returns {Promise<void>}
 * @throws 接口调用失败时通过ElMessage提示用户
 */
async function handleSubmit(): Promise<void> {
  if (selectedInterests.value.length < 3) {
    ElMessage.warning('请至少选择3个兴趣标签')
    return
  }

  submitLoading.value = true
  try {
    await updateInterests(selectedInterests.value)
    userStore.updateInterests(selectedInterests.value)
    ElMessage.success('兴趣标签设置成功')
    await router.push('/')
  } catch (error: unknown) {
    const msg = error instanceof Error ? error.message : '设置失败，请稍后重试'
    ElMessage.error(msg)
  } finally {
    submitLoading.value = false
  }
}

/**
 * 跳过兴趣标签选择
 * @description 直接跳转首页，不设置兴趣标签
 * @returns {void}
 */
function handleSkip(): void {
  router.push('/')
}

// ============================================================
// 粒子动画
// ============================================================

interface Particle {
  x: number
  y: number
  vx: number
  vy: number
  size: number
  opacity: number
  color: string
}

/**
 * 初始化粒子动画
 * @description 在Canvas上绘制漂浮的六边形粒子，营造蜂巢氛围
 * @returns {void}
 */
function initParticles(): void {
  const canvas = particleCanvas.value
  if (!canvas) return

  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const resize = () => {
    canvas.width = window.innerWidth
    canvas.height = window.innerHeight
  }
  resize()
  window.addEventListener('resize', resize)

  const particles: Particle[] = []
  const count = Math.min(Math.floor(window.innerWidth / 30), 40)
  const colors = ['#4ECDC4', '#6BCB77', '#FFB627', '#8FE3D6']

  for (let i = 0; i < count; i++) {
    particles.push({
      x: Math.random() * canvas.width,
      y: Math.random() * canvas.height,
      vx: (Math.random() - 0.5) * 0.3,
      vy: (Math.random() - 0.5) * 0.3,
      size: Math.random() * 2.5 + 0.8,
      opacity: Math.random() * 0.4 + 0.08,
      color: colors[Math.floor(Math.random() * colors.length)]
    })
  }

  /**
   * 绘制六边形
   * @param {CanvasRenderingContext2D} context - Canvas 2D上下文
   * @param {number} cx - 中心点X坐标
   * @param {number} cy - 中心点Y坐标
   * @param {number} size - 六边形半径
   * @returns {void}
   */
  function drawHex(context: CanvasRenderingContext2D, cx: number, cy: number, size: number): void {
    context.beginPath()
    for (let i = 0; i < 6; i++) {
      const angle = (Math.PI / 3) * i - Math.PI / 6
      const px = cx + size * Math.cos(angle)
      const py = cy + size * Math.sin(angle)
      if (i === 0) context.moveTo(px, py)
      else context.lineTo(px, py)
    }
    context.closePath()
  }

  function animate(): void {
    if (!ctx || !canvas) return
    ctx.clearRect(0, 0, canvas.width, canvas.height)

    particles.forEach((p) => {
      p.x += p.vx
      p.y += p.vy

      if (p.x < 0) p.x = canvas.width
      if (p.x > canvas.width) p.x = 0
      if (p.y < 0) p.y = canvas.height
      if (p.y > canvas.height) p.y = 0

      ctx.globalAlpha = p.opacity
      ctx.fillStyle = p.color
      drawHex(ctx, p.x, p.y, p.size)
      ctx.fill()
    })

    animationFrameId = requestAnimationFrame(animate)
  }

  animate()
}

onMounted(() => {
  initParticles()
})

onUnmounted(() => {
  if (animationFrameId !== null) {
    cancelAnimationFrame(animationFrameId)
  }
})
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;
@use '@/styles/mixins.scss' as *;

// ============================================================
// 兴趣标签选择页 - 暗色沉浸式 + 蜂巢装饰
// ============================================================

.interest-page {
  position: relative;
  width: 100%;
  min-height: 100vh;
  overflow-x: hidden;
  --ip-bg: #{$ink-900};
  --ip-glow-mint: rgba(78, 205, 196, 0.12);
  --ip-glow-amber: rgba(255, 182, 39, 0.08);
  --ip-hex-grid: rgba(78, 205, 196, 0.03);
  --ip-text-primary: #fff;
  --ip-text-secondary: #{$ink-300};
  --ip-text-muted: #{$ink-500};
  --ip-divider: rgba(255, 255, 255, 0.06);
  --ip-card-bg: rgba(255, 255, 255, 0.03);
  --ip-card-border: rgba(255, 255, 255, 0.06);
  --ip-dot-bg: rgba(255, 255, 255, 0.04);
  --ip-dot-border: rgba(255, 255, 255, 0.1);
  --ip-progress-bg: rgba(255, 255, 255, 0.08);
  --ip-counter-bg: rgba(255, 255, 255, 0.06);
  --ip-btn-border: rgba(255, 255, 255, 0.08);
  --ip-btn-bg: rgba(255, 255, 255, 0.04);
  background: var(--ip-bg);
  transition: background-color 0.3s ease;
}

// ---------- 主题切换按钮 ----------
.theme-toggle {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 200;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: $ink-300;
  cursor: pointer;
  backdrop-filter: blur(8px);
  transition: all $dur-base $ease-out;

  &:hover {
    background: rgba(255, 255, 255, 0.14);
    color: #fff;
    transform: scale(1.08);
  }

  &:active {
    transform: scale(0.92);
  }

  svg {
    width: 20px;
    height: 20px;
    transition: transform 0.4s $ease-spring;
  }

  &--light {
    background: rgba(0, 0, 0, 0.06);
    border-color: rgba(0, 0, 0, 0.08);
    color: $ink-500;

    &:hover {
      background: rgba(0, 0, 0, 0.1);
      color: $ink-700;
    }
  }

  &:active svg {
    transform: rotate(360deg);
  }
}

// ---------- 背景层 ----------
.interest-page__bg {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.interest-page__canvas {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.interest-page__hex-grid {
  position: absolute;
  inset: 0;
  @include honeycomb-bg(var(--ip-hex-grid), 40px);
}

.interest-page__glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(140px);

  &--mint {
    width: 600px;
    height: 600px;
    top: -15%;
    left: 10%;
    background: radial-gradient(circle, var(--ip-glow-mint) 0%, transparent 70%);
  }

  &--amber {
    width: 400px;
    height: 400px;
    bottom: 5%;
    right: 5%;
    background: radial-gradient(circle, var(--ip-glow-amber) 0%, transparent 70%);
  }
}

// ---------- 内容区 ----------
.interest-page__content {
  position: relative;
  z-index: 1;
  max-width: 720px;
  margin: 0 auto;
  padding: $space-8 $space-6 $space-12;
  animation: fade-up 0.6s $ease-out both;

  @include mobile {
    padding: $space-5 $space-4 $space-10;
  }
}

// ---------- 进度指示 ----------
.progress-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $space-3;
  margin-bottom: $space-10;

  @include mobile {
    margin-bottom: $space-6;
  }

  &__step {
    display: flex;
    align-items: center;
    gap: $space-2;

    &--active {
      .progress-bar__dot {
        background: $grad-mint;
        color: $ink-900;
        border-color: transparent;
      }

      .progress-bar__label {
        color: var(--ip-text-primary);
      }
    }
  }

  &__dot {
    width: 28px;
    height: 28px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    font-size: 13px;
    font-weight: 700;
    color: var(--ip-text-muted);
    border: 2px solid var(--ip-dot-border);
    background: var(--ip-dot-bg);
    transition: all $dur-base $ease-out;
  }

  &__label {
    font-size: 13px;
    color: var(--ip-text-muted);
    transition: color $dur-base $ease-out;

    @include mobile {
      display: none;
    }
  }

  &__line {
    width: 60px;
    height: 2px;
    background: var(--ip-progress-bg);
    border-radius: 1px;

    &--done {
      background: $grad-mint;
    }
  }
}

// ---------- 标题区 ----------
.interest-header {
  text-align: center;
  margin-bottom: $space-6;

  &__title {
    font-family: $font-heading;
    font-size: 28px;
    font-weight: 700;
    color: var(--ip-text-primary);
    margin-bottom: $space-3;

    @include mobile {
      font-size: 22px;
    }
  }

  &__subtitle {
    font-size: 15px;
    color: var(--ip-text-secondary);
    line-height: 1.6;
  }

  &__min {
    color: $mint-500;
    font-weight: 600;
  }

  &__max {
    color: $amber-500;
    font-weight: 600;
  }
}

// ---------- 已选计数器 ----------
.interest-counter {
  display: flex;
  align-items: center;
  gap: $space-3;
  margin-bottom: $space-6;
  padding: 0 $space-2;

  &__bar {
    flex: 1;
    height: 4px;
    background: var(--ip-counter-bg);
    border-radius: 2px;
    overflow: hidden;
  }

  &__fill {
    height: 100%;
    background: $grad-mint;
    border-radius: 2px;
    transition: width $dur-base $ease-spring;
    min-width: 0;
  }

  &__text {
    font-size: 13px;
    color: var(--ip-text-secondary);
    white-space: nowrap;

    strong {
      color: $mint-500;
      font-size: 15px;
    }
  }
}

// ---------- 兴趣标签网格 ----------
.interest-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: $space-3;
  margin-bottom: $space-10;

  @include mobile {
    grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
    gap: $space-2;
  }
}

// ---------- 单个兴趣标签 ----------
.interest-tag {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: $space-1;
  padding: $space-4 $space-2;
  border-radius: $radius-md;
  background: var(--ip-card-bg);
  border: 1px solid var(--ip-card-border);
  cursor: pointer;
  transition: all $dur-base $ease-spring;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background: $grad-mint;
    opacity: 0;
    transition: opacity $dur-base $ease-out;
    border-radius: inherit;
  }

  &:hover:not(&--disabled) {
    border-color: rgba(78, 205, 196, 0.25);
    background: rgba(78, 205, 196, 0.06);
    transform: translateY(-2px);
  }

  // 选中态
  &--selected {
    border-color: rgba(78, 205, 196, 0.5);
    background: rgba(78, 205, 196, 0.1);
    box-shadow: 0 0 20px rgba(78, 205, 196, 0.12);

    &::before {
      opacity: 0.06;
    }

    .interest-tag__icon {
      transform: scale(1.1);
    }

    .interest-tag__text {
      color: $mint-500;
      font-weight: 600;
    }
  }

  // 禁用态（已达上限且未选中）
  &--disabled {
    opacity: 0.35;
    cursor: not-allowed;
  }

  &__icon {
    position: relative;
    z-index: 1;
    transition: transform $dur-base $ease-spring;
    color: var(--ip-text-secondary);

    .interest-tag--selected &,
    .interest-tag:hover:not(.interest-tag--disabled) & {
      color: $mint-300;
    }

    @include mobile {
      :deep(svg) {
        width: 24px !important;
        height: 24px !important;
      }
    }
  }

  &__text {
    font-size: 13px;
    color: var(--ip-text-secondary);
    position: relative;
    z-index: 1;
    transition: all $dur-fast $ease-out;

    @include mobile {
      font-size: 12px;
    }
  }

  // 勾选标记
  &__check {
    position: absolute;
    top: 6px;
    right: 6px;
    width: 18px;
    height: 18px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    background: $mint-500;
    z-index: 2;
    animation: check-pop 0.3s $ease-spring both;

    svg {
      width: 10px;
      height: 10px;
      color: $ink-900;
    }
  }
}

@keyframes check-pop {
  0% {
    transform: scale(0);
    opacity: 0;
  }
  60% {
    transform: scale(1.2);
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

// ---------- 底部操作区 ----------
.interest-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: $space-4;
  padding-top: $space-6;
  border-top: 1px solid var(--ip-divider);

  &__skip {
    padding: $space-3 $space-6;
    font-size: 14px;
    color: var(--ip-text-muted);
    background: none;
    border: 1px solid var(--ip-btn-border);
    border-radius: $radius-sm;
    cursor: pointer;
    transition: all $dur-fast $ease-out;

    &:hover {
      color: var(--ip-text-secondary);
      border-color: rgba(255, 255, 255, 0.15);
      background: rgba(255, 255, 255, 0.03);
    }
  }

  &__done {
    padding: $space-3 $space-8;
    font-size: 15px;
    font-weight: 600;
    color: var(--ip-text-muted);
    background: var(--ip-btn-bg);
    border: 1px solid var(--ip-card-border);
    border-radius: $radius-sm;
    cursor: not-allowed;
    transition: all $dur-base $ease-out;
    display: flex;
    align-items: center;
    gap: $space-2;

    &--active {
      color: $ink-900;
      background: $grad-mint;
      border-color: transparent;
      cursor: pointer;
      box-shadow: 0 0 0 0 rgba(78, 205, 196, 0);

      &:hover {
        transform: translateY(-1px);
        box-shadow: $shadow-glow-mint;
      }

      &:active {
        transform: translateY(0);
      }
    }

    &:disabled:not(&--active) {
      opacity: 0.5;
    }
  }

  &__loading {
    display: flex;
    align-items: center;

    svg {
      width: 18px;
      height: 18px;
    }
  }
}

// ---------- 动画关键帧 ----------
@keyframes fade-up {
  from {
    opacity: 0;
    transform: translateY(24px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/**
 * 移动端安全区域适配
 * @description 为页面根容器添加顶部安全区域内边距，避免内容被刘海屏遮挡
 */
@include mobile {
  .interest-page {
    padding-top: env(safe-area-inset-top);
  }
}
</style>

<style lang="scss">
@use '@/styles/variables.scss' as *;

[data-theme="light"] .interest-page {
  --ip-bg: #f8f9fb;
  --ip-glow-mint: rgba(78, 205, 196, 0.08);
  --ip-glow-amber: rgba(255, 182, 39, 0.06);
  --ip-hex-grid: rgba(78, 205, 196, 0.02);
  --ip-text-primary: #1a1d23;
  --ip-text-secondary: #5a6071;
  --ip-text-muted: #9298a8;
  --ip-divider: rgba(0, 0, 0, 0.06);
  --ip-card-bg: rgba(0, 0, 0, 0.02);
  --ip-card-border: rgba(0, 0, 0, 0.06);
  --ip-dot-bg: rgba(0, 0, 0, 0.03);
  --ip-dot-border: rgba(0, 0, 0, 0.1);
  --ip-progress-bg: rgba(0, 0, 0, 0.08);
  --ip-counter-bg: rgba(0, 0, 0, 0.05);
  --ip-btn-border: rgba(0, 0, 0, 0.08);
  --ip-btn-bg: rgba(0, 0, 0, 0.03);

  .interest-actions__skip:hover {
    border-color: rgba(0, 0, 0, 0.12);
    background: rgba(0, 0, 0, 0.02);
  }
}
</style>
