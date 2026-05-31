<template>
  <div class="monitor-page">
    <!-- 楼层切换 -->
    <div class="floor-switcher">
      <el-radio-group v-model="store.viewFloor" @change="store.setFloor" size="large">
        <el-radio-button :label="1">🟢 一楼 (面食/特色菜)</el-radio-button>
        <el-radio-button :label="2">🔵 二楼 (风味/粉面)</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 仿真时钟 -->
    <div class="sim-clock-bar" v-if="store.isRunning || store.results.tickCount > 0">
      <span class="clock-icon">🕐</span>
      <span class="clock-time">{{ simClock }}</span>
      <el-divider direction="vertical" />
      <span>已过 {{ formatDuration(store.results.tickCount || 0) }} / {{ formatDuration(store.config.simulationDuration || 7200) }}</span>
      <el-divider direction="vertical" />
      <span v-if="store.isRunning && !store.isPaused">预计剩余真实时间: <b>{{ remainingReal }}秒</b></span>
      <span v-if="store.isPaused" style="color:#e6a23c">⏸ 已暂停</span>
      <span v-if="!store.isRunning && store.results.tickCount > 0" style="color:#67c23a">✅ 仿真已结束</span>
      <el-progress :percentage="simProgress" :color="progressColor" style="flex:1;margin-left:16px;max-width:300px" />
    </div>

    <!-- 第一行：入场统计 + 座位热力图 + 拥堵评估 -->
    <el-row :gutter="20" class="row-top">
      <el-col :span="6">
        <EntryCounter />
      </el-col>
      <el-col :span="12">
        <SeatHeatmap />
      </el-col>
      <el-col :span="6">
        <CongestionBadge />
      </el-col>
    </el-row>

    <!-- 第二行：窗口排队 + 流量图表 -->
    <el-row :gutter="20" class="row-bottom">
      <el-col :span="10">
        <WindowTable />
      </el-col>
      <el-col :span="14">
        <FlowChart />
      </el-col>
    </el-row>

    <!-- 底部状态栏 -->
    <div class="status-bar">
      仿真运行时长：<strong>{{ store.elapsedFormatted }}</strong> |
      累计入场：<strong>{{ store.results.entryCount || 0 }}</strong> 人 |
      已服务：<strong>{{ store.results.totalServed || 0 }}</strong> 人 |
      当前占用座位：<strong>{{ store.results.currentOccupiedSeats || 0 }}</strong> /
      <strong>{{ store.results.totalSeats || 129 }}</strong>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useSimulationStore } from '../stores/simulation'
import EntryCounter from '../components/EntryCounter.vue'
import SeatHeatmap from '../components/SeatHeatmap.vue'
import CongestionBadge from '../components/CongestionBadge.vue'
import WindowTable from '../components/WindowTable.vue'
import FlowChart from '../components/FlowChart.vue'

const store = useSimulationStore()

const simClock = computed(() => {
  const start = store.config.periodStart || '11:00'
  const [h, m] = start.split(':').map(Number)
  const totalMin = h * 60 + m + Math.floor((store.results.tickCount || 0) / 60)
  const ch = Math.floor(totalMin / 60) % 24
  const cm = totalMin % 60
  const cs = (store.results.tickCount || 0) % 60
  return `${String(ch).padStart(2,'0')}:${String(cm).padStart(2,'0')}:${String(cs).padStart(2,'0')}`
})

const remainingReal = computed(() => {
  const remaining = (store.config.simulationDuration || 7200) - (store.results.tickCount || 0)
  return Math.max(0, Math.ceil(remaining / (store.config.speedMultiplier || 1)))
})

const simProgress = computed(() => {
  const dur = store.config.simulationDuration || 7200
  return Math.min(Math.round((store.results.tickCount || 0) / dur * 100), 100)
})

const progressColor = computed(() => {
  if (simProgress.value > 90) return '#67c23a'
  if (simProgress.value > 50) return '#409eff'
  return '#e6a23c'
})

function formatDuration(secs) {
  const h = Math.floor(secs / 3600), m = Math.floor((secs % 3600) / 60), s = secs % 60
  return `${String(h).padStart(2,'0')}:${String(m).padStart(2,'0')}:${String(s).padStart(2,'0')}`
}
</script>

<style scoped>
.monitor-page { margin-top: 10px; }
.floor-switcher { margin-bottom: 12px; text-align: center; }

.sim-clock-bar {
  display: flex; align-items: center; gap: 8px;
  background: linear-gradient(135deg, #ecf5ff, #f0f9eb);
  padding: 10px 16px; border-radius: 8px;
  margin-bottom: 14px; font-size: 14px; color: #303133;
  flex-wrap: wrap;
}
.sim-clock-bar b { color: #409eff; font-family: 'Courier New', monospace; }
.clock-icon { font-size: 20px; }
.clock-time { font-size: 20px; font-weight: bold; font-family: 'Courier New', monospace; color: #303133; }

.row-top {
  margin-bottom: 16px;
}

.row-bottom {
  margin-bottom: 16px;
}

.status-bar {
  text-align: center;
  padding: 10px;
  background: #f0f2f5;
  border-radius: 6px;
  font-size: 13px;
  color: #606266;
}

.status-bar strong {
  color: #303133;
  font-family: 'Courier New', monospace;
}
</style>
