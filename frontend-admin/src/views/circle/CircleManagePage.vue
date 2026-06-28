<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import DataTable from '@/components/DataTable.vue'
import StatusTag from '@/components/StatusTag.vue'
import {
  getCircleList,
  getCircleCreationApplyList,
  approveCircleCreation,
  rejectCircleCreation,
  updateCircle,
  offlineCircle,
  transferOwner,
  setRecommend,
  getCircleCategories,
  type CircleInfo,
  type CircleCreationApply,
  type CircleCategory
} from '@/api/circle'
import { searchNormalUser, createUserWithRole, setUserRole, type NormalUser } from '@/api/config'

/**
 * CircleManagePage 圈子管理页
 * 功能：圈子列表、创建申请审核、编辑、下架、转让圈主、推荐位
 * 参数：无
 * 注意事项：通过 Tab 切换圈子列表与创建申请
 */
const activeTab = ref<'list' | 'apply'>('list')
const loading = ref(false)

// 圈子列表
const circleList = ref<CircleInfo[]>([])
const total = ref(0)
const query = reactive({ page: 1, pageSize: 10, keyword: '', status: undefined as string | number | undefined, categoryId: undefined as number | undefined })
const categoryList = ref<CircleCategory[]>([])

/** 圈子表格列 */
const circleColumns = [
  { prop: 'circleId', label: '圈子ID', width: 90 },
  { prop: 'name', label: '圈子名称', minWidth: 160, slot: 'name' },
  { prop: 'categoryName', label: '分类', width: 110 },
  { prop: 'type', label: '类型', width: 90, align: 'center' as const, slot: 'type' },
  { prop: 'ownerName', label: '圈主', width: 130 },
  { prop: 'memberCount', label: '成员数', width: 90, align: 'center' as const },
  { prop: 'postCount', label: '帖子数', width: 90, align: 'center' as const },
  { prop: 'recommended', label: '推荐', width: 90, align: 'center' as const, slot: 'recommended' },
  { prop: 'status', label: '状态', width: 100, align: 'center' as const, slot: 'status' },
  { prop: 'actions', label: '操作', width: 260, fixed: 'right' as const, slot: 'actions' }
]

// 创建申请
const applyList = ref<CircleCreationApply[]>([])
const applyTotal = ref(0)
const applyQuery = reactive({ page: 1, pageSize: 10 })

/** 申请表格列 */
const applyColumns = [
  { prop: 'applyId', label: '申请ID', width: 90 },
  { prop: 'ownerName', label: '申请人', width: 130 },
  { prop: 'name', label: '圈子名称', minWidth: 160 },
  { prop: 'categoryName', label: '分类', width: 110 },
  { prop: 'type', label: '类型', width: 90, align: 'center' as const, slot: 'applyType' },
  { prop: 'intro', label: '简介', minWidth: 200, slot: 'intro' },
  { prop: 'applyTime', label: '申请时间', width: 170 },
  { prop: 'status', label: '状态', width: 100, align: 'center' as const, slot: 'applyStatus' },
  { prop: 'actions', label: '操作', width: 180, fixed: 'right' as const, slot: 'applyActions' }
]

// 编辑弹窗
const editVisible = ref(false)
const editForm = reactive({
  circleId: 0,
  name: '',
  categoryId: undefined as number | undefined,
  intro: '',
  type: 'PUBLIC' as 'PUBLIC' | 'PRIVATE'
})

// 转让弹窗
const transferVisible = ref(false)
const transferForm = reactive({ circleId: 0, name: '', newOwnerId: 0 })

// 下架弹窗
const offlineVisible = ref(false)
const offlineForm = reactive({ circleId: 0, name: '', reason: '' })

// 驳回申请弹窗
const rejectApplyVisible = ref(false)
const rejectApplyForm = reactive({ circleId: 0, reason: '' })

// 新增圈主弹窗
const ownerCreateVisible = ref(false)
const ownerCreateForm = reactive({ account: '', nickname: '', password: '' })
const ownerCreateSubmitting = ref(false)

// 从用户选取圈主弹窗
const ownerPickVisible = ref(false)
const ownerPickKeyword = ref('')
const ownerPickResults = ref<NormalUser[]>([])
const ownerPickLoading = ref(false)

/**
 * 加载圈子列表
 */
async function loadList() {
  loading.value = true
  try {
    const res = await getCircleList(query)
    circleList.value = res.list
    total.value = res.total
  } catch (e) {
    // ignore
  } finally {
    loading.value = false
  }
}

/**
 * 加载申请列表
 */
async function loadApplyList() {
  loading.value = true
  try {
    const res = await getCircleCreationApplyList(applyQuery)
    applyList.value = res.list
    applyTotal.value = res.total
  } catch (e) {
    // ignore
  } finally {
    loading.value = false
  }
}

/**
 * Tab 切换
 */
function handleTabChange(tab: string) {
  activeTab.value = tab as any
  if (tab === 'list') {
    loadList()
  } else {
    loadApplyList()
  }
}

/**
 * 打开编辑弹窗
 * @param row 行数据
 */
function handleEdit(row: CircleInfo) {
  editForm.circleId = row.circleId
  editForm.name = row.name
  editForm.categoryId = row.categoryId || undefined
  editForm.intro = row.intro
  editForm.type = row.type
  editVisible.value = true
}

/**
 * 确认编辑
 */
async function confirmEdit() {
  try {
    await updateCircle(editForm)
    ElMessage.success('修改成功')
    editVisible.value = false
    loadList()
  } catch (e) {
    // ignore
  }
}

/**
 * 打开下架弹窗
 * @param row 行数据
 */
function handleOffline(row: CircleInfo) {
  offlineForm.circleId = row.circleId
  offlineForm.name = row.name
  offlineForm.reason = ''
  offlineVisible.value = true
}

/**
 * 确认下架
 */
async function confirmOffline() {
  if (!offlineForm.reason) {
    ElMessage.warning('请填写下架原因')
    return
  }
  try {
    await offlineCircle(offlineForm.circleId, offlineForm.reason)
    ElMessage.success('已下架')
    offlineVisible.value = false
    loadList()
  } catch (e) {
    // ignore
  }
}

/**
 * 打开转让弹窗
 * @param row 行数据
 */
function handleTransfer(row: CircleInfo) {
  transferForm.circleId = row.circleId
  transferForm.name = row.name
  transferForm.newOwnerId = 0
  transferVisible.value = true
}

/**
 * 确认转让
 */
async function confirmTransfer() {
  if (!transferForm.newOwnerId) {
    ElMessage.warning('请输入新圈主用户ID')
    return
  }
  try {
    await ElMessageBox.confirm(`确认将圈子「${transferForm.name}」转让给用户 ${transferForm.newOwnerId}？`, '转让确认', { type: 'warning' })
    await transferOwner(transferForm.circleId, transferForm.newOwnerId)
    ElMessage.success('转让成功')
    transferVisible.value = false
    loadList()
  } catch (e) {
    // ignore
  }
}

/**
 * 切换推荐位
 * @param row 行数据
 * @param val 开关新值（true=推荐, false=取消）
 */
async function handleRecommend(row: CircleInfo, val: boolean) {
  try {
    await setRecommend(row.circleId, val)
    ElMessage.success(val ? '已设为推荐' : '已取消推荐')
    loadList()
  } catch (e) {
    // ignore
  }
}

/**
 * 通过创建申请
 * @param row 行数据
 */
async function handleApproveApply(row: CircleCreationApply) {
  try {
    await ElMessageBox.confirm(`确认通过「${row.name}」的创建申请？`, '审核确认', { type: 'success' })
    await approveCircleCreation(row.applyId)
    ElMessage.success('已通过')
    loadApplyList()
  } catch (e) {
    // ignore
  }
}

function handleRejectApply(row: CircleCreationApply) {
  rejectApplyForm.circleId = row.applyId
  rejectApplyForm.reason = ''
  rejectApplyVisible.value = true
}

async function confirmRejectApply() {
  if (!rejectApplyForm.reason) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  try {
    await rejectCircleCreation(rejectApplyForm.circleId, rejectApplyForm.reason)
    ElMessage.success('已驳回')
    rejectApplyVisible.value = false
    loadApplyList()
  } catch (e) {
    // ignore
  }
}

/**
 * 打开新建圈主弹窗
 */
function handleOwnerCreate() {
  ownerCreateForm.account = ''
  ownerCreateForm.nickname = ''
  ownerCreateForm.password = ''
  ownerCreateVisible.value = true
}

/**
 * 提交新建圈主
 */
async function confirmOwnerCreate() {
  if (!ownerCreateForm.account || !ownerCreateForm.password) {
    ElMessage.warning('请填写账号和密码')
    return
  }
  ownerCreateSubmitting.value = true
  try {
    await createUserWithRole({
      account: ownerCreateForm.account,
      password: ownerCreateForm.password,
      nickname: ownerCreateForm.nickname || ownerCreateForm.account,
      role: 'CIRCLE_OWNER'
    })
    ElMessage.success('圈主创建成功')
    ownerCreateVisible.value = false
  } catch (e) {
    // ignore
  } finally {
    ownerCreateSubmitting.value = false
  }
}

/**
 * 打开从用户选取圈主弹窗
 */
function handleOwnerPick() {
  ownerPickKeyword.value = ''
  ownerPickResults.value = []
  ownerPickVisible.value = true
}

/**
 * 搜索普通用户设为圈主
 */
async function doSearchOwner() {
  if (!ownerPickKeyword.value.trim()) {
    ElMessage.warning('请输入搜索关键词')
    return
  }
  ownerPickLoading.value = true
  try {
    const res: any = await searchNormalUser(ownerPickKeyword.value.trim())
    ownerPickResults.value = res
    if ((res as any[]).length === 0) {
      ElMessage.info('未找到匹配的普通用户')
    }
  } catch (e) {
    // ignore
  } finally {
    ownerPickLoading.value = false
  }
}

/**
 * 确认将用户设为圈主
 */
async function confirmOwnerPick(user: NormalUser) {
  try {
    await ElMessageBox.confirm(
      `确认将用户「${user.nickname}」(${user.account}) 设为圈主？`,
      '角色设置确认',
      { type: 'info' }
    )
    await setUserRole(user.userId, 'CIRCLE_OWNER')
    ElMessage.success('已设为圈主')
    ownerPickVisible.value = false
  } catch (e) {
    // ignore
  }
}

async function loadCategories() {
  try {
    const list = await getCircleCategories()
    categoryList.value = list
  } catch (e) {
    // ignore
  }
}

// 搜索防抖
let searchTimer: ReturnType<typeof setTimeout> | null = null
watch(ownerPickKeyword, (val) => {
  if (searchTimer) clearTimeout(searchTimer)
  if (val.trim()) {
    searchTimer = setTimeout(() => doSearchOwner(), 300)
  } else {
    ownerPickResults.value = []
  }
})

onMounted(() => {
  loadList()
  loadCategories()
})
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-title">圈子管理</div>
    </div>

    <div class="tab-bar">
      <div
        v-for="tab in [
          { key: 'list', label: '圈子列表' },
          { key: 'apply', label: '创建申请' }
        ]"
        :key="tab.key"
        class="tab-item"
        :class="{ active: activeTab === tab.key }"
        @click="handleTabChange(tab.key)"
      >
        {{ tab.label }}
      </div>
    </div>

    <!-- 圈子列表 -->
    <template v-if="activeTab === 'list'">
      <div class="base-card search-bar">
        <el-form inline>
          <el-form-item label="关键词">
            <el-input v-model="query.keyword" placeholder="圈子名称" clearable style="width: 200px" @keyup.enter="loadList" />
          </el-form-item>
          <el-form-item label="分类">
            <el-select v-model="query.categoryId" placeholder="全部" clearable style="width: 140px">
              <el-option v-for="cat in categoryList" :key="cat.id" :label="cat.name" :value="cat.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px">
              <el-option label="已上线" :value="1" />
              <el-option label="已下架" :value="0" />
              <el-option label="待审核" :value="2" />
              <el-option label="已驳回" :value="3" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="'Search'" @click="loadList">查询</el-button>
          </el-form-item>
          <el-form-item style="margin-left: auto">
            <el-button type="success" :icon="'Plus'" @click="handleOwnerCreate">新增圈主</el-button>
            <el-button :icon="'Search'" @click="handleOwnerPick">从用户选取</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="base-card table-card">
        <DataTable
          :data="circleList"
          :columns="circleColumns"
          :loading="loading"
          :total="total"
          v-model:page="query.page"
          v-model:pageSize="query.pageSize"
          @update:page="loadList"
          @update:pageSize="loadList"
        >
          <template #name="{ row }">
            <div class="circle-cell">
              <el-avatar :size="28" :src="row.icon" shape="square">{{ row.name?.charAt(0) }}</el-avatar>
              <span>{{ row.name }}</span>
            </div>
          </template>
          <template #type="{ row }">
            <el-tag :type="row.type === 'PUBLIC' ? 'primary' : 'warning'" size="small" effect="dark">
              {{ row.type === 'PUBLIC' ? '公开' : '私密' }}
            </el-tag>
          </template>
          <template #recommended="{ row }">
            <el-switch :model-value="!!row.recommended" @change="(val: boolean) => handleRecommend(row, val)" />
          </template>
          <template #status="{ row }">
            <StatusTag :status="row.status" type="circle" />
          </template>
          <template #actions="{ row }">
            <el-button text type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button text type="warning" size="small" @click="handleTransfer(row)">转让</el-button>
            <el-button v-if="row.status === 'ONLINE'" text type="danger" size="small" @click="handleOffline(row)">下架</el-button>
          </template>
        </DataTable>
      </div>
    </template>

    <!-- 创建申请 -->
    <template v-else>
      <div class="base-card table-card">
        <DataTable
          :data="applyList"
          :columns="applyColumns"
          :loading="loading"
          :total="applyTotal"
          v-model:page="applyQuery.page"
          v-model:pageSize="applyQuery.pageSize"
          @update:page="loadApplyList"
          @update:pageSize="loadApplyList"
        >
          <template #applyType="{ row }">
            <el-tag :type="row.type === 'PUBLIC' ? 'primary' : 'warning'" size="small" effect="dark">
              {{ row.type === 'PUBLIC' ? '公开' : '私密' }}
            </el-tag>
          </template>
          <template #intro="{ row }">
            <div class="content-cell">{{ row.intro }}</div>
          </template>
          <template #applyStatus="{ row }">
            <el-tag :type="row.status === 'PENDING' ? 'warning' : 'danger'" size="small">
              {{ row.status === 'PENDING' ? '待审核' : '已驳回' }}
            </el-tag>
          </template>
          <template #applyActions="{ row }">
            <template v-if="row.status === 'PENDING'">
              <el-button text type="primary" size="small" @click="handleApproveApply(row)">通过</el-button>
              <el-button text type="warning" size="small" @click="handleRejectApply(row)">驳回</el-button>
            </template>
            <span v-else class="text-secondary">已处理</span>
          </template>
        </DataTable>
      </div>
    </template>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editVisible" title="编辑圈子" width="480px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="editForm.name" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="editForm.categoryId" placeholder="请选择分类" clearable style="width: 100%">
            <el-option v-for="cat in categoryList" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="editForm.type">
            <el-radio value="PUBLIC">公开圈</el-radio>
            <el-radio value="PRIVATE">私密圈</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="editForm.intro" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 转让弹窗 -->
    <el-dialog v-model="transferVisible" title="转让圈主" width="440px">
      <el-form label-width="100px">
        <el-form-item label="圈子">
          <span>{{ transferForm.name }}</span>
        </el-form-item>
        <el-form-item label="新圈主ID">
          <el-input-number v-model="transferForm.newOwnerId" :min="1" controls-position="right" style="width: 200px" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="transferVisible = false">取消</el-button>
        <el-button type="warning" @click="confirmTransfer">确认转让</el-button>
      </template>
    </el-dialog>

    <!-- 下架弹窗 -->
    <el-dialog v-model="offlineVisible" title="下架圈子" width="440px">
      <el-form label-width="80px">
        <el-form-item label="圈子">
          <span>{{ offlineForm.name }}</span>
        </el-form-item>
        <el-form-item label="下架原因">
          <el-input v-model="offlineForm.reason" type="textarea" :rows="3" placeholder="请填写下架原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="offlineVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmOffline">确认下架</el-button>
      </template>
    </el-dialog>

    <!-- 驳回申请弹窗 -->
    <el-dialog v-model="rejectApplyVisible" title="驳回创建申请" width="440px">
      <el-form label-width="80px">
        <el-form-item label="驳回原因">
          <el-input v-model="rejectApplyForm.reason" type="textarea" :rows="3" placeholder="请填写驳回原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectApplyVisible = false">取消</el-button>
        <el-button type="warning" @click="confirmRejectApply">确认驳回</el-button>
      </template>
    </el-dialog>

    <!-- 新建圈主弹窗 -->
    <el-dialog v-model="ownerCreateVisible" title="新建圈主" width="460px">
      <el-form :model="ownerCreateForm" label-width="70px">
        <el-form-item label="账号">
          <el-input v-model="ownerCreateForm.account" placeholder="登录账号" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="ownerCreateForm.nickname" placeholder="默认同账号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="ownerCreateForm.password" type="password" show-password placeholder="登录密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ownerCreateVisible = false">取消</el-button>
        <el-button type="primary" :loading="ownerCreateSubmitting" @click="confirmOwnerCreate">保存</el-button>
      </template>
    </el-dialog>

    <!-- 从用户选取圈主弹窗 -->
    <el-dialog v-model="ownerPickVisible" title="从已有用户选取圈主" width="540px">
      <div class="pick-search">
        <el-input v-model="ownerPickKeyword" placeholder="搜索账号/昵称" clearable @keyup.enter="doSearchOwner" />
        <el-button type="primary" :icon="'Search'" @click="doSearchOwner" :loading="ownerPickLoading">搜索</el-button>
      </div>
      <div v-if="ownerPickResults.length > 0" class="pick-results">
        <div v-for="user in ownerPickResults" :key="user.userId" class="pick-user-item">
          <div class="pick-user-info">
            <span class="pick-user-nickname">{{ user.nickname }}</span>
            <span class="pick-user-account">@{{ user.account }}</span>
          </div>
          <el-button type="primary" size="small" @click="confirmOwnerPick(user)">设为圈主</el-button>
        </div>
      </div>
      <div v-else-if="ownerPickKeyword && !ownerPickLoading" class="pick-empty">
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
.search-bar {
  margin-bottom: 16px;
  padding: 16px 20px;
}
.table-card {
  padding: 16px 20px;
}
.circle-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.content-cell {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  color: $text-regular;
}
.text-secondary {
  color: $text-secondary;
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
