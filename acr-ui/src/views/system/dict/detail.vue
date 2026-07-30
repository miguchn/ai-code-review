<template>
  <el-drawer :model-value="visible" direction="rtl" size="min(700px, calc(100vw - 32px))" append-to-body @update:model-value="$emit('update:visible', $event)">
    <!-- 自定义标题 -->
    <template #header>
      <div class="drawer-head">
        <el-icon class="drawer-head-icon"><List /></el-icon>
        <span class="drawer-head-name">{{ row.dictName }}</span>
        <span class="drawer-head-type">{{ row.dictType }}</span>
      </div>
    </template>

    <div class="drawer-wrap">
      <!-- 加载中 -->
      <div v-if="loading" class="drawer-loading">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载中...</span>
      </div>

      <!-- 空数据 -->
      <div v-else-if="!dataList.length" class="drawer-empty">
        <el-icon style="font-size:36px;"><Document /></el-icon>
        <div>暂无字典数据</div>
      </div>

      <template v-else>
        <!-- 统计卡片 -->
        <el-row :gutter="12" class="stat-row">
          <el-col :span="disabledCount > 0 ? 8 : 12">
            <div class="stat-card">
              <div class="stat-num">{{ dataList.length }}</div>
              <div class="stat-label">共计条目</div>
            </div>
          </el-col>
          <el-col :span="disabledCount > 0 ? 8 : 12">
            <div class="stat-card">
              <div class="stat-num success">{{ normalCount }}</div>
              <div class="stat-label">正常</div>
            </div>
          </el-col>
          <el-col v-if="disabledCount > 0" :span="8">
            <div class="stat-card">
              <div class="stat-num danger">{{ disabledCount }}</div>
              <div class="stat-label">停用</div>
            </div>
          </el-col>
        </el-row>

        <!-- 数据列表 -->
        <div v-for="item in dataList" :key="item.dictCode" class="dict-item">
          <div class="dict-cell">
            <div class="dict-cell-key">标签</div>
            <div class="dict-cell-val">
              <el-tag v-if="item.listClass && item.listClass !== 'default'" :type="item.listClass === 'primary' ? undefined : item.listClass" size="small">{{ item.dictLabel }}</el-tag>
              <span v-else>{{ item.dictLabel }}</span>
            </div>
          </div>
          <div class="dict-cell">
            <div class="dict-cell-key">键值</div>
            <div class="dict-cell-val">{{ item.dictValue }}</div>
          </div>
          <div class="dict-cell">
            <div class="dict-cell-key">状态</div>
            <div class="dict-cell-val">
              <el-tag :type="item.status === '0' ? 'success' : 'danger'" size="small">
                {{ item.status === '0' ? '正常' : '停用' }}
              </el-tag>
            </div>
          </div>
        </div>
      </template>
    </div>
  </el-drawer>
</template>

<script setup>
import { listData } from '@/api/system/dict/data'

const props = defineProps({
  visible: { type: Boolean, default: false },
  row: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['update:visible'])

const loading = ref(false)
const dataList = ref([])

const normalCount = computed(() => dataList.value.filter(r => r.status === '0').length)
const disabledCount = computed(() => dataList.value.filter(r => r.status !== '0').length)

watch(() => props.visible, (val) => {
  if (val) {
    loadData()
  } else {
    dataList.value = []
  }
})

function loadData() {
  if (!props.row?.dictType) return
  loading.value = true
  dataList.value = []
  listData({ dictType: props.row.dictType, pageSize: 100, pageNum: 1 }).then(response => {
    dataList.value = response.rows || []
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
</script>

<style scoped>
.drawer-head {
  display: flex;
  align-items: center;
}
.drawer-head-icon {
  margin-right: 8px;
  color: var(--brand-500);
}
.drawer-head-name {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin-right: 8px;
}
.drawer-head-type {
  font-size: 13px;
  color: var(--text-secondary);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}
.drawer-wrap {
  padding: 0;
}
.drawer-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 120px;
  color: var(--text-assist);
  font-size: 13px;
  gap: 8px;
}
.drawer-empty {
  text-align: center;
  color: var(--text-assist);
  padding: 60px 0;
  font-size: 13px;
}
.drawer-empty .el-icon {
  display: block;
  margin: 0 auto 8px;
}
.stat-row {
  margin-bottom: 16px;
}
.stat-card {
  background: var(--neutral-content);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-card);
  padding: 12px 16px;
  text-align: center;
}
.stat-num {
  font-size: 28px;
  line-height: 34px;
  font-weight: 600;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}
.stat-num.success { color: var(--status-success-text); }
.stat-num.danger  { color: var(--status-danger-text); }
.stat-label {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 4px;
}
.dict-item {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-card);
  overflow: hidden;
  margin-bottom: 8px;
}
.dict-cell {
  display: grid;
  grid-template-columns: 70px 1fr;
  border-right: 1px solid var(--border-light);
}
.dict-cell:last-child {
  border-right: 0;
}
.dict-cell-key {
  padding: 9px 14px;
  font-size: 12px;
  color: var(--text-secondary);
  background: var(--neutral-content);
  border-right: 1px solid var(--border-light);
}
.dict-cell-val {
  padding: 9px 14px;
  font-size: 13px;
  color: var(--text-regular);
  word-break: break-all;
  display: flex;
  align-items: center;
}

@media (max-width: 640px) {
  .drawer-head-type {
    display: none;
  }

  .dict-item {
    grid-template-columns: 1fr;
  }

  .dict-cell {
    border-right: 0;
    border-bottom: 1px solid var(--border-light);
  }

  .dict-cell:last-child {
    border-bottom: 0;
  }
}
</style>
