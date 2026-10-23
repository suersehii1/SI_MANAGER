<script setup>
defineProps({
  summary: { type: Object, default: null }
})
</script>

<template>
  <el-row :gutter="16" class="summary-cards" data-testid="summary-cards">
    <el-col :xs="12" :sm="6">
      <el-card shadow="never">
        <div class="summary-cards__label">전체 프로젝트</div>
        <div class="summary-cards__value">{{ summary?.totalProjects ?? '-' }}</div>
        <div class="text-muted" v-if="summary">
          진행 {{ summary.projectsByStatus.IN_PROGRESS }} · 계획 {{ summary.projectsByStatus.PLANNED }}
          · 보류 {{ summary.projectsByStatus.ON_HOLD }} · 완료 {{ summary.projectsByStatus.COMPLETED }}
        </div>
      </el-card>
    </el-col>
    <el-col :xs="12" :sm="6">
      <el-card shadow="never">
        <div class="summary-cards__label">현재 투입 인원</div>
        <div class="summary-cards__value">{{ summary?.activePersonnel ?? '-' }}</div>
        <div class="text-muted">기준일 {{ summary?.asOf }}</div>
      </el-card>
    </el-col>
    <el-col :xs="12" :sm="6">
      <el-card shadow="never">
        <div class="summary-cards__label">전체 할당 수</div>
        <div class="summary-cards__value">{{ summary?.totalAssignments ?? '-' }}</div>
      </el-card>
    </el-col>
    <el-col :xs="12" :sm="6">
      <el-card shadow="never">
        <div class="summary-cards__label">미해결 이슈</div>
        <div class="summary-cards__value">{{ summary?.openIssues ?? '-' }}</div>
        <div class="text-muted">열림 + 진행 중</div>
      </el-card>
    </el-col>
  </el-row>
</template>

<style scoped>
.summary-cards {
  margin-bottom: 16px;
}
.summary-cards__label {
  color: #606266;
  font-size: 13px;
}
.summary-cards__value {
  font-size: 28px;
  font-weight: 600;
  margin: 6px 0;
}
</style>
