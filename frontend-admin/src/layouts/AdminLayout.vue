<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useAdminStore } from '@/stores/admin'
import HexagonLogo from '@/components/HexagonLogo.vue'

/**
 * AdminLayout 管理后台主布局
 * 功能：左侧固定侧边栏（蜂巢 Logo + 菜单）+ 顶部用户信息 + 主内容区
 * 参数：无
 * 注意事项：菜单项根据路由 meta.icon 渲染图标，选中态薄荷绿高亮
 */
const route = useRoute()
const router = useRouter()
const adminStore = useAdminStore()

/** 菜单项配置 */
const menuItems = [
  { index: '/dashboard', title: '数据大屏', icon: 'DataAnalysis' },
  { index: '/user', title: '用户管理', icon: 'User' },
  { index: '/content', title: '内容审核', icon: 'Document' },
  { index: '/circle', title: '圈子管理', icon: 'Connection' },
  { index: '/report', title: '举报工单', icon: 'Warning' },
  { index: '/config', title: '系统配置', icon: 'Setting' }
]

/** 当前激活菜单 */
const activeMenu = computed(() => {
  const path = route.path
  const matched = menuItems.find((m) => path.startsWith(m.index))
  return matched?.index || '/dashboard'
})

/** 当前页面标题 */
const currentTitle = computed(() => route.meta.title as string || '')

/** 管理员昵称 */
const adminName = computed(() => adminStore.adminInfo?.nickname || '管理员')

/**
 * 退出登录确认
 */
async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await adminStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } catch {
    // 用户取消
  }
}

/**
 * 切换全屏
 */
function handleFullscreen() {
  if (document.fullscreenElement) {
    document.exitFullscreen?.()
  } else {
    document.documentElement.requestFullscreen?.()
  }
}
</script>

<template>
  <div class="admin-layout">
    <!-- 侧边栏 -->
    <aside class="sidebar">
      <div class="sidebar-logo">
        <HexagonLogo :size="36" />
      </div>
      <nav class="sidebar-menu">
        <router-link
          v-for="item in menuItems"
          :key="item.index"
          :to="item.index"
          class="menu-item"
          :class="{ active: activeMenu === item.index }"
        >
          <el-icon class="menu-icon"><component :is="item.icon" /></el-icon>
          <span class="menu-text">{{ item.title }}</span>
        </router-link>
      </nav>
      <div class="sidebar-footer">
        <div class="version mono">v1.0.0</div>
      </div>
    </aside>

    <!-- 主区域 -->
    <div class="main-area">
      <!-- 顶栏 -->
      <header class="topbar">
        <div class="topbar-left">
          <h2 class="topbar-title">{{ currentTitle }}</h2>
        </div>
        <div class="topbar-right">
          <el-tooltip content="刷新数据" placement="bottom">
            <el-icon class="topbar-icon" @click="$router.go(0)"><Refresh /></el-icon>
          </el-tooltip>
          <el-tooltip content="全屏" placement="bottom">
            <el-icon class="topbar-icon" @click="handleFullscreen"><FullScreen /></el-icon>
          </el-tooltip>
          <el-dropdown @command="(cmd) => cmd === 'logout' && handleLogout()">
            <div class="admin-info">
              <el-avatar :size="32" class="admin-avatar">
                {{ adminName.charAt(0) }}
              </el-avatar>
              <span class="admin-name">{{ adminName }}</span>
              <el-icon><CaretBottom /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- 内容区 -->
      <main class="content-area">
        <router-view v-slot="{ Component }">
          <transition name="page" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.admin-layout {
  display: flex;
  width: 100%;
  height: 100%;
  background: $bg-base;
}

// 侧边栏
.sidebar {
  width: $sidebar-width;
  height: 100%;
  background: linear-gradient(180deg, #1F2340 0%, #1A1D2E 100%);
  border-right: 1px solid $border-base;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  position: relative;
  z-index: 10;

  &::before {
    content: '';
    position: absolute;
    top: 0; right: 0;
    width: 1px; height: 100%;
    background: linear-gradient(180deg, transparent, $color-primary 50%, transparent);
    opacity: 0.3;
  }
}
.sidebar-logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid $border-divider;
}
.sidebar-menu {
  flex: 1;
  padding: 16px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  overflow-y: auto;
}
.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: $radius-md;
  color: $text-regular;
  font-size: 14px;
  transition: $transition-base;
  position: relative;
  cursor: pointer;

  .menu-icon {
    font-size: 18px;
  }

  &:hover {
    background: rgba(78, 205, 196, 0.08);
    color: $color-primary;
  }

  &.active {
    background: rgba(78, 205, 196, 0.14);
    color: $color-primary;
    font-weight: 600;
    box-shadow: inset 0 0 0 1px rgba(78, 205, 196, 0.3);

    &::before {
      content: '';
      position: absolute;
      left: 0; top: 8px; bottom: 8px;
      width: 3px;
      background: $color-primary;
      border-radius: 2px;
      box-shadow: $shadow-glow-primary;
    }
  }
}
.sidebar-footer {
  padding: 12px;
  border-top: 1px solid $border-divider;
  text-align: center;
  .version {
    font-size: 11px;
    color: $text-placeholder;
    letter-spacing: 1px;
  }
}

// 主区域
.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}
.topbar {
  height: 64px;
  background: $bg-elevated;
  border-bottom: 1px solid $border-base;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  flex-shrink: 0;
}
.topbar-title {
  font-size: 18px;
  font-weight: 600;
  color: $text-primary;
}
.topbar-right {
  display: flex;
  align-items: center;
  gap: 20px;
}
.topbar-icon {
  font-size: 18px;
  color: $text-secondary;
  cursor: pointer;
  transition: $transition-base;
  &:hover {
    color: $color-primary;
  }
}
.admin-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: $radius-md;
  transition: $transition-base;
  &:hover {
    background: $bg-surface;
  }
}
.admin-avatar {
  background: $color-primary;
  color: $bg-base;
  font-weight: 600;
}
.admin-name {
  font-size: 14px;
  color: $text-primary;
}

.content-area {
  flex: 1;
  overflow: hidden;
}

// 页面切换动画
.page-enter-active {
  transition: all 0.3s ease;
}
.page-enter-from {
  opacity: 0;
  transform: translateY(12px);
}
</style>
