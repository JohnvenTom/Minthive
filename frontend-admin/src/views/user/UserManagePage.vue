<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import DataTable from '@/components/DataTable.vue'
import StatusTag from '@/components/StatusTag.vue'
import {
  getUserList,
  getUserDetail,
  banUser,
  unbanUser,
  resetPassword,
  createUser,
  cleanZombieUsers,
  type UserInfo,
  type UserQuery,
  type CreateUserParams
} from '@/api/user'

/**
 * UserManagePage 用户管理页
 * 功能：用户列表、搜索、详情、封禁解封、重置密码、清理僵尸号
 * 参数：无
 * 注意事项：封禁支持限时/永久，重置密码需二次确认
 */
const loading = ref(false)
const userList = ref<UserInfo[]>([])
const total = ref(0)
const query = reactive<UserQuery>({
  page: 1,
  pageSize: 10,
  keyword: '',
  status: ''
})

/** 表格列配置 */
const columns = [
  { prop: 'userId', label: '用户ID', width: 90 },
  { prop: 'nickname', label: '昵称', minWidth: 140, slot: 'nickname' },
  { prop: 'account', label: '账号', width: 140 },
  { prop: 'phone', label: '手机号', width: 140 },
  { prop: 'postCount', label: '发帖数', width: 90, align: 'center' as const },
  { prop: 'fansCount', label: '粉丝数', width: 90, align: 'center' as const },
  { prop: 'status', label: '状态', width: 100, align: 'center' as const, slot: 'status' },
  { prop: 'registerTime', label: '注册时间', width: 170 },
  { prop: 'lastLoginTime', label: '最近登录', width: 170 },
  { prop: 'actions', label: '操作', width: 220, fixed: 'right' as const, slot: 'actions' }
]

/** 详情弹窗 */
const detailVisible = ref(false)
const detailData = ref<UserInfo | null>(null)

/** 封禁弹窗 */
const banVisible = ref(false)
const banFormRef = ref<FormInstance>()
const banForm = reactive({
  userId: 0,
  nickname: '',
  duration: 24,
  reason: ''
})

/** 创建用户弹窗 */
const createVisible = ref(false)
const createForm = reactive<CreateUserParams>({
  account: '',
  password: '',
  nickname: '',
  phone: ''
})
const createLoading = ref(false)

function handleCreate() {
  createForm.account = ''
  createForm.password = ''
  createForm.nickname = ''
  createForm.phone = ''
  createVisible.value = true
}

async function confirmCreate() {
  if (!createForm.account) { ElMessage.warning('请输入账号'); return }
  if (!createForm.password || createForm.password.length < 6) { ElMessage.warning('密码至少 6 位'); return }
  if (!createForm.nickname) { ElMessage.warning('请输入昵称'); return }
  createLoading.value = true
  try {
    await createUser(createForm)
    ElMessage.success('用户创建成功')
    createVisible.value = false
    loadList()
  } catch (e) {
    // 拦截器已提示
  } finally {
    createLoading.value = false
  }
}

/** 重置密码弹窗 */
const resetVisible = ref(false)
const resetForm = reactive({
  userId: 0,
  nickname: '',
  newPassword: ''
})

/**
 * 加载用户列表
 */
async function loadList() {
  loading.value = true
  try {
    const res = await getUserList(query)
    userList.value = res.list
    total.value = res.total
  } catch (e) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

/**
 * 搜索
 */
function handleSearch() {
  query.page = 1
  loadList()
}

/**
 * 重置搜索
 */
function handleReset() {
  query.keyword = ''
  query.status = ''
  query.page = 1
  loadList()
}

/**
 * 查看用户详情
 * @param row 行数据
 */
async function handleDetail(row: UserInfo) {
  try {
    const res = await getUserDetail(row.userId)
    detailData.value = res
    detailVisible.value = true
  } catch (e) {
    // ignore
  }
}

/**
 * 打开封禁弹窗
 * @param row 行数据
 */
function handleBan(row: UserInfo) {
  banForm.userId = row.userId
  banForm.nickname = row.nickname
  banForm.duration = 24
  banForm.reason = ''
  banVisible.value = true
}

/**
 * 确认封禁
 * @param formEl 表单实例
 */
async function confirmBan(formEl: FormInstance | undefined) {
  if (!formEl) return
  if (!banForm.reason) {
    ElMessage.warning('请填写封禁原因')
    return
  }
  try {
    await banUser({
      userId: banForm.userId,
      duration: banForm.duration,
      reason: banForm.reason
    })
    ElMessage.success('封禁成功')
    banVisible.value = false
    loadList()
  } catch (e) {
    // ignore
  }
}

/**
 * 解封用户
 * @param row 行数据
 */
async function handleUnban(row: UserInfo) {
  try {
    await ElMessageBox.confirm(`确定解封用户「${row.nickname}」吗？`, '解封确认', { type: 'warning' })
    await unbanUser(row.userId)
    ElMessage.success('解封成功')
    loadList()
  } catch (e) {
    // ignore
  }
}

/**
 * 打开重置密码弹窗
 * @param row 行数据
 */
function handleResetPwd(row: UserInfo) {
  resetForm.userId = row.userId
  resetForm.nickname = row.nickname
  resetForm.newPassword = ''
  resetVisible.value = true
}

/**
 * 确认重置密码
 */
async function confirmReset() {
  if (resetForm.newPassword.length < 6) {
    ElMessage.warning('密码至少 6 位')
    return
  }
  try {
    await resetPassword(resetForm.userId, resetForm.newPassword)
    ElMessage.success('密码重置成功')
    resetVisible.value = false
  } catch (e) {
    // ignore
  }
}

/**
 * 清理僵尸账号
 */
async function handleCleanZombie() {
  try {
    await ElMessageBox.confirm('将清理长期未登录且无发帖的僵尸账号，此操作不可撤销，是否继续？', '清理确认', { type: 'warning' })
    const res = await cleanZombieUsers()
    ElMessage.success(`已清理 ${res.cleaned} 个僵尸账号`)
    loadList()
  } catch (e) {
    // ignore
  }
}

onMounted(() => {
  loadList()
})
</script>

<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <div class="base-card search-bar">
      <el-form inline>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="账号/昵称/手机号" clearable style="width: 220px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="正常" value="NORMAL" />
            <el-option label="已封禁" value="BANNED" />
            <el-option label="已注销" value="DELETED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
          <el-button :icon="'Refresh'" @click="handleReset">重置</el-button>
          <el-button type="primary" :icon="'Plus'" @click="handleCreate">添加用户</el-button>
          <el-button type="warning" :icon="'Delete'" @click="handleCleanZombie">清理僵尸号</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 表格 -->
    <div class="base-card table-card">
      <DataTable
        :data="userList"
        :columns="columns"
        :loading="loading"
        :total="total"
        v-model:page="query.page"
        v-model:pageSize="query.pageSize"
        @update:page="loadList"
        @update:pageSize="loadList"
      >
        <template #nickname="{ row }">
          <div class="user-cell">
            <el-avatar :size="28" :src="row.avatar">{{ row.nickname?.charAt(0) }}</el-avatar>
            <span>{{ row.nickname }}</span>
          </div>
        </template>
        <template #status="{ row }">
          <StatusTag :status="row.status" type="user" />
        </template>
        <template #actions="{ row }">
          <el-button text type="primary" size="small" @click="handleDetail(row)">详情</el-button>
          <el-button v-if="row.status === 'NORMAL'" text type="warning" size="small" @click="handleBan(row)">封禁</el-button>
          <el-button v-if="row.status === 'BANNED'" text type="primary" size="small" @click="handleUnban(row)">解封</el-button>
          <el-button text type="info" size="small" @click="handleResetPwd(row)">重置密码</el-button>
        </template>
      </DataTable>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="用户详情" width="560px">
      <div class="detail-content" v-if="detailData">
        <div class="detail-header">
          <el-avatar :size="56" :src="detailData.avatar">{{ detailData.nickname?.charAt(0) }}</el-avatar>
          <div class="detail-base">
            <div class="detail-name">{{ detailData.nickname }}</div>
            <div class="detail-account mono">@{{ detailData.account }}</div>
            <StatusTag :status="detailData.status" type="user" />
          </div>
        </div>
        <el-descriptions :column="2" border class="detail-desc">
          <el-descriptions-item label="用户ID">{{ detailData.userId }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ detailData.phone }}</el-descriptions-item>
          <el-descriptions-item label="性别">{{ detailData.gender === 1 ? '男' : detailData.gender === 2 ? '女' : '保密' }}</el-descriptions-item>
          <el-descriptions-item label="发帖数">{{ detailData.postCount }}</el-descriptions-item>
          <el-descriptions-item label="关注数">{{ detailData.followCount }}</el-descriptions-item>
          <el-descriptions-item label="粉丝数">{{ detailData.fansCount }}</el-descriptions-item>
          <el-descriptions-item label="注册时间">{{ detailData.registerTime }}</el-descriptions-item>
          <el-descriptions-item label="最近登录">{{ detailData.lastLoginTime }}</el-descriptions-item>
          <el-descriptions-item label="兴趣标签" :span="2">
            <el-tag v-for="(t, i) in (typeof detailData.interests === 'string' ? detailData.interests.split(',').map(s => s.trim()) : detailData.interests)" :key="i" size="small" style="margin-right: 4px">{{ t }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>

    <!-- 创建用户弹窗 -->
    <el-dialog v-model="createVisible" title="添加用户" width="440px">
      <el-form label-width="80px">
        <el-form-item label="账号" required>
          <el-input v-model="createForm.account" placeholder="登录账号" />
        </el-form-item>
        <el-form-item label="密码" required>
          <el-input v-model="createForm.password" type="password" show-password placeholder="至少 6 位" />
        </el-form-item>
        <el-form-item label="昵称" required>
          <el-input v-model="createForm.nickname" placeholder="显示昵称" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="createForm.phone" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="confirmCreate">确认创建</el-button>
      </template>
    </el-dialog>

    <!-- 封禁弹窗 -->
    <el-dialog v-model="banVisible" title="封禁用户" width="440px">
      <el-form ref="banFormRef" :model="banForm" label-width="80px">
        <el-form-item label="用户">
          <span>{{ banForm.nickname }}</span>
        </el-form-item>
        <el-form-item label="封禁时长">
          <el-radio-group v-model="banForm.duration">
            <el-radio :value="24">24小时</el-radio>
            <el-radio :value="72">3天</el-radio>
            <el-radio :value="720">30天</el-radio>
            <el-radio :value="0">永久</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="封禁原因">
          <el-input v-model="banForm.reason" type="textarea" :rows="3" placeholder="请填写封禁原因，将通知用户" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="banVisible = false">取消</el-button>
        <el-button type="warning" @click="confirmBan(banFormRef)">确认封禁</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码弹窗 -->
    <el-dialog v-model="resetVisible" title="重置密码" width="440px">
      <el-form label-width="80px">
        <el-form-item label="用户">
          <span>{{ resetForm.nickname }}</span>
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="resetForm.newPassword" type="password" show-password placeholder="请输入新密码（至少6位）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmReset">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.search-bar {
  margin-bottom: 16px;
  padding: 16px 20px;
}
.table-card {
  padding: 16px 20px;
}
.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.detail-content {
  padding: 0 8px;
}
.detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid $border-divider;
}
.detail-name {
  font-size: 18px;
  font-weight: 600;
  color: $text-primary;
}
.detail-account {
  font-size: 13px;
  color: $text-secondary;
  margin: 4px 0 8px;
}
</style>
