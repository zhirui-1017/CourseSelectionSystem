# Spring Cloud 微服务优化计划

## 1. 优化目标

本计划用于指导 `CourseSelectionSystem` 从单体应用逐步演进为 Spring Cloud 微服务项目。优化目标不是立即重写业务，而是在保持现有功能稳定的前提下，逐步增加服务注册、统一网关、熔断、健康检查和模块化边界。

第一阶段目标：

- 保留现有业务功能。
- 保留 Session 登录。
- 保留共享 MySQL 数据库。
- 引入 Eureka 注册中心。
- 引入 Spring Cloud Gateway。
- 在 Gateway 层增加基础熔断和 fallback。
- 为后续按业务域拆分服务做好结构准备。

## 2. 版本策略

原项目使用 Spring Boot `4.0.0`，但源码存在旧 API：

- `javax.servlet`
- `WebSecurityConfigurerAdapter`
- `antMatchers`

第一阶段已选择降级兼容策略：

- Spring Boot：`2.7.18`
- Spring Cloud：`2021.0.9`
- Java：`17`

选择原因：现有代码大量使用 Spring Boot 2 / Spring Security 5 时代 API，采用该版本组合能最大限度保持功能不变。后续如果要升级到 Spring Boot 3 或 4，应单独规划 Jakarta 包名迁移和新版 Spring Security 配置改造。

## 3. 服务规划

建议服务如下：

| 服务 | 默认端口 | 说明 |
| --- | ---: | --- |
| `eureka-server` | `8761` | 服务注册中心 |
| `gateway-server` | `9000` | 系统统一入口 |
| `web-service` | `8080` | 页面、静态资源、原有页面跳转 |
| `user-service` | `8101` | 用户、角色、权限、登录能力 |
| `student-service` | `8102` | 学生信息 |
| `teacher-service` | `8103` | 教师信息 |
| `course-service` | `8104` | 课程、学院、系部、专业 |
| `selection-service` | `8105` | 选课、退课、成绩、学分统计 |

服务命名建议统一使用小写短横线：

- `eureka-server`
- `gateway-server`
- `web-service`
- `user-service`
- `student-service`
- `teacher-service`
- `course-service`
- `selection-service`

## 4. 网关规划

Gateway 作为统一入口，负责：

- 路由转发。
- 请求超时控制。
- 熔断和 fallback。
- 后续可扩展为统一鉴权入口。

第一阶段不在 Gateway 中重写登录认证，只保证 Cookie、Session 和页面跳转可用。

建议访问入口：

```text
http://localhost:9000
```

核心路由：

```text
/login/**                  -> web-service
/admin/**                  -> web-service
/student/**                -> web-service 或 student-service
/teacher/**                -> web-service 或 teacher-service
/api/v1/users/**           -> user-service
/api/v1/roles/**           -> user-service
/api/v1/permissions/**     -> user-service
/api/v1/courses/**         -> course-service
/api/v1/colleges/**        -> course-service
/api/v1/departments/**     -> course-service
/api/v1/majors/**          -> course-service
/api/v1/selections/**      -> selection-service
/api/v1/course-selections/** -> selection-service
```

## 5. 熔断规划

第一阶段建议只在 `gateway-server` 层加入熔断：

- 对核心业务服务设置统一超时时间。
- 对不可用服务返回明确 fallback JSON。
- 页面请求可以返回简单错误页或转发到统一错误提示。

建议 fallback 响应格式：

```json
{
  "code": 503,
  "message": "服务暂时不可用，请稍后重试",
  "data": null,
  "success": false
}
```

后续如果服务之间开始互相调用，再在业务服务内部加入 OpenFeign + Resilience4j。

## 6. 认证规划

第一阶段保留现有 Session 登录方式：

- 登录页面和静态资源先归 `web-service`。
- Gateway 转发时保留 Cookie。
- 不引入 JWT。
- 不引入 OAuth2。

后续可选升级路线：

1. 统一认证中心。
2. JWT 登录态。
3. Gateway 全局鉴权过滤器。
4. 细粒度权限控制。

## 7. 实施阶段

### 阶段 0：基线修复

- 修复 `pom.xml` 缺失依赖。
- 处理 `application.properties` 中的乱码和配置粘连。
- 确认 Spring Boot/Spring Cloud 版本策略。
- 确认当前项目可以运行基础测试。

### 阶段 1：多模块骨架

- 已完成根项目 Maven 父工程改造。
- 已新建 `common-lib`、`eureka-server`、`gateway-server`。
- 已新建业务服务模块目录。
- 已将原单体业务整体迁移到 `web-service`。
- 当前业务域服务仍是可启动骨架，具体业务代码后续逐步迁移。

### 阶段 2：注册中心和网关

- 启动 Eureka。
- 让 Gateway 和现有业务服务注册到 Eureka。
- 通过 Gateway 访问原有页面和 API。

### 阶段 3：业务域拆分

- 优先拆 `course-service` 和 `selection-service`。
- 再拆 `user-service`、`student-service`、`teacher-service`。
- 每拆一个模块，都要保留原有访问路径兼容。

### 阶段 4：熔断与健康检查

- Gateway 增加熔断。
- 所有服务增加 Actuator 健康检查。
- 服务不可用时返回明确 fallback。

### 阶段 5：认证和前端整理

- 评估是否继续保留 Session。
- 评估是否升级 JWT 或统一认证中心。
- 整理静态页面归属。

## 8. 测试清单

基础测试：

- Maven 父工程可以识别所有模块。
- 各模块能单独编译。
- Eureka 可以启动。
- Gateway 可以启动。
- 业务服务可以注册到 Eureka。

访问测试：

- `http://localhost:9000/login`
- `http://localhost:9000/admin/index`
- `http://localhost:9000/student/index`
- `http://localhost:9000/teacher/index`
- `http://localhost:9000/api/v1/courses/list`

业务回归：

- 登录。
- 查询课程。
- 选课。
- 退课。
- 查看已选课程。
- 查看成绩或学分统计。

熔断测试：

- 停止某个业务服务。
- 通过 Gateway 访问该服务路径。
- 确认返回 fallback 响应。

## 9. 暂不处理事项

第一阶段暂不处理：

- 分库分表。
- 分布式事务。
- 消息队列。
- 配置中心。
- 链路追踪。
- JWT/OAuth2。
- 前端重构。
- Kubernetes 或 Docker 编排。

这些内容可以在多模块和基础 Spring Cloud 能力稳定后再规划。
