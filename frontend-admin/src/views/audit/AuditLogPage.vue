<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import DataTable from '@/components/DataTable.vue'
import {
  exportAudit,
  getAuditDetail,
  getAuditPage,
  type AuditChange,
  type AuditDetail,
  type AuditLog,
  type AuditQuery
} from '@/api/audit'

/**
 * AuditLogPage 操作审计日志页
 * 功能：按时间/主体/表名/操作/结果/路径/traceId 查询审计记录，
 *   查看字段级前后值，并导出 CSV（导出动作本身会留痕）
 * 注意事项：仅管理员可访问，接口由 /api/admin 拦截器保护
 */

const loading = ref(false)
const logs = ref<AuditLog[]>([])
const total = ref(0)
const query = reactive<AuditQuery>({
  page: 1,
  pageSize: 20,
  operatorType: undefined,
  tableName: '',
  operation: undefined,
  resultCode: undefined,
  path: '',
  traceId: ''
})

/** 结果码输入（文本，查询时转数字，避免未填写时被当作 0） */
const resultCodeText = ref('')

/** 时间范围（Element Plus datetimerange） */
const timeRange = ref<[string, string] | null>(null)

/** 详情弹窗 */
const detailVisible = ref(false)
const detail = ref<AuditDetail | null>(null)
const detailLoading = ref(false)

/** 表格列配置 */
const columns = [
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'createTime', label: '时间', width: 165, slot: 'createTime' },
  { prop: 'operatorType', label: '主体', width: 100, slot: 'operator' },
  { prop: 'operatorLabel', label: '账号/任务', width: 130, slot: 'label' },
  { prop: 'requestPath', label: '方法 / 路径', minWidth: 240, slot: 'path' },
  { prop: 'clientIp', label: 'IP', width: 130, slot: 'ip' },
  { prop: 'resultCode', label: '结果', width: 120, slot: 'result' },
  { prop: 'durationMs', label: '耗时(ms)', width: 100, align: 'right' as const },
  { prop: 'changeCount', label: '变更数', width: 90, align: 'center' as const, slot: 'changes' },
  { prop: 'actions', label: '操作', width: 90, fixed: 'right' as const, align: 'center' as const, slot: 'actions' }
]

/** 主体类型选项 */
const operatorOptions = [
  { label: '用户', value: 'USER' },
  { label: '管理员', value: 'ADMIN' },
  { label: '匿名', value: 'ANONYMOUS' },
  { label: '系统', value: 'SYSTEM' }
]

/** 操作类型选项 */
const operationOptions = [
  { label: 'INSERT', value: 'INSERT' },
  { label: 'UPDATE', value: 'UPDATE' },
  { label: 'DELETE', value: 'DELETE' }
]

/**
 * 组装查询参数（去掉空值并处理时间范围）
 * @returns 后端可接受的查询参数
 */
function buildParams(): AuditQuery {
  return {
    page: query.page,
    pageSize: query.pageSize,
    operatorType: query.operatorType || undefined,
    tableName: query.tableName || undefined,
    operation: query.operation || undefined,
    resultCode: resultCodeText.value.trim() ? Number(resultCodeText.value.trim()) : undefined,
    path: query.path || undefined,
    traceId: query.traceId || undefined,
    startTime: timeRange.value?.[0] || undefined,
    endTime: timeRange.value?.[1] || undefined
  }
}

/**
 * 加载审计列表
 * @returns 无返回值
 */
async function loadList(): Promise<void> {
  loading.value = true
  try {
    const res = await getAuditPage(buildParams())
    logs.value = res.list || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

/**
 * 查询（重置到第一页）
 */
function search(): void {
  query.page = 1
  loadList()
}

/**
 * 重置筛选条件
 */
function reset(): void {
  query.operatorType = undefined
  query.tableName = ''
  query.operation = undefined
  query.resultCode = undefined
  resultCodeText.value = ''
  query.path = ''
  query.traceId = ''
  timeRange.value = null
  search()
}

/**
 * 查看审计详情
 * @param row 审计主记录
 */
async function openDetail(row: AuditLog): Promise<void> {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await getAuditDetail(row.id)
  } finally {
    detailLoading.value = false
  }
}

/**
 * 格式化时间（后端 ISO → 可读格式）
 * @param value 时间字符串
 * @returns 形如 2026-11-12 01:23:45
 */
function formatTime(value?: string): string {
  return value ? value.replace('T', ' ').replace(/\.\d+$/, '') : '-'
}

/**
 * 格式化前后快照（校验 JSON 并缩进，失败时原样展示）
 * @param value 快照字符串
 * @returns 展示文本
 */
function formatSnapshot(value?: string): string {
  if (!value) return '（无）'
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

/**
 * 导出 CSV
 */
async function handleExport(): Promise<void> {
  try {
    const blob = await exportAudit(buildParams())
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `audit-${Date.now()}.csv`
    link.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出已开始')
  } catch {
    ElMessage.error('导出失败')
  }
}

/**
 * 变更操作类型对应的标签颜色
 * @param operation 操作类型
 */
function operationType(operation: AuditChange['operation']): 'success' | 'warning' | 'danger' {
  if (operation === 'INSERT') return 'success'
  if (operation === 'DELETE') return 'danger'
  return 'warning'
}

onMounted(loadList)
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-title">操作审计</div>
      <div class="header-actions">
        <el-button type="primary" plain @click="handleExport">导出 CSV</el-button>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="base-card search-bar">
      <el-form inline>
        <el-form-item label="时间">
          <el-date-picker
            v-model="timeRange"
            type="datetimerange"
            value-format="YYYY-MM-DDTHH:mm:ss"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 360px"
          />
        </el-form-item>
        <el-form-item label="主体">
          <el-select v-model="query.operatorType" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="item in operatorOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="表名">
          <el-input v-model="query.tableName" placeholder="如 user/post" clearable style="width: 140px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="操作">
          <el-select v-model="query.operation" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="item in operationOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="结果码">
          <el-input v-model="resultCodeText" placeholder="如 200" clearable style="width: 110px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="路径">
          <el-input v-model="query.path" placeholder="/api/message" clearable style="width: 180px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="traceId">
          <el-input v-model="query.traceId" placeholder="链路ID" clearable style="width: 180px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 列表 -->
    <div class="base-card table-card">
      <DataTable
        :data="logs"
        :columns="columns"
        :loading="loading"
        :total="total"
        v-model:page="query.page"
        v-model:pageSize="query.pageSize"
        @update:page="loadList"
        @update:pageSize="loadList"
      >
        <template #createTime="{ row }">{{ formatTime(row.createTime) }}</template>
        <template #operator="{ row }">
          <el-tag size="small" :type="row.operatorType === 'ADMIN' ? 'danger' : row.operatorType === 'SYSTEM' ? 'info' : 'success'">
            {{ row.operatorType }}
          </el-tag>
        </template>
        <template #label="{ row }">{{ row.operatorLabel || row.operatorId || '-' }}</template>
        <template #path="{ row }">
          <div class="path-cell">
            <span class="method">{{ row.httpMethod || '-' }}</span>
            <span>{{ row.requestPath || '-' }}</span>
          </div>
        </template>
        <template #ip="{ row }">{{ row.clientIp || row.remoteAddr || '-' }}</template>
        <template #result="{ row }">
          <span :class="['result-code', row.resultCode === 200 ? 'ok' : 'fail']">{{ row.resultCode ?? '-' }}</span>
          <span class="result-msg">{{ row.resultMsg }}</span>
        </template>
        <template #changes="{ row }">
          <el-badge :value="row.changeCount || 0" :type="row.truncated ? 'warning' : 'primary'" />
        </template>
        <template #actions="{ row }">
          <el-button text type="primary" size="small" @click="openDetail(row)">详情</el-button>
        </template>
      </DataTable>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="审计详情" width="920px" destroy-on-close>
      <div v-loading="detailLoading">
        <template v-if="detail">
          <el-descriptions :column="3" border size="small">
            <el-descriptions-item label="记录ID">{{ detail.log.id }}</el-descriptions-item>
            <el-descriptions-item label="时间">{{ formatTime(detail.log.createTime) }}</el-descriptions-item>
            <el-descriptions-item label="主体">
              {{ detail.log.operatorType }} / {{ detail.log.operatorLabel || detail.log.operatorId || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="方法">{{ detail.log.httpMethod || '-' }}</el-descriptions-item>
            <el-descriptions-item label="路径" :span="2">{{ detail.log.requestPath || '-' }}</el-descriptions-item>
            <el-descriptions-item label="IP">{{ detail.log.clientIp || detail.log.remoteAddr || '-' }}</el-descriptions-item>
            <el-descriptions-item label="结果">
              {{ detail.log.resultCode ?? '-' }} / {{ detail.log.resultMsg || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="耗时">{{ detail.log.durationMs ?? '-' }} ms</el-descriptions-item>
            <el-descriptions-item label="traceId" :span="3">{{ detail.log.traceId || '-' }}</el-descriptions-item>
          </el-descriptions>

          <el-table :data="detail.changes" border size="small" class="change-table">
            <el-table-column prop="tableName" label="表名" width="130" />
            <el-table-column prop="recordId" label="主键" width="90" />
            <el-table-column label="操作" width="90" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="operationType(row.operation)">{{ row.operation }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="affectedRows" label="影响行" width="80" align="center" />
            <el-table-column label="字段级前后值" min-width="420">
              <template #default="{ row }">
                <div class="snapshot-block">
                  <div class="snapshot-title">变更前</div>
                  <pre class="snapshot-pre">{{ formatSnapshot(row.beforeJson) }}</pre>
                  <div class="snapshot-title">变更后</div>
                  <pre class="snapshot-pre">{{ formatSnapshot(row.afterJson) }}</pre>
                  <div v-if="row.maskedColumns" class="masked-tip">已脱敏列：{{ row.maskedColumns }}</div>
                  <div v-if="row.truncated" class="truncated-tip">快照超长已截断</div>
                  <div v-if="row.sqlDigest && !row.beforeJson && !row.afterJson" class="masked-tip">
                    无法解析主键，仅记录 SQL 摘要：{{ row.sqlDigest }}
                  </div>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;

.header-actions {
  display: flex;
  gap: 8px;
}

.search-bar {
  margin-bottom: 16px;
  padding: 16px 20px;
}

.table-card {
  padding: 16px 20px;
}

.path-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  overflow: hidden;

  .method {
    flex-shrink: 0;
    font-size: 11px;
    color: $text-secondary;
    border: 1px solid $border-divider;
    border-radius: 3px;
    padding: 0 4px;
  }

  span:last-child {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.result-code {
  font-weight: 600;
  margin-right: 6px;

  &.ok {
    color: $color-success;
  }

  &.fail {
    color: $color-danger;
  }
}

.result-msg {
  font-size: 12px;
  color: $text-secondary;
}

.change-table {
  margin-top: 16px;
}

.snapshot-block {
  padding: 4px 0;
}

.snapshot-title {
  font-size: 12px;
  font-weight: 600;
  color: $text-secondary;
  margin: 6px 0 4px;
}

.snapshot-pre {
  margin: 0;
  padding: 8px 10px;
  max-height: 180px;
  overflow: auto;
  background: $bg-input;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-all;
}

.masked-tip {
  margin-top: 6px;
  font-size: 12px;
  color: $color-warning;
}

.truncated-tip {
  margin-top: 4px;
  font-size: 12px;
  color: $color-danger;
}
</style>
