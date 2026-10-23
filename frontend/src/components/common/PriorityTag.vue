<script setup>
import { computed } from 'vue'
import { ISSUE_PRIORITY } from '@/utils/constants'

const props = defineProps({
  value: { type: String, default: '' }
})

const item = computed(() => ISSUE_PRIORITY[props.value] || { label: props.value, type: '' })
</script>

<template>
  <el-tag v-if="value" :type="item.type || undefined" size="small" effect="plain">{{ item.label }}</el-tag>
</template>
