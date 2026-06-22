<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
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
  getOperators,
  createOperator,
  updateOperator,
  deleteOperator,
  getAiSwitch,
  updateAiSwitch,
  type Announcement,
  type Banner,
  type PlatformRule,
  type Operator,
  type AiSwitchConfig
} from '@/api/config'

/**
 * SystemConfigPage 系统配置页
 * 功能：公告、轮播图、平台规则、操作员管理、AI 开关
 * 参数：无
 * 注意事项：通过 Tab 切换五个配置模块
 */
const activeTab = ref<'announce' | 'banner' | 'rule' | 'operator' | 'ai'>('announce')

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

// 操作员
const operators = ref<Operator[]>([])
const operatorLoading = ref(false)
const operatorVisible = ref(false)
const operatorForm = reactive<Partial<Operator> & { password: string }>({
  account: '',
  nickname: '',
  role: 'OPERATOR',
  password: ''
})
const isEditOperator = ref(false)

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
 * 加载操作员
 */
async function loadOperators() {
  operatorLoading.value = true
  try {
    const res = await getOperators()
    operators.value = res
  } catch (e) {
    // ignore
  } finally {
    operatorLoading.value = false
  }
}

/**
 * 新增/编辑操作员
 * @param row 行数据
 */
function handleOperatorEdit(row?: Operator) {
  isEditOperator.value = !!row
  if (row) {
    operatorForm.adminId = row.adminId
    operatorForm.account = row.account
    operatorForm.nickname = row.nickname
    operatorForm.role = row.role
    operatorForm.password = ''
  } else {
    operatorForm.adminId = undefined
    operatorForm.account = ''
    operatorForm.nickname = ''
    operatorForm.role = 'OPERATOR'
    operatorForm.password = ''
  }
  operatorVisible.value = true
}

/**
 * 保存操作员
 */
async function confirmOperator() {
  if (!operatorForm.account || !operatorForm.nickname) {
    ElMessage.warning('请填写账号和昵称')
    return
  }
  if (!isEditOperator.value && !operatorForm.password) {
    ElMessage.warning('请设置密码')
    return
  }
  try {
    if (isEditOperator.value) {
      await updateOperator(operatorForm)
    } else {
      await createOperator(operatorForm)
    }
    ElMessage.success('保存成功')
    operatorVisible.value = false
    loadOperators()
  } catch (e) {
    // ignore
  }
}

/**
 * 删除操作员
 * @param row 行数据，el-table 原生插槽 row 类型为 any
 */
async function handleOperatorDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确认删除操作员「${row.nickname}」？`, '删除确认', { type: 'warning' })
    await deleteOperator(row.adminId)
    ElMessage.success('已删除')
    loadOperators()
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
    case 'operator': loadOperators(); break
    case 'ai': loadAiSwitch(); break
  }
}

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
          { key: 'operator', label: '操作员管理' },
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

    <!-- 操作员管理 -->
    <div v-else-if="activeTab === 'operator'" class="base-card" v-loading="operatorLoading">
      <div class="card-toolbar">
        <el-button type="primary" :icon="'Plus'" @click="handleOperatorEdit()">新增操作员</el-button>
      </div>
      <el-table :data="operators" stripe>
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="adminId" label="ID" width="80" />
        <el-table-column prop="account" label="账号" width="160" />
        <el-table-column prop="nickname" label="昵称" width="160" />
        <el-table-column prop="role" label="角色" width="140">
          <template #default="{ row }">
            <el-tag :type="row.role === 'SUPER_ADMIN' ? 'warning' : 'primary'" size="small" effect="dark">
              {{ row.role === 'SUPER_ADMIN' ? '超级管理员' : '操作员' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'primary' : 'info'" size="small" effect="dark">
              {{ row.status === 'ACTIVE' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="handleOperatorEdit(row)">编辑</el-button>
            <el-button text type="danger" size="small" @click="handleOperatorDelete(row)">删除</el-button>
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
          <el-input v-model="bannerForm.imageUrl" placeholder="图片 URL" />
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

    <!-- 操作员弹窗 -->
    <el-dialog v-model="operatorVisible" :title="isEditOperator ? '编辑操作员' : '新增操作员'" width="480px">
      <el-form :model="operatorForm" label-width="80px">
        <el-form-item label="账号">
          <el-input v-model="operatorForm.account" :disabled="isEditOperator" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="operatorForm.nickname" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="operatorForm.role" style="width: 200px">
            <el-option label="超级管理员" value="SUPER_ADMIN" />
            <el-option label="操作员" value="OPERATOR" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!isEditOperator" label="密码">
          <el-input v-model="operatorForm.password" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="operatorVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmOperator">保存</el-button>
      </template>
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
.card-toolbar {
  padding: 0 0 16px;
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
  background: linear-gradient(135deg, rgba(78, 205, 196, 0.08), rgba(255, 182, 39, 0.05));
  border: 1px solid rgba(78, 205, 196, 0.3);
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
</style>
