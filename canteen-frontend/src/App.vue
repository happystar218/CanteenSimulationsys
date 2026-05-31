<template>
  <div class="container">
    <el-card>
      <template #header>
        <div class="header-bar">
          <div class="header-left">
            <span class="header-title">北交大四食堂仿真监控系统</span>
            <el-tag :type="store.isRunning ? 'success' : 'info'" size="large">
              {{ store.isRunning ? '实时仿真中' : '离线/就绪' }}
            </el-tag>
            <span v-if="store.isRunning" class="elapsed-time">
              ⏱ {{ store.elapsedFormatted }}
            </span>
          </div>
          <div class="header-right">
            <el-menu
              :default-active="currentRoute"
              mode="horizontal"
              @select="handleMenuSelect"
              :ellipsis="false"
            >
              <el-menu-item index="/">
                <el-icon><Setting /></el-icon>
                <span>仿真设置</span>
              </el-menu-item>
              <el-menu-item index="/monitor">
                <el-icon><Monitor /></el-icon>
                <span>可视化监控</span>
              </el-menu-item>
              <el-menu-item index="/records">
                <el-icon><Clock /></el-icon>
                <span>历史记录</span>
              </el-menu-item>
            </el-menu>
            <el-button
              :type="store.isRunning ? 'danger' : 'primary'"
              :loading="store.btnLoading"
              @click="toggleSimulation"
              style="margin-left: 16px;"
            >
              {{ store.isRunning ? '停止仿真' : '启动仿真' }}
            </el-button>
          </div>
        </div>
      </template>

      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </el-card>
  </div>
</template>

<script setup>
import { computed, watch, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useSimulationStore } from './stores/simulation'
import { Setting, Monitor, Clock } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const store = useSimulationStore()

const currentRoute = computed(() => route.path)

let pollTimer = null

// 导航切换
function handleMenuSelect(index) {
  router.push(index)
}

// 启动/停止
async function toggleSimulation() {
  if (store.isRunning) {
    await store.stopSimulation()
    stopPolling()
  } else {
    const ok = await store.startSimulation()
    if (ok) {
      startPolling()
    }
  }
}

function startPolling() {
  stopPolling()
  store.fetchAll()
  pollTimer = setInterval(() => store.fetchAll(), 2000)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

// 仿真结束 → 自动刷新历史记录
watch(() => store.isRunning, (running, wasRunning) => {
  if (!running && wasRunning) {
    // 仿真刚结束，自动拉取最新记录
    store.fetchRecords()
  }
})

// 页面关闭时清理
onBeforeUnmount(() => {
  stopPolling()
})

// 如果已经在运行中（刷新页面后），恢复轮询
watch(() => store.isRunning, (running) => {
  if (running && !pollTimer) {
    startPolling()
  }
}, { immediate: true })
</script>

<style scoped>
.container {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-title {
  font-weight: bold;
  font-size: 18px;
  white-space: nowrap;
}

.elapsed-time {
  font-family: 'Courier New', monospace;
  font-size: 14px;
  color: #409eff;
  background: #ecf5ff;
  padding: 2px 10px;
  border-radius: 4px;
}

.header-right {
  display: flex;
  align-items: center;
}

/* 页面切换动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* Element Plus menu 边界去除 */
.el-menu--horizontal {
  border-bottom: none !important;
}
</style>
