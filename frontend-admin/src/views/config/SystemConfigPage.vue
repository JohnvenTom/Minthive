<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAnnouncements,
  saveAnnouncement,
  deleteAnnouncement,
  getBanners,
  saveBanner,
  deleteBanner,
  getPlatformRules,
  updatePlatformRules,
  getRoleList,
  searchNormalUser,
  createUserWithRole,
  setUserRole,
  removeUserRole,
  getAiSwitch,
  updateAiSwitch,
  type Announcement,
  type Banner,
  type PlatformRule,
  type RoleUser,
  type NormalUser,
  type AiSwitchConfig
} from '@/api/config'
import ImageSelector from '@/components/ImageSelector.vue'

/**
 * SystemConfigPage 系统配置页
 * 功能：公告、轮播图、平台规则、圈主·管理员管理、AI 开关
 * 参数：无
 * 注意事项：通过 Tab 切换五个配置模块
 */
const activeTab = ref<'announce' | 'banner' | 'rule' | 'role' | 'ai'>('announce')

// 公告
const announcements = ref<Announcement[]>([])
const announceLoading = ref(false)
const announceVisible = ref(false)
const announceForm = reactive<Partial<Announcement>>({ title: '', content: '', status: 1 })

// 轮播图
const banners = ref<Banner[]>([])
const bannerLoading = ref(false)
const bannerVisible = ref(false)
const bannerForm = reactive<Partial<Banner>>({ title: '', imageUrl: '', linkUrl: '', sort: 0, status: 'ACTIVE' })

// 平台规则
const rulesLoading = ref(false)
const rulesForm = reactive<PlatformRule>({
  dailyPostLimit: 20,
  imageMaxSize: 5,
  videoMaxDuration: 60,
  videoMaxSize: 20,
  aiDailyLimit: 50
})

// 管理员管理
const roleUsers = ref<RoleUser[]>([])
const roleLoading = ref(false)

// 新建账号弹窗
const createVisible = ref(false)
const createForm = reactive({ account: '', nickname: '', password: '' })
const createSubmitting = ref(false)

// 从已有用户选取弹窗
const pickVisible = ref(false)
const pickKeyword = ref('')
const pickResults = ref<NormalUser[]>([])
const pickLoading = ref(false)

// AI 开关
const aiLoading = ref(false)
const aiForm = reactive<AiSwitchConfig>({
  enabled: true,
  textGenerate: true,
  commentGenerate: true,
  contentDetect: true,
  recommend: true,
  imageProcess: true,
  privateReply: true,
  qaAssistant: true
})

/** AI 开关项配置 */
const aiSwitchItems = [
  { key: 'textGenerate', label: 'AI 文案生成', desc: '用户发帖时 AI 一键生成文案' },
  { key: 'commentGenerate', label: 'AI 智能评论', desc: 'AI 生成贴合内容的评论' },
  { key: 'contentDetect', label: 'AI 内容风控', desc: 'AI 语义复审违规内容' },
  { key: 'recommend', label: 'AI 兴趣推荐', desc: '千人千面信息流推荐' },
  { key: 'imageProcess', label: 'AI 图片处理', desc: '美颜、修复、背景消除' },
  { key: 'privateReply', label: 'AI 私信代回复', desc: '离线时 AI 自动回复私信' },
  { key: 'qaAssistant', label: 'AI 问答助手', desc: '全局悬浮 AI 客服' }
] as const

/**
 * 加载公告（后端返回分页数据）
 */
async function loadAnnouncements() {
  announceLoading.value = true
  try {
    const res = await getAnnouncements()
    announcements.value = res.records || []
  } catch (e) {
    // ignore
  } finally {
    announceLoading.value = false
  }
}

/**
 * 新增/编辑公告
 * @param row 行数据（编辑时传入），el-table 原生插槽 row 类型为 any
 */
function handleAnnounceEdit(row?: any) {
  if (row) {
    announceForm.id = row.id
    announceForm.title = row.title
    announceForm.content = row.content
    announceForm.status = 1
  } else {
    announceForm.id = undefined
    announceForm.title = ''
    announceForm.content = ''
    announceForm.status = 1
  }
  announceVisible.value = true
}

/**
 * 保存公告
 */
async function confirmAnnounce() {
  if (!announceForm.title || !announceForm.content) {
    ElMessage.warning('请填写标题和内容')
    return
  }
  try {
    await saveAnnouncement(announceForm)
    ElMessage.success('保存成功')
    announceVisible.value = false
    loadAnnouncements()
  } catch (e) {
    // ignore
  }
}

/**
 * 删除公告
 * @param row 行数据
 */
async function handleAnnounceDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确认删除公告「${row.title}」？`, '删除确认', { type: 'warning' })
    await deleteAnnouncement(row.id)
    ElMessage.success('已删除')
    loadAnnouncements()
  } catch (e) {
    // ignore
  }
}

/**
 * 加载轮播图
 */
async function loadBanners() {
  bannerLoading.value = true
  try {
    const res = await getBanners()
    banners.value = res
  } catch (e) {
    // ignore
  } finally {
    bannerLoading.value = false
  }
}

/**
 * 新增/编辑轮播图
 * @param row 行数据，el-table 原生插槽 row 类型为 any
 */
function handleBannerEdit(row?: any) {
  if (row) {
    bannerForm.id = row.id
    bannerForm.title = row.title
    bannerForm.imageUrl = row.imageUrl
    bannerForm.linkUrl = row.linkUrl
    bannerForm.sort = row.sort
    bannerForm.status = row.status
  } else {
    bannerForm.id = undefined
    bannerForm.title = ''
    bannerForm.imageUrl = ''
    bannerForm.linkUrl = ''
    bannerForm.sort = 0
    bannerForm.status = 'ACTIVE'
  }
  bannerVisible.value = true
}

/**
 * 保存轮播图
 */
async function confirmBanner() {
  if (!bannerForm.title || !bannerForm.imageUrl) {
    ElMessage.warning('请填写标题和图片地址')
    return
  }
  try {
    await saveBanner(bannerForm)
    ElMessage.success('保存成功')
    bannerVisible.value = false
    loadBanners()
  } catch (e) {
    // ignore
  }
}

/**
 * 删除轮播图
 * @param row 行数据，el-table 原生插槽 row 类型为 any
 */
async function handleBannerDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确认删除轮播图「${row.title}」？`, '删除确认', { type: 'warning' })
    await deleteBanner(row.id)
    ElMessage.success('已删除')
    loadBanners()
  } catch (e) {
    // ignore
  }
}

/**
 * 加载平台规则
 */
async function loadRules() {
  rulesLoading.value = true
  try {
    const res = await getPlatformRules()
    Object.assign(rulesForm, res)
  } catch (e) {
    // ignore
  } finally {
    rulesLoading.value = false
  }
}

/**
 * 保存平台规则
 */
async function saveRules() {
  try {
    await updatePlatformRules(rulesForm)
    ElMessage.success('规则保存成功')
  } catch (e) {
    // ignore
  }
}

/**
 * 加载管理员列表
 */
async function loadRoleList() {
  roleLoading.value = true
  try {
    const res: any = await getRoleList('2')
    roleUsers.value = res
  } catch (e) {
    // ignore
  } finally {
    roleLoading.value = false
  }
}

/**
 * 打开新建账号弹窗
 */
function handleCreate() {
  createForm.account = ''
  createForm.nickname = ''
  createForm.password = ''
  createVisible.value = true
}

/**
 * 提交新建账号
 */
async function confirmCreate() {
  if (!createForm.account || !createForm.password) {
    ElMessage.warning('请填写账号和密码')
    return
  }
  createSubmitting.value = true
  try {
    await createUserWithRole({
      account: createForm.account,
      password: createForm.password,
      nickname: createForm.nickname || createForm.account,
      role: 'ADMIN'
    })
    ElMessage.success('创建成功')
    createVisible.value = false
    loadRoleList()
  } catch (e) {
    // ignore
  } finally {
    createSubmitting.value = false
  }
}

/**
 * 打开选取用户弹窗
 */
function handlePick() {
  pickKeyword.value = ''
  pickResults.value = []
  pickVisible.value = true
}

/**
 * 搜索普通用户
 */
async function doSearchNormalUser() {
  if (!pickKeyword.value.trim()) {
    ElMessage.warning('请输入搜索关键词')
    return
  }
  pickLoading.value = true
  try {
    const res: any = await searchNormalUser(pickKeyword.value.trim())
    pickResults.value = res
    if ((res as any[]).length === 0) {
      ElMessage.info('未找到匹配的普通用户')
    }
  } catch (e) {
    // ignore
  } finally {
    pickLoading.value = false
  }
}

/**
 * 确认将用户设为管理员
 */
async function confirmPickUser(user: NormalUser) {
  try {
    await ElMessageBox.confirm(
      `确认将用户「${user.nickname}」(${user.account}) 设为管理员？`,
      '角色设置确认',
      { type: 'info' }
    )
    await setUserRole(user.userId, 'ADMIN')
    ElMessage.success('设置成功')
    pickVisible.value = false
    loadRoleList()
  } catch (e) {
    // ignore
  }
}

/**
 * 移除管理员角色（降级为普通用户）
 */
async function handleRemoveRole(row: any) {
  try {
    await ElMessageBox.confirm(
      `确认将管理员「${row.nickname}」降级为普通用户？`,
      '移除角色确认',
      { type: 'warning' }
    )
    await removeUserRole(row.userId)
    ElMessage.success('已移除角色')
    loadRoleList()
  } catch (e) {
    // ignore
  }
}

/**
 * 加载 AI 开关
 */
async function loadAiSwitch() {
  aiLoading.value = true
  try {
    const res = await getAiSwitch()
    Object.assign(aiForm, res)
  } catch (e) {
    // ignore
  } finally {
    aiLoading.value = false
  }
}

/**
 * 保存 AI 开关
 */
async function saveAiSwitch() {
  try {
    await updateAiSwitch(aiForm)
    ElMessage.success('AI 配置保存成功')
  } catch (e) {
    // ignore
  }
}

/**
 * Tab 切换加载对应数据
 * @param tab 目标 tab
 */
function handleTabChange(tab: string) {
  activeTab.value = tab as any
  switch (tab) {
    case 'announce': loadAnnouncements(); break
    case 'banner': loadBanners(); break
    case 'rule': loadRules(); break
    case 'role': loadRoleList(); break
    case 'ai': loadAiSwitch(); break
  }
}

// 搜索防抖
let searchTimer: ReturnType<typeof setTimeout> | null = null
watch(pickKeyword, (val) => {
  if (searchTimer) clearTimeout(searchTimer)
  if (val.trim()) {
    searchTimer = setTimeout(() => doSearchNormalUser(), 300)
  } else {
    pickResults.value = []
  }
})

onMounted(() => {
  loadAnnouncements()
})
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-title">系统配置</div>
    </div>

    <div class="tab-bar">
      <div
        v-for="tab in [
          { key: 'announce', label: '系统公告' },
          { key: 'banner', label: '首页轮播' },
          { key: 'rule', label: '平台规则' },
          { key: 'role', label: '管理员管理' },
          { key: 'ai', label: 'AI 开关' }
        ]"
        :key="tab.key"
        class="tab-item"
        :class="{ active: activeTab === tab.key }"
        @click="handleTabChange(tab.key)"
      >
        {{ tab.label }}
      </div>
    </div>

    <!-- 系统公告 -->
    <div v-if="activeTab === 'announce'" class="base-card" v-loading="announceLoading">
      <div class="card-toolbar">
        <el-button type="primary" :icon="'Plus'" @click="handleAnnounceEdit()">发布公告</el-button>
      </div>
      <el-table :data="announcements" stripe>
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column prop="content" label="内容" min-width="300" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'primary' : 'info'" size="small" effect="dark">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publishTime" label="发布时间" width="180" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="handleAnnounceEdit(row)">编辑</el-button>
            <el-button text type="danger" size="small" @click="handleAnnounceDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 首页轮播 -->
    <div v-else-if="activeTab === 'banner'" class="base-card" v-loading="bannerLoading">
      <div class="card-toolbar">
        <el-button type="primary" :icon="'Plus'" @click="handleBannerEdit()">新增轮播</el-button>
      </div>
      <el-table :data="banners" stripe>
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="title" label="标题" min-width="160" />
        <el-table-column prop="imageUrl" label="图片" width="120">
          <template #default="{ row }">
            <el-image :src="row.imageUrl" fit="cover" class="banner-thumb" />
          </template>
        </el-table-column>
        <el-table-column prop="linkUrl" label="跳转链接" min-width="200" show-overflow-tooltip />
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'primary' : 'info'" size="small" effect="dark">
              {{ row.status === 'ACTIVE' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="handleBannerEdit(row)">编辑</el-button>
            <el-button text type="danger" size="small" @click="handleBannerDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 平台规则 -->
    <div v-else-if="activeTab === 'rule'" class="base-card" v-loading="rulesLoading">
      <el-form :model="rulesForm" label-width="160px" style="max-width: 560px; padding: 20px">
        <el-form-item label="每日发帖上限">
          <el-input-number v-model="rulesForm.dailyPostLimit" :min="1" :max="100" controls-position="right" />
          <span class="form-tip">条/天</span>
        </el-form-item>
        <el-form-item label="单张图片大小限制">
          <el-input-number v-model="rulesForm.imageMaxSize" :min="1" :max="20" controls-position="right" />
          <span class="form-tip">MB</span>
        </el-form-item>
        <el-form-item label="视频时长限制">
          <el-input-number v-model="rulesForm.videoMaxDuration" :min="10" :max="300" controls-position="right" />
          <span class="form-tip">秒</span>
        </el-form-item>
        <el-form-item label="视频大小限制">
          <el-input-number v-model="rulesForm.videoMaxSize" :min="10" :max="200" controls-position="right" />
          <span class="form-tip">MB</span>
        </el-form-item>
        <el-form-item label="AI 每日调用上限">
          <el-input-number v-model="rulesForm.aiDailyLimit" :min="0" :max="500" controls-position="right" />
          <span class="form-tip">次/天/用户</span>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="saveRules">保存规则</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 管理员管理 -->
    <div v-else-if="activeTab === 'role'" class="base-card" v-loading="roleLoading">
      <div class="card-toolbar">
        <div class="card-toolbar-actions">
          <el-button type="primary" :icon="'Plus'" @click="handleCreate()">新建管理员</el-button>
          <el-button :icon="'Search'" @click="handlePick()">从用户选取</el-button>
        </div>
      </div>
      <el-table :data="roleUsers" stripe>
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="userId" label="ID" width="80" />
        <el-table-column prop="account" label="账号" width="160" />
        <el-table-column prop="nickname" label="昵称" width="180" />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'primary' : 'danger'" size="small" effect="plain">
              {{ row.status === 1 ? '正常' : '封禁' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="registerTime" label="注册时间" width="180" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button text type="danger" size="small" @click="handleRemoveRole(row)">移除角色</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- AI 开关 -->
    <div v-else-if="activeTab === 'ai'" class="base-card" v-loading="aiLoading">
      <div class="ai-config">
        <div class="ai-master">
          <div class="master-info">
            <div class="master-title">
              <el-icon><MagicStick /></el-icon>
              <span>AI 智能功能总开关</span>
            </div>
            <div class="master-desc">关闭后全站所有 AI 功能将自动降级，不影响基础发帖互动业务</div>
          </div>
          <el-switch v-model="aiForm.enabled" size="large" />
        </div>
        <el-divider />
        <div class="ai-items">
          <div v-for="item in aiSwitchItems" :key="item.key" class="ai-item">
            <div class="ai-item-info">
              <div class="ai-item-title">{{ item.label }}</div>
              <div class="ai-item-desc">{{ item.desc }}</div>
            </div>
            <el-switch v-model="aiForm[item.key]" :disabled="!aiForm.enabled" />
          </div>
        </div>
        <div class="ai-actions">
          <el-button type="primary" @click="saveAiSwitch">保存 AI 配置</el-button>
        </div>
      </div>
    </div>

    <!-- 公告弹窗 -->
    <el-dialog v-model="announceVisible" :title="announceForm.id ? '编辑公告' : '发布公告'" width="520px">
      <el-form :model="announceForm" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="announceForm.title" placeholder="公告标题" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="announceForm.content" type="textarea" :rows="5" placeholder="公告内容" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="announceForm.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="announceVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAnnounce">保存</el-button>
      </template>
    </el-dialog>

    <!-- 轮播图弹窗 -->
    <el-dialog v-model="bannerVisible" :title="bannerForm.id ? '编辑轮播' : '新增轮播'" width="520px">
      <el-form :model="bannerForm" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="bannerForm.title" />
        </el-form-item>
        <el-form-item label="图片地址">
          <ImageSelector v-model="bannerForm.imageUrl" dialog-width="520px" />
        </el-form-item>
        <el-form-item label="跳转链接">
          <el-input v-model="bannerForm.linkUrl" placeholder="点击跳转地址" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="bannerForm.sort" :min="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="bannerForm.status">
            <el-radio value="ACTIVE">启用</el-radio>
            <el-radio value="INACTIVE">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bannerVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmBanner">保存</el-button>
      </template>
    </el-dialog>

    <!-- 新建管理员弹窗 -->
    <el-dialog v-model="createVisible" title="新建管理员" width="460px">
      <el-form :model="createForm" label-width="70px">
        <el-form-item label="账号">
          <el-input v-model="createForm.account" placeholder="登录账号" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="createForm.nickname" placeholder="默认同账号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="createForm.password" type="password" show-password placeholder="登录密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="createSubmitting" @click="confirmCreate">保存</el-button>
      </template>
    </el-dialog>

    <!-- 从用户选取弹窗 -->
    <el-dialog v-model="pickVisible" title="从已有用户选取" width="540px">
      <div class="pick-search">
        <el-input v-model="pickKeyword" placeholder="搜索账号/昵称" clearable @keyup.enter="doSearchNormalUser" />
        <el-button type="primary" :icon="'Search'" @click="doSearchNormalUser" :loading="pickLoading">搜索</el-button>
      </div>
      <div v-if="pickResults.length > 0" class="pick-results">
        <div v-for="user in pickResults" :key="user.userId" class="pick-user-item">
          <div class="pick-user-info">
            <span class="pick-user-nickname">{{ user.nickname }}</span>
            <span class="pick-user-account">@{{ user.account }}</span>
          </div>
          <el-button type="primary" size="small" @click="confirmPickUser(user)">设为管理员</el-button>
        </div>
      </div>
      <div v-else-if="pickKeyword && !pickLoading" class="pick-empty">
        未找到匹配的普通用户
      </div>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.tab-bar {
  display: flex;
  gap: 4px;
  margin-bottom: 16px;
  border-bottom: 1px solid $border-divider;
}
.tab-item {
  padding: 10px 20px;
  cursor: pointer;
  color: $text-secondary;
  font-size: 14px;
  position: relative;
  transition: $transition-base;
  &:hover { color: $color-primary; }
  &.active {
    color: $color-primary;
    font-weight: 600;
    &::after {
      content: '';
      position: absolute;
      left: 0; right: 0; bottom: -1px;
      height: 2px;
      background: $color-primary;
      box-shadow: $shadow-glow-primary;
    }
  }
}
.banner-thumb {
  width: 80px;
  height: 45px;
  border-radius: $radius-sm;
}
.form-tip {
  margin-left: 8px;
  color: $text-secondary;
  font-size: 12px;
}
.ai-config {
  padding: 8px 4px;
}
.ai-master {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px;
  background: linear-gradient(135deg, rgba(232, 121, 169, 0.08), rgba(245, 158, 11, 0.05));
  border: 1px solid rgba(232, 121, 169, 0.3);
  border-radius: $radius-md;
}
.master-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: $text-primary;
  .el-icon { color: $color-primary; font-size: 20px; }
}
.master-desc {
  font-size: 12px;
  color: $text-secondary;
  margin-top: 6px;
}
.ai-items {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}
.ai-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  background: $bg-input;
  border: 1px solid $border-base;
  border-radius: $radius-md;
  transition: $transition-base;
  &:hover {
    border-color: $color-primary;
  }
}
.ai-item-title {
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
}
.ai-item-desc {
  font-size: 12px;
  color: $text-secondary;
  margin-top: 4px;
}
.ai-actions {
  margin-top: 24px;
  text-align: right;
}
.card-toolbar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
}
.card-toolbar-actions {
  display: flex;
  gap: 8px;
}
.pick-search {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}
.pick-results {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 360px;
  overflow-y: auto;
}
.pick-user-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  border: 1px solid $border-base;
  border-radius: $radius-md;
  transition: $transition-base;
  &:hover { border-color: $color-primary; }
}
.pick-user-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.pick-user-nickname {
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
}
.pick-user-account {
  font-size: 12px;
  color: $text-secondary;
}
.pick-empty {
  text-align: center;
  color: $text-secondary;
  padding: 40px 0;
  font-size: 14px;
}
</style>
