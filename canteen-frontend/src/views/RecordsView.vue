<template>
  <div class="records-page">
    <el-card>
      <template #header>
        <span class="card-title">
          <el-icon><Clock /></el-icon> 仿真历史记录
          <el-button size="small" @click="store.fetchRecords()" style="margin-left:12px">刷新</el-button>
        </span>
      </template>

      <el-empty v-if="store.savedRecords.length===0" description="暂无记录，请先运行仿真并保存结果" :image-size="120" />

      <el-table v-else :data="store.savedRecords" border stripe size="small">
        <el-table-column prop="id" label="ID" width="50" />
        <el-table-column label="仿真时段" min-width="130">
          <template #default="s">{{ s.row.periodStart || '?' }} ({{ formatDur(s.row.periodDuration) }})</template>
        </el-table-column>
        <el-table-column label="保存时间" min-width="150">
          <template #default="s">{{ fmtTime(s.row.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="avgWaitTime" label="平均等待" width="90" align="center">
          <template #default="s">{{ s.row.avgWaitTime }}s</template>
        </el-table-column>
        <el-table-column prop="totalServed" label="服务人数" width="85" align="center" />
        <el-table-column prop="entryCount" label="入场人数" width="85" align="center" />
        <el-table-column label="座位利用率" width="140" align="center">
          <template #default="s">
            平均{{ s.row.seatUtilization }}% / 最高{{ s.row.maxSeatUtilization || '?' }}%
          </template>
        </el-table-column>
        <el-table-column prop="congestionLevel" label="拥堵" width="85" align="center">
          <template #default="s">
            <el-tag :type="ctag(s.row.congestionLevel)" size="small">{{ s.row.congestionLevel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center">
          <template #default="s">
            <el-button size="small" type="primary" @click="showDetail(s.row)">详情</el-button>
            <el-popconfirm title="删除？" @confirm="store.deleteRecord(s.row.id)">
              <template #reference><el-button size="small" type="danger">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="仿真结果详情" width="800px" top="5vh">
      <template v-if="detail">
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="时段">{{ detail.periodStart || '?' }} ({{ formatDur(detail.periodDuration) }})</el-descriptions-item>
          <el-descriptions-item label="平均等待">{{ detail.avgWaitTime }}秒</el-descriptions-item>
          <el-descriptions-item label="服务人数">{{ detail.totalServed }}</el-descriptions-item>
          <el-descriptions-item label="入场人数">{{ detail.entryCount }}</el-descriptions-item>
          <el-descriptions-item label="平均利用率">{{ detail.seatUtilization }}%</el-descriptions-item>
          <el-descriptions-item label="最高利用率">{{ detail.maxSeatUtilization || '?' }}%</el-descriptions-item>
          <el-descriptions-item label="拥堵评估">
            <el-tag :type="ctag(detail.congestionLevel)">{{ detail.congestionLevel }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <!-- 每窗口统计 -->
        <h4 style="margin-top:20px"><el-icon><Shop /></el-icon> 各窗口排队统计</h4>
        <el-table :data="perWindow" border stripe size="small" max-height="350">
          <el-table-column prop="windowName" label="窗口" min-width="110" />
          <el-table-column prop="floor" label="楼层" width="55" align="center" />
          <el-table-column label="平均排队" width="90" align="center">
            <template #default="s">{{ s.row.avgQueue }}人</template>
          </el-table-column>
          <el-table-column label="最高排队" width="90" align="center">
            <template #default="s"><span :style="{color: s.row.maxQueue > 30 ? '#f56c6c' : ''}">{{ s.row.maxQueue }}人</span></template>
          </el-table-column>
          <el-table-column label="平均等待" width="95" align="center">
            <template #default="s">{{ s.row.avgWait }}秒</template>
          </el-table-column>
          <el-table-column label="最高等待" width="95" align="center">
            <template #default="s"><span :style="{color: s.row.maxWait > 600 ? '#f56c6c' : ''}">{{ s.row.maxWait }}秒</span></template>
          </el-table-column>
          <el-table-column prop="attraction" label="吸引力" width="65" align="center" />
        </el-table>

        <!-- 分析建议 -->
        <h4 style="margin-top:20px"><el-icon><WarningFilled /></el-icon> 分析与建议</h4>
        <div v-if="analysis.length === 0" style="color:#909399;padding:10px">暂无分析</div>
        <div v-for="(a,i) in analysis" :key="i" class="advice-item" :class="{ 'advice-warn': a.startsWith('⚠')||a.startsWith('🔴'), 'advice-info': a.startsWith('💡')||a.startsWith('📊'), 'advice-ok': a.startsWith('✅') }">
          {{ a }}
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useSimulationStore } from '../stores/simulation'
import { Clock, Shop, WarningFilled } from '@element-plus/icons-vue'

const store = useSimulationStore()
const detailVisible = ref(false)
const detail = ref(null)

onMounted(() => {
  store.fetchRecords()
})

const perWindow = computed(() => {
  if (!detail.value) return []
  try { return JSON.parse(detail.value.perWindowData || '[]') } catch { return [] }
})

const analysis = computed(() => {
  if (!detail.value) return []
  try { return JSON.parse(detail.value.configJson || '{}').analysis || [] } catch { return [] }
})

function showDetail(rec) {
  detail.value = rec
  detailVisible.value = true
}

function fmtTime(dt) { return dt ? new Date(dt).toLocaleString('zh-CN') : '' }

function formatDur(s) {
  if (!s) return '?'
  const h = Math.floor(s / 3600), m = Math.floor((s % 3600) / 60)
  return h > 0 ? `${h}小时${m}分` : `${m}分钟`
}

function ctag(l) {
  const m = { '空闲': 'success', '适中': 'warning', '拥挤': 'danger', '严重拥堵': 'danger' }
  return m[l] || 'info'
}
</script>

<style scoped>
.records-page { margin-top: 10px; }
.card-title { font-weight: bold; font-size: 15px; display: flex; align-items: center; gap: 6px; }
.advice-item { padding: 8px 12px; margin: 6px 0; border-radius: 6px; font-size: 13px; line-height: 1.6; }
.advice-warn { background: #fef0f0; border-left: 3px solid #f56c6c; color: #c0392b; }
.advice-info { background: #ecf5ff; border-left: 3px solid #409eff; color: #2c6fad; }
.advice-ok { background: #f0f9eb; border-left: 3px solid #67c23a; color: #3d7a3d; }
</style>
