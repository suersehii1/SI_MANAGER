<script setup>
import { onMounted, reactive, ref } from 'vue'
import { getPlanVsActual } from '@/api/admin'
import { useLookupStore } from '@/stores/lookup'
import { formatMM, today } from '@/utils/format'
import PlanActualChart from '@/components/admin/PlanActualChart.vue'
import PlanActualTable from '@/components/admin/PlanActualTable.vue'

const lookup = useLookupStore()
// PlanActualToolbar 상태. from/to를 비우면 서버가 프로젝트/할당 기간으로 기본 범위를 정한다.
const query = reactive({ projectId: null, from: null, to: null, asOf: today() })
const data = ref(null)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    data.value = await getPlanVsActual({
      projectId: query.projectId || undefined,
      from: query.from || undefined,
      to: query.to || undefined,
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
      <h2 class="page-title">계획 vs 실제 투입 공기</h2>
    </div>
    <div class="card">
      <div class="toolbar">
        <el-select v-model="query.projectId" placeholder="전체 프로젝트" clearable style="width: 240px">
          <el-option v-for="p in lookup.projects" :key="p.id" :label="`${p.code} ${p.name}`" :value="p.id" />
        </el-select>
        <el-date-picker v-model="query.from" type="month" value-format="YYYY-MM" placeholder="시작 월(자동)" clearable style="width: 140px" />
        <span>~</span>
        <el-date-picker v-model="query.to" type="month" value-format="YYYY-MM" placeholder="종료 월(자동)" clearable style="width: 140px" />
        <span>기준일</span>
        <el-date-picker v-model="query.asOf" type="date" value-format="YYYY-MM-DD" style="width: 140px" />
        <el-button type="primary" @click="load">조회</el-button>
      </div>

      <!-- PlanActualSummaryCards -->
      <el-row v-if="data" :gutter="12" style="margin-bottom: 12px" data-testid="plan-actual-summary">
        <el-col :xs="12" :sm="4"><el-statistic title="총 계획 MM" :value="Number(data.summary.totalPlannedMM)" :precision="2" /></el-col>
        <el-col :xs="12" :sm="4"><el-statistic title="계획 MM (기준일까지)" :value="Number(data.summary.plannedMMToDate)" :precision="2" /></el-col>
        <el-col :xs="12" :sm="4"><el-statistic title="실제 MM (기준일까지)" :value="Number(data.summary.actualMMToDate)" :precision="2" /></el-col>
        <el-col :xs="12" :sm="4">
          <el-statistic title="일정 차이 MM" :value="Number(data.summary.scheduleVarianceMM)" :precision="2"
            :value-style="{ color: Number(data.summary.scheduleVarianceMM) < 0 ? '#f56c6c' : '#67c23a' }" />
        </el-col>
        <el-col :xs="12" :sm="4"><el-statistic title="계획 일수" :value="data.summary.plannedDurationDays ?? '-'" /></el-col>
        <el-col :xs="12" :sm="4"><el-statistic title="실제 일수" :value="data.summary.actualDurationDays ?? '-'" /></el-col>
      </el-row>
      <p v-if="data" class="text-muted">
        범위 {{ data.from || '-' }} ~ {{ data.to || '-' }}, 기준일 {{ data.asOf }}.
        기준일 이후 월의 실제값은 표시하지 않습니다. 누계 실제 {{ formatMM(data.series.filter((s) => s.cumulativeActualMM !== null).slice(-1)[0]?.cumulativeActualMM) }}
      </p>
      <PlanActualChart :series="data?.series || []" />
    </div>
    <div class="card">
      <PlanActualTable :series="data?.series || []" />
    </div>
  </div>
</template>
