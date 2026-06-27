<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { login, type LoginParams } from '@/api/auth'
import { clearToken } from '@/api/request'
import { useAdminStore } from '@/stores/admin'
import HexagonLogo from '@/components/HexagonLogo.vue'

const adminStore = useAdminStore()

/**
 * AdminLoginPage 管理员登录页
 * 功能：深色专业风登录，账号密码登录，登录成功跳转后台
 * 参数：无
 * 注意事项：表单校验通过后调用登录接口，Token 存入 store
 */
const route = useRoute()
const router = useRouter()

const loginFormRef = ref<FormInstance>()
const loading = ref(false)

const loginForm = reactive<LoginParams>({
  account: '',
  password: '',
  captcha: ''
})

/** 表单校验规则 */
const rules: FormRules = {
  account: [
    { required: true, message: '请输入管理员账号', trigger: 'blur' },
    { min: 3, max: 32, message: '账号长度 3-32 位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度 6-32 位', trigger: 'blur' }
  ]
}

/**
 * 提交登录
 * @param formEl 表单实例
 */
async function handleLogin(formEl: FormInstance | undefined) {
  if (!formEl) return
  await formEl.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      // 1. 调用登录接口获取 Token
      const res = await login(loginForm)
      // 2. 存储 Token
      adminStore.setAdminToken(res.token)
      // 3. 用 Token 拉取管理员信息（含角色）
      const info = await adminStore.fetchAdminInfo()
      // 4. 校验角色：仅 ROLE_ADMIN(2) 及以上可进入后台
      if (info && Number(info.role) < 2) {
        adminStore.setAdminToken('')
        adminStore.adminInfo = null
        clearToken()
        ElMessage.error('该账号无管理员权限')
        return
      }
      // 5. 全部成功后跳转
      ElMessage.success('登录成功，欢迎回来')
      const redirect = (route.query.redirect as string) || '/dashboard'
      router.push(redirect)
    } catch (e) {
      // 登录或拉取信息失败，仅清除本地残留 Token（不调用登出接口，避免二次报错）
      adminStore.setAdminToken('')
      adminStore.adminInfo = null
      clearToken()
    } finally {
      loading.value = false
    }
  })
}
</script>

<template>
  <div class="login-page">
    <!-- 主题切换按钮 -->
    <button
      class="theme-toggle"
      :class="{ 'theme-toggle--light': adminStore.theme === 'light' }"
      @click="adminStore.setTheme()"
      :aria-label="adminStore.theme === 'dark' ? '切换到浅色模式' : '切换到深色模式'"
    >
      <svg v-if="adminStore.theme === 'dark'" key="sun" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <circle cx="12" cy="12" r="5" />
        <path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42" />
      </svg>
      <svg v-else key="moon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M21 12.79A9 9 0 1111.21 3 7 7 0 0021 12.79z" />
      </svg>
    </button>
    <!-- 背景蜂巢装饰 -->
    <div class="bg-hex-grid"></div>
    <div class="bg-glow glow-1"></div>
    <div class="bg-glow glow-2"></div>

    <div class="login-container">
      <!-- 左侧品牌区 -->
      <div class="brand-panel">
        <HexagonLogo :size="56" />
        <h1 class="brand-title">MintHive Admin</h1>
        <p class="brand-subtitle">兴趣社交平台 · 管理控制台</p>
        <div class="brand-features">
          <div class="feature-item">
            <el-icon><DataAnalysis /></el-icon>
            <span>社区健康度实时监控</span>
          </div>
          <div class="feature-item">
            <el-icon><Warning /></el-icon>
            <span>AI 风险工单智能分拣</span>
          </div>
          <div class="feature-item">
            <el-icon><MagicStick /></el-icon>
            <span>AI 日报辅助运营决策</span>
          </div>
        </div>
        <div class="brand-footer mono">© 2026 MintHive · 私有化部署</div>
      </div>

      <!-- 右侧登录表单 -->
      <div class="form-panel">
        <div class="form-wrapper">
          <h2 class="form-title">管理员登录</h2>
          <p class="form-desc">请使用管理员账号登录控制台</p>

          <el-form
            ref="loginFormRef"
            :model="loginForm"
            :rules="rules"
            size="large"
            @keyup.enter="handleLogin(loginFormRef)"
          >
            <el-form-item prop="account">
              <el-input
                v-model="loginForm.account"
                placeholder="管理员账号"
                :prefix-icon="'User'"
                clearable
              />
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="登录密码"
                :prefix-icon="'Lock'"
                show-password
                clearable
              />
            </el-form-item>

            <el-button
              type="primary"
              class="login-btn"
              :loading="loading"
              @click="handleLogin(loginFormRef)"
            >
              <span v-if="!loading">登 录</span>
              <span v-else>登录中...</span>
            </el-button>
          </el-form>

          <div class="form-tips">
            <el-icon><InfoFilled /></el-icon>
            <span>本系统仅供授权管理员使用，所有操作均被审计记录</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.login-page {
  position: relative;
  width: 100%;
  height: 100%;
  // CSS 变量，默认暗色值
  --lp-bg: #{$bg-base};
  --lp-elevated: #{$bg-elevated};
  --lp-border: #{$border-base};
  --lp-text-primary: #{$text-primary};
  --lp-text-secondary: #{$text-secondary};
  --lp-text-regular: #{$text-regular};
  --lp-text-placeholder: #{$text-placeholder};
  --lp-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
  background: var(--lp-bg);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.3s ease;
}

// 主题切换按钮
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
  color: #A1A1AA;
  cursor: pointer;
  backdrop-filter: blur(8px);
  transition: all 0.32s cubic-bezier(0.22, 1, 0.36, 1);

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
    transition: transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  }

  &--light {
    background: rgba(0, 0, 0, 0.06);
    border-color: rgba(0, 0, 0, 0.08);
    color: #71717A;

    &:hover {
      background: rgba(0, 0, 0, 0.1);
      color: #18181b;
    }
  }

  &:active svg {
    transform: rotate(360deg);
  }
}

// 背景蜂巢网格 — 暮光玫瑰色调
.bg-hex-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(30deg, rgba(232, 121, 169, 0.04) 12%, transparent 12.5%, transparent 87%, rgba(232, 121, 169, 0.04) 87.5%),
    linear-gradient(150deg, rgba(232, 121, 169, 0.04) 12%, transparent 12.5%, transparent 87%, rgba(232, 121, 169, 0.04) 87.5%),
    linear-gradient(30deg, rgba(245, 158, 11, 0.03) 12%, transparent 12.5%, transparent 87%, rgba(245, 158, 11, 0.03) 87.5%),
    linear-gradient(150deg, rgba(245, 158, 11, 0.03) 12%, transparent 12.5%, transparent 87%, rgba(245, 158, 11, 0.03) 87.5%);
  background-size: 60px 105px;
  background-position: 0 0, 0 0, 30px 52px, 30px 52px;
  opacity: 0.6;
}
.bg-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.18;
}
.glow-1 {
  width: 400px; height: 400px;
  background: $color-primary;
  top: -100px; left: -100px;
}
.glow-2 {
  width: 500px; height: 500px;
  background: $color-warning;
  bottom: -150px; right: -150px;
  opacity: 0.1;
}

.login-container {
  position: relative;
  z-index: 1;
  width: 880px;
  max-width: 92%;
  height: 520px;
  background: var(--lp-elevated);
  border: 1px solid var(--lp-border);
  border-radius: $radius-xl;
  overflow: hidden;
  display: flex;
  box-shadow: var(--lp-shadow);
}

// 品牌区 — 暮光玫瑰深灰底座
.brand-panel {
  width: 44%;
  background: linear-gradient(160deg, var(--lp-elevated) 0%, var(--lp-bg) 100%);
  padding: 48px 40px;
  display: flex;
  flex-direction: column;
  position: relative;
  border-right: 1px solid var(--lp-border);

  &::before {
    content: '';
    position: absolute;
    top: 0; right: 0;
    width: 2px; height: 100%;
    background: linear-gradient(180deg, transparent, $color-primary, transparent);
    opacity: 0.5;
  }
}
.brand-title {
  font-size: 28px;
  font-weight: 700;
  margin-top: 24px;
  color: var(--lp-text-primary);
  letter-spacing: 1px;
}
.brand-subtitle {
  font-size: 13px;
  color: var(--lp-text-secondary);
  margin-top: 8px;
  letter-spacing: 1px;
}
.brand-features {
  margin-top: 48px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--lp-text-regular);
  font-size: 14px;
  .el-icon {
    color: $color-primary;
    font-size: 18px;
  }
}
.brand-footer {
  margin-top: auto;
  font-size: 11px;
  color: var(--lp-text-placeholder);
  letter-spacing: 1px;
}

// 表单区
.form-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}
.form-wrapper {
  width: 100%;
  max-width: 340px;
}
.form-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--lp-text-primary);
}
.form-desc {
  font-size: 13px;
  color: var(--lp-text-secondary);
  margin-top: 6px;
  margin-bottom: 32px;
}
.login-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 4px;
  margin-top: 8px;
}
.form-tips {
  margin-top: 24px;
  display: flex;
  align-items: flex-start;
  gap: 6px;
  font-size: 11px;
  color: var(--lp-text-placeholder);
  line-height: 1.5;
  .el-icon {
    flex-shrink: 0;
    margin-top: 1px;
  }
}
</style>

<style lang="scss">
// ============================================================
// 非 scoped 样式 — 登录页主题切换 CSS 变量覆盖
// ============================================================

[data-theme="light"] .login-page {
  --lp-bg: #F5F5F4;
  --lp-elevated: #FFFFFF;
  --lp-border: #E4E4E7;
  --lp-text-primary: #18181b;
  --lp-text-secondary: #52525b;
  --lp-text-regular: #3f3f46;
  --lp-text-placeholder: #A1A1AA;
  --lp-shadow: 0 20px 60px rgba(0, 0, 0, 0.1);

  // 登录容器卡片阴影在浅色下更轻
  .login-container {
    box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
  }

  // 背景发光在浅色下仍然可见
  .bg-glow {
    opacity: 0.25;
  }

  // 蜂巢网格在浅色背景下提高可见度
  .bg-hex-grid {
    opacity: 0.3;
  }

  // Element Plus 输入框浅色覆盖（修复暗色 !important 残留）
  .el-input__wrapper {
    background: #fff !important;
    box-shadow: 0 0 0 1px #E4E4E7 inset !important;
  }
  .el-input__wrapper.is-focus {
    box-shadow: 0 0 0 1px #E879A9 inset !important;
  }
  .el-input__inner {
    color: #18181b !important;
  }
  .el-input__inner::placeholder {
    color: #A1A1AA !important;
  }
  .el-input__prefix .el-icon {
    color: #A1A1AA !important;
  }
}
</style>
