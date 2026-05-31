import { defineStore } from 'pinia'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
const api = axios.create({ baseURL: API_BASE })

export const useSimulationStore = defineStore('simulation', {
  state: () => ({
    // 仿真配置
    config: {
      arrivalRate: 60,
      distributionType: 'uniform',
      floorPreference: 70,
      speedMultiplier: 1,
      simulationDuration: 7200,
      periodStart: '11:00'
    },

    // 运行状态
    isRunning: false,
    isPaused: false,
    tickCount: 0,
    simulationDuration: 7200,
    periodStart: '11:00',

    // 窗口数据
    windowList: [],

    // 座位数据
    seatList: [],
    viewFloor: 1,

    // 仿真结果
    results: {
      avgWaitTime: 0,
      totalServed: 0,
      entryCount: 0,
      currentMinuteCount: 0,
      seatUtilization: 0,
      currentOccupiedSeats: 0,
      totalSeats: 129,
      congestionLevel: '空闲',
      flowHistory: [],
      elapsedSeconds: 0,
      tickCount: 0,
      simulationDuration: 7200,
      periodStart: '11:00',
      perWindowData: [],
      maxSeatUtilization: 0,
      analysis: []
    },

    // 加载状态
    windowLoading: false,
    seatLoading: false,
    btnLoading: false,

    // 轮询计数器
    consecutiveFailures: 0,

    // 历史记录
    savedRecords: []
  }),

  getters: {
    // 按楼层过滤窗口
    filteredWindows: (state) => {
      return (state.windowList || []).filter(w => w.floor === state.viewFloor)
    },

    // 当前楼层营业窗口数
    openWindowCount: (state) => {
      return (state.windowList || []).filter(w => w.floor === state.viewFloor && w.status === 1).length
    },

    // 座位网格信息
    seatGrid: (state) => {
      if (!state.seatList || state.seatList.length === 0) return { rows: 0, cols: 0 }
      const maxRow = Math.max(...state.seatList.map(s => s.rowIndex || 0)) + 1
      const maxCol = Math.max(...state.seatList.map(s => s.colIndex || 0)) + 1
      return { rows: maxRow, cols: maxCol }
    },

    // 当前楼层占用的座位数
    occupiedCount: (state) => {
      return (state.seatList || []).filter(s => s.isOccupied === 1).length
    },

    // 当前楼层座位总数
    totalSeatsOnFloor: (state) => {
      return (state.seatList || []).length
    },

    // 拥堵标签颜色
    congestionTagType: (state) => {
      const map = { '空闲': 'success', '适中': 'warning', '拥挤': 'danger', '严重拥堵': 'danger' }
      return map[state.results.congestionLevel] || 'info'
    },

    // 拥堵仪表盘颜色
    congestionColor: (state) => {
      const u = state.results.seatUtilization || 0
      if (u < 30) return '#67c23a'
      if (u < 60) return '#e6a23c'
      return '#f56c6c'
    },

    // 仿真运行时间格式化
    elapsedFormatted: (state) => {
      const secs = state.results.elapsedSeconds || 0
      const m = Math.floor(secs / 60)
      const s = secs % 60
      return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
    }
  },

  actions: {
    // 获取窗口列表
    async fetchWindows() {
      this.windowLoading = true
      try {
        const res = await api.get('/window/list')
        if (res.data.code === 200) {
          this.windowList = res.data.data
        }
      } catch {
        // 轮询时静默失败
      } finally {
        this.windowLoading = false
      }
    },

    // 获取座位列表
    async fetchSeats() {
      this.seatLoading = true
      try {
        const res = await api.get(`/seat/list?floor=${this.viewFloor}`)
        if (res.data.code === 200) {
          this.seatList = res.data.data
        }
      } catch {
        // 轮询时静默失败
      } finally {
        this.seatLoading = false
      }
    },

    // 获取仿真结果
    async fetchResults() {
      try {
        const res = await api.get('/window/simulation/results')
        if (res.data.code === 200) {
          this.results = res.data.data
        }
      } catch {
        // 轮询时静默失败
      }
    },

    // 获取所有数据
    async fetchAll() {
      try {
        await Promise.all([
          this.fetchWindows(),
          this.fetchSeats(),
          this.fetchResults()
        ])
        this.consecutiveFailures = 0
      } catch {
        this.consecutiveFailures++
        if (this.consecutiveFailures >= 5) {
          this.isRunning = false
          ElMessage.error('连续连接失败，仿真可能已停止')
        }
      }
    },

    // 启动仿真
    async startSimulation() {
      this.btnLoading = true
      try {
        const res = await api.post('/window/simulation/start', this.config)
        if (res.data.code === 200) {
          this.isRunning = true
          this.consecutiveFailures = 0
          ElMessage.success('仿真引擎已启动')
          return true
        } else {
          ElMessage.error(res.data.message || '启动失败')
        }
      } catch {
        ElMessage.error('后端服务连接失败，请检查后端是否已启动')
      } finally {
        this.btnLoading = false
      }
      return false
    },

    // 停止仿真
    async stopSimulation() {
      try {
        const res = await api.post('/window/simulation/stop')
        if (res.data.code === 200) {
          this.isRunning = false
          this.isPaused = false
          return true
        }
      } catch { /* silent */ }
      return false
    },

    // 暂停
    async pauseSimulation() {
      try {
        const res = await api.post('/window/simulation/pause')
        if (res.data.code === 200) {
          this.isPaused = true
          ElMessage.info('仿真已暂停')
          return true
        }
      } catch { ElMessage.error('暂停失败') }
      return false
    },

    // 继续
    async resumeSimulation() {
      try {
        const res = await api.post('/window/simulation/resume')
        if (res.data.code === 200) {
          this.isPaused = false
          ElMessage.success('仿真已继续')
          return true
        }
      } catch { ElMessage.error('继续失败') }
      return false
    },

    // 检查仿真是否自动结束
    checkAutoEnd() {
      if (this.isRunning && !this.isPaused && this.results.tickCount >= this.results.simulationDuration) {
        this.isRunning = false
        ElMessage.warning('仿真时段已结束')
        return true
      }
      return false
    },

    // 切换楼层
    setFloor(floor) {
      this.viewFloor = floor
      this.fetchSeats()
    },

    // 应用快速预设
    applyPreset(type) {
      switch (type) {
        case 'low':
          this.config = { arrivalRate: 30, distributionType: 'uniform', floorPreference: 70, speedMultiplier: 1, simulationDuration: 7200, periodStart: '07:00' }
          break
        case 'peak':
          this.config = { arrivalRate: 200, distributionType: 'normal', floorPreference: 50, speedMultiplier: 1, simulationDuration: 7200, periodStart: '11:00' }
          break
        case 'evening':
          this.config = { arrivalRate: 150, distributionType: 'uniform', floorPreference: 60, speedMultiplier: 1, simulationDuration: 7200, periodStart: '17:00' }
          break
      }
      ElMessage.info('预设参数已应用')
    },

    // ===== 窗口管理 CRUD =====

    async addWindow(windowData) {
      try {
        const res = await api.post('/window/add', windowData)
        if (res.data.code === 200) {
          ElMessage.success('窗口添加成功')
          await this.fetchWindows()
          return true
        } else {
          ElMessage.error(res.data.message || '添加失败')
        }
      } catch {
        ElMessage.error('添加窗口失败')
      }
      return false
    },

    async updateWindow(id, windowData) {
      try {
        const res = await api.put(`/window/update/${id}`, windowData)
        if (res.data.code === 200) {
          ElMessage.success('窗口修改成功')
          await this.fetchWindows()
          return true
        } else {
          ElMessage.error(res.data.message || '修改失败')
        }
      } catch {
        ElMessage.error('修改窗口失败')
      }
      return false
    },

    async deleteWindow(id) {
      try {
        const res = await api.delete(`/window/${id}`)
        if (res.data.code === 200) {
          ElMessage.success('窗口已删除')
          await this.fetchWindows()
          return true
        } else {
          ElMessage.error(res.data.message || '删除失败')
        }
      } catch {
        ElMessage.error('删除窗口失败')
      }
      return false
    },

    // ===== 仿真结果保存/删除 =====

    async saveResult() {
      try {
        const res = await api.post('/simulation/save')
        if (res.data.code === 200) {
          ElMessage.success('仿真结果已保存')
          await this.fetchRecords()
          return true
        } else {
          ElMessage.error(res.data.message || '保存失败')
        }
      } catch {
        ElMessage.error('保存结果失败')
      }
      return false
    },

    async fetchRecords() {
      try {
        const res = await api.get('/simulation/records')
        if (res.data.code === 200) {
          this.savedRecords = res.data.data || []
        }
      } catch {
        // silent
      }
    },

    async deleteRecord(id) {
      try {
        const res = await api.delete(`/simulation/records/${id}`)
        if (res.data.code === 200) {
          ElMessage.success('记录已删除')
          await this.fetchRecords()
          return true
        }
      } catch {
        ElMessage.error('删除失败')
      }
      return false
    }
  }
})
