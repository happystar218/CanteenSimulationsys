<template>
  <el-card class="entry-card">
    <template #header>
      <span class="card-title"><el-icon><UserFilled /></el-icon> 实时入场统计</span>
    </template>
    <div class="counter-main">
      <div class="big-number">{{ store.results.entryCount || 0 }}</div>
      <div class="big-label">累计入场人数</div>
    </div>
    <el-divider style="margin: 12px 0" />
    <div class="counter-secondary">
      <span class="small-label">当前分钟入场</span>
      <span class="small-number">{{ store.results.currentMinuteCount || 0 }} 人</span>
    </div>
    <el-divider style="margin: 12px 0" />
    <div class="counter-secondary">
      <span class="small-label">平均等待时间</span>
      <span class="small-number">{{ store.results.avgWaitTime || 0 }} 秒</span>
    </div>
  </el-card>
</template>

<script setup>
import { useSimulationStore } from '../stores/simulation'
import { UserFilled } from '@element-plus/icons-vue'

const store = useSimulationStore()
</script>

<style scoped>
.entry-card {
  height: 100%;
}

.card-title {
  font-weight: bold;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.counter-main {
  text-align: center;
  padding: 10px 0;
}

.big-number {
  font-size: 48px;
  font-weight: bold;
  color: #409eff;
  font-family: 'Courier New', monospace;
  line-height: 1.1;
}

.big-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.counter-secondary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 8px;
}

.small-label {
  font-size: 13px;
  color: #909399;
}

.small-number {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
  font-family: 'Courier New', monospace;
}
</style>
