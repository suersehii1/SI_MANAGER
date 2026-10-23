<template>
  <el-result icon="info" title="404" sub-title="페이지를 찾을 수 없습니다." data-testid="not-found-view">
    <template #extra>
      <el-button type="primary" @click="$router.push({ name: 'dashboard' })">대시보드로</el-button>
    </template>
  </el-result>
</template>
