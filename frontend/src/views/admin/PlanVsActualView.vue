<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
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

/**
 * 화면은 3영역: ① 조회 조건·요약(항상 표시) ② 차트 ③ 표.
 * ②와 ③은 남은 높이를 60:40으로 나누고, ② 맨 아래 가운데 ▲/▼로 접는다.
 * - 'both'      : 차트 60 / 표 40
 * - 'tableOnly' : ▲ → 차트를 접고 표만 크게
 * - 'chartOnly' : ▼ → 표를 접고 차트만 크게
 * 접힌 상태에서 반대 방향 버튼을 누르면 다시 'both'로 돌아온다.
 */
const view = ref('both')
const chartVisible = computed(() => view.value !== 'tableOnly')
const tableVisible = computed(() => view.value !== 'chartOnly')

function collapseUp() {
  view.value = view.value === 'chartOnly' ? 'both' : 'tableOnly'
}
function collapseDown() {
  view.value = view.value === 'tableOnly' ? 'both' : 'chartOnly'
}

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
  <div v-loading="loading" class="pva">
    <!-- ① 조회 조건 + 요약: 접기와 상관없이 항상 표시 -->
    <div class="card pva__filter" data-testid="plan-actual-filter">
      <div class="pva__filter-head">
        <h2 class="page-title">계획 vs 실제 투입 공기</h2>
        <div class="toolbar pva__toolbar">
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
      </div>

      <!-- PlanActualSummaryCards -->
      <el-row v-if="data" :gutter="12" data-testid="plan-actual-summary">
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
      <p v-if="data" class="text-muted pva__note">
        범위 {{ data.from || '-' }} ~ {{ data.to || '-' }}, 기준일 {{ data.asOf }}.
        기준일 이후 월의 실제값은 표시하지 않습니다. 누계 실제 {{ formatMM(data.series.filter((s) => s.cumulativeActualMM !== null).slice(-1)[0]?.cumulativeActualMM) }}
      </p>
    </div>

    <!-- ②+③ 남은 높이를 차트 60 : 표 40 으로 나눈다 -->
    <div class="pva__split">
      <!-- ② 차트 영역: 맨 아래 가운데에 접기 버튼 -->
      <div class="card pva__chart" :class="{ 'is-full': !tableVisible, 'is-collapsed': !chartVisible }" data-testid="plan-actual-chart-pane">
        <div v-show="chartVisible" class="pva__chart-body">
          <PlanActualChart :series="data?.series || []" height="100%" />
        </div>
        <div class="pva__toggle">
          <el-tooltip :content="view === 'chartOnly' ? '표 다시 보기' : '차트 접기 (표 크게 보기)'" placement="top">
            <el-button size="small" circle :disabled="view === 'tableOnly'" data-testid="pva-collapse-up" @click="collapseUp">▲</el-button>
          </el-tooltip>
          <el-tooltip :content="view === 'tableOnly' ? '차트 다시 보기' : '표 접기 (차트 크게 보기)'" placement="top">
            <el-button size="small" circle :disabled="view === 'chartOnly'" data-testid="pva-collapse-down" @click="collapseDown">▼</el-button>
          </el-tooltip>
        </div>
      </div>

      <!-- ③ 표 영역 -->
      <div v-show="tableVisible" class="card pva__table" :class="{ 'is-full': !chartVisible }" data-testid="plan-actual-table-pane">
        <PlanActualTable :series="data?.series || []" height="100%" />
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 화면 전체를 탭 아래 높이에 맞춘다 (페이지 스크롤 없이 한 화면) */
.pva {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 480px;
}
.pva .card {
  margin-bottom: 12px;
}
.pva__filter {
  flex: none;
}
.pva__filter-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px 16px;
  margin-bottom: 12px;
}
.pva__toolbar {
  margin-bottom: 0;
}
.pva__note {
  margin: 8px 0 0;
}

/* ②+③: 남은 높이 전체 */
.pva__split {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.pva__split .card {
  margin-bottom: 0;
}

/* ② 차트 60 */
.pva__chart {
  flex: 6;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding-bottom: 4px;
}
.pva__chart.is-full {
  flex: 1;
}
/* 차트를 접으면 접기 버튼 줄만 남는다 */
.pva__chart.is-collapsed {
  flex: none;
  padding-top: 4px;
}
.pva__chart-body {
  flex: 1;
  min-height: 0;
}
.pva__toggle {
  flex: none;
  display: flex;
  justify-content: center;
  gap: 8px;
  padding-top: 4px;
}
.pva__toggle .el-button + .el-button {
  margin-left: 0;
}

/* ③ 표 40 */
.pva__table {
  flex: 4;
  min-height: 0;
  margin-top: 12px;
}
.pva__table.is-full {
  flex: 1;
}
</style>
