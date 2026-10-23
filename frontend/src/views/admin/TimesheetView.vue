<script setup>
import { onMounted, reactive, ref } from 'vue'
import dayjs from 'dayjs'
import { getTimesheet } from '@/api/admin'
import { useLookupStore } from '@/stores/lookup'
import { formatMM, today } from '@/utils/format'
import TimesheetGrid from '@/components/admin/TimesheetGrid.vue'
import TimesheetGantt from '@/components/admin/TimesheetGantt.vue'

const lookup = useLookupStore()
const year = dayjs().year()
// TimesheetToolbar 상태
const query = reactive({ from: `${year}-01`, to: `${year}-12`, basis: 'ACTUAL', projectId: null, asOf: today() })
const data = ref(null)
const loading = ref(false)
const tab = ref('grid')

async function load() {
  loading.value = true
  try {
    data.value = await getTimesheet({
      from: query.from,
      to: query.to,
      basis: query.basis,
      projectId: query.projectId || undefined,
      asOf: query.asOf || undefined
    })
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  lookup.loadProjects().catch(() => {})
  load()
})
</script>

<template>
  <div v-loading="loading">
    <div class="page-header">
      <h2 class="page-title">월별 타임시트 (MM)</h2>
    </div>
    <div class="card">
      <div class="toolbar" data-testid="timesheet-toolbar">
        <el-date-picker v-model="query.from" type="month" value-format="YYYY-MM" placeholder="시작 월" style="width: 130px" />
        <span>~</span>
        <el-date-picker v-model="query.to" type="month" value-format="YYYY-MM" placeholder="종료 월" style="width: 130px" />
        <el-radio-group v-model="query.basis">
          <el-radio-button value="PLAN">계획</el-radio-button>
          <el-radio-button value="ACTUAL">실제</el-radio-button>
        </el-radio-group>
        <el-select v-model="query.projectId" placeholder="전체 프로젝트" clearable style="width: 220px">
          <el-option v-for="p in lookup.projects" :key="p.id" :label="`${p.code} ${p.name}`" :value="p.id" />
        </el-select>
        <span>기준일</span>
        <el-date-picker v-model="query.asOf" type="date" value-format="YYYY-MM-DD" style="width: 140px" />
        <el-button type="primary" @click="load">조회</el-button>
      </div>
      <p class="text-muted">
        MM = 투입률 × (해당 월 투입 일수 ÷ 해당 월 달력 일수). 실제(ACTUAL)는 기준일 이후를 계산하지 않습니다.
        사용자 월합이 1.00을 초과하면 빨간색으로 표시됩니다. 총합: {{ formatMM(data?.grandTotal) }}
      </p>
      <el-tabs v-model="tab">
        <el-tab-pane label="Grid" name="grid">
          <TimesheetGrid :data="data" />
        </el-tab-pane>
        <el-tab-pane label="Gantt" name="gantt" lazy>
          <TimesheetGantt :data="data" />
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>
