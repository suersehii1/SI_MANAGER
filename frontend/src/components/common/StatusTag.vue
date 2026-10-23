<script setup>
import { computed } from 'vue'
import { ISSUE_STATUS, PROJECT_STATUS, WBS_STATUS } from '@/utils/constants'

const props = defineProps({
  value: { type: String, default: '' },
  kind: { type: String, default: 'issue' } // issue | project | wbs
})

const item = computed(() => {
  const map = props.kind === 'project' ? PROJECT_STATUS : props.kind === 'wbs' ? WBS_STATUS : ISSUE_STATUS
  return map[props.value] || { label: props.value, type: '' }
})
</script>

<template>
  <el-tag v-if="value" :type="item.type || undefined" size="small">{{ item.label }}</el-tag>
</template>
