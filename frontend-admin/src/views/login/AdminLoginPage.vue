<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { login, type LoginParams } from '@/api/auth'
import { clearToken } from '@/api/request'
import { useAdminStore } from '@/stores/admin'
import HexagonLogo from '@/components/HexagonLogo.vue'

/**
 * AdminLoginPage 管理员登录页
 * 功能：深色专业风登录，账号密码登录，登录成功跳转后台
 * 参数：无
 * 注意事项：表单校验通过后调用登录接口，Token 存入 store
 */
const route = useRoute()
const router = useRouter()
const adminStore = useAdminStore()

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
      // 3. 用 Token 拉取管理员信息（验证 Token 有效性）
      await adminStore.fetchAdminInfo()
      // 4. 全部成功后跳转
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
  background: $bg-base;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
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
  opacity: 0.3;
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
  opacity: 0.15;
}

.login-container {
  position: relative;
  z-index: 1;
  width: 880px;
  max-width: 92%;
  height: 520px;
  background: $bg-elevated;
  border: 1px solid $border-base;
  border-radius: $radius-xl;
  overflow: hidden;
  display: flex;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
}

// 品牌区 — 暮光玫瑰深灰底座
.brand-panel {
  width: 44%;
  background: linear-gradient(160deg, $bg-elevated 0%, $bg-base 100%);
  padding: 48px 40px;
  display: flex;
  flex-direction: column;
  position: relative;
  border-right: 1px solid $border-base;

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
  color: $text-primary;
  letter-spacing: 1px;
}
.brand-subtitle {
  font-size: 13px;
  color: $text-secondary;
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
  color: $text-regular;
  font-size: 14px;
  .el-icon {
    color: $color-primary;
    font-size: 18px;
  }
}
.brand-footer {
  margin-top: auto;
  font-size: 11px;
  color: $text-placeholder;
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
  color: $text-primary;
}
.form-desc {
  font-size: 13px;
  color: $text-secondary;
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
  color: $text-placeholder;
  line-height: 1.5;
  .el-icon {
    flex-shrink: 0;
    margin-top: 1px;
  }
}
</style>
