# 项目结构优化计划

## 1. 目标

当前项目是单模块 Spring Boot 应用，代码按技术层分包，例如 `controller`、`service`、`mapper`、`entity`。这种结构适合单体开发，但后续如果要加入 Spring Cloud、Eureka、服务网关和熔断能力，并进一步按业务域拆分服务，就需要提前规划多模块结构。

本计划的目标是：将项目逐步演进为 Maven 多模块微服务结构，按业务域拆分服务，但第一阶段仍共享原有 MySQL 数据库 `course_selection_system`，不做分库、不引入分布式事务、不改变现有业务功能。

## 2. 目标目录结构

当前已按第一阶段形成以下结构：

```text
CourseSelectionSystem/
  pom.xml
  AGENTS.md
  HELP.md
  docs/
    module-structure-plan.md
    microservice-optimization-plan.md

  common-lib/
  eureka-server/
  gateway-server/

  user-service/
  student-service/
  teacher-service/
  course-service/
  selection-service/
  web-service/
```

各模块职责如下：

| 模块 | 职责 |
| --- | --- |
| `common-lib` | 公共响应对象、常量、异常模型、通用 VO、基础工具类 |
| `eureka-server` | Eureka 服务注册中心 |
| `gateway-server` | 统一访问入口，负责路由、超时、熔断、fallback |
| `user-service` | 登录、用户、角色、权限 |
| `student-service` | 学生信息、学生端个人资料 |
| `teacher-service` | 教师信息、教师端个人资料 |
| `course-service` | 课程、学院、系部、专业 |
| `selection-service` | 选课、退课、选课记录、成绩、学分统计 |
| `web-service` | 原有静态页面、登录页面、页面跳转控制器；第一阶段用于保护现有前端入口 |

## 3. 业务服务内部结构

每个业务服务内部统一采用以下包结构：

```text
src/main/java/.../
  controller/
  service/
  service/impl/
  mapper/
  entity/
  vo/
  config/
```

说明：

- `controller`：对外 HTTP 接口或页面跳转控制器。
- `service`：业务接口。
- `service/impl`：业务实现。
- `mapper`：MyBatis-Plus Mapper。
- `entity`：数据库实体。
- `vo`：请求和响应对象。
- `config`：服务内配置。

第一阶段不强行按领域模型重构内部代码，优先完成模块边界和服务注册。

## 4. 数据库边界

第一阶段采用共享数据库策略：

- 所有业务服务仍连接同一个 MySQL 数据库：`course_selection_system`。
- 每个服务只访问自己负责的表。
- 不拆库，不拆 schema，不引入分布式事务。
- 跨服务数据查询第一阶段可以通过网关/API 调用逐步替换，避免一次性重写。

建议表归属：

| 服务 | 表或实体范围 |
| --- | --- |
| `user-service` | `sys_user`、`sys_role`、`sys_permission` |
| `student-service` | `student` |
| `teacher-service` | `teacher` |
| `course-service` | `course`、`college`、`department`、`major` |
| `selection-service` | `course_selection` |

## 5. 迁移顺序与当前状态

建议按以下顺序实施：

1. 已新建 `docs/` 文档，记录结构规划、服务职责、端口、路由、数据库表归属。
2. 已将根 `pom.xml` 改为 Maven 父工程，只负责模块聚合、版本管理和依赖管理。
3. 已新建 `common-lib`，放入统一响应对象、服务名常量和公共业务异常。
4. 已新建 `eureka-server`，提供服务注册中心。
5. 已新建 `gateway-server`，提供统一入口、路由和基础熔断 fallback。
6. 已将现有单体功能整体迁移到 `web-service`，保证页面和登录流程优先不中断。
7. 已新建 `course-service`、`selection-service`、`user-service`、`student-service`、`teacher-service` 骨架。
8. 后续再逐步从 `web-service` 拆分具体业务代码到对应服务。
9. 最后整理前端页面归属、认证方案、跨服务调用方式。

## 6. 路由规划

建议由 `gateway-server` 统一暴露入口，默认端口可设为 `9000`。

第一阶段路由建议：

| 网关路径 | 目标服务 |
| --- | --- |
| `/login/**` | `web-service` |
| `/admin/**` | `web-service` |
| `/student/**` | `web-service` 或 `student-service` |
| `/teacher/**` | `web-service` 或 `teacher-service` |
| `/static/**` | `web-service` |
| `/css/**` | `web-service` |
| `/js/**` | `web-service` |
| `/api/v1/users/**` | `user-service` |
| `/api/v1/roles/**` | `user-service` |
| `/api/v1/permissions/**` | `user-service` |
| `/api/v1/courses/**` | `course-service` |
| `/api/v1/colleges/**` | `course-service` |
| `/api/v1/departments/**` | `course-service` |
| `/api/v1/majors/**` | `course-service` |
| `/api/v1/selections/**` | `selection-service` |
| `/api/v1/course-selections/**` | `selection-service` |

## 7. 风险与注意事项

- 原单体 `pom.xml` 中依赖不完整，第一阶段已在 `web-service/pom.xml` 补齐 MyBatis-Plus、Spring Security、Spring Data JPA、Thymeleaf、Servlet API 等依赖。
- 为兼容现有 `javax.servlet`、`WebSecurityConfigurerAdapter`、`antMatchers` 等旧 API，第一阶段已采用 Spring Boot `2.7.18` 和 Spring Cloud `2021.0.9`。
- 当前存在多个 `Result` 类型：`common.Result`、`vo.Result`、`util.Result`。拆分前应逐步统一到 `common-lib`。
- 当前存在两套全局异常处理：`exception/GlobalExceptionHandler` 和 `handler/GlobalExceptionHandler`。拆分前应确认保留哪一套。
- 当前登录认证同时涉及 Spring Security 和 `LoginInterceptor`，经过 Gateway 后要确保 Cookie 和 Session 正常传递。
- 第一阶段不建议立刻改 JWT/OAuth2，否则会扩大改造范围。

## 8. 验收标准

结构优化完成后，应满足：

- 根项目可以识别所有 Maven 子模块。
- Eureka 控制台可以看到各服务注册。
- 通过 Gateway 可以访问原有页面入口。
- 通过 Gateway 可以访问核心 API。
- 停止任一业务服务后，Gateway 返回明确 fallback 响应。
- 原有选课核心流程不变：登录、查询课程、选课、退课、查看已选课程、查看成绩或学分统计。
