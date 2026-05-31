<template>
  <el-card class="flow-card">
    <template #header>
      <span class="card-title">
        <el-icon><DataAnalysis /></el-icon>
        每分钟入场流量
        <el-tag size="small" style="margin-left: 8px">
          当前分钟 {{ store.results.currentMinuteCount || 0 }} 人
        </el-tag>
      </span>
    </template>

    <v-chart
      v-if="hasData"
      :option="chartOption"
      autoresize
      style="height: 320px;"
    />
    <el-empty v-else description="等待仿真数据..." :image-size="80" />
  </el-card>
</template>

<script setup>
import { computed } from 'vue'
import VChart from 'vue-echarts'
import { useSimulationStore } from '../stores/simulation'
import { DataAnalysis } from '@element-plus/icons-vue'

const store = useSimulationStore()

const hasData = computed(() => {
  return (store.results.flowHistory && store.results.flowHistory.length > 0)
})

const chartOption = computed(() => {
  const data = store.results.flowHistory || []
  if (data.length === 0) return null

  // 取最近20个数据点
  const recent = data.slice(-20)

  const labels = recent.map((_, i) => {
    if (i === recent.length - 1) return '当前'
    return `${recent.length - 1 - i}分前`
  })

  const values = recent.map(d => {
    // d could be {minute, count} or just a number
    if (typeof d === 'object' && d !== null) return d.count || 0
    return d || 0
  })

  const maxVal = Math.max(...values, 1)

  // 添加当前分钟的计数作为最后一个点
  const allValues = [...values]
  if (store.results.currentMinuteCount > 0) {
    // 当前分钟已在最近的flowHistory中或需要追加
  }

  // 趋势线（3点移动平均）
  const trendValues = allValues.map((_, i) => {
    if (i < 1) return allValues[0]
    if (i >= allValues.length - 1) return allValues[i]
    return Math.round((allValues[i - 1] + allValues[i] + allValues[i + 1]) / 3 * 10) / 10
  })

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: function(params) {
        if (!params || params.length === 0) return ''
        const p = params[0]
        return `${p.axisValue}<br/>${p.marker} 入场: <b>${p.value} 人/分钟</b>`
      }
    },
    grid: {
      left: 45,
      right: 25,
      top: 15,
      bottom: 40
    },
    xAxis: {
      type: 'category',
      data: labels,
      axisLabel: {
        rotate: 45,
        fontSize: 10,
        color: '#909399'
      },
      axisTick: { alignWithLabel: true }
    },
    yAxis: {
      type: 'value',
      name: '人/分钟',
      nameTextStyle: { fontSize: 11, color: '#909399' },
      axisLabel: { fontSize: 10 },
      splitLine: { lineStyle: { type: 'dashed', color: '#e0e0e0' } },
      minInterval: 1
    },
    series: [
      {
        name: '入场人数',
        type: 'bar',
        data: allValues.map((val) => ({
          value: val,
          itemStyle: {
            color: val > maxVal * 0.7 ? '#f56c6c'
                 : val > maxVal * 0.4 ? '#e6a23c'
                 : '#67c23a',
            borderRadius: [5, 5, 0, 0]
          }
        })),
        barMaxWidth: 35,
        emphasis: {
          itemStyle: { color: '#409eff' }
        }
      },
      {
        name: '趋势线',
        type: 'line',
        data: trendValues,
        smooth: true,
        lineStyle: { color: '#409eff', width: 2.5 },
        itemStyle: { color: '#409eff' },
        symbol: 'circle',
        symbolSize: 5,
        z: 10
      }
    ]
  }
})
</script>

<style scoped>
.flow-card {
  height: 100%;
}

.card-title {
  font-weight: bold;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 6px;
}
</style>
