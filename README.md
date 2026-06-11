# 🎓 网上选课系统 (CourseSelectionSystem)

基于 Spring Cloud 微服务架构的网上选课系统，支持学生、教师、管理员三种角色。

## 技术栈

| 组件 | 技术 |
|------|------|
| 后端 | Java 17 + Spring Boot 2.7.18 |
| 微服务 | Spring Cloud 2021.0.9 + Eureka + Gateway |
| 数据库 | MySQL 8.0+ |
| 数据访问 | MyBatis-Plus + Spring Data JPA |
| 前端 | 纯静态 HTML + CSS + JavaScript (Font Awesome + Chart.js) |
| 构建 | Maven Wrapper |

## 项目结构

```
CourseSelectionSystem/
├── common-lib/          # 公共库（Result、Constants、异常）
├── eureka-server/       # 服务注册中心（端口 8761）
├── gateway-server/      # 统一网关入口（端口 9000）
├── web-service/         # 页面服务 + API（端口 8080）
├── user-service/        # 用户服务（当前未启用）
├── student-service/     # 学生服务（当前未启用）
├── teacher-service/     # 教师服务（当前未启用）
├── course-service/      # 课程服务（当前未启用）
├── selection-service/   # 选课服务（当前未启用）
├── db/                  # 数据库脚本
│   └── init.sql         # 建表 + 初始数据
└── scripts/             # 启动/测试脚本
```

> **答辩演示模式**：所有 API 请求统一由 `web-service` 处理，独立微服务路由已注释，避免分布式 Session 问题。

## 环境要求

- **JDK 17**（已配置 `JAVA_HOME` 环境变量）
- **MySQL 8.0+**（本地运行，端口 3306）
- **Maven**（使用项目自带的 `mvnw.cmd`，无需单独安装）

## 快速启动

### 第一步：初始化数据库

在 MySQL 中执行以下命令：

```sql
CREATE DATABASE IF NOT EXISTS course_selection_system
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

然后运行数据库脚本：

```powershell
mysql -u root -p course_selection_system < db\init.sql
```

### 第二步：启动所有服务

一键启动（会自动编译并依次启动所有服务）：

```powershell
.\scripts\start-microservices.ps1
```

或者按顺序手动启动：

```powershell
# 1. 编译
.\mvnw.cmd -DskipTests package

# 2. 启动 Eureka（端口 8761）
start java -jar eureka-server\target\eureka-server-0.0.1-SNAPSHOT.jar

# 3. 启动 web-service（端口 8080）
start java -jar web-service\target\web-service-0.0.1-SNAPSHOT.jar

# 4. 启动 Gateway（端口 9000）
start java -jar gateway-server\target\gateway-server-0.0.1-SNAPSHOT.jar
```

### 第三步：访问系统

打开浏览器访问：**http://localhost:9000**

## 测试账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | `admin` | `admin123` |
| 学生 | `2024001` | `123456` |
| 教师 | `T001` | `123456` |

### 更多测试账号

**学生：**
| 学号 | 姓名 | 密码 |
|------|------|------|
| 2024001 | 张三 | 123456 |
| 2024002 | 李四 | 123456 |
| 2024003 | 王五 | 123456 |
| 2024004 | 赵六 | 123456 |
| 2024005 | 孙七 | 123456 |
| 2024006 | 周八 | 123456 |

**教师：**
| 工号 | 姓名 | 密码 |
|------|------|------|
| T001 | 张教授 | 123456 |
| T002 | 李教授 | 123456 |
| T003 | 王老师 | 123456 |
| T004 | 刘教授 | 123456 |
| T005 | 陈老师 | 123456 |

## 核心功能

### 管理员端 (`/admin`)
- 📊 **仪表盘**：系统概览统计（学生数、教师数、课程数、选课记录）
- 👥 **用户管理**：学生/教师的增删改查、密码重置
- 📚 **课程管理**：课程的增删改查
- 🏫 **学院专业**：学院、系部、专业管理
- 📋 **选课记录**：查看选课情况
- ⚙️ **系统设置**、操作日志

### 学生端 (`/student`)
- 📝 **选课中心**：浏览可选课程、选课/退课
- 📖 **我的课程**：查看已选课程
- 📅 **课表**：课程表查看
- 📊 **成绩查询**：查看已修课程成绩
- ⭐ **教学评价**：对课程进行评价
- 👤 **个人信息**：查看/修改个人资料

### 教师端 (`/teacher`)
- 📊 **教学统计**：授课课程统计、选课学生统计
- 📚 **课程管理**：查看授课课程、学生名单
- 📝 **成绩管理**：录入/修改学生成绩
- 👥 **学生管理**：查看选课学生信息
- 👤 **个人信息**：查看/修改个人资料

## 注意事项

1. **答辩演示时**：
   - 确保 MySQL 服务已启动
   - 先执行 `db/init.sql` 初始化数据库
   - 用 `.\scripts\start-microservices.ps1` 一键启动
   - 访问 `http://localhost:9000` 进入系统

2. **常见问题**：
   - 如果端口被占用，修改对应模块的 `application.properties`
   - 数据库连接失败 → 检查 MySQL 服务是否启动，配置是否正确
   - 登录后页面数据显示异常 → 检查 `web-service` 日志
