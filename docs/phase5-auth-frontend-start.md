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

1. 统一登录入口：已确认使用 `templates/login.html` 的 `/login`，并将历史 `/login.html` 请求重定向到 `/login`。
2. 统一退出入口：已将学生、教师页面里的 `../login.html`、`#logout` 等静态退出链接收敛为 `/login/logout`；管理员页面中的旧注释也已同步改为统一退出地址。
3. 收敛前端 API 调用：优先保留 `/api/v1/...` 业务接口经 Gateway 调用，标记仍依赖 `/admin/...`、`/teacher/...` 兼容接口的页面脚本。
4. 明确认证升级条件：只有在页面 API 调用已基本从兼容控制器迁出后，再评估 JWT 或统一认证中心。

## 本次入口整理

- `LegacyLoginController` 负责把 `/login.html` 重定向到 `/login`，兼容旧书签和历史静态链接。
- `LoginInterceptor` 的未登录重定向地址已从 `/user/login` 收敛为 `/login`。
- `SecurityConfig` 放行 `/login.html`，确保旧入口能正常跳转到模板登录页。
- `static/login.html` 作为历史静态资源暂时保留；浏览器访问 `/login.html` 时优先由后端重定向到 `/login`，文件内部的兜底跳转也已从 `/login.html` 改为 `/login`。
- `/login.html`、`/login/logout`、`LoginInterceptor` 和 Spring Security 未认证入口均使用相对 `Location: /login`，避免经 Gateway 访问时跳出统一入口。

## 本次前端兼容接口清单

| 页面脚本 | 当前调用 | 当前归属 | 后续处理 |
| --- | --- | --- | --- |
| `static/js/student-courses.js` | `/login/current`、`/api/v1/courses/search`、`/api/v1/courses/active`、`/api/v1/course-selections/...` | 登录态在 `web-service`，业务接口经 Gateway 进入课程/选课服务 | 保持现状，作为阶段 5 的迁移参考基线 |
| `static/js/admin-dashboard.js` | 列表与计数已改为 `/api/v1/students/list`、`/api/v1/teachers/list`、`/api/v1/courses/list`、`/api/v1/course-selections/stats`；新增/删除已改为 `/api/v1/students/from-map`、`/api/v1/teachers/from-map`、`/api/v1/courses` 和对应 `DELETE /api/v1/.../{id}` | 管理员仪表盘主要业务数据经 Gateway 进入学生/教师/课程/选课服务 | 后续继续迁移编辑、重置密码等未接入的管理操作 |
| `static/js/teacher-dashboard.js` | `/login/current`、`/api/v1/courses/teacher/{teacherId}`、`/api/v1/course-selections/{selectionId}/grade`；`/teacher/courseStudents`、`/teacher/dashboard` 暂留 | 教师课程只读列表经 Gateway 进入 `course-service`；成绩写入进入 `selection-service` 并显式校验 `teacherId` 与课程归属；登录态、学生列表聚合和仪表盘聚合仍在 `web-service` | 后续先补齐教师视图所需学生详情与统计聚合，再迁移剩余组合接口 |

## 阶段 5 下一步边界

- 优先迁移只读列表接口，降低页面从兼容控制器迁出的风险。
- 写操作接口需要先确认请求参数和返回结构，避免静态页面与微服务 API 的字段名不一致。
- 在管理员、教师页面主要业务调用从 `/admin/...`、`/teacher/...` 迁出前，暂不升级为 JWT 或独立认证中心。

## 本次只读接口迁移

- 管理员仪表盘的学生、教师、课程列表已从 `/admin/...` 兼容接口切换到 `/api/v1/.../list`。
- 管理员仪表盘的选课统计已从 `/admin/stats` 切换到 `/api/v1/course-selections/stats`，由 `selection-service` 提供只读计数。
- 教师端课程管理、学生管理、成绩管理页面共用的课程下拉/课程卡片列表已从 `/teacher/myCourses` 切换到 `/api/v1/courses/teacher/{teacherId}`，由 `course-service` 提供只读课程列表。
- `AppApi.pageItems` 继续兼容 `items`、`content`、`records`；新增 `AppApi.pageTotal` 兼容 `total`、`totalElements`、`totalCount`，用于不同分页返回结构的总数显示。
- 管理员新增学生/教师/课程以及删除学生/教师/课程已从 `/admin/add*`、`/admin/delete*` 切换到 `/api/v1/...` 业务服务接口；前端仍保留字段默认值，避免改变表单填写范围。
- 教师端成绩保存已从 `/teacher/updateGrade` 切换到 `/api/v1/course-selections/{selectionId}/grade`，由 `selection-service` 写入成绩并校验教师课程归属；当前仍通过前端传递 `teacherId`，后续统一认证前不把它视为最终鉴权方案。
