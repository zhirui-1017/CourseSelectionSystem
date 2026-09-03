# 🎯 网上选课系统 — Vue 前后端分离迁移方案

> **版本**: v1.0  
> **日期**: 2026-07-14  
> **状态**: 待启动

---

## 目录

1. [现状评估](#1-现状评估)
2. [就绪度分析](#2-就绪度分析)
3. [技术栈选型](#3-技术栈选型)
4. [Vue 项目结构设计](#4-vue-项目结构设计)
5. [路由设计](#5-路由设计)
6. [认证流程设计](#6-认证流程设计)
7. [组件架构设计](#7-组件架构设计)
8. [API 集成方案](#8-api-集成方案)
9. [分阶段迁移计划](#9-分阶段迁移计划)
10. [构建与部署](#10-构建与部署)
11. [风险与应对](#11-风险与应对)
12. [附录](#12-附录)

---

## 1. 现状评估

### 1.1 当前架构

```
┌─────────┐    ┌──────────────┐    ┌──────────────────┐
│ 浏览器  │───▶│ Gateway:9000 │───▶│  web-service:8080 │
│ (静态   │    │ (Spring Cloud│    │  (前端静态页 +    │
│  HTML)  │    │  Gateway +   │    │   兜底路由)       │
│         │    │  JWT Filter) │    └──────────────────┘
└─────────┘    │              │───▶│  user-service:8101│
                │              │    └──────────────────┘
                │              │───▶│ student-svc:8102 │
                │              │    └──────────────────┘
                │              │───▶│ teacher-svc:8103 │
                │              │    └──────────────────┘
                │              │───▶│ course-svc:8104  │
                │              │    └──────────────────┘
                │              │───▶│ selection-svc:8105│
                └──────────────┘    └──────────────────┘
```

### 1.2 前端现状

| 维度 | 现状 |
|------|------|
| **技术栈** | 纯静态 HTML + CSS + JavaScript（无框架） |
| **页面数** | 32 个 HTML 页面（admin 16 + student 9 + teacher 6 + login 1） |
| **样式** | 手写 CSS + Font Awesome 图标库 |
| **图表** | Chart.js |
| **认证** | 双机制并存：JWT（Gateway 层）+ Session（web-service） |
| **API 调用** | 已大部分迁移到 `/api/v1/...` 路径，经 Gateway 转发 |
| **状态管理** | 无；依赖 localStorage + DOM 操作 |

### 1.3 后端 API 已就绪

所有业务域均已提供完整的 RESTful API（`/api/v1/**`），经 Gateway 统一路由：

| 服务 | 端口 | API 前缀 | 就绪度 |
|------|------|----------|--------|
| user-service | 8101 | `/api/v1/auth/**`, `/api/v1/users/**` | ✅ 完整 |
| student-service | 8102 | `/api/v1/students/**`, `/api/v1/classes/**` | ✅ 完整 |
| teacher-service | 8103 | `/api/v1/teachers/**` | ✅ 完整 |
| course-service | 8104 | `/api/v1/courses/**`, `/api/v1/colleges/**` 等 | ✅ 完整 |
| selection-service | 8105 | `/api/v1/selections/**`, `/api/v1/grades/**` 等 | ✅ 完整 |
| web-service | 8080 | `/api/v1/dashboard/**`, `/api/v1/ai/**` | ✅ 完整 |

---

## 2. 就绪度分析

### ✅ 已就绪的条件

| 条件 | 状态 | 说明 |
|------|------|------|
| RESTful API 完整度 | ✅ | 所有业务操作都有 `/api/v1/**` 端点 |
| JWT 认证机制 | ✅ | Gateway 层 JwtAuthFilter 已就绪 |
| Gateway 路由配置 | ✅ | 6 条路由清晰匹配各微服务 |
| 服务注册发现 | ✅ | Eureka 正常运行 |
| 数据库 | ✅ | 共享 MySQL 实例，数据就绪 |
| API 返回格式统一 | ✅ | 统一 `Result` 结构（`code`, `message`, `data`） |
| CORS | ✅ | 经 Gateway 同源访问，无需额外 CORS 配置 |

### ⚠️ 需要调整的事项

| 事项 | 影响 | 解决方式 |
|------|------|----------|
| Vue 开发服务器跨域 | Vue dev server 默认 5173 端口，与 Gateway 9000 不同源 | Vite proxy 配置代理到 Gateway |
| 登录态双机制 | 部分 JS 仍依赖 Session | Vue 全部切换为 JWT Token 无状态认证 |
| web-service 兜底路由 | `/**` 会捕获前端 404 页面请求 | 迁移后调整路由或让 Vue Router 接管 |
| 部分 API 字段兼容 | `pageItems`/`pageTotal` 兼容多种分页格式 | Vue 层统一使用 Axios 拦截器标准化 |
| 系统设置/日志等 API | 仍在 web-service 中 | 后续可下沉到独立服务或保留 |

### 🎯 结论：**可以开始迁移**

后端 API 体系已经足够成熟，具备前后端分离的充分条件。建议**不等待所有微服务完全独立**，立即启动 Vue 前端项目，与后端并行开发。

---

## 3. 技术栈选型

### 3.1 推荐技术栈

| 层 | 技术 | 版本 | 选择理由 |
|----|------|------|----------|
| **框架** | Vue 3 | 3.4+ | 组合式 API、TypeScript 友好、生态成熟 |
| **构建** | Vite | 5.x | 极速 HMR、原生 ESM、构建快 |
| **语言** | TypeScript | 5.x | 类型安全、更好的 IDE 支持 |
| **路由** | Vue Router 4 | 4.x | Vue 3 官方路由、动态路由、导航守卫 |
| **状态管理** | Pinia | 2.x | Vue 3 官方状态管理、简洁、TypeScript 原生支持 |
| **HTTP 客户端** | Axios | 1.x | 拦截器机制完善、请求/响应转换 |
| **UI 框架** | Element Plus | 2.x | 企业级组件库、表单/表格/菜单开箱即用、中文文档完善 |
| **CSS 工具** | UnoCSS / Tailwind CSS | — | 按需生成、极小产物体积 |
| **图标** | @element-plus/icons-vue | — | 与 Element Plus 无缝集成 |
| **图表** | ECharts | 5.x | 功能强于 Chart.js、Vue 3 生态支持好 |
| **包管理** | pnpm | — | 速度快、磁盘空间省 |

### 3.2 备选方案

| 场景 | 备选 |
|------|------|
| UI 框架 | Ant Design Vue（更丰富但较重）、Naive UI（TypeScript 原生） |
| 图表 | Chart.js（当前项目已用，迁移成本低） |
| CSS 方案 | Scoped CSS（Vue 内置，无需额外工具） |

---

## 4. Vue 项目结构设计

### 4.1 目录位置

```
CourseSelectionSystem/
├── web-frontend/              # ★ 新增：Vue 前端项目
│   ├── package.json
│   ├── vite.config.ts
│   ├── tsconfig.json
│   ├── index.html
│   ├── public/
│   │   └── favicon.ico
│   ├── src/
│   │   ├── main.ts
│   │   ├── App.vue
│   │   ├── env.d.ts
│   │   ├── api/               # API 接口封装
│   │   │   ├── index.ts       # Axios 实例 + 拦截器
│   │   │   ├── auth.ts        # 认证相关
│   │   │   ├── user.ts        # 用户管理
│   │   │   ├── student.ts     # 学生
│   │   │   ├── teacher.ts     # 教师
│   │   │   ├── course.ts      # 课程
│   │   │   ├── selection.ts   # 选课
│   │   │   ├── college.ts     # 学院
│   │   │   ├── department.ts  # 系部
│   │   │   ├── major.ts       # 专业
│   │   │   ├── grade.ts       # 成绩
│   │   │   ├── role.ts        # 角色
│   │   │   ├── permission.ts  # 权限
│   │   │   ├── dashboard.ts   # 仪表盘聚合
│   │   │   └── announcement.ts# 公告
│   │   ├── layout/            # 布局组件
│   │   │   ├── AdminLayout.vue
│   │   │   ├── StudentLayout.vue
│   │   │   ├── TeacherLayout.vue
│   │   │   └── components/
│   │   │       ├── Sidebar.vue
│   │   │       ├── HeaderBar.vue
│   │   │       └── FooterBar.vue
│   │   ├── views/             # 页面组件
│   │   │   ├── login/
│   │   │   │   └── LoginView.vue
│   │   │   ├── admin/
│   │   │   │   ├── DashboardView.vue
│   │   │   │   ├── StudentManage.vue
│   │   │   │   ├── TeacherManage.vue
│   │   │   │   ├── CourseManage.vue
│   │   │   │   ├── CollegeManage.vue
│   │   │   │   ├── DepartmentManage.vue
│   │   │   │   ├── MajorManage.vue
│   │   │   │   ├── ClassManage.vue
│   │   │   │   ├── GradeManage.vue
│   │   │   │   ├── SemesterManage.vue
│   │   │   │   ├── RoleManage.vue
│   │   │   │   ├── PermissionManage.vue
│   │   │   │   ├── AnnouncementManage.vue
│   │   │   │   ├── AdminManage.vue
│   │   │   │   ├── SystemLogs.vue
│   │   │   │   └── SystemSettings.vue
│   │   │   ├── student/
│   │   │   │   ├── DashboardView.vue
│   │   │   │   ├── CourseSelection.vue
│   │   │   │   ├── MyCourses.vue
│   │   │   │   ├── Schedule.vue
│   │   │   │   ├── Grades.vue
│   │   │   │   ├── Evaluations.vue
│   │   │   │   ├── Messages.vue
│   │   │   │   ├── Profile.vue
│   │   │   │   └── Settings.vue
│   │   │   └── teacher/
│   │   │       ├── DashboardView.vue
│   │   │       ├── CourseManage.vue
│   │   │       ├── StudentManage.vue
│   │   │       ├── GradeManage.vue
│   │   │       ├── TeachingStats.vue
│   │   │       └── PersonalInfo.vue
│   │   ├── router/            # 路由配置
│   │   │   └── index.ts
│   │   ├── stores/            # Pinia 状态管理
│   │   │   ├── auth.ts        # 认证状态
│   │   │   ├── app.ts         # 全局应用状态
│   │   │   └── settings.ts    # 用户设置
│   │   ├── composables/       # 组合式函数
│   │   │   ├── useAuth.ts
│   │   │   ├── usePagination.ts
│   │   │   └── usePermission.ts
│   │   ├── utils/             # 工具函数
│   │   │   ├── constants.ts   # 常量
│   │   │   ├── helpers.ts     # 通用工具
│   │   │   └── validators.ts  # 表单校验
│   │   ├── types/             # TypeScript 类型定义
│   │   │   ├── api.ts         # API 响应类型
│   │   │   ├── user.ts        # 用户相关类型
│   │   │   ├── course.ts      # 课程相关类型
│   │   │   └── ...
│   │   └── assets/            # 静态资源
│   │       ├── styles/
│   │       │   ├── variables.scss
│   │       │   ├── reset.scss
│   │       │   └── global.scss
│   │       └── images/
│   └── .env.development       # 开发环境变量
│   └── .env.production        # 生产环境变量
├── web-service/               # ★ 后续可缩减或移除
└── ...
```

### 4.2 环境变量配置

**`.env.development`**
```env
VITE_API_BASE_URL=http://localhost:9000
VITE_APP_TITLE=网上选课系统（开发）
```

**`.env.production`**
```env
VITE_API_BASE_URL=
VITE_APP_TITLE=网上选课系统
```

---

## 5. 路由设计

### 5.1 路由结构

```typescript
// src/router/index.ts
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/admin',
    component: () => import('@/layout/AdminLayout.vue'),
    meta: { requiresAuth: true, roles: ['admin'] },
    redirect: '/admin/dashboard',
    children: [
      { path: 'dashboard',         name: 'AdminDashboard',    component: () => import('@/views/admin/DashboardView.vue') },
      { path: 'students',          name: 'StudentManage',     component: () => import('@/views/admin/StudentManage.vue') },
      { path: 'teachers',          name: 'TeacherManage',     component: () => import('@/views/admin/TeacherManage.vue') },
      { path: 'courses',           name: 'CourseManage',      component: () => import('@/views/admin/CourseManage.vue') },
      { path: 'colleges',          name: 'CollegeManage',     component: () => import('@/views/admin/CollegeManage.vue') },
      { path: 'departments',       name: 'DepartmentManage',  component: () => import('@/views/admin/DepartmentManage.vue') },
      { path: 'majors',            name: 'MajorManage',       component: () => import('@/views/admin/MajorManage.vue') },
      { path: 'classes',           name: 'ClassManage',       component: () => import('@/views/admin/ClassManage.vue') },
      { path: 'grades',            name: 'AdminGradeManage',  component: () => import('@/views/admin/GradeManage.vue') },
      { path: 'semesters',         name: 'SemesterManage',    component: () => import('@/views/admin/SemesterManage.vue') },
      { path: 'roles',             name: 'RoleManage',        component: () => import('@/views/admin/RoleManage.vue') },
      { path: 'permissions',       name: 'PermissionManage',  component: () => import('@/views/admin/PermissionManage.vue') },
      { path: 'announcements',     name: 'AnnouncementManage',component: () => import('@/views/admin/AnnouncementManage.vue') },
      { path: 'admins',            name: 'AdminManage',       component: () => import('@/views/admin/AdminManage.vue') },
      { path: 'logs',              name: 'SystemLogs',        component: () => import('@/views/admin/SystemLogs.vue') },
      { path: 'settings',          name: 'SystemSettings',    component: () => import('@/views/admin/SystemSettings.vue') },
    ]
  },
  {
    path: '/student',
    component: () => import('@/layout/StudentLayout.vue'),
    meta: { requiresAuth: true, roles: ['student'] },
    redirect: '/student/dashboard',
    children: [
      { path: 'dashboard',        name: 'StudentDashboard',   component: () => import('@/views/student/DashboardView.vue') },
      { path: 'course-selection', name: 'CourseSelection',    component: () => import('@/views/student/CourseSelection.vue') },
      { path: 'my-courses',       name: 'MyCourses',          component: () => import('@/views/student/MyCourses.vue') },
      { path: 'schedule',         name: 'Schedule',           component: () => import('@/views/student/Schedule.vue') },
      { path: 'grades',           name: 'StudentGrades',      component: () => import('@/views/student/Grades.vue') },
      { path: 'evaluations',      name: 'Evaluations',        component: () => import('@/views/student/Evaluations.vue') },
      { path: 'messages',         name: 'Messages',           component: () => import('@/views/student/Messages.vue') },
      { path: 'profile',          name: 'StudentProfile',     component: () => import('@/views/student/Profile.vue') },
      { path: 'settings',         name: 'StudentSettings',    component: () => import('@/views/student/Settings.vue') },
    ]
  },
  {
    path: '/teacher',
    component: () => import('@/layout/TeacherLayout.vue'),
    meta: { requiresAuth: true, roles: ['teacher'] },
    redirect: '/teacher/dashboard',
    children: [
      { path: 'dashboard',        name: 'TeacherDashboard',   component: () => import('@/views/teacher/DashboardView.vue') },
      { path: 'courses',          name: 'TeacherCourseManage', component: () => import('@/views/teacher/CourseManage.vue') },
      { path: 'students',         name: 'TeacherStudentManage',component: () => import('@/views/teacher/StudentManage.vue') },
      { path: 'grades',           name: 'TeacherGradeManage', component: () => import('@/views/teacher/GradeManage.vue') },
      { path: 'statistics',       name: 'TeachingStats',      component: () => import('@/views/teacher/TeachingStats.vue') },
      { path: 'profile',          name: 'TeacherProfile',     component: () => import('@/views/teacher/PersonalInfo.vue') },
    ]
  },
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: () => import('@/views/NotFound.vue') }
]
```

### 5.2 导航守卫

```typescript
// src/router/index.ts
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()

  if (to.meta.requiresAuth !== false && !authStore.isAuthenticated) {
    // 未登录 → 跳转登录页，保留目标路径
    return next({ path: '/login', query: { redirect: to.fullPath } })
  }

  if (to.meta.roles && !to.meta.roles.includes(authStore.userRole)) {
    // 角色不匹配 → 跳转对应角色首页
    const roleHome: Record<string, string> = {
      admin: '/admin/dashboard',
      student: '/student/dashboard',
      teacher: '/teacher/dashboard'
    }
    return next({ path: roleHome[authStore.userRole] || '/login' })
  }

  next()
})
```

---

## 6. 认证流程设计

### 6.1 完整登录流程

```
┌──────────┐         ┌──────────────┐         ┌──────────────┐
│  Vue SPA │         │ Gateway:9000 │         │ user-service │
│  (浏览器) │         │ JwtAuthFilter│         │    :8101     │
└────┬─────┘         └──────┬───────┘         └──────┬───────┘
     │                      │                        │
     │  POST /api/v1/auth/login                      │
     │  {username,password} │                        │
     │─────────────────────▶│───────────────────────▶│
     │                      │                        │
     │                      │           登录验证      │
     │                      │        生成 JWT Token  │
     │                      │                        │
     │  {code:200, data:    │◀───────────────────────│
     │   {token, user}}     │                        │
     │◀─────────────────────│                        │
     │                      │                        │
     │  localStorage:       │                        │
     │  · token             │                        │
     │  · userInfo          │                        │
     │  · selectedRole      │                        │
     │                      │                        │
     │  重定向到角色首页      │                        │
     │──────────────────────│                        │
     │                      │                        │
     │  GET /admin/dashboard │                       │
     │  Authorization:      │                        │
     │  Bearer <token>      │                        │
     │─────────────────────▶│  校验 Token 有效       │
     │                      │  转发到 web-service    │
     │                      │  （或直接返回前端静态） │
     │◀─────────────────────│                        │
```

### 6.2 Axios 拦截器配置

```typescript
// src/api/index.ts
import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL, // http://localhost:9000
  timeout: 15000,
})

// 请求拦截器：自动携带 JWT Token
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：统一错误处理
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200) return res.data
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message))
  },
  (error) => {
    if (error.response?.status === 401) {
      // Token 过期或无效 → 清除登录态 → 跳转登录
      const authStore = useAuthStore()
      authStore.logout()
      router.push('/login')
    }
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request
```

### 6.3 Pinia 认证 Store

```typescript
// src/stores/auth.ts
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { loginApi, getCurrentUser } from '@/api/auth'
import router from '@/router'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))
  const selectedRole = ref(localStorage.getItem('selectedRole') || '')

  const isAuthenticated = computed(() => !!token.value)
  const userRole = computed(() => userInfo.value?.userType ?? selectedRole.value)

  async function login(username: string, password: string) {
    const data = await loginApi({ username, password })
    token.value = data.token
    userInfo.value = data.user
    localStorage.setItem('token', data.token)
    localStorage.setItem('userInfo', JSON.stringify(data.user))
    // 根据角色跳转
    const roleMap: Record<number, string> = {
      1: '/student/dashboard',
      2: '/teacher/dashboard',
      3: '/admin/dashboard'
    }
    const homePath = roleMap[data.user.userType] || '/login'
    router.push(homePath)
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    selectedRole.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    localStorage.removeItem('selectedRole')
    router.push('/login')
  }

  return { token, userInfo, selectedRole, isAuthenticated, userRole, login, logout }
})
```

---

## 7. 组件架构设计

### 7.1 通用组件抽离

| 组件 | 用途 | 基于 |
|------|------|------|
| `AppTable` | 通用表格（分页、排序、筛选） | Element Plus el-table |
| `AppForm` | 通用表单弹窗（新增/编辑） | Element Plus el-dialog + el-form |
| `AppSearch` | 搜索栏组件 | Element Plus el-input + el-select |
| `AppConfirm` | 确认操作弹窗 | Element Plus el-message-box |
| `AppUpload` | 文件上传（批量导入） | Element Plus el-upload |
| `AppTree` | 树形选择（权限/菜单） | Element Plus el-tree |
| `ChartCard` | 统计卡片（仪表盘用） | ECharts |

### 7.2 页面模板模式

每个管理页面遵循统一模式：

```vue
<template>
  <AppPage title="学生管理">
    <!-- 搜索/筛选栏 -->
    <SearchBar :fields="searchFields" @search="handleSearch" />

    <!-- 操作按钮 -->
    <ActionBar>
      <el-button type="primary" @click="openAdd">+ 添加学生</el-button>
      <el-button @click="batchImport">批量导入</el-button>
      <el-button @click="exportData">导出</el-button>
    </ActionBar>

    <!-- 数据表格 -->
    <AppTable
      :data="students"
      :columns="columns"
      :loading="loading"
      :pagination="pagination"
      @page-change="handlePageChange"
    >
      <template #action="{ row }">
        <el-button size="small" @click="openEdit(row)">编辑</el-button>
        <el-button size="small" @click="handleDelete(row)">删除</el-button>
      </template>
    </AppTable>

    <!-- 新增/编辑弹窗 -->
    <FormDialog v-model:visible="dialogVisible" :mode="dialogMode" :data="currentRow"
                @submit="handleSubmit" />
  </AppPage>
</template>
```

### 7.3 布局结构

每个角色布局统一：

```
┌──────────────────────────────────────┐
│  HeaderBar                           │
│  ┌──────────┬───────────────────────┐│
│  │          │                       ││
│  │ Sidebar  │   <router-view />     ││
│  │ (菜单)    │   (页面内容)          ││
│  │          │                       ││
│  │          │                       ││
│  └──────────┴───────────────────────┘│
│  FooterBar (可选)                    │
└──────────────────────────────────────┘
```

---

## 8. API 集成方案

### 8.1 Vite 开发代理配置

```typescript
// vite.config.ts
export default defineConfig({
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:9000',
        changeOrigin: true,
      },
      '/login': {
        target: 'http://localhost:9000',
        changeOrigin: true,
      },
    }
  }
})
```

### 8.2 API 模块封装示例

```typescript
// src/api/student.ts
import request from './index'

export interface StudentQuery {
  page?: number
  size?: number
  keyword?: string
  collegeId?: number
  majorId?: number
  status?: string
}

export function getStudentList(params: StudentQuery) {
  return request.get('/api/v1/students/list', { params })
}

export function getStudentById(id: number) {
  return request.get(`/api/v1/students/${id}`)
}

export function createStudent(data: Record<string, any>) {
  return request.post('/api/v1/students', data)
}

export function updateStudent(id: number, data: Record<string, any>) {
  return request.put(`/api/v1/students/${id}`, data)
}

export function deleteStudent(id: number) {
  return request.delete(`/api/v1/students/${id}`)
}

export function resetPassword(id: number) {
  return request.put(`/api/v1/students/${id}/reset-password`)
}
```

### 8.3 前端 API 调用清单（完整）

| 功能域 | 前端 API 模块 | 后端服务 | Gateway 路径 |
|--------|--------------|----------|-------------|
| 登录/认证 | `api/auth.ts` | user-service | `/api/v1/auth/**` |
| 用户管理 | `api/user.ts` | user-service | `/api/v1/users/**` |
| 角色管理 | `api/role.ts` | user-service | `/api/v1/roles/**` |
| 权限管理 | `api/permission.ts` | user-service | `/api/v1/permissions/**` |
| 学生管理 | `api/student.ts` | student-service | `/api/v1/students/**` |
| 班级管理 | `api/class.ts` | student-service | `/api/v1/classes/**` |
| 教师管理 | `api/teacher.ts` | teacher-service | `/api/v1/teachers/**` |
| 课程管理 | `api/course.ts` | course-service | `/api/v1/courses/**` |
| 学院管理 | `api/college.ts` | course-service | `/api/v1/colleges/**` |
| 系部管理 | `api/department.ts` | course-service | `/api/v1/departments/**` |
| 专业管理 | `api/major.ts` | course-service | `/api/v1/majors/**` |
| 学期管理 | `api/semester.ts` | course-service | `/api/v1/semesters/**` |
| 公告管理 | `api/announcement.ts` | course-service | `/api/v1/course-announcements/**` |
| 选课操作 | `api/selection.ts` | selection-service | `/api/v1/selections/**` |
| 成绩管理 | `api/grade.ts` | selection-service | `/api/v1/grades/**` |
| 课程评价 | `api/evaluation.ts` | selection-service | `/api/v1/evaluations/**` |
| 仪表盘 | `api/dashboard.ts` | web-service | `/api/v1/dashboard/**` |
| AI 助手 | `api/ai.ts` | web-service | `/api/v1/ai/**` |

---

## 9. 分阶段迁移计划

### 阶段划分总览

```
阶段1 (第1周)    阶段2 (第2-3周)    阶段3 (第4-5周)    阶段4 (第6周)     阶段5 (收尾)
┌──────────┐    ┌──────────────┐   ┌──────────────┐   ┌──────────┐    ┌──────────┐
│ 项目初始化 │──▶│ Admin模块迁移 │──▶│ Student模块  │──▶│ Teacher   │──▶│ 清理与部署│
│ + 登录    │    │ (16个页面)   │   │ Teacher模块  │   │ 模块收尾   │    │ + 文档   │
│ + 认证    │    │              │   │ (9+6个页面)  │   │ + 集成测试 │    │          │
│ + 布局    │    │              │   │              │   │          │    │          │
└──────────┘    └──────────────┘   └──────────────┘   └──────────┘    └──────────┘
```

### 阶段 1：项目初始化与认证（预计 1 周）

**目标**：搭建 Vue 项目骨架，完成登录认证全流程

| 任务 | 产出 | 依赖 |
|------|------|------|
| 1.1 初始化 Vue 3 + Vite + TS 项目 | `web-frontend/` 目录 | — |
| 1.2 配置 ESLint + Prettier | 代码规范 | 1.1 |
| 1.3 集成 Element Plus + 主题定制 | 全局样式 | 1.1 |
| 1.4 配置 Vite 代理到 Gateway | `vite.config.ts` | 1.1 |
| 1.5 实现 Axios 封装 + 拦截器 | `src/api/index.ts` | 1.4 |
| 1.6 实现 Pinia auth store | `src/stores/auth.ts` | 1.5 |
| 1.7 实现登录页面 `LoginView.vue` | 登录页 | 1.6 |
| 1.8 实现 Vue Router + 导航守卫 | 路由配置 | 1.6 |
| 1.9 实现三个角色布局组件 | Admin/Student/Teacher Layout | 1.8 |
| 1.10 实现退出登录功能 | `auth.logout()` | 1.6 |

**交付检查**：
- [ ] 启动 `pnpm dev` 后能打开登录页
- [ ] 输入 admin/123456 能成功登录
- [ ] 登录后跳转到 `/admin/dashboard`
- [ ] 刷新页面后登录态保持
- [ ] 退出登录清除 Token 并跳回登录页
- [ ] 未登录访问受保护路由自动跳转到登录页

### 阶段 2：管理员模块迁移（预计 2 周）

**目标**：将 16 个管理员页面从静态 HTML 迁移到 Vue 组件

| 任务 | 页面 | 核心组件 |
|------|------|----------|
| 2.1 仪表盘 | `DashboardView.vue` | ECharts 图表、统计卡片、Feign 聚合数据 |
| 2.2 学生管理 | `StudentManage.vue` | 表格+分页、搜索、新增/编辑/删除弹窗、批量导入 |
| 2.3 教师管理 | `TeacherManage.vue` | 同上模式 |
| 2.4 课程管理 | `CourseManage.vue` | 课程 CRUD、筛选、关联教师/学院选择器 |
| 2.5 学院管理 | `CollegeManage.vue` | 简单 CRUD 表格 |
| 2.6 系部管理 | `DepartmentManage.vue` | 简单 CRUD 表格 |
| 2.7 专业管理 | `MajorManage.vue` | 简单 CRUD 表格 |
| 2.8 班级管理 | `ClassManage.vue` | 班级 CRUD + 关联专业选择 |
| 2.9 成绩管理 | `AdminGradeManage.vue` | 成绩查询 + 筛选 |
| 2.10 学期管理 | `SemesterManage.vue` | 设置当前学期 |
| 2.11 角色管理 | `RoleManage.vue` | 角色 + 权限树分配 |
| 2.12 权限管理 | `PermissionManage.vue` | 权限树管理 |
| 2.13 管理员管理 | `AdminManage.vue` | 管理员账号管理 |
| 2.14 公告管理 | `AnnouncementManage.vue` | 公告 CRUD |
| 2.15 系统日志 | `SystemLogs.vue` | 日志查询/筛选 |
| 2.16 系统设置 | `SystemSettings.vue` | 设置表单 |

**提取的通用组件**：
- `AppTable.vue`（含分页）
- `AppFormDialog.vue`（新增/编辑弹窗）
- `AppSearchBar.vue`（搜索筛选栏）

**交付检查**：
- [ ] 16 个管理页面均可正常访问
- [ ] 每个页面正确调用 `/api/v1/**` 接口
- [ ] 分页、搜索、筛选、排序功能正常
- [ ] 新增/编辑/删除操作可正常提交
- [ ] 与旧 HTML 页面功能保持一致

### 阶段 3：学生 + 教师模块迁移（预计 2 周）

**目标**：将学生 9 个页面和教师 6 个页面迁移到 Vue

| 任务 | 页面 | 说明 |
|------|------|------|
| 3.1 学生首页 | `StudentDashboardView.vue` | 个人概览、通知、快捷入口 |
| 3.2 在线选课 | `CourseSelection.vue` | 课程列表、筛选、选课/退课操作、学分统计 |
| 3.3 我的课程 | `MyCourses.vue` | 已选课程列表 |
| 3.4 课表 | `Schedule.vue` | 周课表视图 |
| 3.5 成绩查询 | `StudentGrades.vue` | 成绩列表、学期筛选 |
| 3.6 课程评价 | `Evaluations.vue` | 评价表单 |
| 3.7 消息通知 | `Messages.vue` | 消息列表 |
| 3.8 个人资料 | `StudentProfile.vue` | 资料编辑、改密 |
| 3.9 设置 | `StudentSettings.vue` | 设置入口 |
| 3.10 教师首页 | `TeacherDashboardView.vue` | 课程统计、教学概览 |
| 3.11 课程管理 | `TeacherCourseManage.vue` | 我的课程列表 |
| 3.12 学生管理 | `TeacherStudentManage.vue` | 课程学生名单 |
| 3.13 成绩录入 | `TeacherGradeManage.vue` | 成绩录入表单 |
| 3.14 教学统计 | `TeachingStats.vue` | 统计图表 |
| 3.15 个人资料 | `TeacherPersonalInfo.vue` | 资料编辑、改密 |

**交付检查**：
- [ ] 学生/教师登录后跳转到各自首页
- [ ] 侧边栏菜单仅显示对应角色的页面
- [ ] 所有业务操作（选课、评价、成绩录入）正常
- [ ] 课表视图正确显示

### 阶段 4：集成测试与优化（预计 1 周）

**目标**：全面测试、性能优化、修复 Bug

| 任务 | 说明 |
|------|------|
| 4.1 全功能回归测试 | 覆盖所有角色 + 所有业务场景 |
| 4.2 移动端适配 | 确保表格在窄屏可用 |
| 4.3 路由懒加载优化 | 按页面拆分包 |
| 4.4 加载状态优化 | Skeleton、Loading 组件 |
| 4.5 错误处理完善 | 全局错误边界、友好提示 |
| 4.6 权限控制完善 | 按钮级权限指令 |

### 阶段 5：清理与部署（预计 1 周）

**目标**：移除旧前端、配置生产部署、更新文档

| 任务 | 说明 |
|------|------|
| 5.1 移除旧静态 HTML 文件 | 删除 `web-service/src/main/resources/static/` 下所有 HTML |
| 5.2 调整 `PageController` | 移除 `/`、`/login` 重定向；仅保留 `/login/logout` 和 `/health` |
| 5.3 Vue 构建产物部署 | 构建后放入 `web-service` 的 `static/` 或独立部署 |
| 5.4 更新 Gateway 路由 | 移除 web-service 的兜底路由或调整顺序 |
| 5.5 移除 Session 认证残留 | 删除 LoginInterceptor、Spring Security 相关配置 |
| 5.6 更新 AGENTS.md | 记录新的前端架构信息 |
| 5.7 更新 README.md | 新增前端启动/构建说明 |

---

## 10. 构建与部署

### 10.1 开发环境

```bash
# 1. 启动后端微服务（推荐一键脚本）
.\scripts\start-microservices.ps1

# 2. 安装前端依赖
cd web-frontend
pnpm install

# 3. 启动 Vue 开发服务器（端口 5173，自动代理到 Gateway:9000）
pnpm dev
```

### 10.2 生产构建

```bash
cd web-frontend
pnpm build    # 输出到 dist/
```

### 10.3 部署方案（二选一）

**方案 A：由 web-service 托管（推荐过渡期）**

```bash
# 构建后将 dist/ 内容复制到 web-service 的静态资源目录
Copy-Item -Path web-frontend/dist/* -Destination web-service/src/main/resources/static/ -Recurse -Force

# 重新打包 web-service
.\mvnw.cmd -pl web-service -am package -DskipTests
```

此时所有前端路由由 Vue Router 处理，`web-service` 只需配置一个 catch-all 转发：

```java
// PageController 新增
@GetMapping("/{path:[^\\.]*}")
public String redirectToIndex() {
    return "forward:/index.html";
}
```

**方案 B：独立部署（推荐正式环境）**

```nginx
# Nginx 配置示例
server {
    listen 80;
    server_name course-select.example.com;

    # Vue 前端
    location / {
        root /var/www/course-selection-frontend;
        try_files $uri $uri/ /index.html;
    }

    # API 请求 → Gateway
    location /api/ {
        proxy_pass http://localhost:9000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # 登录退出等
    location /login/ {
        proxy_pass http://localhost:9000;
    }
}
```

---

## 11. 风险与应对

| 风险 | 影响 | 概率 | 应对措施 |
|------|------|------|----------|
| API 字段不兼容 | 前端数据显示错误 | 中 | 阶段 1 先建立 API 类型定义，逐一核对字段 |
| JWT Token 过期 | 用户操作中断 | 高 | Axios 拦截器自动检测 401，引导重新登录 |
| 双认证机制过渡期混乱 | 部分接口仍需 Session | 中 | Session 认证保留到阶段 5，Vue 优先使用 JWT |
| 迁移期间业务中断 | 用户无法使用 | 低 | 旧 HTML 和 Vue 并行部署，通过 URL 参数切换 |
| 开发代理跨域 | 开发环境请求失败 | 低 | Vite proxy 配置已验证可行 |
| Element Plus 定制难度 | UI 与原风格不一致 | 低 | 覆盖 Element Plus 主题变量即可 |
| 页面数量多，迁移耗时长 | 项目周期拉长 | 中 | 分阶段交付，每个阶段可独立上线 |

### 并行策略

迁移期间，旧 HTML 和新 Vue 可以**共存**：

```
Gateway:9000
  ├── /admin/*            → 旧 HTML（web-service）
  ├── /student/*          → 旧 HTML（web-service）
  ├── /teacher/*          → 旧 HTML（web-service）
  └── /vue/*              → Vue SPA（开发时直连 Vite dev server）
```

等到 Vue 版本完成验收后，一刀切切换：

```
Gateway:9000
  └── /*                  → Vue SPA（web-service 或独立部署）
```

---

## 12. 附录

### 12.1 迁移前后对比

| 维度 | 迁移前 | 迁移后 |
|------|--------|--------|
| 前端框架 | 无（纯 HTML/JS） | Vue 3 + TypeScript |
| UI 组件 | 手写 CSS | Element Plus |
| 状态管理 | localStorage + DOM | Pinia |
| API 调用 | 散落在各 HTML 中 | 统一 Axios + API 模块 |
| 路由 | 后端控制 | Vue Router 前端控制 |
| 构建工具 | 无 | Vite |
| 包管理 | 无 | pnpm |
| 代码复用 | 极低 | 组件化 + 组合式 API |
| 可维护性 | 低 | 高 |
| 开发体验 | 手动刷新 | HMR 热更新 |

### 12.2 常用命令速查

```bash
# 前端
cd web-frontend
pnpm dev              # 开发服务器
pnpm build            # 生产构建
pnpm preview          # 预览构建产物
pnpm lint             # 代码检查
pnpm type-check       # TS 类型检查

# 后端
.\mvnw.cmd -DskipTests package           # 编译全部
.\scripts\start-microservices.ps1        # 一键启动
```

### 12.3 参考资源

- [Vue 3 官方文档](https://vuejs.org/)
- [Vite 配置指南](https://vitejs.dev/config/)
- [Element Plus 组件库](https://element-plus.org/)
- [Pinia 状态管理](https://pinia.vuejs.org/)
- [Axios 拦截器](https://axios-http.com/docs/interceptors)

---

> **下一步行动建议**：如果确认启动，推荐从 **阶段 1** 开始——先跑通 Vue 项目骨架 + 登录流程，验证前后端联调链路，再逐步迁移页面模块。
