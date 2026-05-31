import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Settings',
    component: () => import('../views/SettingsView.vue'),
    meta: { title: '仿真设置' }
  },
  {
    path: '/monitor',
    name: 'Monitor',
    component: () => import('../views/MonitorView.vue'),
    meta: { title: '可视化监控' }
  },
  {
    path: '/records',
    name: 'Records',
    component: () => import('../views/RecordsView.vue'),
    meta: { title: '仿真记录' }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router
