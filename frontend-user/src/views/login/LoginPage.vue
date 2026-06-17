<template>
  <div class="login-page">
    <!-- 蜂巢粒子背景 -->
    <div class="login-page__bg">
      <canvas ref="particleCanvas" class="login-page__canvas"></canvas>
      <div class="login-page__hex-grid"></div>
      <div class="login-page__glow login-page__glow--mint"></div>
      <div class="login-page__glow login-page__glow--amber"></div>
    </div>

    <div class="login-page__content">
      <!-- 左侧品牌展示区（桌面端） -->
      <aside class="login-page__brand">
        <div class="brand-inner">
          <!-- Logo -->
          <div class="brand-logo">
            <svg class="brand-logo__hex" viewBox="0 0 120 138" fill="none">
              <path
                d="M60 0L120 34.5V103.5L60 138L0 103.5V34.5L60 0Z"
                fill="url(#hex-grad)"
                opacity="0.9"
              />
              <path
                d="M60 12L110 42V102L60 132L10 102V42L60 12Z"
                stroke="rgba(78,205,196,0.4)"
                stroke-width="1.5"
                fill="none"
              />
              <defs>
                <linearGradient id="hex-grad" x1="0" y1="0" x2="120" y2="138">
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

          <!-- 3个核心特性卡片 -->
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
      <main class="login-page__form-area">
        <div class="form-card">
          <!-- 移动端顶部Logo -->
          <div class="form-card__mobile-logo">
            <svg class="form-card__mobile-hex" viewBox="0 0 48 55" fill="none">
              <path
                d="M24 0L48 13.75V41.25L24 55L0 41.25V13.75L24 0Z"
                fill="url(#mhex-grad)"
              />
              <defs>
                <linearGradient id="mhex-grad" x1="0" y1="0" x2="48" y2="55">
                  <stop offset="0%" stop-color="#4ECDC4" />
                  <stop offset="100%" stop-color="#6BCB77" />
                </linearGradient>
              </defs>
            </svg>
            <span class="form-card__mobile-text">MintHive</span>
          </div>

          <!-- Tab 切换 -->
          <div class="form-card__tabs">
            <button
              :class="['tab-btn', { 'tab-btn--active': activeTab === 'login' }]"
              @click="activeTab = 'login'"
            >
              登录
            </button>
            <button
              :class="['tab-btn', { 'tab-btn--active': activeTab === 'register' }]"
              @click="activeTab = 'register'"
            >
              注册
            </button>
            <div
              class="form-card__tab-indicator"
              :style="{ transform: activeTab === 'register' ? 'translateX(100%)' : 'translateX(0)' }"
            ></div>
          </div>

          <!-- 登录表单 -->
          <div v-show="activeTab === 'login'" class="form-section">
            <!-- 登录方式切换 -->
            <div class="login-mode-switch">
              <button
                :class="['mode-btn', { 'mode-btn--active': loginMode === 'password' }]"
                @click="loginMode = 'password'"
              >
                密码登录
              </button>
              <button
                :class="['mode-btn', { 'mode-btn--active': loginMode === 'sms' }]"
                @click="loginMode = 'sms'"
              >
                验证码登录
              </button>
            </div>

            <el-form
              ref="loginFormRef"
              :model="loginForm"
              :rules="loginRules"
              class="auth-form"
              @keyup.enter="handleLogin"
            >
              <!-- 密码登录 -->
              <template v-if="loginMode === 'password'">
                <el-form-item prop="account">
                  <el-input
                    v-model="loginForm.account"
                    placeholder="请输入账号/手机号"
                    :prefix-icon="User"
                    size="large"
                  />
                </el-form-item>
                <el-form-item prop="password">
                  <el-input
                    v-model="loginForm.password"
                    type="password"
                    placeholder="请输入密码"
                    :prefix-icon="Lock"
                    size="large"
                    show-password
                  />
                </el-form-item>
              </template>

              <!-- 验证码登录 -->
              <template v-else>
                <el-form-item prop="phone">
                  <el-input
                    v-model="loginForm.phone"
                    placeholder="请输入手机号"
                    :prefix-icon="Iphone"
                    size="large"
                    maxlength="11"
                  />
                </el-form-item>
                <el-form-item prop="smsCode">
                  <div class="sms-input">
                    <el-input
                      v-model="loginForm.smsCode"
                      placeholder="请输入验证码"
                      :prefix-icon="Message"
                      size="large"
                      maxlength="6"
                    />
                    <el-button
                      :disabled="smsCountdown > 0"
                      class="sms-input__btn"
                      @click="handleSendSms('login')"
                    >
                      {{ smsCountdown > 0 ? `${smsCountdown}s` : '获取验证码' }}
                    </el-button>
                  </div>
                </el-form-item>
              </template>

              <div class="form-extra">
                <el-checkbox v-model="rememberMe" class="form-extra__remember">
                  记住我
                </el-checkbox>
                <a class="form-extra__forgot" @click="handleForgotPassword">忘记密码？</a>
              </div>

              <el-button
                type="primary"
                class="submit-btn"
                :loading="loginLoading"
                @click="handleLogin"
              >
                登录
              </el-button>
            </el-form>

            <!-- 第三方登录 -->
            <div class="third-party">
              <div class="third-party__divider">
                <span>其他方式登录</span>
              </div>
              <div class="third-party__list">
                <button class="third-party__item" title="微信登录">
                  <svg viewBox="0 0 24 24" fill="currentColor">
                    <path
                      d="M8.691 2.188C3.891 2.188 0 5.476 0 9.53c0 2.212 1.17 4.203 3.002 5.55a.59.59 0 01.213.665l-.39 1.48c-.019.07-.048.141-.048.213 0 .163.13.295.29.295a.326.326 0 00.167-.054l1.903-1.114a.864.864 0 01.717-.098 10.16 10.16 0 002.837.403c.276 0 .543-.027.811-.05-.857-2.578.157-4.972 1.932-6.446 1.703-1.415 3.882-1.98 5.853-1.838-.576-3.583-4.196-6.348-8.596-6.348zM5.785 5.991c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 01-1.162 1.178A1.17 1.17 0 014.623 7.17c0-.651.52-1.18 1.162-1.18zm5.813 0c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 01-1.162 1.178 1.17 1.17 0 01-1.162-1.178c0-.651.52-1.18 1.162-1.18zm5.34 2.867c-1.797-.052-3.746.512-5.28 1.786-1.72 1.428-2.687 3.72-1.78 6.22.942 2.453 3.666 4.229 6.884 4.229.826 0 1.622-.12 2.361-.336a.722.722 0 01.598.082l1.584.926a.272.272 0 00.14.045c.133 0 .241-.108.241-.243 0-.06-.024-.118-.04-.177l-.325-1.233a.492.492 0 01.177-.554C23.016 18.842 24 17.08 24 15.109c0-3.407-3.159-6.199-7.062-6.251zM14.53 13.39c.535 0 .969.44.969.982a.976.976 0 01-.969.983.976.976 0 01-.969-.983c0-.542.434-.982.97-.982zm4.844 0c.535 0 .969.44.969.982a.976.976 0 01-.969.983.976.976 0 01-.969-.983c0-.542.434-.982.97-.982z"
                    />
                  </svg>
                  <span class="third-party__label">微信</span>
                </button>
              </div>
            </div>
          </div>

          <!-- 注册表单 -->
          <div v-show="activeTab === 'register'" class="form-section">
            <el-form
              ref="registerFormRef"
              :model="registerForm"
              :rules="registerRules"
              class="auth-form"
              @keyup.enter="handleRegister"
            >
              <el-form-item prop="account">
                <el-input
                  v-model="registerForm.account"
                  placeholder="请输入账号（4-20位字母数字）"
                  :prefix-icon="User"
                  size="large"
                  maxlength="20"
                />
              </el-form-item>
              <el-form-item prop="password">
                <el-input
                  v-model="registerForm.password"
                  type="password"
                  placeholder="请输入密码（6-20位）"
                  :prefix-icon="Lock"
                  size="large"
                  show-password
                />
              </el-form-item>
              <el-form-item prop="confirmPassword">
                <el-input
                  v-model="registerForm.confirmPassword"
                  type="password"
                  placeholder="请确认密码"
                  :prefix-icon="Lock"
                  size="large"
                  show-password
                />
              </el-form-item>
              <el-form-item prop="phone">
                <el-input
                  v-model="registerForm.phone"
                  placeholder="请输入手机号"
                  :prefix-icon="Iphone"
                  size="large"
                  maxlength="11"
                />
              </el-form-item>
              <el-form-item prop="smsCode">
                <div class="sms-input">
                  <el-input
                    v-model="registerForm.smsCode"
                    placeholder="请输入验证码"
                    :prefix-icon="Message"
                    size="large"
                    maxlength="6"
                  />
                  <el-button
                    :disabled="smsCountdown > 0"
                    class="sms-input__btn"
                    @click="handleSendSms('register')"
                  >
                    {{ smsCountdown > 0 ? `${smsCountdown}s` : '获取验证码' }}
                  </el-button>
                </div>
              </el-form-item>

              <el-form-item prop="agreement" class="agreement-item">
                <el-checkbox v-model="registerForm.agreement">
                  我已阅读并同意
                  <a class="agreement-link" @click.stop>《用户协议》</a>
                  和
                  <a class="agreement-link" @click.stop>《隐私政策》</a>
                </el-checkbox>
              </el-form-item>

              <el-button
                type="primary"
                class="submit-btn"
                :loading="registerLoading"
                @click="handleRegister"
              >
                注册
              </el-button>
            </el-form>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Iphone, Message } from '@element-plus/icons-vue'
import { loginByPassword, loginBySms, register, sendSmsCode } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import { setToken } from '@/utils/token'
import type { FormInstance, FormRules } from 'element-plus'

// ============================================================
// 路由与Store
// ============================================================
const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// ============================================================
// 状态定义
// ============================================================
const activeTab = ref<'login' | 'register'>('login')
const loginMode = ref<'password' | 'sms'>('password')
const loginLoading = ref(false)
const registerLoading = ref(false)
const rememberMe = ref(false)
const smsCountdown = ref(0)
let smsTimer: ReturnType<typeof setInterval> | null = null

// ============================================================
// 粒子画布
// ============================================================
const particleCanvas = ref<HTMLCanvasElement | null>(null)
let animationFrameId: number | null = null

// ============================================================
// 表单引用
// ============================================================
const loginFormRef = ref<FormInstance>()
const registerFormRef = ref<FormInstance>()

// ============================================================
// 登录表单数据
// ============================================================
const loginForm = reactive({
  account: '',
  password: '',
  phone: '',
  smsCode: ''
})

// ============================================================
// 注册表单数据
// ============================================================
const registerForm = reactive({
  account: '',
  password: '',
  confirmPassword: '',
  phone: '',
  smsCode: '',
  agreement: false
})

// ============================================================
// 手机号校验正则
// ============================================================
const phoneReg = /^1[3-9]\d{9}$/

// ============================================================
// 登录表单校验规则
// ============================================================
const loginRules = computed<FormRules>(() => {
  if (loginMode.value === 'password') {
    return {
      account: [
        { required: true, message: '请输入账号', trigger: 'blur' },
        { min: 4, max: 20, message: '账号长度为4-20位', trigger: 'blur' }
      ],
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 6, max: 20, message: '密码长度为6-20位', trigger: 'blur' }
      ]
    }
  }
  return {
    phone: [
      { required: true, message: '请输入手机号', trigger: 'blur' },
      { pattern: phoneReg, message: '手机号格式不正确', trigger: 'blur' }
    ],
    smsCode: [
      { required: true, message: '请输入验证码', trigger: 'blur' },
      { len: 6, message: '验证码为6位', trigger: 'blur' }
    ]
  }
})

// ============================================================
// 确认密码校验器
// ============================================================
const validateConfirmPassword = (
  _rule: unknown,
  value: string,
  callback: (error?: Error) => void
) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

// ============================================================
// 用户协议校验器
// ============================================================
const validateAgreement = (
  _rule: unknown,
  value: boolean,
  callback: (error?: Error) => void
) => {
  if (!value) {
    callback(new Error('请阅读并同意用户协议'))
  } else {
    callback()
  }
}

// ============================================================
// 注册表单校验规则
// ============================================================
const registerRules: FormRules = {
  account: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 4, max: 20, message: '账号长度为4-20位', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '账号只能包含字母、数字和下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: phoneReg, message: '手机号格式不正确', trigger: 'blur' }
  ],
  smsCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位', trigger: 'blur' }
  ],
  agreement: [{ validator: validateAgreement, trigger: 'change' }]
}

/**
 * 处理登录操作
 * @description 根据当前登录模式（密码/验证码）调用对应API，成功后存储Token并跳转
 * @returns {Promise<void>}
 * @throws 登录失败时通过ElMessage提示用户
 */
async function handleLogin(): Promise<void> {
  const formRef = loginFormRef.value
  if (!formRef) return

  await formRef.validate(async (valid) => {
    if (!valid) return

    loginLoading.value = true
    try {
      let res
      if (loginMode.value === 'password') {
        res = await loginByPassword(loginForm.account, loginForm.password)
      } else {
        res = await loginBySms(loginForm.phone, loginForm.smsCode)
      }

      setToken(res.token)
      userStore.setUser(res.user)

      ElMessage.success('登录成功')

      // 跳转到 redirect 参数页面或首页
      const redirect = (route.query.redirect as string) || '/'
      await router.push(redirect)
    } catch (error: unknown) {
      const msg = error instanceof Error ? error.message : '登录失败，请稍后重试'
      ElMessage.error(msg)
    } finally {
      loginLoading.value = false
    }
  })
}

/**
 * 处理注册操作
 * @description 校验注册表单后调用注册API，成功后存储Token并跳转到兴趣标签选择页
 * @returns {Promise<void>}
 * @throws 注册失败时通过ElMessage提示用户
 */
async function handleRegister(): Promise<void> {
  const formRef = registerFormRef.value
  if (!formRef) return

  await formRef.validate(async (valid) => {
    if (!valid) return

    registerLoading.value = true
    try {
      const res = await register({
        account: registerForm.account,
        password: registerForm.password,
        phone: registerForm.phone,
        code: registerForm.smsCode
      })

      setToken(res.token)
      userStore.setUser(res.user)

      ElMessage.success('注册成功')
      await router.push('/interest-select')
    } catch (error: unknown) {
      const msg = error instanceof Error ? error.message : '注册失败，请稍后重试'
      ElMessage.error(msg)
    } finally {
      registerLoading.value = false
    }
  })
}

/**
 * 发送短信验证码
 * @param {'login' | 'register'} scene - 发送场景，登录或注册
 * @returns {Promise<void>}
 * @throws 发送失败时通过ElMessage提示用户
 * @note 发送成功后启动60秒倒计时，倒计时期间按钮禁用
 */
async function handleSendSms(scene: 'login' | 'register'): Promise<void> {
  const phone = loginMode.value === 'password' || activeTab.value === 'register'
    ? (activeTab.value === 'register' ? registerForm.phone : loginForm.phone)
    : loginForm.phone

  if (!phoneReg.test(phone)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }

  try {
    await sendSmsCode(phone, scene)
    ElMessage.success('验证码已发送')
    startSmsCountdown()
  } catch (error: unknown) {
    const msg = error instanceof Error ? error.message : '发送失败，请稍后重试'
    ElMessage.error(msg)
  }
}

/**
 * 启动短信验证码倒计时
 * @description 设置60秒倒计时，每秒递减，到0时清除定时器
 * @returns {void}
 */
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

/**
 * 处理忘记密码
 * @description 跳转到忘记密码页面（预留）
 * @returns {void}
 */
function handleForgotPassword(): void {
  ElMessage.info('忘记密码功能开发中')
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

    // 绘制粒子间连线
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

// ============================================================
// 登录注册页 - 暗色沉浸式 + 蜂巢装饰 + 粒子光效 + 毛玻璃
// ============================================================

.login-page {
  position: relative;
  width: 100%;
  height: 100vh;
  overflow: hidden;
  background: $ink-900;
}

// ---------- 背景层 ----------
.login-page__bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.login-page__canvas {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.login-page__hex-grid {
  position: absolute;
  inset: 0;
  @include honeycomb-bg(rgba(78, 205, 196, 0.04), 40px);
}

.login-page__glow {
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

// ---------- 内容区 ----------
.login-page__content {
  position: relative;
  z-index: 1;
  display: flex;
  height: 100%;
  width: 100%;
}

// ---------- 左侧品牌区 ----------
.login-page__brand {
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

// ---------- 特性卡片 ----------
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

// ---------- 右侧表单区 ----------
.login-page__form-area {
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

  // 移动端Logo
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

  // Tab 切换
  &__tabs {
    position: relative;
    display: flex;
    margin-bottom: $space-6;
    background: rgba(255, 255, 255, 0.04);
    border-radius: $radius-sm;
    padding: 3px;
  }

  &__tab-indicator {
    position: absolute;
    top: 3px;
    left: 3px;
    width: calc(50% - 3px);
    height: calc(100% - 6px);
    background: $grad-mint;
    border-radius: $radius-xs;
    transition: transform $dur-base $ease-spring;
    z-index: 0;
  }
}

.tab-btn {
  flex: 1;
  position: relative;
  z-index: 1;
  padding: $space-2 $space-4;
  font-size: 15px;
  font-weight: 600;
  color: $ink-300;
  background: none;
  border: none;
  cursor: pointer;
  transition: color $dur-fast $ease-out;
  border-radius: $radius-xs;

  &--active {
    color: $ink-900;
  }
}

// ---------- 登录方式切换 ----------
.login-mode-switch {
  display: flex;
  gap: $space-4;
  margin-bottom: $space-5;
}

.mode-btn {
  font-size: 13px;
  color: $ink-300;
  background: none;
  border: none;
  cursor: pointer;
  padding-bottom: $space-1;
  border-bottom: 2px solid transparent;
  transition: all $dur-fast $ease-out;

  &--active {
    color: $mint-500;
    border-bottom-color: $mint-500;
  }

  &:hover {
    color: $mint-300;
  }
}

// ---------- 表单样式 ----------
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

// ---------- 表单附加区域 ----------
.form-extra {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: $space-5;

  &__remember {
    :deep(.el-checkbox__label) {
      color: $ink-300;
      font-size: 13px;
    }
  }

  &__forgot {
    font-size: 13px;
    color: $mint-500;
    cursor: pointer;
    transition: color $dur-fast $ease-out;

    &:hover {
      color: $mint-300;
    }
  }
}

// ---------- 提交按钮 ----------
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

// ---------- 用户协议 ----------
.agreement-item {
  :deep(.el-checkbox__label) {
    color: $ink-300;
    font-size: 13px;
  }
}

.agreement-link {
  color: $mint-500;
  cursor: pointer;

  &:hover {
    text-decoration: underline;
  }
}

// ---------- 第三方登录 ----------
.third-party {
  margin-top: $space-6;

  &__divider {
    display: flex;
    align-items: center;
    margin-bottom: $space-5;

    &::before,
    &::after {
      content: '';
      flex: 1;
      height: 1px;
      background: rgba(255, 255, 255, 0.06);
    }

    span {
      padding: 0 $space-4;
      font-size: 12px;
      color: $ink-500;
      white-space: nowrap;
    }
  }

  &__list {
    display: flex;
    justify-content: center;
    gap: $space-6;
  }

  &__item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: $space-1;
    padding: $space-3;
    border-radius: $radius-sm;
    background: rgba(255, 255, 255, 0.04);
    border: 1px solid rgba(255, 255, 255, 0.06);
    cursor: pointer;
    transition: all $dur-fast $ease-out;

    svg {
      width: 24px;
      height: 24px;
      color: #07C160;
    }

    &:hover {
      background: rgba(255, 255, 255, 0.08);
      border-color: rgba(78, 205, 196, 0.15);
      transform: translateY(-2px);
    }
  }

  &__label {
    font-size: 11px;
    color: $ink-500;
  }
}

// ---------- 动画关键帧引用 ----------
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
</style>
