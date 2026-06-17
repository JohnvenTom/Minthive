<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import DataTable from '@/components/DataTable.vue'
import StatusTag from '@/components/StatusTag.vue'
import RiskLevelTag from '@/components/RiskLevelTag.vue'
import {
  getReportList,
  getReportDetail,
  rejectReport,
  deleteReportContent,
  punishUser,
  type ReportWorkOrder,
  type ReportQuery,
  type PunishParams
} from '@/api/report'
import type { RiskLevel, WorkOrderStatus } from '@/api/types'

/**
 * ReportWorkOrderPage 举报工单页
 * 功能：列表、查看、驳回、删除内容、处罚，按 AI 风险等级排序
 * 参数：无
 * 注意事项：默认按风险等级降序排列，高危工单优先展示
 */
const loading = ref(false)
const reportList = ref<ReportWorkOrder[]>([])
const total = ref(0)
const query = reactive<ReportQuery>({
  page: 1,
  pageSize: 10,
  status: undefined,
  riskLevel: undefined,
  type: ''
})

/** 表格列配置 - 风险等级列在前 */
const columns = [
  { prop: 'riskLevel', label: '风险等级', width: 120, align: 'center' as const, slot: 'riskLevel' },
  { prop: 'workOrderId', label: '工单ID', width: 90 },
  { prop: 'type', label: '举报类型', width: 120 },
  { prop: 'reporterName', label: '举报人', width: 120 },
  { prop: 'accusedName', label: '被举报人', width: 120 },
  { prop: 'targetContent', label: '举报内容', minWidth: 220, slot: 'targetContent' },
  { prop: 'status', label: '状态', width: 100, align: 'center' as const, slot: 'status' },
  { prop: 'createTime', label: '举报时间', width: 170 },
  { prop: 'actions', label: '操作', width: 240, fixed: 'right' as const, slot: 'actions' }
]

/** 详情弹窗 */
const detailVisible = ref(false)
const detailData = ref<ReportWorkOrder | null>(null)

/** 驳回弹窗 */
const rejectVisible = ref(false)
const rejectForm = reactive({ workOrderId: 0, remark: '' })

/** 处罚弹窗 */
const punishVisible = ref(false)
const punishForm = reactive<PunishParams>({
  workOrderId: 0,
  action: 'WARN',
  duration: 24,
  remark: ''
})

/** 风险等级排序权重（用于前端兜底排序） */
const riskWeight: Record<RiskLevel, number> = { HIGH: 3, MEDIUM: 2, LOW: 1 }

/**
 * 加载工单列表
 */
async function loadList() {
  loading.value = true
  try {
    const res = await getReportList(query)
    // 前端兜底按风险等级降序排序
    reportList.value = res.list.sort((a, b) => riskWeight[b.riskLevel] - riskWeight[a.riskLevel])
    total.value = res.total
  } catch (e) {
    // ignore
  } finally {
    loading.value = false
  }
}

/**
 * 查看详情
 * @param row 行数据
 */
async function handleDetail(row: ReportWorkOrder) {
  try {
    const res = await getReportDetail(row.workOrderId)
    detailData.value = res
    detailVisible.value = true
  } catch (e) {
    // ignore
  }
}

/**
 * 打开驳回弹窗
 * @param row 行数据
 */
function handleReject(row: ReportWorkOrder) {
  rejectForm.workOrderId = row.workOrderId
  rejectForm.remark = ''
  rejectVisible.value = true
}

/**
 * 确认驳回
 */
async function confirmReject() {
  if (!rejectForm.remark) {
    ElMessage.warning('请填写驳回说明')
    return
  }
  try {
    await rejectReport(rejectForm.workOrderId, rejectForm.remark)
    ElMessage.success('已驳回')
    rejectVisible.value = false
    loadList()
  } catch (e) {
    // ignore
  }
}

/**
 * 删除违规内容
 * @param row 行数据
 */
async function handleDeleteContent(row: ReportWorkOrder) {
  try {
    await ElMessageBox.confirm(`确认删除被举报内容（工单 ${row.workOrderId}）？`, '删除确认', { type: 'warning' })
    await deleteReportContent(row.workOrderId)
    ElMessage.success('内容已删除')
    loadList()
  } catch (e) {
    // ignore
  }
}

/**
 * 打开处罚弹窗
 * @param row 行数据
 */
function handlePunish(row: ReportWorkOrder) {
  punishForm.workOrderId = row.workOrderId
  punishForm.action = 'WARN'
  punishForm.duration = 24
  punishForm.remark = ''
  punishVisible.value = true
}

/**
 * 确认处罚
 */
async function confirmPunish() {
  if (!punishForm.remark) {
    ElMessage.warning('请填写处罚说明')
    return
  }
  try {
    await punishUser(punishForm)
    ElMessage.success('处罚已执行')
    punishVisible.value = false
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
    <div class="page-header">
      <div class="page-title">举报工单</div>
      <div class="header-tip">
        <el-icon><InfoFilled /></el-icon>
        <span>工单按 AI 风险等级自动排序，高危工单优先处理</span>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="base-card search-bar">
      <el-form inline>
        <el-form-item label="风险等级">
          <el-select v-model="query.riskLevel" placeholder="全部" clearable style="width: 140px">
            <el-option label="高风险" value="HIGH" />
            <el-option label="中风险" value="MEDIUM" />
            <el-option label="低风险" value="LOW" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="待处理" value="PENDING" />
            <el-option label="处理中" value="PROCESSING" />
            <el-option label="已处理" value="RESOLVED" />
            <el-option label="已驳回" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.type" placeholder="全部" clearable style="width: 140px">
            <el-option label="低俗色情" value="低俗色情" />
            <el-option label="广告引流" value="广告引流" />
            <el-option label="人身攻击" value="人身攻击" />
            <el-option label="违法内容" value="违法内容" />
            <el-option label="抄袭搬运" value="抄袭搬运" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="'Search'" @click="loadList">查询</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 表格 -->
    <div class="base-card table-card">
      <DataTable
        :data="reportList"
        :columns="columns"
        :loading="loading"
        :total="total"
        v-model:page="query.page"
        v-model:pageSize="query.pageSize"
        @update:page="loadList"
        @update:pageSize="loadList"
      >
        <template #riskLevel="{ row }">
          <RiskLevelTag :level="row.riskLevel" />
        </template>
        <template #targetContent="{ row }">
          <div class="content-cell">{{ row.targetContent }}</div>
        </template>
        <template #status="{ row }">
          <StatusTag :status="row.status" type="workorder" />
        </template>
        <template #actions="{ row }">
          <el-button text type="primary" size="small" @click="handleDetail(row)">详情</el-button>
          <template v-if="row.status === 'PENDING'">
            <el-button text type="info" size="small" @click="handleReject(row)">驳回</el-button>
            <el-button text type="warning" size="small" @click="handleDeleteContent(row)">删除内容</el-button>
            <el-button text type="danger" size="small" @click="handlePunish(row)">处罚</el-button>
          </template>
        </template>
      </DataTable>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="工单详情" width="600px">
      <div v-if="detailData" class="report-detail">
        <div class="detail-top">
          <div class="detail-id mono">工单 #{{ detailData.workOrderId }}</div>
          <RiskLevelTag :level="detailData.riskLevel" />
          <StatusTag :status="detailData.status" type="workorder" />
        </div>
        <el-descriptions :column="2" border class="detail-desc">
          <el-descriptions-item label="举报类型">{{ detailData.type }}</el-descriptions-item>
          <el-descriptions-item label="举报时间">{{ detailData.createTime }}</el-descriptions-item>
          <el-descriptions-item label="举报人">{{ detailData.reporterName }}（ID: {{ detailData.reporterId }}）</el-descriptions-item>
          <el-descriptions-item label="被举报人">{{ detailData.accusedName }}（ID: {{ detailData.accusedId }}）</el-descriptions-item>
          <el-descriptions-item label="举报原因" :span="2">{{ detailData.reason }}</el-descriptions-item>
          <el-descriptions-item label="被举报内容" :span="2">
            <div class="target-content">{{ detailData.targetContent }}</div>
          </el-descriptions-item>
          <el-descriptions-item v-if="detailData.aiAnalysis" label="AI 分析" :span="2">
            <div class="ai-analysis">{{ detailData.aiAnalysis }}</div>
          </el-descriptions-item>
          <el-descriptions-item v-if="detailData.handleResult" label="处理结果" :span="2">
            {{ detailData.handleResult }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>

    <!-- 驳回弹窗 -->
    <el-dialog v-model="rejectVisible" title="驳回举报" width="440px">
      <el-form label-width="80px">
        <el-form-item label="驳回说明">
          <el-input v-model="rejectForm.remark" type="textarea" :rows="4" placeholder="请填写驳回说明，将反馈给举报人" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="info" @click="confirmReject">确认驳回</el-button>
      </template>
    </el-dialog>

    <!-- 处罚弹窗 -->
    <el-dialog v-model="punishVisible" title="处罚违规用户" width="480px">
      <el-form :model="punishForm" label-width="100px">
        <el-form-item label="处罚方式">
          <el-radio-group v-model="punishForm.action">
            <el-radio value="WARN">警告</el-radio>
            <el-radio value="BAN_USER">封禁用户</el-radio>
            <el-radio value="DELETE_CONTENT">删除内容</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="punishForm.action === 'BAN_USER'" label="封禁时长">
          <el-radio-group v-model="punishForm.duration">
            <el-radio :value="24">24小时</el-radio>
            <el-radio :value="72">3天</el-radio>
            <el-radio :value="0">永久</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="处罚说明">
          <el-input v-model="punishForm.remark" type="textarea" :rows="3" placeholder="请填写处罚说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="punishVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmPunish">确认处罚</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.header-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: $text-secondary;
  .el-icon { color: $color-warning; }
}
.search-bar {
  margin-bottom: 16px;
  padding: 16px 20px;
}
.table-card {
  padding: 16px 20px;
}
.content-cell {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  color: $text-regular;
}
.report-detail {
  padding: 0 8px;
}
.detail-top {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid $border-divider;
}
.detail-id {
  font-size: 16px;
  font-weight: 600;
  color: $text-primary;
}
.target-content {
  background: $bg-input;
  padding: 10px;
  border-radius: $radius-md;
  color: $text-regular;
  line-height: 1.6;
}
.ai-analysis {
  background: rgba(78, 205, 196, 0.08);
  border: 1px solid rgba(78, 205, 196, 0.3);
  padding: 10px;
  border-radius: $radius-md;
  color: $color-primary;
  font-size: 13px;
  line-height: 1.6;
}
</style>
