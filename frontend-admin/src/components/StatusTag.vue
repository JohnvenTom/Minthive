<script setup lang="ts">
import { computed } from 'vue'

/**
 * StatusTag 状态标签组件
 * 功能：根据状态码渲染对应颜色的状态标签
 * Props:
 *   - status: 状态值
 *   - type: 业务类型，决定状态映射 user / audit / workorder / circle / common
 */
const props = withDefaults(defineProps<{
  status: string
  type?: 'user' | 'audit' | 'workorder' | 'circle' | 'common'
}>(), {
  type: 'common'
})

/** 状态映射表 */
const statusMap: Record<string, Record<string, { label: string; color: string }>> = {
  user: {
    NORMAL: { label: '正常', color: 'primary' },
    BANNED: { label: '已封禁', color: 'danger' },
    DELETED: { label: '已注销', color: 'info' }
  },
  audit: {
    PENDING: { label: '待审核', color: 'warning' },
    APPROVED: { label: '已通过', color: 'primary' },
    REJECTED: { label: '已驳回', color: 'danger' }
  },
  workorder: {
    PENDING: { label: '待处理', color: 'warning' },
    PROCESSING: { label: '处理中', color: 'primary' },
    RESOLVED: { label: '已处理', color: 'success' },
    REJECTED: { label: '已驳回', color: 'info' }
  },
  circle: {
    ONLINE: { label: '已上线', color: 'primary' },
    OFFLINE: { label: '已下架', color: 'danger' },
    PENDING: { label: '待审核', color: 'warning' },
    REJECTED: { label: '已驳回', color: 'info' }
  },
  common: {
    ACTIVE: { label: '启用', color: 'primary' },
    INACTIVE: { label: '禁用', color: 'info' }
  }
}

/** 当前状态配置 */
const config = computed(() => {
  const map = statusMap[props.type] || statusMap.common
  return map[props.status] || { label: props.status, color: 'info' }
})
</script>

<template>
  <el-tag :type="config.color as any" effect="dark" size="small" round>
    {{ config.label }}
  </el-tag>
</template>
