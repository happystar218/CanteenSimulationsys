<template>
  <el-card class="seat-card">
    <template #header>
      <span class="card-title">
        <el-icon><Place /></el-icon>
        {{ store.viewFloor }}楼 座位热力图
        <small style="font-weight: normal; color: #999; margin-left: 8px">
          占用 {{ store.occupiedCount }}/{{ store.totalSeatsOnFloor }}
        </small>
      </span>
    </template>

    <div v-if="store.seatGrid.rows > 0" class="seat-heatmap" :style="heatmapStyle">
      <div
        v-for="seat in store.seatList"
        :key="seat.id"
        :class="['seat-cell', seat.isOccupied ? 'occupied' : 'free']"
        :style="{
          gridRow: seat.rowIndex + 1,
          gridColumn: seat.colIndex + 1
        }"
        :title="`${store.viewFloor}楼 座位 ${seat.rowIndex+1}行${seat.colIndex+1}列`"
      />
    </div>
    <el-empty v-else description="暂无座位数据" :image-size="60" />

    <div class="legend">
      <span><span class="dot free"></span> 空闲</span>
      <span><span class="dot occupied"></span> 占用</span>
    </div>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'
import { useSimulationStore } from '../stores/simulation'
import { Place } from '@element-plus/icons-vue'

const store = useSimulationStore()

const heatmapStyle = computed(() => ({
  gridTemplateRows: `repeat(${store.seatGrid.rows}, 1fr)`,
  gridTemplateColumns: `repeat(${store.seatGrid.cols}, 1fr)`
}))
</script>

<style scoped>
.seat-card {
  height: 100%;
}

.card-title {
  font-weight: bold;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.seat-heatmap {
  display: grid;
  gap: 3px;
  background: #2c3e50;
  padding: 8px;
  border-radius: 6px;
  min-height: 200px;
}

.seat-cell {
  aspect-ratio: 1;
  border-radius: 2px;
  transition: all 0.3s;
  min-width: 14px;
  min-height: 14px;
}

.occupied {
  background-color: #f56c6c;
  box-shadow: 0 0 3px rgba(245, 108, 108, 0.5);
}

.free {
  background-color: #67c23a;
}

.legend {
  margin-top: 8px;
  display: flex;
  gap: 15px;
  font-size: 12px;
  justify-content: center;
}

.dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 2px;
  vertical-align: middle;
  margin-right: 4px;
}

.dot.free { background-color: #67c23a; }
.dot.occupied { background-color: #f56c6c; }
</style>
