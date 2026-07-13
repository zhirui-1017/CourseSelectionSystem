# Java 后端 50 道八股文速记（每天背 3-5 道）

> 配合 12 周计划使用，第 1-4 周集中背诵
> 来源：基于 JavaGuide + 鱼皮面试题 + 真实面试高频题整理

---

## 一、Java 基础（10 道）

### 1. == 和 equals 的区别？
- `==` 对基本类型比较值，对引用类型比较内存地址
- `equals` 默认是 `==`，但 String、Integer 等重写了 equals 比较内容
- **面试加分**：重写 equals 必须重写 hashCode（HashMap/HashSet 规则）

### 2. HashMap 底层原理？
- JDK 1.8 后：数组 + 链表 + 红黑树
- 链表长度 > 8 且数组长度 > 64 时转红黑树
- 扩容：容量翻倍，重新 hash 分布
- **关键点**：为什么是 8？符合泊松分布，正常情况下链表不会很长
- **追问**：为什么负载因子 0.75？空间和查询时间平衡

### 3. ConcurrentHashMap 怎么保证线程安全？
- JDK 1.7：分段锁（Segment）
- JDK 1.8：CAS + synchronized（锁住链表头/红黑树根）
- **读不加锁，写局部加锁，并发度大幅提升**

### 4. volatile 关键字的作用？
- 三大特性：可见性、禁止指令重排、**不保证原子性**
- 底层：MESI 缓存一致性协议 + 内存屏障
- **应用场景**：单例模式 double-check、状态标记位

### 5. synchronized 锁升级过程？
- 无锁 → 偏向锁（线程 ID 写入对象头）→ 轻量级锁（CAS + 自旋）→ 重量级锁（OS mutex）
- **目的**：减少锁开销，根据竞争激烈程度动态升级

### 6. 线程池 7 大参数？
- corePoolSize、maximumPoolSize、keepAliveTime、unit、workQueue、threadFactory、handler
- 4 种拒绝策略：Abort（抛异常）、CallerRuns（调用方执行）、Discard（丢弃）、DiscardOldest（丢最老）
- **实战**：你的选课系统选课接口可以用线程池异步处理通知

### 7. JVM 内存模型？
- 堆：新生代（Eden + 2 Survivor）+ 老年代
- 栈：每个线程一个，方法调用即入栈
- 方法区（元空间）：类信息、常量、静态变量
- 本地方法栈、PC 寄存器

### 8. GC 回收算法？
- 标记-清除（产生碎片）
- 标记-整理（老年代常用）
- 复制算法（新生代常用，效率高）
- 分代收集：新生代复制，老年代标记-整理

### 9. 类加载过程？
- 加载 → 验证 → 准备 → 解析 → 初始化
- **双亲委派**：Bootstrap → Extension → AppClassLoader → 自定义
- 打破双亲委派的场景：Tomcat WebAppClassLoader、JDBC SPI

### 10. 反射的优缺点？
- 优点：动态性、框架基石（Spring IoC、MyBatis 映射）
- 缺点：性能差（无法 JIT 优化）、破坏封装、安全隐患
- **追问**：setAccessible(true) 可以访问 private 字段

---

## 二、Java 集合（5 道）

### 11. ArrayList vs LinkedList？
- ArrayList：动态数组，随机访问 O(1)，插入删除 O(n)
- LinkedList：双向链表，插入删除 O(1)，随机访问 O(n)
- **绝大多数场景用 ArrayList**（CPU 缓存友好）

### 12. HashSet 怎么保证不重复？
- 底层就是 HashMap，元素作为 key，value 是 PRESENT 常量
- 依赖 hashCode + equals 保证唯一

### 13. List/Set/Map 区别？
- List：有序可重复
- Set：无序不可重复
- Map：键值对，键不可重复

### 14. fail-fast 和 fail-safe？
- fail-fast：ArrayList 遍历时修改抛 ConcurrentModificationException
- fail-safe：CopyOnWriteArrayList 在副本上遍历，不抛异常
- **原理**：modCount 版本号

### 15. TreeMap 和 LinkedHashMap 怎么保证有序？
- TreeMap：红黑树，按 key 排序
- LinkedHashMap：双向链表，按插入顺序或访问顺序

---

## 三、多线程（8 道）

### 16. 进程和线程的区别？
- 进程：资源分配的基本单位，独立内存空间
- 线程：CPU 调度的基本单位，共享进程内存
- **线程更轻量，但需要处理共享数据同步问题**

### 17. sleep 和 wait 的区别？
- sleep：Thread 静态方法，不释放锁
- wait：Object 方法，释放锁，进入等待池
- notify/notifyAll 唤醒

### 18. start 和 run 的区别？
- start 启动新线程，调用 run
- 直接调用 run 在当前线程执行

### 19. 死锁产生的 4 个条件？
- 互斥、占有并等待、不可剥夺、循环等待
- **解决**：破坏任一条件（最常用：按顺序加锁）

### 20. ThreadLocal 原理？
- 每个 Thread 有 ThreadLocalMap，key 是 ThreadLocal 实例（弱引用），value 是值
- **内存泄漏风险**：key 弱引用会被 GC，value 强引用不释放
- 解决：用完调用 remove()

### 21. AQS 原理？
- AbstractQueuedSynchronizer，JUC 核心
- 维护 state 和 CLH 双向队列
- ReentrantLock、Semaphore、CountDownLatch 都基于 AQS

### 22. ReentrantLock 和 synchronized 区别？
- Lock 接口，手动 lock/unlock，灵活
- synchronized JVM 内置，可重入，自动释放
- ReentrantLock 支持公平锁、可中断、多条件变量

### 23. 乐观锁和悲观锁？
- 悲观锁：先加锁再操作（synchronized、ReentrantLock）
- 乐观锁：不加锁，更新时检查版本号（CAS、@Version）
- **你的项目**：选课用 MySQL 行锁是悲观锁，库存场景更推荐

---

## 四、MySQL（10 道）

### 24. MySQL 事务 4 大特性？
- ACID：原子性（undo log）、一致性、一致性（redo log）、持久性
- 实现：redo log（持久性） + undo log（原子性） + MVCC（隔离性）

### 25. 事务隔离级别？
- READ UNCOMMITTED < READ COMMITTED < REPEATABLE READ < SERIALIZABLE
- MySQL 默认 **REPEATABLE READ**（通过 MVCC + next-key lock）
- 各级别解决的并发问题：脏读、不可重复读、幻读

### 26. MVCC 原理？
- 多版本并发控制：每行记录有 trx_id 和 roll_pointer
- 读视图（Read View）：活跃事务 ID 集合
- 快照读读 undo log 中可见版本
- **RR 级别下快照读不幻读，RC 每次读都生成新 Read View**

### 27. 索引底层结构？
- InnoDB：B+ 树，叶子节点存数据或主键
- 为什么用 B+ 树：磁盘 IO 少、范围查询快、叶子节点链表
- **聚簇索引**：数据按主键顺序存储（InnoDB 必有）
- **非聚簇索引**：叶子存主键（回表）

### 28. 最左前缀原则？
- 联合索引 (a, b, c)：能用到索引的有 a、(a,b)、(a,b,c)
- 不能用到：b、(b,c)
- **原因**：B+ 树先按 a 排序

### 29. 索引失效场景？
- 函数/计算（`WHERE YEAR(create_time)=2024`）
- 类型转换（`WHERE phone=123` phone 是 varchar）
- LIKE 前导模糊（`LIKE '%xxx'`）
- OR 条件（除非两边都有索引）
- **你的项目**：选课表用 (student_id, course_id) 联合索引

### 30. 慢查询优化思路？
- 定位：`EXPLAIN` 看 type、key、rows
- 优化：建索引、改写 SQL、避免 SELECT *、分页优化
- 分页：`LIMIT 1000000, 10` 慢，可改 `WHERE id > xxx LIMIT 10`
- **你的项目**：选课历史记录用 (student_id, status) 联合索引

### 31. 主从复制原理？
- binlog（主）→ IO Thread（从）→ relay log → SQL Thread → 重放
- 三种格式：STATEMENT、ROW、MIXED

### 32. 分库分表方案？
- 垂直：按业务拆分
- 水平：按 ID hash / 范围
- 中间件：Sharding-JDBC、MyCat
- **你的项目当前不需要**，但要能讲清楚适用场景

### 33. InnoDB 和 MyISAM 区别？
- InnoDB：事务、行锁、外键、聚簇索引
- MyISAM：表锁、查询快、无事务
- **现在基本都用 InnoDB**

---

## 五、Spring（8 道）

### 34. Spring IoC 原理？
- 控制反转：对象由 Spring 容器创建和管理
- 实现：工厂模式 + 反射 + BeanDefinition
- **生命周期**：实例化 → 属性注入 → 初始化前/后 → 使用 → 销毁

### 35. Spring AOP 原理？
- 面向切面：动态代理（JDK Proxy / CGLIB）
- 5 种通知：@Before、@After、@Around、@AfterReturning、@AfterThrowing
- 应用：日志、事务（@Transactional）、权限

### 36. Spring Bean 作用域？
- singleton（默认）、prototype、request、session、application
- 单例 Bean 注入多例 Bean：用 @Lookup 或 ObjectProvider

### 37. Spring 事务传播机制？
- 7 种：REQUIRED（默认）、REQUIRES_NEW、NESTED、SUPPORTS、NOT_SUPPORTED、MANDATORY、NEVER
- **你的项目**：选课和扣减库存用 REQUIRED，保证同时成功/失败

### 38. @Transactional 失效场景？
- 非 public 方法
- 自调用（同类内 this.xxx()）
- 异常被 catch 吞掉
- 抛出非 RuntimeException（默认只回滚 RuntimeException）
- 没用动态代理（final 类/方法）

### 39. Spring MVC 执行流程？
- DispatcherServlet → HandlerMapping → HandlerAdapter → Controller → ModelAndView → ViewResolver → 渲染
- **简化版**：前端控制器 + 处理器映射 + 适配器 + 视图解析器

### 40. Spring 启动流程？
- 加载配置文件 → 创建 ApplicationContext → 解析 BeanDefinition → BeanFactory 后置处理器 → 实例化 Bean → 注入依赖 → 初始化 → 监听器回调

### 41. Spring Boot 自动装配原理？
- @SpringBootApplication = @SpringBootConfiguration + @EnableAutoConfiguration + @ComponentScan
- @EnableAutoConfiguration → AutoConfigurationImportSelector → META-INF/spring.factories → 条件装配（@Conditional）

---

## 六、Spring Cloud（5 道）

### 42. Eureka 原理？
- AP 模型（可用性 + 分区容错）
- 客户端每 30s 发送心跳，90s 没收到就剔除
- **你的项目**：eureka-server 已搭好

### 43. Ribbon 负载均衡策略？
- 轮询、随机、加权响应时间、最少并发、重试
- **默认轮询**

### 44. Hystrix/Sentinel 熔断降级？
- 熔断：服务异常率超阈值，断路器打开，快速失败
- 降级：服务不可用时返回兜底数据
- **你的项目**：可以加 Resilience4j 演示

### 45. Gateway 过滤器？
- GatewayFilter：路由级
- GlobalFilter：全局
- **执行顺序**：Ordered 接口控制

### 46. 分布式 Session 怎么解决？
- Session 复制（性能差）
- 客户端存储 Cookie（不安全）
- **Redis 集中存储**（推荐）Spring Session
- JWT（无状态，跨服务首选）

---

## 七、Redis（4 道）

### 47. Redis 数据类型？
- String、Hash、List、Set、ZSet、Stream、Bitmap、HyperLogLog、GEO

### 48. Redis 持久化？
- RDB：快照，全量备份
- AOF：日志，增量
- **混合持久化（RDB+AOF）**：Redis 4.0+

### 49. 缓存三大问题？
- **穿透**：查不存在的数据 → 布隆过滤器 / 缓存空值
- **雪崩**：大量 key 同时过期 → 随机过期时间 / 熔断
- **击穿**：热点 key 过期 → 分布式锁 / 永不过期

### 50. Redis 分布式锁？
- SETNX + EXPIRE（原子）
- SET key value NX EX 30（单命令原子）
- **Redisson** 推荐（可重入、看门狗续期）
- **你的项目**：选课抢课可以用分布式锁防超卖

---

## 八、实战场景题（重点准备）

### 51. 设计选课系统怎么防超卖？
1. 课程预扣库存：Redis DECR 原子操作
2. MySQL 行锁：SELECT ... FOR UPDATE
3. 乐观锁：version 字段，CAS 更新
4. 异步队列：用户进入候补，成功后异步通知

### 52. 怎么设计一个高并发短链系统？
1. 发号器：Snowflake ID
2. 短码生成：hash(id) + base62
3. 布隆过滤器：拦截不存在的短码
4. 多级缓存：Redis + JVM Caffeine

### 53. AI Agent 怎么设计工具调用？
1. Tool 接口抽象：name + description + parameters schema
2. Function Calling 协议：LLM 返回工具名和参数
3. 执行器：根据工具名反射调用 Java 方法
4. 结果回传：再调一次 LLM 生成自然语言回复

---

> **使用建议**：
> 1. 每天背 3-5 道，配合简历项目串起来讲
> 2. 重点准备 8 道"项目场景题"，结合你的选课系统讲
> 3. 每个答案练到能流畅说出来，不要只是"知道"
