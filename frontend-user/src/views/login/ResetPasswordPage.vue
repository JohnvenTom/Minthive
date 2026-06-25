<template>
  <div class="reset-page">
    <!-- 蜂巢粒子背景 -->
    <div class="reset-page__bg">
      <canvas ref="particleCanvas" class="reset-page__canvas"></canvas>
      <div class="reset-page__hex-grid"></div>
      <div class="reset-page__glow reset-page__glow--mint"></div>
      <div class="reset-page__glow reset-page__glow--amber"></div>
    </div>

    <div class="reset-page__content">
      <!-- 左侧品牌展示区（桌面端） -->
      <aside class="reset-page__brand">
        <div class="brand-inner">
          <div class="brand-logo">
            <svg class="brand-logo__hex" viewBox="0 0 120 138" fill="none">
              <path
                d="M60 0L120 34.5V103.5L60 138L0 103.5V34.5L60 0Z"
                fill="url(#hex-grad-r)"
                opacity="0.9"
              />
              <path
                d="M60 12L110 42V102L60 132L10 102V42L60 12Z"
                stroke="rgba(78,205,196,0.4)"
                stroke-width="1.5"
                fill="none"
              />
              <defs>
                <linearGradient id="hex-grad-r" x1="0" y1="0" x2="120" y2="138">
                  <stop offset="0%" stop-color="#4ECDC4" />
                  <stop offset="100%" stop-color="#6BCB77" />
                </linearGradient>
              </defs>
            </svg>
            <span class="brand-logo__text">MintHive</span>
          </div>

          <h1 class="brand-slogan">
            找到你的<span class="brand-slogan__accent">蜂巢</span>，<br />
            连接志趣相投的灵魂
          </h1>

          <p class="brand-desc">
            在 MintHive，每一个兴趣都是一枚蜂巢，等待与你共鸣。
          </p>

          <div class="brand-features">
            <div class="feature-card">
              <div class="feature-card__icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                  <circle cx="12" cy="12" r="10" />
                  <path d="M12 6v6l4 2" />
                </svg>
              </div>
              <div class="feature-card__body">
                <h4>兴趣圈子</h4>
                <p>加入蜂巢，与同好畅聊</p>
              </div>
            </div>
            <div class="feature-card">
              <div class="feature-card__icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                  <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z" />
                </svg>
              </div>
              <div class="feature-card__body">
                <h4>实时互动</h4>
                <p>即时消息，灵感零延迟</p>
              </div>
            </div>
            <div class="feature-card">
              <div class="feature-card__icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                  <path d="M12 2a7 7 0 017 7c0 5.25-7 13-7 13S5 14.25 5 9a7 7 0 017-7z" />
                  <circle cx="12" cy="9" r="2.5" />
                </svg>
              </div>
              <div class="feature-card__body">
                <h4>AI 助手</h4>
                <p>智能创作，灵感源源不断</p>
              </div>
            </div>
          </div>
        </div>
      </aside>

      <!-- 右侧表单区 -->
      <main class="reset-page__form-area">
        <div class="form-card">
          <!-- 移动端顶部Logo -->
          <div class="form-card__mobile-logo">
            <svg class="form-card__mobile-hex" viewBox="0 0 48 55" fill="none">
              <path
                d="M24 0L48 13.75V41.25L24 55L0 41.25V13.75L24 0Z"
                fill="url(#mhex-grad-r)"
              />
              <defs>
                <linearGradient id="mhex-grad-r" x1="0" y1="0" x2="48" y2="55">
                  <stop offset="0%" stop-color="#4ECDC4" />
                  <stop offset="100%" stop-color="#6BCB77" />
                </linearGradient>
              </defs>
            </svg>
            <span class="form-card__mobile-text">MintHive</span>
          </div>

          <!-- 页面标题与返回 -->
          <div class="form-card__header">
            <router-link to="/login" class="form-card__back">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M19 12H5M12 19l-7-7 7-7" />
              </svg>
              <span>返回登录</span>
            </router-link>
            <h2 class="form-card__title">忘记密码</h2>
          </div>

          <!-- 步骤指示器 -->
          <div class="step-indicator">
            <div
              :class="['step-dot', { 'step-dot--active': currentStep === 1, 'step-dot--done': currentStep > 1 }]"
            >
              <span class="step-dot__num">1</span>
            </div>
            <div class="step-line" :class="{ 'step-line--filled': currentStep > 1 }"></div>
            <div
              :class="['step-dot', { 'step-dot--active': currentStep === 2 }]"
            >
              <span class="step-dot__num">2</span>
            </div>
          </div>

          <el-form
            ref="formRef"
            :model="resetForm"
            :rules="formRules"
            class="auth-form"
          >
            <!-- Step 1: 验证手机号 + 输入验证码 -->
            <div v-show="currentStep === 1" class="form-section">
              <p class="step-desc">请输入注册时绑定的手机号，获取验证码验证身份</p>
              <el-form-item prop="phone">
                <el-input
                  v-model="resetForm.phone"
                  placeholder="请输入手机号"
                  :prefix-icon="Iphone"
                  size="large"
                  maxlength="11"
                />
              </el-form-item>
              <el-form-item prop="smsCode">
                <div class="sms-input">
                  <el-input
                    v-model="resetForm.smsCode"
                    placeholder="请输入验证码"
                    :prefix-icon="Message"
                    size="large"
                    maxlength="6"
                  />
                  <el-button
                    :disabled="smsCountdown > 0"
                    class="sms-input__btn"
                    @click="handleSendSms"
                  >
                    {{ smsCountdown > 0 ? `${smsCountdown}s` : '获取验证码' }}
                  </el-button>
                </div>
              </el-form-item>
              <el-button
                type="primary"
                class="submit-btn"
                :loading="loading"
                @click="handleNext"
              >
                下一步
              </el-button>
            </div>

            <!-- Step 2: 设置新密码 -->
            <div v-show="currentStep === 2" class="form-section">
              <p class="step-desc">请设置新密码，完成后需重新登录</p>
              <el-form-item prop="password">
                <el-input
                  v-model="resetForm.password"
                  type="password"
                  placeholder="请输入新密码（6-20位）"
                  :prefix-icon="Lock"
                  size="large"
                  show-password
                />
              </el-form-item>
              <el-form-item prop="confirmPassword">
                <el-input
                  v-model="resetForm.confirmPassword"
                  type="password"
                  placeholder="请确认新密码"
                  :prefix-icon="Lock"
                  size="large"
                  show-password
                />
              </el-form-item>
              <div class="step-actions">
                <el-button class="step-actions__prev" @click="handlePrev">上一步</el-button>
                <el-button
                  type="primary"
                  class="step-actions__next"
                  :loading="loading"
                  @click="handleSubmitReset"
                >
                  重置密码
                </el-button>
              </div>
            </div>
          </el-form>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, Iphone, Message } from '@element-plus/icons-vue'
import { sendSmsCode, resetPassword } from '@/api/auth'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()

const currentStep = ref(1)
const loading = ref(false)
const smsCountdown = ref(0)
let smsTimer: ReturnType<typeof setInterval> | null = null

const particleCanvas = ref<HTMLCanvasElement | null>(null)
let animationFrameId: number | null = null

const formRef = ref<FormInstance>()

const resetForm = reactive({
  phone: '',
  smsCode: '',
  password: '',
  confirmPassword: ''
})

const phoneReg = /^1[3-9]\d{9}$/

const validateConfirmPassword = (
  _rule: unknown,
  value: string,
  callback: (error?: Error) => void
) => {
  if (value !== resetForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const step1Rules: FormRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: phoneReg, message: '手机号格式不正确', trigger: 'blur' }
  ],
  smsCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位', trigger: 'blur' }
  ]
}

const step2Rules: FormRules = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const formRules = computed<FormRules>(() => {
  return currentStep.value === 1 ? step1Rules : step2Rules
})

const stepFields: Record<number, string[]> = {
  1: ['phone', 'smsCode'],
  2: ['password', 'confirmPassword']
}

async function handleSendSms(): Promise<void> {
  if (!phoneReg.test(resetForm.phone)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }

  try {
    await sendSmsCode(resetForm.phone, 'reset')
    ElMessage.success('验证码已发送')
    startSmsCountdown()
  } catch (error: unknown) {
    const msg = error instanceof Error ? error.message : '发送失败，请稍后重试'
    ElMessage.error(msg)
  }
}

function startSmsCountdown(): void {
  smsCountdown.value = 60
  smsTimer = setInterval(() => {
    smsCountdown.value--
    if (smsCountdown.value <= 0) {
      if (smsTimer) {
        clearInterval(smsTimer)
        smsTimer = null
      }
    }
  }, 1000)
}

async function handleNext(): Promise<void> {
  const form = formRef.value
  if (!form) return

  const fields = stepFields[currentStep.value]
  try {
    await form.validateField(fields)
  } catch {
    return
  }

  if (smsCountdown.value === 0 && !resetForm.smsCode) {
    ElMessage.warning('请先获取验证码')
    return
  }

  currentStep.value++
}

function handlePrev(): void {
  if (currentStep.value > 1) {
    currentStep.value--
  }
}

async function handleSubmitReset(): Promise<void> {
  const form = formRef.value
  if (!form) return

  const fields = stepFields[3]
  try {
    await form.validateField(fields)
  } catch {
    return
  }

  loading.value = true
  try {
    await resetPassword(resetForm.phone, resetForm.smsCode, resetForm.password)
    ElMessage.success('密码重置成功，请重新登录')
    await router.push('/login')
  } catch (error: unknown) {
    const msg = error instanceof Error ? error.message : '重置失败，请稍后重试'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}

interface Particle {
  x: number
  y: number
  vx: number
  vy: number
  size: number
  opacity: number
  color: string
}

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
  const count = Math.min(Math.floor(window.innerWidth / 25), 50)
  const colors = ['#4ECDC4', '#6BCB77', '#FFB627', '#8FE3D6']

  for (let i = 0; i < count; i++) {
    particles.push({
      x: Math.random() * canvas.width,
      y: Math.random() * canvas.height,
      vx: (Math.random() - 0.5) * 0.4,
      vy: (Math.random() - 0.5) * 0.4,
      size: Math.random() * 3 + 1,
      opacity: Math.random() * 0.5 + 0.1,
      color: colors[Math.floor(Math.random() * colors.length)]
    })
  }

  function drawHex(context: CanvasRenderingContext2D, cx: number, cy: number, s: number): void {
    context.beginPath()
    for (let i = 0; i < 6; i++) {
      const angle = (Math.PI / 3) * i - Math.PI / 6
      const px = cx + s * Math.cos(angle)
      const py = cy + s * Math.sin(angle)
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

    ctx.globalAlpha = 0.06
    ctx.strokeStyle = '#4ECDC4'
    ctx.lineWidth = 0.5
    for (let i = 0; i < particles.length; i++) {
      for (let j = i + 1; j < particles.length; j++) {
        const dx = particles[i].x - particles[j].x
        const dy = particles[i].y - particles[j].y
        const dist = Math.sqrt(dx * dx + dy * dy)
        if (dist < 120) {
          ctx.beginPath()
          ctx.moveTo(particles[i].x, particles[i].y)
          ctx.lineTo(particles[j].x, particles[j].y)
          ctx.stroke()
        }
      }
    }

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
  if (smsTimer) {
    clearInterval(smsTimer)
  }
})
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;
@use '@/styles/mixins.scss' as *;

.reset-page {
  position: fixed;
  inset: 0;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background: $ink-900;
}

.reset-page__bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.reset-page__canvas {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.reset-page__hex-grid {
  position: absolute;
  inset: 0;
  @include honeycomb-bg(rgba(78, 205, 196, 0.04), 40px);
}

.reset-page__glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(120px);

  &--mint {
    width: 500px;
    height: 500px;
    top: -10%;
    left: -5%;
    background: radial-gradient(circle, rgba(78, 205, 196, 0.15) 0%, transparent 70%);
  }

  &--amber {
    width: 400px;
    height: 400px;
    bottom: -5%;
    right: -5%;
    background: radial-gradient(circle, rgba(255, 182, 39, 0.1) 0%, transparent 70%);
  }
}

.reset-page__content {
  position: relative;
  z-index: 1;
  display: flex;
  height: 100%;
  width: 100%;
}

.reset-page__brand {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: $space-12 $space-10;

  @include mobile {
    display: none;
  }
}

.brand-inner {
  max-width: 480px;
  animation: fade-up 0.8s $ease-out both;
}

.brand-logo {
  display: flex;
  align-items: center;
  gap: $space-3;
  margin-bottom: $space-8;

  &__hex {
    width: 56px;
    height: 64px;
    animation: hex-breathe 4s ease-in-out infinite;
  }

  &__text {
    font-family: $font-display;
    font-size: 32px;
    font-weight: 700;
    background: $grad-aurora;
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
  }
}

.brand-slogan {
  font-family: $font-heading;
  font-size: 36px;
  font-weight: 700;
  color: #fff;
  line-height: 1.4;
  margin-bottom: $space-4;

  &__accent {
    background: $grad-mint;
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
  }
}

.brand-desc {
  font-size: 16px;
  color: $ink-300;
  line-height: 1.7;
  margin-bottom: $space-10;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: $space-4;
}

.feature-card {
  display: flex;
  align-items: center;
  gap: $space-4;
  padding: $space-4 $space-5;
  border-radius: $radius-md;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(78, 205, 196, 0.1);
  backdrop-filter: blur(8px);
  transition: all $dur-base $ease-out;

  &:hover {
    background: rgba(78, 205, 196, 0.06);
    border-color: rgba(78, 205, 196, 0.2);
    transform: translateX(4px);
  }

  &__icon {
    flex-shrink: 0;
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: $radius-sm;
    background: rgba(78, 205, 196, 0.1);
    color: $mint-500;

    svg {
      width: 20px;
      height: 20px;
    }
  }

  &__body {
    h4 {
      font-family: $font-heading;
      font-size: 15px;
      font-weight: 600;
      color: #fff;
      margin-bottom: 2px;
    }

    p {
      font-size: 13px;
      color: $ink-300;
    }
  }
}

.reset-page__form-area {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: $space-6;

  @include mobile {
    padding: $space-4;
  }
}

.form-card {
  width: 100%;
  max-width: 440px;
  padding: $space-8 $space-8 $space-6;
  border-radius: $radius-lg;
  @include glass(20px, rgba(26, 29, 46, 0.75));
  border: 1px solid rgba(78, 205, 196, 0.1);
  box-shadow: $shadow-lg, 0 0 60px rgba(78, 205, 196, 0.05);
  animation: scale-in 0.5s $ease-spring both;

  @include mobile {
    padding: $space-6 $space-5 $space-5;
    max-width: 100%;
  }

  &__mobile-logo {
    display: none;
    align-items: center;
    justify-content: center;
    gap: $space-2;
    margin-bottom: $space-6;

    @include mobile {
      display: flex;
    }
  }

  &__mobile-hex {
    width: 32px;
    height: 37px;
  }

  &__mobile-text {
    font-family: $font-display;
    font-size: 22px;
    font-weight: 700;
    background: $grad-aurora;
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
  }

  &__header {
    margin-bottom: $space-6;
  }

  &__back {
    display: inline-flex;
    align-items: center;
    gap: $space-1;
    font-size: 13px;
    color: $mint-500;
    text-decoration: none;
    margin-bottom: $space-3;
    transition: color $dur-fast $ease-out;

    svg {
      width: 16px;
      height: 16px;
    }

    &:hover {
      color: $mint-300;
    }
  }

  &__title {
    font-family: $font-heading;
    font-size: 24px;
    font-weight: 700;
    color: #fff;
  }
}

.step-indicator {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  margin-bottom: $space-6;
  padding: 0 $space-10;
}

.step-dot {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  color: $ink-500;
  border: 2px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.03);
  transition: all $dur-base $ease-out;
  z-index: 1;

  &--active {
    color: $ink-900;
    border-color: $mint-500;
    background: $grad-mint;
  }

  &--done {
    color: $ink-900;
    border-color: $mint-500;
    background: $mint-500;
  }
}

.step-line {
  position: absolute;
  top: 50%;
  height: 2px;
  width: 60px;
  left: calc(50% - 30px);
  background: rgba(255, 255, 255, 0.1);
  transform: translateY(-50%);
  transition: background $dur-base $ease-out;

  &--filled {
    background: $mint-500;
  }
}

.form-section {
  animation: fade-up 0.3s $ease-out both;
}

.step-desc {
  font-size: 14px;
  color: $ink-300;
  line-height: 1.6;
  margin-bottom: $space-5;
}

.auth-form {
  :deep(.el-form-item) {
    margin-bottom: $space-5;
  }

  :deep(.el-input__wrapper) {
    background: rgba(255, 255, 255, 0.05);
    border: 1px solid rgba(78, 205, 196, 0.1);
    border-radius: $radius-sm;
    box-shadow: none;
    transition: all $dur-fast $ease-out;

    &:hover {
      border-color: rgba(78, 205, 196, 0.25);
    }

    &.is-focus {
      border-color: $mint-500;
      box-shadow: 0 0 0 3px rgba(78, 205, 196, 0.12);
    }
  }

  :deep(.el-input__inner) {
    color: #fff;

    &::placeholder {
      color: $ink-500;
    }
  }

  :deep(.el-input__prefix .el-icon) {
    color: $ink-300;
  }
}

.sms-input {
  display: flex;
  gap: $space-3;
  width: 100%;

  .el-input {
    flex: 1;
  }

  &__btn {
    flex-shrink: 0;
    width: 110px;
    font-size: 13px;
    color: $mint-500;
    background: rgba(78, 205, 196, 0.08);
    border: 1px solid rgba(78, 205, 196, 0.2);
    border-radius: $radius-sm;

    &:hover:not(:disabled) {
      background: rgba(78, 205, 196, 0.15);
      border-color: rgba(78, 205, 196, 0.35);
    }

    &:disabled {
      color: $ink-500;
      background: rgba(255, 255, 255, 0.03);
      border-color: rgba(255, 255, 255, 0.06);
    }
  }
}

.submit-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: $radius-sm;
  background: $grad-mint;
  border: none;
  color: $ink-900;
  letter-spacing: 1px;
  transition: all $dur-base $ease-out;

  &:hover {
    transform: translateY(-1px);
    box-shadow: $shadow-glow-mint;
  }

  &:active {
    transform: translateY(0);
  }
}

.step-actions {
  display: flex;
  gap: $space-4;

  &__prev {
    flex: 1;
    height: 48px;
    font-size: 15px;
    font-weight: 600;
    border-radius: $radius-sm;
    background: rgba(255, 255, 255, 0.04);
    border: 1px solid rgba(78, 205, 196, 0.1);
    color: $ink-300;

    &:hover {
      background: rgba(255, 255, 255, 0.08);
      border-color: rgba(78, 205, 196, 0.2);
    }
  }

  &__next {
    flex: 2;
    height: 48px;
    font-size: 16px;
    font-weight: 600;
    border-radius: $radius-sm;
    background: $grad-mint;
    border: none;
    color: $ink-900;
    letter-spacing: 1px;
    transition: all $dur-base $ease-out;

    &:hover {
      transform: translateY(-1px);
      box-shadow: $shadow-glow-mint;
    }

    &:active {
      transform: translateY(0);
    }
  }
}

@keyframes fade-up {
  from { opacity: 0; transform: translateY(24px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes scale-in {
  from { opacity: 0; transform: scale(0.92); }
  to { opacity: 1; transform: scale(1); }
}

@keyframes hex-breathe {
  0%, 100% { transform: scale(1); opacity: 0.9; }
  50% { transform: scale(1.06); opacity: 1; }
}

@include mobile {
  .reset-page {
    padding-top: env(safe-area-inset-top);
  }
}
</style>
