# 阶段 4 运行验收确认

确认时间：2026-06-08

本次验收范围对应 `microservice-optimization-plan.md` 的阶段 4：Gateway 熔断、所有服务 Actuator 健康检查、服务不可用时返回明确 fallback。

## 已确认结果

- `web-service`、`user-service`、`student-service`、`teacher-service`、`course-service`、`selection-service`、`eureka-server`、`gateway-server` 均可通过 `/actuator/health` 返回 `UP`。
- Gateway 健康检查返回 `UP` 后，启动脚本会继续等待 Gateway 服务发现缓存包含所有下游服务，避免刚启动时路由偶发进入 fallback。
- Gateway 基础路由验收通过：
  - `/login`
  - `/admin/index`
  - `/student/index`
  - `/teacher/index`
  - `/api/v1/courses/list`
- 核心业务只读回归通过：
  - 管理员登录与当前 Session 查询
  - 课程查询
  - 学生查询
  - 已选课程查询
  - 当前课程查询
  - 学分统计
  - 选课状态检查
- 停止 `course-service` 后，经 Gateway 访问课程接口返回 HTTP `503` 和统一 fallback JSON：

```json
{"code":503,"message":"服务暂时不可用，请稍后重试","data":null,"success":false}
```

## 本次修正

- `web-service` 放行 `/actuator/**`，避免健康检查被 Spring Security 重定向到登录页。
- `LoginInterceptor` 放行 `/actuator/**`，保持运行探针不依赖登录 Session。
- `start-microservices.ps1` 改为只接受健康 JSON 中的 `"status":"UP"`，不再把普通 200 页面误判为健康。
- `start-microservices.ps1` 增加端口占用预检和 Gateway 服务发现等待。
- `stop-microservices.ps1` 增加按端口兜底清理和端口释放等待，避免旧进程污染下一轮验收。

## 通过的命令

```powershell
$env:JAVA_HOME='E:\biancheng\jdk\.jdks\oracle_open_jdk-17'
.\mvnw.cmd test
.\mvnw.cmd -pl web-service,gateway-server -am test
.\scripts\start-microservices.ps1 -JavaHome 'E:\biancheng\jdk\.jdks\oracle_open_jdk-17' -SkipBuild -TimeoutSeconds 300
.\scripts\smoke-test-gateway.ps1 -TimeoutSec 20
.\scripts\regression-test-core-flow.ps1 -TimeoutSec 20
.\scripts\stop-microservices.ps1 -Service course-service
.\scripts\smoke-test-gateway.ps1 -ExpectCourseFallback -TimeoutSec 20
.\scripts\stop-microservices.ps1 -All
```

## 当前结论

阶段 4 的主运行验收已通过，可以进入阶段 5。后续可用 `scripts/verify-gateway-fallbacks.ps1` 扩展验证所有业务服务的 fallback 矩阵。

