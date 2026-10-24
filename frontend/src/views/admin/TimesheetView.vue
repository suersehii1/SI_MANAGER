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
  <!-- 화면 2영역: ① 조회 조건(항상 표시) ② Grid/Gantt(남은 높이 전체, 행이 많으면 영역 안에서 스크롤) -->
  <div v-loading="loading" class="ts">
    <div class="card ts__filter" data-testid="timesheet-filter">
      <div class="ts__filter-head">
        <h2 class="page-title">월별 타임시트 (MM)</h2>
        <div class="toolbar ts__toolbar" data-testid="timesheet-toolbar">
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
      </div>
      <p class="text-muted ts__note">
        MM = 투입률 × (해당 월 투입 일수 ÷ 해당 월 달력 일수). 실제(ACTUAL)는 기준일 이후를 계산하지 않습니다.
        사용자 월합이 1.00을 초과하면 빨간색으로 표시됩니다. 총합: {{ formatMM(data?.grandTotal) }}
      </p>
    </div>

    <div class="card ts__content" data-testid="timesheet-content">
      <el-tabs v-model="tab" class="ts__tabs">
        <el-tab-pane label="Grid" name="grid">
          <TimesheetGrid :data="data" height="100%" />
        </el-tab-pane>
        <el-tab-pane label="Gantt" name="gantt" lazy>
          <TimesheetGantt :data="data" height="100%" />
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<style scoped>
/* 화면 전체를 탭 아래 높이에 맞춘다 (사용자가 많아도 페이지가 길어지지 않음) */
.ts {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 420px;
}
.ts__filter {
  flex: none;
  margin-bottom: 12px;
}
.ts__filter-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px 16px;
}
.ts__toolbar {
  margin-bottom: 0;
}
.ts__note {
  margin: 8px 0 0;
}

/* ② Grid/Gantt: 남은 높이 전체 */
.ts__content {
  flex: 1;
  min-height: 0;
  margin-bottom: 0;
  display: flex;
  flex-direction: column;
}
.ts__tabs {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.ts__tabs :deep(.el-tabs__content) {
  flex: 1;
  min-height: 0;
}
.ts__tabs :deep(.el-tab-pane) {
  height: 100%;
}
</style>
