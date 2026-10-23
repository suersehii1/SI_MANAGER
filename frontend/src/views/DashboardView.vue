<script setup>
import { onMounted, ref } from 'vue'
import { getSummary, getProjects } from '@/api/dashboard'
import { today } from '@/utils/format'
import SummaryCards from '@/components/dashboard/SummaryCards.vue'
import ProjectHeadcountChart from '@/components/dashboard/ProjectHeadcountChart.vue'
import ProjectTimelineChart from '@/components/dashboard/ProjectTimelineChart.vue'
import ProjectSummaryTable from '@/components/dashboard/ProjectSummaryTable.vue'

const asOf = ref(today())
const summary = ref(null)
const projects = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const params = { asOf: asOf.value || undefined }
    const [s, p] = await Promise.all([getSummary(params), getProjects(params)])
    summary.value = s
    projects.value = p
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div v-loading="loading">
    <div class="page-header">
      <h2 class="page-title">SI 인력 투입 현황</h2>
      <div class="toolbar" style="margin: 0">
        <span>기준일</span>
        <el-date-picker v-model="asOf" type="date" value-format="YYYY-MM-DD" :clearable="false" @change="load" />
      </div>
    </div>
    <SummaryCards :summary="summary" />
    <el-row :gutter="16">
      <el-col :md="10" :sm="24">
        <div class="card">
          <h3 style="margin-top: 0">프로젝트별 투입 인원</h3>
          <ProjectHeadcountChart :projects="projects" />
        </div>
      </el-col>
      <el-col :md="14" :sm="24">
        <div class="card">
          <h3 style="margin-top: 0">프로젝트 기간 (계획 vs 실제)</h3>
          <ProjectTimelineChart :projects="projects" :as-of="asOf" />
        </div>
      </el-col>
    </el-row>
    <div class="card">
      <h3 style="margin-top: 0">프로젝트 요약</h3>
      <ProjectSummaryTable :projects="projects" />
    </div>
  </div>
</template>
