# AGENTS.md

本文件记录当前项目对后续编码 Agent 有用的上下文、约定和注意事项。请在修改代码前先阅读本文件，再结合具体任务阅读相关源码。

## 项目概况

- 项目名称：`CourseSelectionSystem`
- 项目类型：Java Web 应用
- 业务主题：网上选课系统
- 当前包名：`org.example.courseselectionsystem`
- 主要功能域：登录认证、学生管理、教师管理、课程管理、院系/专业管理、选课、成绩、角色与权限。

项目当前既包含后端接口，也包含静态前端页面。前端页面按角色分为管理员、学生、教师三类，后端按 Controller、Service、Mapper、Entity 等层次组织。

## 技术栈

- Java：17，见 `pom.xml` 的 `java.version`
- 构建工具：Maven Wrapper，根目录包含 `mvnw`、`mvnw.cmd`
- Spring Boot：`4.0.0`
- Web 框架：Spring WebMVC
- 数据库：MySQL，连接配置在 `src/main/resources/application.properties`
- 数据访问：
  - 代码中使用 MyBatis-Plus 的 `BaseMapper`、分页拦截器、`QueryWrapper`
  - 代码中也存在 Spring Data JPA 的 `Repository` 与 JPA 注解实体
- 模板/静态资源：
  - `templates/login.html` 用于登录视图
  - `static/` 下放置管理员、学生、教师页面，以及公共 CSS/JS
- Lombok：实体、VO、Result 等类使用 `@Data` 等注解
- 测试：JUnit + Spring Boot Test，目前只有启动上下文测试

## 重要目录

```text
src/main/java/org/example/courseselectionsystem/
  CourseSelectionSystemApplication.java  # Spring Boot 启动入口
  common/                                # 通用常量、统一响应结果
  config/                                # Web、Security、MyBatis-Plus 配置
  controller/                            # 页面控制器与 REST API 控制器
  entity/                                # JPA 风格实体类
  exception/                             # 业务异常与异常处理
  handler/                               # 另一套全局异常处理
  interceptor/                           # 登录拦截器
  mapper/                                # MyBatis-Plus Mapper 接口
  repository/                            # Spring Data JPA Repository
  service/                               # Service 接口
  service/impl/                          # Service 实现
  util/                                  # 工具类
  vo/                                    # 请求/响应 VO

src/main/resources/
  application.properties                 # 应用、数据库、MyBatis、日志、Thymeleaf 配置
  mapper/                                # MyBatis XML 映射文件
  static/                                # 静态页面、CSS、JS
  templates/                             # Thymeleaf 模板
```

## 运行与配置

常用命令：

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

默认配置要点：

- 应用名：`CourseSelectionSystem`
- 数据库：`course_selection_system`
- 数据库用户：`root`
- 数据库密码：`root`
- 服务端口：`8080`
- Context path：`/`
- MyBatis Mapper XML 路径：`classpath:mapper/*.xml`
- MyBatis 下划线转驼峰：已开启
- Thymeleaf：启用，模板路径 `classpath:/templates/`

注意：`application.properties` 中有若干中文注释显示为乱码，并且部分注释与配置疑似粘连，例如数据库 URL、`server.port` 附近。修改运行配置前应实际打开并核对这些行。

## 主要后端模块

### 页面控制器

- `LoginController`：登录页、登录认证、退出登录，路径前缀 `/login`
- `AdminController`：管理员页面和部分管理接口，路径前缀 `/admin`
- `StudentController`：学生主页、个人信息、学生 CRUD 等，路径前缀 `/student`
- `TeacherController`：教师主页、个人信息、教师 CRUD 等，路径前缀 `/teacher`

### REST API 控制器

REST API 多数采用 `/api/v1/...` 前缀：

- `UserController`：用户登录、注册、查询、状态、删除等
- `RoleController`：角色管理
- `PermissionController`：权限管理
- `CollegeController`：学院管理
- `DepartmentController`：系部管理
- `MajorController`：专业管理
- `CourseController`：课程管理、课程搜索、热门课程等
- `CourseSelectionController`、`SelectionController`：选课、退课、批量选课、选课查询、学分统计、课程人数统计等

### Service 与 Mapper

业务层通常是：

```text
Controller -> Service 接口 -> service/impl 实现 -> Mapper/Repository -> 数据库
```

Mapper 位于 `mapper/`，大多继承 MyBatis-Plus `BaseMapper<T>`，并定义部分自定义查询方法。对应 XML 位于 `src/main/resources/mapper/`，目前看到 `StudentMapper.xml`、`TeacherMapper.xml`。

Repository 位于 `repository/`，使用 Spring Data JPA。例如 `UserRepository`、`RoleRepository`、`PermissionRepository` 等。

## 主要实体

- `User`：系统用户，表 `sys_user`
- `Student`：学生，表 `student`
- `Teacher`：教师
- `Course`：课程，表 `course`
- `CourseSelection`：选课记录，表 `course_selection`
- `College`、`Department`、`Major`：学院、系部、专业
- `Role`、`Permission`：角色与权限，表名分别类似 `sys_role`、`sys_permission`

实体类使用 Lombok，同时混用了 JPA 注解、乐观锁版本字段、创建/更新时间回调等写法。部分业务代码也直接访问字段或使用 getter/setter，修改实体可见性时要谨慎。

## 登录与权限

当前存在两套相关机制：

- `SecurityConfig` 使用 Spring Security 配置登录页 `/login`、放行静态资源和登录注册路径、要求 `/api/**` 认证，并暂时禁用 CSRF。
- `LoginInterceptor` 使用 Session 检查 `username`、`role`，并按角色限制访问 `/student/`、`/teacher/`、`/user/` 等路径。

登录成功后，`LoginController` 会将以下信息写入 Session：

- `username`
- `role`
- `userId`
- `user`

角色字符串主要包括：

- `student`
- `teacher`
- `admin`

管理员登录在 `UserServiceImpl.login(String username, String password, String role)` 中有硬编码逻辑：用户名 `admin`，密码 `admin123`。

## 前端页面

静态页面位于 `src/main/resources/static/`：

- `admin/`：管理员端，如学生管理、教师管理、课程管理、成绩管理、系统日志、系统设置
- `student/`：学生端，如选课、我的课程、课表、成绩、评价、消息、设置
- `teacher/`：教师端，如课程管理、学生管理、成绩管理、教学统计、个人信息
- `css/common.css`：公共样式
- `js/common.js`：公共脚本
- `login.html`：静态登录页

同时 `src/main/resources/templates/login.html` 也存在登录模板。处理登录页时要确认实际返回的是 Thymeleaf 模板还是静态页面。

## 已知风险与维护注意事项

- `pom.xml` 当前声明的依赖可能不足。源码中使用了 MyBatis-Plus、Spring Security、Spring Data JPA、Thymeleaf、`javax.servlet` 等，但 `pom.xml` 里目前主要只有 WebMVC、MySQL、Lombok、DevTools 和测试依赖。运行或编译前需要核对并补齐依赖。
- Spring Boot 版本为 `4.0.0`，但代码中使用 `javax.servlet`、`WebSecurityConfigurerAdapter`、`antMatchers` 等旧式 API。若保持 Boot 4，需要评估 Jakarta 包名和 Spring Security 新 API 的兼容性；若不升级 API，可能需要降级 Spring Boot/Spring Security 版本。
- 多个 Java 文件里的中文注释和字符串显示为乱码，可能是历史编码不一致造成的。修改前先确认文件真实编码，避免继续扩大乱码。
- 项目同时存在 `common.Result`、`vo.Result`、`util.Result`，统一响应类型可能重复。新增接口时优先沿用当前控制器已经使用的同一个 Result 类型，不要随意混用。
- 全局异常处理存在 `exception/GlobalExceptionHandler.java` 和 `handler/GlobalExceptionHandler.java` 两份，新增异常处理逻辑前先确认实际生效和职责边界。
- `UserServiceImpl` 中有不少 TODO 或临时逻辑，例如临时 token、跳过密码校验、注册/分页/密码修改尚未实现。涉及认证或用户功能时要格外小心。
- `CourseSelectionController` 中疑似存在重复的 `@DeleteMapping("/batch")` 方法定义。调整选课 API 前应先检查映射冲突。
- 当前不是 Git 仓库，无法依赖 git diff 追踪改动。修改前后请自行记录涉及文件。

## 编码约定建议

- 使用中文写业务注释和文档，但代码标识符保持英文。
- 新增后端功能时优先遵循现有分层：Controller、Service、ServiceImpl、Mapper、Entity/VO。
- 新增接口路径优先沿用 `/api/v1/...` 风格；页面跳转控制器沿用角色路径前缀。
- 统一返回结果时先查看当前控制器使用的是 `common.Result`、`vo.Result` 还是 `util.Result`，保持局部一致。
- 数据库字段命名倾向下划线，Java 字段使用驼峰，MyBatis 已开启下划线转驼峰。
- 涉及登录、角色、权限、Session 的改动，需要同时检查 Spring Security 配置、`LoginInterceptor` 和前端页面跳转。

## 后续 Agent 开始任务前的建议检查清单

1. 先运行 `rg --files` 或查看目标模块目录，确认相关文件。
2. 如果任务涉及构建或运行，先检查 `pom.xml` 依赖是否满足当前源码。
3. 如果任务涉及中文注释或配置，先确认编码与乱码现状。
4. 如果任务涉及认证/权限，先理清 Spring Security 与 Session 拦截器的关系。
5. 修改完成后尽量运行 `.\mvnw.cmd test`；若无法通过，记录具体失败原因。

