<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import DataTable from '@/components/DataTable.vue'
import StatusTag from '@/components/StatusTag.vue'
import {
  getPendingList,
  getPublishedList,
  approvePost,
  rejectPost,
  deletePost,
  getSensitiveWords,
  addSensitiveWord,
  deleteSensitiveWord,
  importSensitiveWords,
  type PostInfo,
  type ContentQuery,
  type SensitiveWord
} from '@/api/content'

/**
 * ContentAuditPage 内容审核页
 * 功能：待审核列表、通过驳回、已发布管理、敏感词库配置、批量导入
 * 参数：无
 * 注意事项：通过 Tab 切换待审核/已发布/敏感词库三个视图
 */
const activeTab = ref<'pending' | 'published' | 'sensitive'>('pending')
const loading = ref(false)

// 待审核 / 已发布 共用列表
const postList = ref<PostInfo[]>([])
const total = ref(0)
const query = reactive<ContentQuery>({
  page: 1,
  pageSize: 10,
  keyword: '',
  type: ''
})

/** 帖子表格列 */
const postColumns = [
  { prop: 'postId', label: '帖子ID', width: 90 },
  { prop: 'nickname', label: '发布人', width: 130, slot: 'nickname' },
  { prop: 'content', label: '内容', minWidth: 240, slot: 'content' },
  { prop: 'circleName', label: '所属圈子', width: 130 },
  { prop: 'aiGenerated', label: 'AI生成', width: 90, align: 'center' as const, slot: 'aiGenerated' },
  { prop: 'likeCount', label: '点赞', width: 80, align: 'center' as const },
  { prop: 'commentCount', label: '评论', width: 80, align: 'center' as const },
  { prop: 'status', label: '状态', width: 100, align: 'center' as const, slot: 'status' },
  { prop: 'createTime', label: '发布时间', width: 170 },
  { prop: 'actions', label: '操作', width: 200, fixed: 'right' as const, slot: 'actions' }
]

/** 详情弹窗 */
const detailVisible = ref(false)
const detailData = ref<PostInfo | null>(null)

/** 驳回弹窗 */
const rejectVisible = ref(false)
const rejectForm = reactive({ postId: 0, reason: '' })

// 敏感词库
const sensitiveLoading = ref(false)
const sensitiveList = ref<SensitiveWord[]>([])
const sensitiveTotal = ref(0)
const sensitiveQuery = reactive({ page: 1, pageSize: 10, keyword: '' })
const newWord = reactive({ word: '', category: '通用' })

/** 批量导入 */
const importVisible = ref(false)
const importText = ref('')
const importCategory = ref('通用')

/**
 * 加载帖子列表（待审核或已发布）
 */
async function loadPostList() {
  loading.value = true
  try {
    const api = activeTab.value === 'pending' ? getPendingList : getPublishedList
    const res = await api(query)
    postList.value = res.data.list
    total.value = res.data.total
  } catch (e) {
    // ignore
  } finally {
    loading.value = false
  }
}

/**
 * 加载敏感词列表
 */
async function loadSensitiveList() {
  sensitiveLoading.value = true
  try {
    const res = await getSensitiveWords(sensitiveQuery)
    sensitiveList.value = res.data.list
    sensitiveTotal.value = res.data.total
  } catch (e) {
    // ignore
  } finally {
    sensitiveLoading.value = false
  }
}

/**
 * Tab 切换
 * @param tab 目标 tab
 */
function handleTabChange(tab: string) {
  activeTab.value = tab as any
  query.page = 1
  if (tab === 'sensitive') {
    loadSensitiveList()
  } else {
    loadPostList()
  }
}

/**
 * 查看详情
 * @param row 行数据
 */
function handleDetail(row: PostInfo) {
  detailData.value = row
  detailVisible.value = true
}

/**
 * 审核通过
 * @param row 行数据
 */
async function handleApprove(row: PostInfo) {
  try {
    await ElMessageBox.confirm(`确认通过帖子「${row.postId}」的审核？`, '审核确认', { type: 'success' })
    await approvePost(row.postId)
    ElMessage.success('已通过审核')
    loadPostList()
  } catch (e) {
    // ignore
  }
}

/**
 * 打开驳回弹窗
 * @param row 行数据
 */
function handleReject(row: PostInfo) {
  rejectForm.postId = row.postId
  rejectForm.reason = ''
  rejectVisible.value = true
}

/**
 * 确认驳回
 */
async function confirmReject() {
  if (!rejectForm.reason) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  try {
    await rejectPost(rejectForm.postId, rejectForm.reason)
    ElMessage.success('已驳回')
    rejectVisible.value = false
    loadPostList()
  } catch (e) {
    // ignore
  }
}

/**
 * 删除已发布内容
 * @param row 行数据
 */
async function handleDelete(row: PostInfo) {
  try {
    await ElMessageBox.confirm(`确认删除帖子「${row.postId}」？此操作不可撤销`, '删除确认', { type: 'warning' })
    await deletePost(row.postId)
    ElMessage.success('已删除')
    loadPostList()
  } catch (e) {
    // ignore
  }
}

/**
 * 添加敏感词
 */
async function handleAddWord() {
  if (!newWord.word) {
    ElMessage.warning('请输入敏感词')
    return
  }
  try {
    await addSensitiveWord(newWord.word, newWord.category)
    ElMessage.success('添加成功')
    newWord.word = ''
    loadSensitiveList()
  } catch (e) {
    // ignore
  }
}

/**
 * 删除敏感词
 * @param row 行数据，el-table 原生插槽 row 类型为 any
 */
async function handleDeleteWord(row: any) {
  try {
    await ElMessageBox.confirm(`确认删除敏感词「${row.word}」？`, '删除确认', { type: 'warning' })
    await deleteSensitiveWord(row.id)
    ElMessage.success('已删除')
    loadSensitiveList()
  } catch (e) {
    // ignore
  }
}

/**
 * 确认批量导入
 */
async function confirmImport() {
  const words = importText.value.split(/[\n,，]/).map((w) => w.trim()).filter(Boolean)
  if (words.length === 0) {
    ElMessage.warning('请输入敏感词，每行一个或用逗号分隔')
    return
  }
  try {
    const res = await importSensitiveWords(words, importCategory.value)
    ElMessage.success(`成功导入 ${res.data.imported} 个敏感词`)
    importVisible.value = false
    importText.value = ''
    loadSensitiveList()
  } catch (e) {
    // ignore
  }
}

onMounted(() => {
  loadPostList()
})
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-title">内容审核</div>
    </div>

    <!-- Tab 切换 -->
    <div class="tab-bar">
      <div
        v-for="tab in [
          { key: 'pending', label: '待审核' },
          { key: 'published', label: '已发布' },
          { key: 'sensitive', label: '敏感词库' }
        ]"
        :key="tab.key"
        class="tab-item"
        :class="{ active: activeTab === tab.key }"
        @click="handleTabChange(tab.key)"
      >
        {{ tab.label }}
      </div>
    </div>

    <!-- 帖子列表（待审核 / 已发布） -->
    <template v-if="activeTab !== 'sensitive'">
      <div class="base-card search-bar">
        <el-form inline>
          <el-form-item label="关键词">
            <el-input v-model="query.keyword" placeholder="内容/发布人" clearable style="width: 220px" @keyup.enter="loadPostList" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="'Search'" @click="loadPostList">查询</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="base-card table-card">
        <DataTable
          :data="postList"
          :columns="postColumns"
          :loading="loading"
          :total="total"
          v-model:page="query.page"
          v-model:pageSize="query.pageSize"
          @update:page="loadPostList"
          @update:pageSize="loadPostList"
        >
          <template #nickname="{ row }">
            <div class="user-cell">
              <el-avatar :size="24" :src="row.avatar">{{ row.nickname?.charAt(0) }}</el-avatar>
              <span>{{ row.nickname }}</span>
            </div>
          </template>
          <template #content="{ row }">
            <div class="content-cell">{{ row.content }}</div>
          </template>
          <template #aiGenerated="{ row }">
            <el-tag v-if="row.aiGenerated" type="warning" size="small" effect="dark">AI</el-tag>
            <span v-else class="text-secondary">手动</span>
          </template>
          <template #status="{ row }">
            <StatusTag :status="row.status" type="audit" />
          </template>
          <template #actions="{ row }">
            <el-button text type="primary" size="small" @click="handleDetail(row)">详情</el-button>
            <template v-if="activeTab === 'pending'">
              <el-button text type="primary" size="small" @click="handleApprove(row)">通过</el-button>
              <el-button text type="warning" size="small" @click="handleReject(row)">驳回</el-button>
            </template>
            <template v-else>
              <el-button text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
            </template>
          </template>
        </DataTable>
      </div>
    </template>

    <!-- 敏感词库 -->
    <template v-else>
      <div class="base-card search-bar">
        <el-form inline>
          <el-form-item label="敏感词">
            <el-input v-model="sensitiveQuery.keyword" placeholder="搜索敏感词" clearable style="width: 220px" @keyup.enter="loadSensitiveList" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="'Search'" @click="loadSensitiveList">查询</el-button>
            <el-button type="warning" :icon="'Upload'" @click="importVisible = true">批量导入</el-button>
          </el-form-item>
        </el-form>
        <el-divider />
        <el-form inline>
          <el-form-item label="新增敏感词">
            <el-input v-model="newWord.word" placeholder="输入敏感词" style="width: 200px" />
          </el-form-item>
          <el-form-item label="分类">
            <el-select v-model="newWord.category" style="width: 140px">
              <el-option label="通用" value="通用" />
              <el-option label="低俗色情" value="低俗色情" />
              <el-option label="广告引流" value="广告引流" />
              <el-option label="人身攻击" value="人身攻击" />
              <el-option label="违法内容" value="违法内容" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="'Plus'" @click="handleAddWord">添加</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="base-card table-card">
        <el-table :data="sensitiveList" v-loading="sensitiveLoading" stripe>
          <el-table-column type="index" label="#" width="60" align="center" />
          <el-table-column prop="word" label="敏感词" min-width="200" />
          <el-table-column prop="category" label="分类" width="160">
            <template #default="{ row }">
              <el-tag size="small" effect="dark">{{ row.category }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="180" />
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button text type="danger" size="small" @click="handleDeleteWord(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="table-pagination">
          <el-pagination
            v-model:current-page="sensitiveQuery.page"
            v-model:page-size="sensitiveQuery.pageSize"
            :total="sensitiveTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, prev, pager, next"
            background
            @current-change="loadSensitiveList"
          />
        </div>
      </div>
    </template>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="帖子详情" width="600px">
      <div v-if="detailData" class="post-detail">
        <div class="post-author">
          <el-avatar :size="40" :src="detailData.avatar">{{ detailData.nickname?.charAt(0) }}</el-avatar>
          <div>
            <div class="author-name">{{ detailData.nickname }}</div>
            <div class="author-time mono">{{ detailData.createTime }}</div>
          </div>
        </div>
        <div class="post-content">{{ detailData.content }}</div>
        <div v-if="detailData.images?.length" class="post-images">
          <el-image
            v-for="(img, i) in detailData.images"
            :key="i"
            :src="img"
            :preview-src-list="detailData.images"
            fit="cover"
            class="post-img"
          />
        </div>
        <div class="post-meta">
          <span>点赞 {{ detailData.likeCount }}</span>
          <span>评论 {{ detailData.commentCount }}</span>
          <span v-if="detailData.circleName">圈子：{{ detailData.circleName }}</span>
        </div>
      </div>
    </el-dialog>

    <!-- 驳回弹窗 -->
    <el-dialog v-model="rejectVisible" title="驳回帖子" width="440px">
      <el-form label-width="80px">
        <el-form-item label="驳回原因">
          <el-input v-model="rejectForm.reason" type="textarea" :rows="4" placeholder="请填写驳回原因，将通知用户" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="warning" @click="confirmReject">确认驳回</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入弹窗 -->
    <el-dialog v-model="importVisible" title="批量导入敏感词" width="500px">
      <el-form label-width="80px">
        <el-form-item label="分类">
          <el-select v-model="importCategory" style="width: 200px">
            <el-option label="通用" value="通用" />
            <el-option label="低俗色情" value="低俗色情" />
            <el-option label="广告引流" value="广告引流" />
            <el-option label="人身攻击" value="人身攻击" />
            <el-option label="违法内容" value="违法内容" />
          </el-select>
        </el-form-item>
        <el-form-item label="敏感词">
          <el-input v-model="importText" type="textarea" :rows="8" placeholder="每行一个敏感词，或用逗号分隔" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmImport">确认导入</el-button>
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
  &:hover {
    color: $color-primary;
  }
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
.user-cell {
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
.post-detail {
  padding: 0 8px;
}
.post-author {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.author-name {
  font-size: 15px;
  font-weight: 600;
  color: $text-primary;
}
.author-time {
  font-size: 12px;
  color: $text-secondary;
  margin-top: 2px;
}
.post-content {
  color: $text-regular;
  line-height: 1.7;
  margin-bottom: 12px;
  white-space: pre-wrap;
}
.post-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}
.post-img {
  width: 100px;
  height: 100px;
  border-radius: $radius-md;
}
.post-meta {
  display: flex;
  gap: 20px;
  font-size: 13px;
  color: $text-secondary;
  padding-top: 12px;
  border-top: 1px solid $border-divider;
}
.table-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0 4px;
}
</style>
