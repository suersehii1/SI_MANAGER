<script setup>
import { computed } from 'vue'
import { useWbsStore } from '@/stores/wbs'

const props = defineProps({
  projects: { type: Array, default: () => [] },
  projectId: { type: Number, default: null },
  view: { type: String, default: 'grid' }
})
const emit = defineEmits([
  'update:view', 'project-change', 'add-root', 'add-child', 'edit', 'move-up', 'move-down',
  'indent', 'outdent', 'delete', 'expand-all', 'collapse-all'
])

const store = useWbsStore()
const selected = computed(() => store.selectedItem)

// 버튼 활성 조건: 선택 행이 있어야 하고, 이동 헬퍼가 null(불가)이면 비활성
const canAddChild = computed(() => selected.value && selected.value.level < 5)
const canUp = computed(() => selected.value && store.moveUpTarget(selected.value.id) !== null)
const canDown = computed(() => selected.value && store.moveDownTarget(selected.value.id) !== null)
const canIndent = computed(() => selected.value && store.indentTarget(selected.value.id) !== null)
const canOutdent = computed(() => selected.value && store.outdentTarget(selected.value.id) !== null)
</script>

<template>
  <div class="wbs-toolbar">
    <div class="toolbar">
      <el-select
        :model-value="projectId"
        placeholder="프로젝트 선택"
        filterable
        style="width: 260px"
        data-testid="wbs-project-select"
        @change="(v) => emit('project-change', v)"
      >
        <el-option v-for="p in props.projects" :key="p.id" :label="`${p.code} ${p.name}`" :value="p.id" />
      </el-select>
      <div v-if="projectId" class="wbs-toolbar__progress" data-testid="wbs-overall-progress">
        <span>전체 진척률</span>
        <el-progress :percentage="store.overallProgress" :stroke-width="14" style="width: 200px" :format="(p) => `${p.toFixed(1)}%`" />
      </div>
      <span v-if="projectId && !store.canEdit" class="text-muted">조회 전용 (ADMIN 또는 이 프로젝트의 PM만 편집 가능)</span>
    </div>
    <div v-if="projectId" class="toolbar">
      <template v-if="store.canEdit">
        <el-button size="small" type="primary" data-testid="wbs-add-root" @click="emit('add-root')">최상위 추가</el-button>
        <el-button size="small" :disabled="!canAddChild" data-testid="wbs-add-child" @click="emit('add-child')">하위 추가</el-button>
        <el-button size="small" :disabled="!selected" data-testid="wbs-edit" @click="emit('edit')">편집</el-button>
        <el-button-group>
          <el-button size="small" :disabled="!canUp" data-testid="wbs-move-up" @click="emit('move-up')">▲ 위로</el-button>
          <el-button size="small" :disabled="!canDown" data-testid="wbs-move-down" @click="emit('move-down')">▼ 아래로</el-button>
          <el-button size="small" :disabled="!canIndent" data-testid="wbs-indent" @click="emit('indent')">→ 들여쓰기</el-button>
          <el-button size="small" :disabled="!canOutdent" data-testid="wbs-outdent" @click="emit('outdent')">← 내어쓰기</el-button>
        </el-button-group>
        <el-button size="small" type="danger" :disabled="!selected" data-testid="wbs-delete" @click="emit('delete')">삭제</el-button>
      </template>
      <el-button size="small" @click="emit('expand-all')">모두 펼치기</el-button>
      <el-button size="small" @click="emit('collapse-all')">모두 접기</el-button>
      <el-radio-group :model-value="view" size="small" data-testid="wbs-view-toggle" @change="(v) => emit('update:view', v)">
        <el-radio-button value="grid">Grid</el-radio-button>
        <el-radio-button value="gantt">Gantt</el-radio-button>
      </el-radio-group>
    </div>
  </div>
</template>

<style scoped>
.wbs-toolbar__progress {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
