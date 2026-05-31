<template>
  <el-card class="congestion-card">
    <template #header>
      <span class="card-title"><el-icon><WarningFilled /></el-icon> 拥堵评估</span>
    </template>

    <div class="congestion-center">
      <!-- 环形仪表盘 -->
      <el-progress
        type="dashboard"
        :percentage="Math.round(store.results.seatUtilization || 0)"
        :color="store.congestionColor"
        :stroke-width="14"
        :width="150"
      >
        <template #default="{ percentage }">
          <span class="gauge-value">{{ percentage }}%</span>
          <span class="gauge-label">座位利用率</span>
        </template>
      </el-progress>

      <el-tag
        :type="store.congestionTagType"
        size="large"
        class="congestion-tag"
      >
        {{ store.results.congestionLevel || '空闲' }}
      </el-tag>

      <div class="congestion-detail">
        <div>占用座位：<strong>{{ store.results.currentOccupiedSeats || 0 }}</strong></div>
        <div>总座位数：<strong>{{ store.results.totalSeats || 129 }}</strong></div>
        <div>当前楼层营业窗口：<strong>{{ store.openWindowCount }}</strong></div>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { useSimulationStore } from '../stores/simulation'
import { WarningFilled } from '@element-plus/icons-vue'

const store = useSimulationStore()
</script>

<style scoped>
.congestion-card {
  height: 100%;
}

.card-title {
  font-weight: bold;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.congestion-center {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 5px 0;
}

.gauge-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.gauge-label {
  font-size: 11px;
  color: #909399;
  display: block;
  margin-top: 2px;
}

.congestion-tag {
  margin-top: 12px;
  font-size: 16px;
  padding: 6px 18px;
}

.congestion-detail {
  margin-top: 15px;
  text-align: center;
  font-size: 12px;
  color: #909399;
  line-height: 1.8;
}

.congestion-detail strong {
  color: #303133;
  font-family: 'Courier New', monospace;
}
</style>
