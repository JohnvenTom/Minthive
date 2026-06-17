<script setup lang="ts" generic="T extends Record<string, any>">
import { computed } from 'vue'

/**
 * DataTable 通用数据表格组件
 * 功能：封装 Element Plus 表格，统一暗色斑马纹、分页、loading
 * Props:
 *   - data: 表格数据
 *   - columns: 列配置
 *   - loading: 加载状态
 *   - total: 数据总数
 *   - page: 当前页
 *   - pageSize: 每页条数
 *   - height: 表格高度
 * Emits:
 *   - update:page: 页码变化
 *   - update:pageSize: 每页条数变化
 */
interface Column {
  prop: string
  label: string
  width?: string | number
  minWidth?: string | number
  fixed?: 'left' | 'right'
  align?: 'left' | 'center' | 'right'
  slot?: string // 自定义列插槽名
}

const props = withDefaults(defineProps<{
  data: T[]
  columns: Column[]
  loading?: boolean
  total?: number
  page?: number
  pageSize?: number
  height?: string | number
}>(), {
  loading: false,
  total: 0,
  page: 1,
  pageSize: 10
})

const emit = defineEmits<{
  (e: 'update:page', val: number): void
  (e: 'update:pageSize', val: number): void
}>()

const currentPage = computed({
  get: () => props.page,
  set: (v) => emit('update:page', v)
})
const pageSizeModel = computed({
  get: () => props.pageSize,
  set: (v) => emit('update:pageSize', v)
})
</script>

<template>
  <div class="data-table">
    <el-table
      :data="data"
      v-loading="loading"
      :height="height"
      stripe
      style="width: 100%"
      :header-cell-style="{ background: 'var(--el-table-header-bg-color)' }"
    >
      <el-table-column type="index" label="#" width="60" align="center" />
      <template v-for="col in columns" :key="col.prop">
        <el-table-column
          :prop="col.prop"
          :label="col.label"
          :width="col.width"
          :min-width="col.minWidth"
          :fixed="col.fixed"
          :align="col.align || 'left'"
        >
          <template #default="scope">
            <slot v-if="col.slot" :name="col.slot" :row="(scope.row as T)" :index="scope.$index" />
            <span v-else>{{ scope.row[col.prop] }}</span>
          </template>
        </el-table-column>
      </template>
      <slot name="append" />
    </el-table>

    <div class="table-pagination" v-if="total > 0">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSizeModel"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        background
      />
    </div>
  </div>
</template>

<style lang="scss" scoped>
.data-table {
  width: 100%;
}
.table-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0 4px;
}
</style>
