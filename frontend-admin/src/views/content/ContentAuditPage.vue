<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import DataTable from '@/components/DataTable.vue'
import StatusTag from '@/components/StatusTag.vue'
import {
  getPendingList,
  getPublishedList,
  approvePost,
  rejectPost,
  deletePost,
  type PostInfo,
  type ContentQuery
} from '@/api/content'

/**
 * ContentAuditPage 内容审核页
 * 功能：待审核列表、通过驳回、已发布管理
 * 参数：无
 * 注意事项：通过 Tab 切换待审核/已发布两个视图
 */
const activeTab = ref<'pending' | 'published'>('pending')
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
const detailImages = computed<string[]>(() => {
  const raw = detailData.value?.images
  if (!raw) return []
  if (Array.isArray(raw)) return raw
  try { return JSON.parse(raw as unknown as string) } catch { return [] }
})

/** 驳回弹窗 */
const rejectVisible = ref(false)
const rejectForm = reactive({ postId: 0, reason: '' })

/**
 * 加载帖子列表（待审核或已发布）
 */
async function loadPostList() {
  loading.value = true
  try {
    const api = activeTab.value === 'pending' ? getPendingList : getPublishedList
    const res = await api(query)
    postList.value = res.list
    total.value = res.total
  } catch (e) {
    // ignore
  } finally {
    loading.value = false
  }
}

/**
 * Tab 切换
 * @param tab 目标 tab
 */
function handleTabChange(tab: string) {
  activeTab.value = tab as any
  query.page = 1
  loadPostList()
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
          { key: 'published', label: '已发布' }
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
        <div v-if="detailImages.length" class="post-images">
          <el-image
            v-for="(img, i) in detailImages"
            :key="i"
            :src="img"
            :preview-src-list="detailImages"
            fit="cover"
            class="post-img"
          />
        </div>
        <div v-if="detailData.video" class="post-video">
          <video :src="detailData.video" controls class="video-player" />
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
.post-video {
  margin-bottom: 12px;
}
.video-player {
  width: 100%;
  max-height: 300px;
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
