# Redis 接入方案（课程选择系统 · 微服务版）

> 技术栈：Spring Boot 2.7.18 / Spring Cloud（Gateway + Eureka + Feign）微服务
> Redis：本地 `localhost:6379`（默认库 0），使用 Spring Data Redis（Lettuce）
>
> 本项目把 Redis 用在三个典型场景：**热点查询缓存**、**选课并发控制（分布式锁/防超卖）**、**登录验证码（会话类数据）**。

---

## 一、总体设计

| 场景 | 所在服务 | 机制 | 关键点 |
|---|---|---|---|
| 热点查询缓存 | course-service | Spring Cache + `@Cacheable/@CacheEvict` | 缓存课程 active / 按学期 / 详情，课程增删改自动失效 |
| 并发控制（防超卖） | selection-service | 基于 Redis `SETNX` 的分布式锁 | 同一课程选课/退课串行化，锁带 TTL 兜底，UUID 防误删 |
| 验证码（会话数据） | user-service + web-service | 后端出题写入 Redis，登录时取用校验并删除 | 一次性验证码，TTL 2 分钟 |

各服务之间**独立连接同一 Redis**，互不共享内部缓存命名空间（缓存名都带服务语义前缀）。

---

## 二、依赖与配置

每个接入服务 `pom.xml` 增加（版本由父 `spring-boot-dependencies` BOM 管理，无需写版本）：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<!-- 仅 course-service 需要缓存 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

`application.properties`（各服务相同）：

```properties
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.database=0
spring.redis.timeout=3s
spring.redis.lettuce.pool.max-active=16
spring.redis.lettuce.pool.max-idle=8
spring.redis.lettuce.pool.min-idle=2
# 仅 course-service
spring.cache.type=redis
spring.cache.redis.time-to-live=5m
spring.cache.redis.cache-null-values=false
```

---

## 三、场景一：热点查询缓存（course-service）

### 3.1 入口

- 启动类加 `@EnableCaching`（`CourseServiceApplication.java`）。
- `config/RedisConfig.java` 定义 `RedisCacheManager`：值用 `GenericJackson2JsonRedisSerializer`（JSON 可读），键用字符串序列化，默认 TTL 5 分钟，事务感知。

### 3.2 缓存键设计

| 方法 | 缓存名 | 键 |
|---|---|---|
| `getCourseById(id)` | `course:detail` | `#courseId` |
| `getActiveCourses()` | `course:active` | `'list'` |
| `getActiveCoursesBySemester(sem)` | `course:active` | `semester` 或 `'list'` |

### 3.3 失效策略

写方法上加 `@CacheEvict(value = {"course:active","course:detail"}, allEntries = true)`：
`addCourse`、`updateCourse`、`deleteCourse`（两种签名）、`batchDeleteCourses`、`changeCourseStatus`。

> 说明：课程“已选人数 selected_count”会随选课变化。当前采用「写操作即清缓存 + 5 分钟短 TTL」折中；如需实时一致，可在 selection-service 选/退课成功后通过 Feign 或消息通知 course-service 主动 evict（见“扩展”）。

### 3.4 实测效果

```
GET /api/v1/courses/active 首次 ≈ 403ms（回源 DB）
GET /api/v1/courses/active 二次 ≈  56ms（命中 Redis 缓存）
```

---

## 四、场景二：选课分布式锁（selection-service）

### 4.1 为什么需要锁

原 `selectCourse` 的“查已选 count → 判容量 → insert”是**读-判-写**非原子操作，多线程/多实例并发抢同一门课时可能**超员**或**并发插队**。故引入基于 Redis `SETNX` 的分布式锁，把同一课程的容量判断与写入串行化。

### 4.2 实现

- `component/RedisLock.java`：加锁（`setIfAbsent(key, owner, TTL=15s)`）、释放（**仅 value 等于自己的 UUID 才删除**，防误删他人锁）、Redis 不可用时**优雅降级为无锁执行**（不阻断业务）。
- `controller/CourseSelectionController.java`
  - `POST /course-selections`（选课）：按 `courseId` 加锁 `lock:course:{courseId}`，带短重试（约 1.5s），`finally` 释放。service 方法返回时事务已提交，因此释放一定发生在提交之后，锁语义正确。
  - `DELETE /course-selections/{selectionId}`（退课）：按 `selectionId` 加锁 `lock:selection:{selectionId}`，与“候补晋升”串行化。

锁键示例：

```
lock:course:7        ->  value: uuid（持有者）
lock:selection:88    ->  value: uuid（持有者）
```

### 4.3 特性

- 防误删：释放前比对 value=UUID。
- 防死锁：TTL 15s 自动过期兜底。
- 防击穿：选课高峰有短重试。
- 可用性：Redis 挂掉时降级为无锁（业务不中断），并打印告警日志。

### 4.4 实测

- 重复选同一门课 → 业务返回 400「您已选择该课程」（走完整加锁→校验→释放链路，无异常）。
- 正常选课 → 200；正常退课 → 200。

---

## 五、场景三：登录验证码 Redis 化（user-service + login.html）

### 5.1 原来

验证码由**前端 Canvas 自生成、前端自己比对**（`window.generatedCaptcha`），后端完全不参与 → 形同虚设。

### 5.2 改造后

1. `GET /api/v1/auth/captcha`（后端，user-service）：
   - 生成 4 位随机码 → 写入 Redis `captcha:{uuid}`，TTL 2 分钟；
   - 用 `BufferedImage` 绘制干扰验证码图片，以 `data:image/png;base64` 返回 `{captchaId, image}`。
2. 登录 `POST /api/v1/auth/login`：请求体带 `captchaId + captchaCode`；
   - 后端 `verifyCaptcha` 从 Redis 读取比对（忽略大小写），**取用即删（一次性）**；
   - 不匹配 → 400「验证码错误」；不存在/过期 → 400「验证码已过期，请刷新后重试」。
   - 兼容：未携带 `captchaId` 的旧客户端/自动化调用不强制校验（便于接口联调）。
3. 网关 `JwtAuthFilter` 白名单加入 `/api/v1/auth/captcha`（登录前需取码，无需 Token）。
4. 前端 `login.html`：
   - 用 `<img>` 展示后端图片，点击可刷新（重新向后端取新码）；
   - 不再本地生成/比对；登录时提交 `captchaId/captchaCode`；
   - 错误由后端返回并提示。

Redis 键：`captcha:{uuid}`（一次性，TTL 120s）。

### 5.3 实测

- `GET /api/v1/auth/captcha` → 200，返回 captchaId + base64 图片；
- 携带错误验证码登录 → 400「验证码错误」（服务端判断，浏览器端可见）；
- 携带已过期/不存在的 captchaId → 400「验证码已过期」；
- 不带验证码（兼容模式）→ 200 正常登录；
- 点击验证码图片 → captchaId 变化（重新下发）。

---

## 六、Redis 键一览

| 键 | TTL | 用途 | 写入方 |
|---|---|---|---|
| `captcha:{uuid}` | 120s | 登录验证码答案（一次性） | user-service |
| `lock:course:{courseId}` | 15s | 选课分布式锁 | selection-service |
| `lock:selection:{selectionId}` | 15s | 退课分布式锁 | selection-service |
| `course:active::list` / `course:active::{semester}` | 5m | 启用课程缓存 | course-service |
| `course:detail::{courseId}` | 5m | 课程详情缓存 | course-service |

> Spring Cache 实际键名形如 `course:active::list`（cacheName::key）。

---

## 七、如何验证 Redis 确实在起作用

1. `redis-cli KEYS '*'` 观察上述键。
2. 课程列表接口连续两次请求，第二次明显更快（见 3.4）。
3. 课程管理页新增/编辑/删除/改状态后，立即请求课程列表可看到最新数据（缓存已被 evict）。
4. 登录页刷新验证码后，`redis-cli` 可看到新的 `captcha:*` 键；登录成功后该键被删除。
5. 并发抢同一课程时，观察同一时间只有持锁者进入“查容量→写选课”临界区。

---

## 八、常见问题

- **Redis 未启动**：业务服务仍可启动；锁与缓存相关操作打印告警并降级（选课无锁执行、验证码接口报错）。生产建议 Redis 高可用。
- **课程人数显示延迟**：course-service 缓存 TTL 内已选人数可能滞后 ≤5 分钟；可调小 TTL 或做主动 evict。
- **为什么不用 redisson**：本项目选课锁仅需 SETNX+TTL 即满足语义，避免引入额外依赖；若后续需要可重入锁、看门狗等，可平滑替换为 Redisson。
- **多实例共享**：Redis 天然支持多实例/多副本共享锁与缓存，网关后任意实例都可正确协同。

---

## 九、扩展建议（后续可做）

- 选课成功/退课后，通过 Feign 通知 course-service `evict course:active`，实现缓存实时一致。
- 用 Redis 做「课程名额原子扣减」+ Lua 脚本，进一步替代「查库 count」。
- 登录成功后的用户会话/权限快照缓存到 Redis，加速 `current-user`。
- 引入 Redisson 提供可重入锁与看门狗。
