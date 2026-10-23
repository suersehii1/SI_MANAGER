<script setup>
import { reactive, watch } from 'vue'
import { ISSUE_STATUS, ISSUE_PRIORITY, toOptions } from '@/utils/constants'

const props = defineProps({
  filters: { type: Object, required: true },
  projects: { type: Array, default: () => [] },
  users: { type: Array, default: () => [] }
})
const emit = defineEmits(['search', 'reset'])

const local = reactive({ ...props.filters })
watch(() => props.filters, (f) => Object.assign(local, f), { deep: true })

const statusOptions = toOptions(ISSUE_STATUS)
const priorityOptions = toOptions(ISSUE_PRIORITY)

function search() {
  emit('search', { ...local })
}

function reset() {
  Object.assign(local, { projectId: null, status: null, priority: null, assigneeId: null, keyword: '' })
  emit('reset', { ...local })
}

// 프로젝트 선택은 즉시 반영
function onProjectChange() {
  search()
}
</script>

<template>
  <div class="toolbar issue-filter-bar">
    <el-select v-model="local.projectId" placeholder="전체 프로젝트" clearable style="width: 220px" data-testid="project-filter" @change="onProjectChange">
      <el-option v-for="p in projects" :key="p.id" :label="`${p.code} ${p.name}`" :value="p.id" />
    </el-select>
    <el-select v-model="local.status" placeholder="상태" clearable style="width: 120px">
      <el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" />
    </el-select>
    <el-select v-model="local.priority" placeholder="우선순위" clearable style="width: 120px">
      <el-option v-for="o in priorityOptions" :key="o.value" :label="o.label" :value="o.value" />
    </el-select>
    <el-select v-model="local.assigneeId" placeholder="담당자" clearable filterable style="width: 140px">
      <el-option v-for="u in users" :key="u.id" :label="u.name" :value="u.id" />
    </el-select>
    <el-input v-model="local.keyword" placeholder="제목/설명 검색" clearable style="width: 200px" maxlength="100" @keyup.enter="search" />
    <el-button type="primary" @click="search">검색</el-button>
    <el-button @click="reset">초기화</el-button>
  </div>
</template>
