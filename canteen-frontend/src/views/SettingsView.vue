<template>
  <div class="settings-page">
    <el-row :gutter="20">
      <el-col :span="11">
        <el-card>
          <template #header>
            <span class="card-title"><el-icon><Setting /></el-icon> 仿真参数配置</span>
          </template>

          <el-form ref="formRef" :model="store.config" :rules="formRules" label-width="140px">

            <!-- 时段选择 -->
            <el-form-item label="仿真时段">
              <el-button-group>
                <el-button :type="period==='breakfast'?'primary':''" size="small" @click="setPeriod('breakfast')">早餐 7-9点</el-button>
                <el-button :type="period==='lunch'?'primary':''" size="small" @click="setPeriod('lunch')">午餐 11-13点</el-button>
                <el-button :type="period==='dinner'?'primary':''" size="small" @click="setPeriod('dinner')">晚餐 17-19点</el-button>
              </el-button-group>
            </el-form-item>

            <el-form-item label="人员流量 (人/分)" prop="arrivalRate">
              <el-input-number v-model="store.config.arrivalRate" :min="1" :max="500" />
            </el-form-item>

            <el-form-item label="分布类型">
              <el-radio-group v-model="store.config.distributionType">
                <el-radio label="uniform">均匀</el-radio>
                <el-radio label="poisson">泊松</el-radio>
                <el-radio label="normal">正态(午峰)</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-form-item label="一楼选择概率 (%)" prop="floorPreference">
              <el-slider v-model="store.config.floorPreference" :min="0" :max="100" :show-input="true" />
            </el-form-item>

            <el-form-item label="仿真速度倍率">
              <el-slider v-model="store.config.speedMultiplier" :min="1" :max="120"
                :marks="{1:'1x',10:'10x',30:'30x',60:'60x',120:'120x'}" :show-input="true" />
            </el-form-item>

            <el-divider />

            <!-- 控制按钮 -->
            <el-form-item>
              <el-button v-if="!store.isRunning" type="primary" size="large"
                :loading="store.btnLoading" @click="startSim" style="width:120px">
                启动仿真
              </el-button>
              <template v-if="store.isRunning">
                <el-button v-if="!store.isPaused" type="warning" size="large" @click="store.pauseSimulation()">
                  暂停
                </el-button>
                <el-button v-if="store.isPaused" type="success" size="large" @click="store.resumeSimulation()">
                  继续
                </el-button>
                <el-button type="danger" size="large" @click="store.stopSimulation()" style="margin-left:8px">
                  结束
                </el-button>
              </template>
            </el-form-item>

            <!-- 进度条 -->
            <el-form-item v-if="store.isRunning || hasEverRun">
              <div style="width:100%">
                <div style="display:flex;justify-content:space-between;font-size:12px;margin-bottom:4px">
                  <span>仿真进度: {{ simProgress }}%</span>
                  <span>{{ formatDuration(store.results.tickCount||0) }} / {{ formatDuration(store.config.simulationDuration||7200) }}</span>
                </div>
                <el-progress :percentage="simProgress" :color="progressColor" />
              </div>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 结果面板 -->
      <el-col :span="13">
        <el-card v-if="store.isRunning || hasEverRun">
          <template #header>
            <span class="card-title">
              <el-icon><DataAnalysis /></el-icon> 仿真结果
              <el-tag v-if="store.isRunning && !store.isPaused" type="success" size="small" style="margin-left:8px">运行中</el-tag>
              <el-tag v-if="store.isPaused" type="warning" size="small" style="margin-left:8px">已暂停</el-tag>
            </span>
          </template>

          <el-row :gutter="16">
            <el-col :span="8"><el-statistic title="平均等待" :value="store.results.avgWaitTime" suffix="秒" /></el-col>
            <el-col :span="8"><el-statistic title="已服务" :value="store.results.totalServed" /></el-col>
            <el-col :span="8"><el-statistic title="总入场" :value="store.results.entryCount" /></el-col>

            <el-col :span="12" style="margin-top:16px">
              <div style="text-align:center">
                <el-progress type="dashboard" :percentage="Math.round(store.results.seatUtilization||0)"
                  :color="store.congestionColor" :stroke-width="14" :width="130">
                  <template #default="{percentage}"><span style="font-size:22px;font-weight:bold">{{percentage}}%</span><br><span style="font-size:11px;color:#909399">平均利用率</span></template>
                </el-progress>
              </div>
            </el-col>
            <el-col :span="12" style="margin-top:16px">
              <div style="text-align:center;padding-top:20px">
                <el-tag :type="store.congestionTagType" size="large" style="font-size:16px;padding:6px 16px">{{store.results.congestionLevel||'空闲'}}</el-tag>
                <div style="margin-top:8px;font-size:12px;color:#909399">最高利用率 {{store.results.maxSeatUtilization||0}}%</div>
              </div>
            </el-col>

            <el-col :span="24" style="margin-top:12px;text-align:center">
              <span v-if="!store.isRunning && hasEverRun" style="color:#67c23a;font-size:13px">
                ✅ 结果已自动保存至历史记录
              </span>
            </el-col>
          </el-row>
        </el-card>
        <el-empty v-if="!store.isRunning && !hasEverRun" description="选择时段并启动仿真" :image-size="100" />
      </el-col>
    </el-row>

    <!-- 窗口管理 (保持不变) -->
    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="14">
        <el-card>
          <template #header>
            <span class="card-title"><el-icon><Shop /></el-icon> 窗口管理
              <el-button type="primary" size="small" @click="openAddDialog" style="margin-left:12px">新增</el-button>
            </span>
          </template>
          <el-table :data="store.windowList" size="small" border stripe max-height="300">
            <el-table-column prop="id" label="ID" width="45" />
            <el-table-column prop="name" label="名称" min-width="100" />
            <el-table-column prop="floor" label="楼层" width="55" align="center" />
            <el-table-column prop="avgSpeed" label="打饭(s)" width="75" align="center" />
            <el-table-column prop="attraction" label="吸引力" width="65" align="center">
              <template #default="s"><el-tag :type="s.row.attraction>7?'danger':s.row.attraction>5?'warning':''" size="small">{{s.row.attraction||5}}</el-tag></template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="60" align="center">
              <template #default="s"><el-tag :type="s.row.status===1?'success':'info'" size="small">{{s.row.status===1?'营业':'关闭'}}</el-tag></template>
            </el-table-column>
            <el-table-column label="操作" width="110" align="center">
              <template #default="s">
                <el-button size="small" @click="openEditDialog(s.row)">编辑</el-button>
                <el-popconfirm title="确定删除？" @confirm="store.deleteWindow(s.row.id)"><template #reference><el-button size="small" type="danger">删</el-button></template></el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 窗口编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="editingId?'编辑窗口':'新增窗口'" width="420px">
      <el-form :model="editForm" label-width="110px">
        <el-form-item label="名称"><el-input v-model="editForm.name" /></el-form-item>
        <el-form-item label="楼层"><el-radio-group v-model="editForm.floor"><el-radio :label="1">一楼</el-radio><el-radio :label="2">二楼</el-radio></el-radio-group></el-form-item>
        <el-form-item label="打饭时间(秒)"><el-input-number v-model="editForm.avgSpeed" :min="1" :max="600" /></el-form-item>
        <el-form-item label="吸引力"><el-slider v-model="editForm.attraction" :min="1" :max="10" :show-input="true" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="editForm.statusBool" active-text="营业" inactive-text="关闭" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="saveWindow">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useSimulationStore } from '../stores/simulation'
import { Setting, DataAnalysis, Shop } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const store = useSimulationStore()
const formRef = ref(null)
const hasEverRun = ref(false)
const period = ref('lunch')

const formRules = {
  arrivalRate: [{ required: true, message: '请输入人员流量' }],
  floorPreference: [{ required: true, message: '请设置楼层偏好' }]
}

const simProgress = computed(() => {
  const dur = store.config.simulationDuration || 7200
  return Math.min(Math.round((store.results.tickCount || 0) / dur * 100), 100)
})

const progressColor = computed(() => {
  if (simProgress.value > 90) return '#67c23a'
  if (simProgress.value > 50) return '#409eff'
  return '#e6a23c'
})

function setPeriod(type) {
  period.value = type
  switch (type) {
    case 'breakfast': store.config.periodStart = '07:00'; store.config.simulationDuration = 7200; break
    case 'lunch': store.config.periodStart = '11:00'; store.config.simulationDuration = 7200; break
    case 'dinner': store.config.periodStart = '17:00'; store.config.simulationDuration = 7200; break
  }
}

function formatDuration(secs) {
  const h = Math.floor(secs / 3600), m = Math.floor((secs % 3600) / 60), s = secs % 60
  return `${String(h).padStart(2,'0')}:${String(m).padStart(2,'0')}:${String(s).padStart(2,'0')}`
}

async function startSim() {
  try { await formRef.value?.validate() } catch { ElMessage.warning('请完善参数'); return }
  const ok = await store.startSimulation()
  if (ok) hasEverRun.value = true
}

watch(() => store.results.tickCount, (t) => {
  if (t >= (store.config.simulationDuration || 7200) && store.isRunning) {
    store.stopSimulation()
    ElMessage.info('仿真时段结束，结果已保存')
  }
})

// 窗口编辑
const dialogVisible = ref(false)
const editingId = ref(null)
const editForm = ref({ name: '', floor: 1, avgSpeed: 30, attraction: 5, statusBool: true })
function openAddDialog() { editingId.value = null; editForm.value = { name: '', floor: 1, avgSpeed: 30, attraction: 5, statusBool: true }; dialogVisible.value = true }
function openEditDialog(row) { editingId.value = row.id; editForm.value = { name: row.name, floor: row.floor, avgSpeed: row.avgSpeed, attraction: row.attraction || 5, statusBool: row.status === 1 }; dialogVisible.value = true }
async function saveWindow() {
  const data = { name: editForm.value.name, floor: editForm.value.floor, avgSpeed: editForm.value.avgSpeed, attraction: editForm.value.attraction, status: editForm.value.statusBool ? 1 : 0 }
  const ok = editingId.value ? await store.updateWindow(editingId.value, data) : await store.addWindow(data)
  if (ok) dialogVisible.value = false
}
</script>

<style scoped>
.settings-page { margin-top: 10px; }
.card-title { font-weight: bold; font-size: 15px; display: flex; align-items: center; gap: 6px; }
</style>
