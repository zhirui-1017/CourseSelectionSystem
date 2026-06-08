# 第一阶段运行验收说明

本说明用于验证 `module-structure-plan.md` 和 `microservice-optimization-plan.md` 中第一阶段的微服务改造目标。

## 前置条件

- JDK 17 可用，`JAVA_HOME` 应指向 JDK 根目录，不是 `bin` 目录。
- MySQL 中存在共享数据库 `course_selection_system`，账号密码与各服务 `application.properties` 一致。
- 端口 `8761`、`9000`、`8080`、`8101` 到 `8105` 未被其他程序占用。

如果当前环境的 `JAVA_HOME` 指向了 `...\bin`，脚本会自动取其父目录作为 JDK 根目录。

## 基础构建

```powershell
$env:JAVA_HOME='E:\biancheng\jdk\.jdks\oracle_open_jdk-17'
.\mvnw.cmd test
```

该命令应通过所有模块测试，证明 Maven 父工程可以识别所有子模块，且各服务可以在测试环境加载 Spring 上下文。

## 启动微服务栈

```powershell
.\scripts\start-microservices.ps1 -JavaHome 'E:\biancheng\jdk\.jdks\oracle_open_jdk-17'
```

脚本会按以下顺序启动服务，并等待每个服务的 Actuator 健康检查：

1. `eureka-server`
2. `web-service`
3. `user-service`
4. `student-service`
5. `teacher-service`
6. `course-service`
7. `selection-service`
8. `gateway-server`

运行日志和 PID 文件会写入 `.runtime/`，每个服务分别有 `.out.log` 和 `.err.log`，方便后续排查和停止服务。

## Gateway 访问验收

```powershell
.\scripts\smoke-test-gateway.ps1
```

该脚本会验证：

- Gateway 健康检查可访问。
- `/login` 页面入口可访问。
- `/admin/index`、`/student/index`、`/teacher/index` 页面路由可经 Gateway 访问。
- `/api/v1/courses/list` 会路由到 `course-service`。

## 熔断 fallback 验收

先停止一个业务服务，例如 `course-service`：

```powershell
.\scripts\stop-microservices.ps1 -Service course-service
```

再执行：

```powershell
.\scripts\smoke-test-gateway.ps1 -ExpectCourseFallback
```

预期 Gateway 返回 HTTP `503`，响应体包含：

```json
{
  "code": 503,
  "message": "服务暂时不可用，请稍后重试",
  "data": null,
  "success": false
}
```

## 停止服务

```powershell
.\scripts\stop-microservices.ps1 -All
```

如果只需要停止某个服务，可以使用：

```powershell
.\scripts\stop-microservices.ps1 -Service user-service
```

## 当前边界

第一阶段仍保留 Session 登录、共享 MySQL 数据库和 `web-service` 作为页面入口，不引入 JWT、OAuth2、分布式事务、配置中心、消息队列或前端重构。
