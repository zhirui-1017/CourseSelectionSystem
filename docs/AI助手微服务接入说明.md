# AI 智能助手微服务接入说明

> 从旧版单体工程（LangChain4j + 通义千问）改造成微服务后的 `assistant-service`。
> 技术栈：Spring Boot 2.7.18 / Spring Cloud（Eureka + Gateway）/ MyBatis-Plus / LangChain4j 1.3.0（OpenAI 兼容）。

---

## 一、架构

```
浏览器页面（chat-widget 悬浮组件）
        │  POST /api/v1/ai/**   （带 JWT）
        ▼
Gateway (9000)  ——  JwtAuthFilter 校验并注入 X-User-Id / X-Role / X-Username
        ▼  Route5: /api/v1/ai/** -> lb://assistant-service
assistant-service (8106)
        ├─ AiChatController       会话/消息/同步/SSE 流式
        ├─ CourseAssistant        LangChain4j 代理（学生/教师/管理员 角色提示词）
        ├─ AssistantDataTools     @Tool 函数工具（JdbcTemplate 直连业务库）
        └─ chat_session / chat_message  （会话与消息持久化）
```

- 数据库：与其它业务服务同库 `course_selection_system_cloud`（只读查询课程/学生/选课等；写操作仅聊天会话/消息）。
- 鉴权：复用 Gateway JWT。旧版 HttpSession 版身份已改为「网关注入的请求头」。

---

## 二、新增内容

| 类别 | 内容 |
|---|---|
| 模块 | `assistant-service/`（新微服务，端口 8106，已注册进根 pom 与 Eureka） |
| 网关 | 路由 5：`/api/v1/ai/**` → `lb://assistant-service`（在 web-service 兜底路由之前） |
| 脚本 | `start/stop-microservices.ps1` 登记 `assistant-service` |
| 数据库 | `chat_session`、`chat_message`（云库已建，DDL 见下） |
| 前端 | `web-service/static/js/chat-widget.js`、`css/chat-widget.css`；已接入 学生/教师/管理端 首页 |
| 文档 | 本文件 |

### 2.1 依赖说明
- 引入 LangChain4j `langchain4j` 与 `langchain4j-open-ai`（**非** Spring Boot Starter，因为官方 Starter 面向 Boot 3/jakarta；本模块手动装配 Bean 以兼容 Boot 2.7）。
- 会话持久化沿用 MyBatis-Plus（实体/Service/Mapper 从旧版移植）。

### 2.2 建表 DDL（已在云库执行，重新部署其它环境时执行一次）
```sql
CREATE TABLE IF NOT EXISTS chat_session (
  id BIGINT NOT NULL AUTO_INCREMENT,
  session_uid VARCHAR(64) NOT NULL,
  user_id BIGINT NOT NULL,
  user_role VARCHAR(20) NOT NULL,
  title VARCHAR(200) DEFAULT '新对话',
  message_count INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY(id), UNIQUE KEY uk_session_uid(session_uid), KEY idx_user_id_role(user_id,user_role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS chat_message (
  id BIGINT NOT NULL AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  role VARCHAR(20) NOT NULL,
  content TEXT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(id), KEY idx_session_id(session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 三、接口

统一前缀 `/api/v1/ai`（经网关），均需登录（JWT），身份取请求头。

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/health` | 健康检查 |
| GET | `/sessions` | 当前用户会话列表 |
| POST | `/sessions/new` | 新建会话 |
| DELETE | `/sessions/{sessionUid}` | 删除会话（含消息） |
| GET | `/sessions/{sessionUid}/messages` | 会话历史消息 |
| DELETE | `/sessions/{sessionUid}/messages` | 清空会话消息 |
| POST | `/chat` | 同步对话 `{message, sessionId}` |
| POST | `/chat/stream` | SSE 流式对话 `{message, sessionId}`（`text/event-stream`） |

SSE 事件：流式返回 `{"token":"..."}`，结束返回 `{"done":true,"sessionId":"..."}`。

---

## 四、配置与大模型 Key

`assistant-service/src/main/resources/application.properties`：
```properties
# 通义千问 / 百炼平台（OpenAI 兼容格式）
langchain4j.open-ai.chat-model.base-url=https://dashscope.aliyuncs.com/compatible-mode/v1
langchain4j.open-ai.chat-model.api-key=YOUR_API_KEY_HERE   # ← 换成你的 DashScope Key
langchain4j.open-ai.chat-model.model-name=qwen-plus
langchain4j.open-ai.streaming-chat-model.base-url=https://dashscope.aliyuncs.com/compatible-mode/v1
langchain4j.open-ai.streaming-chat-model.api-key=YOUR_API_KEY_HERE
langchain4j.open-ai.streaming-chat-model.model-name=qwen-plus
```

> 不填 Key 也能跑（会话、历史、前端组件均正常）；真正问答时才需要。Key 未填时后端会以 SSE 友好错误回显。

---

## 五、运行

```powershell
# 1) 先启动 eureka / gateway 及其它依赖服务
# 2) 编译并启动 assistant-service
.\scripts\start-microservices.ps1 -Service assistant-service
# 需要改代码时：
mvn -pl assistant-service -am -DskipTests package
```

验证：
```
# 浏览器打开学生端首页，右下角 💬 打开助手
GET  http://localhost:9000/api/v1/ai/health   (带 token) → {"status":"ok","service":"assistant-service"}
```

---

## 六、工具函数（AssistantDataTools，@Tool）

旧版工具重度依赖单体整套业务 Service，微服务版改为同库 JdbcTemplate 直查，目前提供：

| 角色 | 工具 |
|---|---|
| 通用 | 获取当前日期时间 |
| 学生 | 搜索课程 / 我的课表 / 我的成绩 |
| 教师 | 所授课程及选课人数 / 课程学生名单 |
| 管理员 | 系统运营统计（学生/教师/课程/选课/评价数） |

后续可继续补齐：成绩分布分析、热门课程 TOP5、课程评价查询、异常检测、操作日志、学期信息等（方法模式见 `AssistantDataTools`，每条 `@Tool` 一个方法即可被 LLM 调用）。

---

## 七、前端悬浮组件

- 组件由 `chat-widget.js` 自建 DOM（右下角 💬），自动为所有请求附加 `Authorization: Bearer <token>`。
- 已接入页面：`student/index.html`、`teacher/index.html`、`admin/index.html`（首页）。
- 在其它页面启用只需在两处插入：
  ```html
  <link rel="stylesheet" href="/css/chat-widget.css">
  ...
  <script src="/js/chat-widget.js"></script>
  ```

---

## 八、常见问题

- **对话报“AI 助手处理出错”**：多半是 api-key 未填 / 余额不足 / 网络无法访问 DashScope。填好 Key 后重启 assistant-service。
- **历史会话看不到**：检查是否以同一账号登录（会话按 userId+role 隔离），且 token 已写入 localStorage。
- **Gateway 返回 503**：Eureka 实例未同步，等几秒重试；确认 assistant-service 已在 `http://localhost:8761` 注册为 UP。
