# 阶段 5 认证与前端整理启动说明

本文档记录阶段 5 的启动结论和首批整理边界。当前目标不是一次性替换认证体系或重构前端，而是在阶段 4 的健康检查和 fallback 稳定后，继续保持访问路径兼容并逐步收敛认证与页面归属。

## 当前结论

- 本阶段先继续保留 Session 登录，不立即引入 JWT、OAuth2 或独立认证中心。
- 原因是页面入口、登录态、`LoginController`、Spring Security 和现有静态页面仍依赖同域 Cookie/Session；贸然替换会扩大改造范围。
- Gateway 继续作为统一入口转发 Cookie，不在本阶段承担完整认证中心职责。
- `/actuator/**` 属于运行健康检查接口，已纳入安全放行范围，避免健康检查被登录页重定向掩盖。

## 当前归属

| 访问域 | 当前归属 | 阶段 5 处理方向 |
| --- | --- | --- |
| `/login/**` | `web-service` | 保留 Session 登录入口，后续整理静态登录页与模板登录页的唯一入口 |
| `/admin/**` | `web-service` | 保留页面和兼容管理接口，后续再拆分页面 API 调用 |
| `/student/**` | `web-service` | 保留静态页面；学生 REST API 已迁到 `student-service` |
| `/teacher/**` | `web-service` | 保留静态页面；教师 REST API 已迁到 `teacher-service` |
| `/api/v1/users/**`、`/api/v1/roles/**`、`/api/v1/permissions/**` | `user-service` | 继续作为认证、角色、权限数据接口归属 |
| `/api/v1/courses/**`、`/api/v1/colleges/**`、`/api/v1/departments/**`、`/api/v1/majors/**` | `course-service` | 保持 Gateway 优先路由到课程域服务 |
| `/api/v1/selections/**`、`/api/v1/course-selections/**` | `selection-service` | 保持 Gateway 优先路由到选课域服务 |

## 首批待整理项

1. 统一登录入口：确认使用 `templates/login.html` 的 `/login`，逐步清理或重定向 `static/login.html`。
2. 统一退出入口：将页面里的 `../login.html` 这类静态退出链接收敛为 `/login/logout`。
3. 收敛前端 API 调用：优先保留 `/api/v1/...` 业务接口经 Gateway 调用，标记仍依赖 `/admin/...`、`/teacher/...` 兼容接口的页面脚本。
4. 明确认证升级条件：只有在页面 API 调用已基本从兼容控制器迁出后，再评估 JWT 或统一认证中心。

