<template>
  <el-result icon="warning" title="403" sub-title="접근 권한이 없습니다." data-testid="forbidden-view">
    <template #extra>
      <el-button type="primary" @click="$router.push({ name: 'dashboard' })">대시보드로</el-button>
    </template>
  </el-result>
</template>
