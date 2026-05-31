<template>
  <el-card class="window-card">
    <template #header>
      <span class="card-title">
        <el-icon><Shop /></el-icon>
        {{ store.viewFloor }}楼 窗口实时负载
        <el-tag size="small" style="margin-left: 8px">营业 {{ store.openWindowCount }} 个</el-tag>
      </span>
    </template>

    <el-table
      :data="store.filteredWindows"
      size="small"
      border
      stripe
      v-loading="store.windowLoading"
      max-height="400"
      :default-sort="{ prop: 'currentQueueCount', order: 'descending' }"
    >
      <el-table-column prop="name" label="窗口" min-width="110" />

      <el-table-column prop="avgSpeed" label="打饭时间" width="85" align="center" sortable>
        <template #default="scope">
          <el-tag :type="scope.row.avgSpeed > 60 ? 'warning' : ''" size="small">
            {{ scope.row.avgSpeed }}秒
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="排队人数" min-width="180" sortable prop="currentQueueCount">
        <template #default="scope">
          <div class="queue-cell">
            <el-progress
              :percentage="Math.min(scope.row.currentQueueCount * 2 + 5, 100)"
              :status="scope.row.currentQueueCount > 20 ? 'exception' : scope.row.currentQueueCount > 10 ? 'warning' : ''"
              :stroke-width="14"
              :show-text="false"
              style="flex: 1"
            />
            <span class="queue-num" :class="{ 'q-high': scope.row.currentQueueCount > 20, 'q-mid': scope.row.currentQueueCount > 10 }">
              {{ scope.row.currentQueueCount }}人
            </span>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="status" label="状态" width="65" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" size="small">
            {{ scope.row.status === 1 ? '营业' : '关闭' }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>

    <el-empty
      v-if="!store.windowLoading && store.filteredWindows.length === 0"
      description="暂无窗口数据"
      :image-size="60"
    />
  </el-card>
</template>

<script setup>
import { useSimulationStore } from '../stores/simulation'
import { Shop } from '@element-plus/icons-vue'

const store = useSimulationStore()
</script>

<style scoped>
.window-card { height: 100%; }
.card-title { font-weight: bold; font-size: 14px; display: flex; align-items: center; gap: 6px; }
.queue-cell { display: flex; align-items: center; gap: 8px; }
.queue-num { font-weight: bold; font-size: 14px; min-width: 45px; text-align: right; }
.q-high { color: #f56c6c; }
.q-mid { color: #e6a23c; }
</style>
