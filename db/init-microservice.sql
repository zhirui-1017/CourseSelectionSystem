-- ============================================================
-- 网上选课系统 - 微服务版数据库初始化脚本
-- 数据库：course_selection_system_cloud
-- 编码：UTF-8
-- 说明：
--   1. 专为 Spring Cloud 微服务架构设计
--   2. 表按微服务模块分组（user-service / student-service / teacher-service / course-service / selection-service）
--   3. 仅包含前端实际 CRUD 需要的表和字段
--   4. 不含未在页面中使用的冗余字段
--   5. 先创建数据库再执行本脚本
--
--   CREATE DATABASE IF NOT EXISTS course_selection_system_cloud
--     DEFAULT CHARACTER SET utf8mb4
--     DEFAULT COLLATE utf8mb4_unicode_ci;
--   USE course_selection_system_cloud;
--   SOURCE db/init-microservice.sql;
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 模块 A：user-service（用户认证、角色、权限、管理员）
-- 所属微服务：user-service（端口 8101）
-- ============================================================

-- A1. 系统用户表（统一登录认证）
-- 前端使用页面：login.html（登录）、admin/index.html（用户管理子模块）
-- CRUD：增（注册/添加用户）、查（登录/用户列表）、改（编辑/重置密码）、删（删除用户）
CREATE TABLE IF NOT EXISTS sys_user (
    id              BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    username        VARCHAR(50)  NOT NULL                 COMMENT '用户名（登录用）',
    password        VARCHAR(255) NOT NULL                 COMMENT '密码（BCrypt加密）',
    real_name       VARCHAR(50)  DEFAULT NULL             COMMENT '真实姓名',
    user_type       TINYINT      DEFAULT NULL             COMMENT '用户类型：1-学生，2-教师，3-管理员',
    user_code       VARCHAR(30)  DEFAULT NULL             COMMENT '学号/工号',
    gender          TINYINT      DEFAULT NULL             COMMENT '性别：1-男，2-女',
    email           VARCHAR(100) DEFAULT NULL             COMMENT '电子邮箱',
    phone           VARCHAR(20)  DEFAULT NULL             COMMENT '手机号码',
    avatar          VARCHAR(255) DEFAULT NULL             COMMENT '头像URL',
    status          TINYINT      DEFAULT 1                COMMENT '状态：1-启用，0-停用',
    department_id   BIGINT       DEFAULT NULL             COMMENT '所属系部ID',
    major_id        BIGINT       DEFAULT NULL             COMMENT '所属专业ID',
    class_name      VARCHAR(50)  DEFAULT NULL             COMMENT '班级名称',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_user_code (user_code),
    KEY idx_user_type (user_type),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表（统一登录认证）';

-- A2. 角色表
-- 前端使用页面：admin/role-management.html
-- CRUD：增、查（列表+搜索）、改、删
-- 表格列：角色名称、角色编码、描述、状态、操作
-- 表单字段：roleName、roleCode、roleDesc、roleStatus
CREATE TABLE IF NOT EXISTS sys_role (
    id              BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    role_name       VARCHAR(50)  NOT NULL                 COMMENT '角色名称（如：管理员、教师、学生）',
    role_code       VARCHAR(50)  NOT NULL                 COMMENT '角色编码（如：ROLE_ADMIN、ROLE_TEACHER、ROLE_STUDENT）',
    description     VARCHAR(255) DEFAULT NULL             COMMENT '角色描述',
    status          TINYINT      DEFAULT 1                COMMENT '状态：1-启用，0-停用',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code),
    UNIQUE KEY uk_role_name (role_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- A3. 权限表
-- 前端使用页面：admin/permission-management.html
-- CRUD：增、查（列表+搜索）、改、删
-- 表格列：权限名称、权限编码、URL、请求方法、类型、状态、操作
-- 表单字段：permName、permCode、permUrl、permMethod(GET/POST/PUT/DELETE)、permType(1菜单/2按钮/3API)、permSort、permStatus
CREATE TABLE IF NOT EXISTS sys_permission (
    id                BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    permission_name   VARCHAR(50)  NOT NULL                 COMMENT '权限名称',
    permission_code   VARCHAR(100) NOT NULL                 COMMENT '权限编码（唯一）',
    url               VARCHAR(255) DEFAULT NULL             COMMENT '请求URL',
    method            VARCHAR(10)  DEFAULT NULL             COMMENT '请求方法：GET/POST/PUT/DELETE',
    permission_type   TINYINT      DEFAULT 3                COMMENT '权限类型：1-菜单，2-按钮，3-API',
    parent_id         BIGINT       DEFAULT NULL             COMMENT '父权限ID',
    icon              VARCHAR(100) DEFAULT NULL             COMMENT '图标',
    sort              INT          DEFAULT 0                COMMENT '排序号',
    status            TINYINT      DEFAULT 1                COMMENT '状态：1-启用，0-停用',
    create_time       DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_permission_code (permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- A4. 用户-角色关联表
-- 前端使用页面：admin/role-management.html（角色分配用户）
CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id     BIGINT NOT NULL COMMENT '用户ID',
    role_id     BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (user_id, role_id),
    KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-角色关联表';

-- A5. 角色-权限关联表
-- 前端使用页面：admin/permission-management.html（权限分配给角色）
CREATE TABLE IF NOT EXISTS sys_role_permission (
    role_id         BIGINT NOT NULL COMMENT '角色ID',
    permission_id   BIGINT NOT NULL COMMENT '权限ID',
    PRIMARY KEY (role_id, permission_id),
    KEY idx_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色-权限关联表';

-- A6. 管理员表
-- 前端使用页面：admin/admin-management.html
-- CRUD：增、查（列表+搜索）、改、删
-- 表格列：用户名、姓名、邮箱、电话、状态、操作
-- 表单字段：adminUsername、adminPassword、adminRealName、adminEmail、adminPhone、adminStatus
-- 注意：管理员有独立的登录入口和管理页面，与 sys_user 分离
CREATE TABLE IF NOT EXISTS admin (
    id              BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    username        VARCHAR(50)  NOT NULL                 COMMENT '用户名',
    password        VARCHAR(255) NOT NULL                 COMMENT '密码（BCrypt加密）',
    real_name       VARCHAR(50)  DEFAULT NULL             COMMENT '真实姓名',
    email           VARCHAR(100) DEFAULT NULL             COMMENT '电子邮箱',
    phone           VARCHAR(20)  DEFAULT NULL             COMMENT '手机号码',
    role            VARCHAR(20)  DEFAULT 'admin'          COMMENT '角色标识',
    status          TINYINT      DEFAULT 1                COMMENT '状态：1-启用，0-停用',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员表';


-- ============================================================
-- 模块 B：student-service（学生信息）
-- 所属微服务：student-service（端口 8102）
-- ============================================================

-- B1. 学生表
-- 前端使用页面：admin/student-management.html、student/profile.html、student/course-selection.html
-- CRUD：增、查（列表+搜索+筛选+分页）、改、删、重置密码
-- 管理端表格列：学号、姓名、性别、学院、专业、班级、状态、入学时间、操作
-- 管理端表单字段：studentId(学号)、studentName(姓名)、studentGender(男/女)、studentCollege(学院ID)、
--               studentMajor(专业ID)、studentClass(班级ID)、enrollmentDate(入学时间)、
--               studentStatus(1在读/0休学/2毕业)、studentEmail、studentPhone
-- 学生端个人资料：idNo(学号只读)、name、gender、phone、email
CREATE TABLE IF NOT EXISTS student (
    id              BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    student_no      VARCHAR(30)  NOT NULL                 COMMENT '学号',
    name            VARCHAR(50)  NOT NULL                 COMMENT '姓名',
    gender          VARCHAR(10)  DEFAULT NULL             COMMENT '性别：男/女',
    phone           VARCHAR(20)  DEFAULT NULL             COMMENT '手机号码',
    email           VARCHAR(100) DEFAULT NULL             COMMENT '电子邮箱',
    password        VARCHAR(255) NOT NULL                 COMMENT '密码（BCrypt加密）',
    college_id      BIGINT       DEFAULT NULL             COMMENT '所属学院ID',
    major_id        BIGINT       DEFAULT NULL             COMMENT '所属专业ID',
    class_id        BIGINT       DEFAULT NULL             COMMENT '所属班级ID',
    enrollment_date DATE         DEFAULT NULL             COMMENT '入学时间',
    status          TINYINT      DEFAULT 1                COMMENT '状态：1-在读，0-休学，2-毕业',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_student_no (student_no),
    KEY idx_college_id (college_id),
    KEY idx_major_id (major_id),
    KEY idx_class_id (class_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生表';


-- ============================================================
-- 模块 C：teacher-service（教师信息）
-- 所属微服务：teacher-service（端口 8103）
-- ============================================================

-- C1. 教师表
-- 前端使用页面：admin/teacher-management.html、teacher/personal-info.html
-- CRUD：增、查（列表+搜索+筛选+分页）、改、删、重置密码
-- 管理端表格列：工号、姓名、性别、学院、职称、联系电话、电子邮箱、入职时间、操作
-- 管理端表单字段：teacherId(工号)、teacherName(姓名)、teacherGender(男/女)、teacherCollege(学院ID)、
--               teacherTitle(职称)、hireDate(入职时间)、teacherEmail、teacherPhone、teacherOffice(办公地点)
-- 教师端个人资料：idNo(工号只读)、name、gender、title、phone、email
CREATE TABLE IF NOT EXISTS teacher (
    id              BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    teacher_no      VARCHAR(30)  NOT NULL                 COMMENT '工号',
    name            VARCHAR(50)  NOT NULL                 COMMENT '姓名',
    gender          VARCHAR(10)  DEFAULT NULL             COMMENT '性别：男/女',
    phone           VARCHAR(20)  DEFAULT NULL             COMMENT '联系电话',
    email           VARCHAR(100) DEFAULT NULL             COMMENT '电子邮箱',
    password        VARCHAR(255) NOT NULL                 COMMENT '密码（BCrypt加密）',
    title           VARCHAR(50)  DEFAULT NULL             COMMENT '职称：教授/副教授/讲师/助教',
    college_id      BIGINT       DEFAULT NULL             COMMENT '所属学院ID',
    department_id   BIGINT       DEFAULT NULL             COMMENT '所属系部ID',
    office          VARCHAR(100) DEFAULT NULL             COMMENT '办公地点',
    hire_date       DATE         DEFAULT NULL             COMMENT '入职时间',
    status          TINYINT      DEFAULT 1                COMMENT '状态：1-启用，0-停用',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_teacher_no (teacher_no),
    KEY idx_college_id (college_id),
    KEY idx_department_id (department_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教师表';


-- ============================================================
-- 模块 D：course-service（学院、系部、专业、课程、班级、公告）
-- 所属微服务：course-service（端口 8104）
-- ============================================================

-- D1. 学院表
-- 前端使用页面：admin/college-management.html、admin/student-management.html（筛选下拉）、
--              admin/teacher-management.html（筛选下拉）、admin/course-management.html
-- CRUD：增、查（列表+搜索+筛选）、改、删
-- 表格列：编号、学院名称、学院代码、描述、状态、操作
-- 表单字段：collegeName、collegeCode、collegeDesc、collegeStatus(1启用/0停用)
CREATE TABLE IF NOT EXISTS college (
    id              BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    college_name    VARCHAR(100) NOT NULL                 COMMENT '学院名称',
    college_code    VARCHAR(30)  NOT NULL                 COMMENT '学院代码',
    description     VARCHAR(255) DEFAULT NULL             COMMENT '描述',
    status          TINYINT      DEFAULT 1                COMMENT '状态：1-启用，0-停用',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_college_code (college_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学院表';

-- D2. 系部表
-- 前端使用页面：admin/department-management.html
-- CRUD：增、查（列表+搜索+筛选）、改、删
-- 表格列：系部编号、系部名称、所属学院、描述、状态、操作
-- 表单字段：deptName、deptCode、deptCollege(所属学院ID)、deptDesc、deptStatus
CREATE TABLE IF NOT EXISTS department (
    id                BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    department_code   VARCHAR(30)  NOT NULL                 COMMENT '系部编号',
    department_name   VARCHAR(100) NOT NULL                 COMMENT '系部名称',
    college_id        BIGINT       NOT NULL                 COMMENT '所属学院ID',
    description       VARCHAR(255) DEFAULT NULL             COMMENT '描述',
    status            TINYINT      DEFAULT 1                COMMENT '状态：1-启用，0-停用',
    create_time       DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dept_code (department_code),
    KEY idx_college_id (college_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系部表';

-- D3. 专业表
-- 前端使用页面：admin/major-management.html
-- CRUD：增、查（列表+搜索+筛选）、改、删
-- 表格列：专业编号、专业名称、所属系部、描述、状态、操作
-- 表单字段：majorName、majorCode、majorDept(所属系部ID)、majorDesc、majorStatus
CREATE TABLE IF NOT EXISTS major (
    id              BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    major_code      VARCHAR(30)  NOT NULL                 COMMENT '专业编号',
    major_name      VARCHAR(100) NOT NULL                 COMMENT '专业名称',
    department_id   BIGINT       NOT NULL                 COMMENT '所属系部ID',
    description     VARCHAR(255) DEFAULT NULL             COMMENT '描述',
    status          TINYINT      DEFAULT 1                COMMENT '状态：1-启用，0-停用',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_major_code (major_code),
    KEY idx_department_id (department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='专业表';

-- D4. 课程表
-- 前端使用页面：admin/course-management.html、student/course-selection.html、student/my-courses.html、
--              student/schedule.html、teacher/course-management.html
-- CRUD：增、查（列表+搜索+筛选+分页+卡片展示）、改、删、批量删除、导入/导出
-- 管理端卡片展示：课程编号、课程名称、课程类型、学分、教师、时间、地点、学期、选课进度、状态
-- 管理端表单字段：courseId(courseCode)、courseName、courseTeacher(teacherId)、courseCredits(credit)、
--               courseCategory(courseType)、courseTotal(availableSlots)、courseSemester(semester)、
--               courseTime(schedule)、courseLocation(classroom)、courseStatus、courseDescription
-- API POST字段：courseCode、courseName、teacherId、credit、totalHours、availableSlots、courseType、
--              semester、schedule、classroom、description、status
-- 学生端卡片展示：课程编号、课程名称、类型、学分、教师、时间、地点、学期、容量进度、描述
CREATE TABLE IF NOT EXISTS course (
    id              BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    course_code     VARCHAR(30)  NOT NULL                 COMMENT '课程编号',
    course_name     VARCHAR(100) NOT NULL                 COMMENT '课程名称',
    course_type     VARCHAR(50)  DEFAULT NULL             COMMENT '课程类型：必修课/选修课/专业基础课/专业课',
    credit          DECIMAL(3,1) DEFAULT NULL             COMMENT '学分（0.5~5）',
    total_hours     INT          DEFAULT NULL             COMMENT '总学时',
    teacher_id      BIGINT       DEFAULT NULL             COMMENT '授课教师ID',
    semester        VARCHAR(30)  DEFAULT NULL             COMMENT '开设学期（如：2024春季/2024秋季）',
    schedule        VARCHAR(100) DEFAULT NULL             COMMENT '上课时间（如：周一 1-2节）',
    classroom       VARCHAR(100) DEFAULT NULL             COMMENT '上课地点',
    available_slots INT          DEFAULT 0                COMMENT '总容量（选课人数上限）',
    selected_count  INT          DEFAULT 0                COMMENT '已选人数',
    status          TINYINT      DEFAULT 1                COMMENT '课程状态：1-正常招生，2-已满，0-已关闭',
    description     TEXT         DEFAULT NULL             COMMENT '课程描述',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_course_code (course_code),
    KEY idx_teacher_id (teacher_id),
    KEY idx_semester (semester),
    KEY idx_course_type (course_type),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程表';

-- D5. 班级表
-- 前端使用页面：admin/class-management.html
-- CRUD：增、查（列表+搜索+筛选+分页）、改、删、批量删除、导入/导出
-- 表格列：班级编号、班级名称、所属学院、专业、年级、班主任、学生人数、班长、状态、操作
-- 表单字段：classId(班级编号)、className(班级名称)、department(所属学院ID)、major(专业ID)、
--          grade(年级)、headTeacher(班主任)、totalStudents(学生容量)、monitor(班长)、
--          contactPhone(联系电话)、createTime、status(normal/graduated/closed)
-- 注意：此表在原单体中存在，但前端有独立管理页面，故保留
CREATE TABLE IF NOT EXISTS class_info (
    id              BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    class_code      VARCHAR(30)  NOT NULL                 COMMENT '班级编号',
    class_name      VARCHAR(100) NOT NULL                 COMMENT '班级名称',
    college_id      BIGINT       DEFAULT NULL             COMMENT '所属学院ID',
    major_id        BIGINT       DEFAULT NULL             COMMENT '所属专业ID',
    grade           VARCHAR(20)  DEFAULT NULL             COMMENT '年级（如：2023级）',
    head_teacher    VARCHAR(50)  DEFAULT NULL             COMMENT '班主任姓名',
    monitor_id      BIGINT       DEFAULT NULL             COMMENT '班长（学生ID）',
    contact_phone   VARCHAR(20)  DEFAULT NULL             COMMENT '联系电话',
    total_students  INT          DEFAULT 0                COMMENT '学生容量',
    status          VARCHAR(20)  DEFAULT 'normal'         COMMENT '状态：normal-正常，graduated-已毕业，closed-已关闭',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_class_code (class_code),
    KEY idx_college_id (college_id),
    KEY idx_major_id (major_id),
    KEY idx_grade (grade)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级表';

-- D6. 公告表
-- 前端使用页面：admin/announcement-management.html
-- CRUD：增、查（列表+搜索+筛选）、改、删
-- 表格列：标题、关联课程、发布人、发布时间、操作
-- 表单字段：announcementTitle(标题)、announcementCourse(关联课程ID)、announcementContent(内容)
CREATE TABLE IF NOT EXISTS announcement (
    id              BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    title           VARCHAR(200) NOT NULL                 COMMENT '公告标题',
    course_id       BIGINT       DEFAULT NULL             COMMENT '关联课程ID',
    content         TEXT         NOT NULL                 COMMENT '公告内容',
    publisher_id    BIGINT       DEFAULT NULL             COMMENT '发布人ID',
    publish_time    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_course_id (course_id),
    KEY idx_publish_time (publish_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告表';


-- ============================================================
-- 模块 E：selection-service（选课、成绩、评价、学期、系统配置）
-- 所属微服务：selection-service（端口 8105）
-- ============================================================

-- E1. 选课记录表（同时承载成绩信息）
-- 前端使用页面：
--   admin/grade-management.html（成绩管理）、student/course-selection.html（选课）、
--   student/my-courses.html（我的课程）、student/schedule.html（课表）、
--   student/grades.html（成绩查询）、teacher/grade-management.html（教师成绩管理）
-- CRUD：增（选课/录入成绩）、查（列表+搜索+筛选+统计）、改（编辑成绩/退课）、删（退课）
-- 管理端成绩页表格列：学号、姓名、班级、课程编号、课程名称、学分、学期、成绩、状态、操作
-- 管理端录入成绩表单：studentId、courseId、semester、score(0-100)、teacherComment
-- 教师端成绩管理：平时成绩(40%)、实验成绩(20%)、考试成绩(40%)、总评、等级、备注
-- 学生端选课：选课操作（新增记录）
-- 学生端成绩：课程名、学分、成绩、等级、绩点、评语
-- 注意：score 为总评成绩，daily_grade/lab_grade/exam_grade 为教师端的分项成绩
CREATE TABLE IF NOT EXISTS course_selection (
    id                BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    student_id        BIGINT       NOT NULL                 COMMENT '学生ID',
    course_id         BIGINT       NOT NULL                 COMMENT '课程ID',
    semester          VARCHAR(30)  DEFAULT NULL             COMMENT '学期',
    status            TINYINT      DEFAULT 0                COMMENT '选课状态：0-已选课，1-已退课，2-已完成',
    -- 成绩相关字段（管理员/教师录入）
    score             DECIMAL(5,1) DEFAULT NULL             COMMENT '总评成绩（0~100）',
    score_level       VARCHAR(10)  DEFAULT NULL             COMMENT '等级：优秀/良好/中等/及格/不及格',
    gpa               DECIMAL(3,1) DEFAULT NULL             COMMENT '绩点（0.0~5.0）',
    -- 教师端分项成绩（平时40% + 实验20% + 考试40%）
    daily_grade       DECIMAL(5,1) DEFAULT NULL             COMMENT '平时成绩（满分100）',
    lab_grade         DECIMAL(5,1) DEFAULT NULL             COMMENT '实验成绩（满分100）',
    exam_grade        DECIMAL(5,1) DEFAULT NULL             COMMENT '考试成绩（满分100）',
    remark            VARCHAR(500) DEFAULT NULL             COMMENT '教师评语/备注',
    -- 时间记录
    selection_time    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '选课时间',
    drop_time         DATETIME     DEFAULT NULL             COMMENT '退课时间',
    create_time       DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_student_course (student_id, course_id, semester),
    KEY idx_student_id (student_id),
    KEY idx_course_id (course_id),
    KEY idx_semester (semester),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='选课记录表（含成绩）';

-- E2. 课程评价表
-- 前端使用页面：student/evaluations.html
-- CRUD：增（提交评价）、查（已评价列表+筛选）
-- 统计卡片：待评价数、已评价数
-- 卡片展示：课程名称、课程编号、教师、评价状态、评分(星级)、评价内容、日期
-- 评价表单：评分（星级1-5）、评价内容文本
CREATE TABLE IF NOT EXISTS course_evaluation (
    id              BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    student_id      BIGINT       NOT NULL                 COMMENT '学生ID',
    course_id       BIGINT       NOT NULL                 COMMENT '课程ID',
    semester        VARCHAR(30)  DEFAULT NULL             COMMENT '学期',
    rating          TINYINT      DEFAULT 5                COMMENT '评分（1~5星）',
    comment         TEXT         DEFAULT NULL             COMMENT '评价内容',
    evaluation_date DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '评价日期',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_student_course_semester (student_id, course_id, semester),
    KEY idx_course_id (course_id),
    KEY idx_rating (rating)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程评价表';

-- E3. 学期表
-- 前端使用页面：admin/semester-management.html
-- CRUD：增、查（列表+搜索+筛选）、改、删
-- 表格列：学期标识、学期名称、开始日期、结束日期、当前学期、状态、操作
-- 表单字段：semesterId(学期标识)、semesterName(学期名称)、semesterStart(开始日期)、
--          semesterEnd(结束日期)、semesterIsCurrent(true/false)、semesterStatus(1启用/0停用)
CREATE TABLE IF NOT EXISTS semester (
    id              BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    semester_id     VARCHAR(30)  NOT NULL                 COMMENT '学期标识（如：2024-2025-1）',
    semester_name   VARCHAR(100) NOT NULL                 COMMENT '学期名称（如：2024-2025学年第一学期）',
    start_date      DATE         NOT NULL                 COMMENT '开始日期',
    end_date        DATE         NOT NULL                 COMMENT '结束日期',
    is_current      TINYINT      DEFAULT 0                COMMENT '是否为当前学期：1-是，0-否',
    status          TINYINT      DEFAULT 1                COMMENT '状态：1-启用，0-停用',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_semester_id (semester_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学期表';


-- ============================================================
-- 模块 F：通用/系统级（归属 web-service 或 user-service）
-- ============================================================

-- F1. 系统配置表
-- 前端使用页面：admin/system-settings.html
-- CRUD：查（读取设置）、改（保存设置）
-- 设置项：systemName(系统名称)、systemVersion(版本只读)、contactEmail(联系邮箱)、
--         loginTimeout(30min-24h)、maxUploadSize(5MB-500MB)、
--         systemStatus(open选课开放/closed选课关闭)、gradeEntryStatus(成绩录入开放/关闭)、
--         maxCourseSelection(每学期最大选课数量1-20)、systemNotice(系统通知公告)
CREATE TABLE IF NOT EXISTS system_config (
    id              BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    config_key      VARCHAR(100) NOT NULL                 COMMENT '配置键',
    config_value    TEXT         DEFAULT NULL             COMMENT '配置值',
    description     VARCHAR(255) DEFAULT NULL             COMMENT '配置说明',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- F2. 系统日志表
-- 前端使用页面：admin/system-logs.html
-- CRUD：查（列表+搜索+筛选+分页+统计）、删（清空）、导出
-- 统计卡片：日志总数、成功操作、警告日志、错误日志
-- 筛选条件：logLevel(info/warning/error/success)、logType(auth/operation/system/data)、
--          logUser(用户名)、dateRange(日期范围)、logKeyword(关键词)
-- 表格列：ID、操作时间、级别、类型、操作、IP地址、操作内容、操作
CREATE TABLE IF NOT EXISTS system_log (
    id              BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    operation_time  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    level           VARCHAR(20)  DEFAULT 'info'           COMMENT '日志级别：info/warning/error/success',
    type            VARCHAR(30)  DEFAULT NULL             COMMENT '日志类型：auth/operation/system/data',
    username        VARCHAR(50)  DEFAULT NULL             COMMENT '操作用户名',
    ip_address      VARCHAR(50)  DEFAULT NULL             COMMENT 'IP地址',
    operation       VARCHAR(200) DEFAULT NULL             COMMENT '操作描述',
    content         TEXT         DEFAULT NULL             COMMENT '操作内容详情',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_operation_time (operation_time),
    KEY idx_level (level),
    KEY idx_type (type),
    KEY idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统日志表';


-- ============================================================
-- 数据初始化
-- ============================================================

-- 清空旧数据（按外键依赖顺序）
TRUNCATE TABLE course_evaluation;
TRUNCATE TABLE course_selection;
TRUNCATE TABLE announcement;
TRUNCATE TABLE course;
TRUNCATE TABLE class_info;
TRUNCATE TABLE major;
TRUNCATE TABLE department;
TRUNCATE TABLE college;
TRUNCATE TABLE teacher;
TRUNCATE TABLE student;
TRUNCATE TABLE admin;
TRUNCATE TABLE sys_role_permission;
TRUNCATE TABLE sys_user_role;
TRUNCATE TABLE sys_permission;
TRUNCATE TABLE sys_role;
TRUNCATE TABLE sys_user;
TRUNCATE TABLE system_config;
TRUNCATE TABLE system_log;
TRUNCATE TABLE semester;

-- ============================================================
-- 1. 学院 (college) — course-service
-- ============================================================
INSERT INTO college (id, college_name, college_code, description, status) VALUES
(1, '计算机与信息技术学院', 'CS', '计算机科学与技术、软件工程等相关专业', 1),
(2, '数学与统计学院', 'MATH', '数学与应用数学、统计学等相关专业', 1),
(3, '外国语学院', 'FL', '英语、日语、翻译等相关专业', 1),
(4, '经济管理学院', 'EM', '经济学、管理学、工商管理等相关专业', 1);

-- ============================================================
-- 2. 系部 (department) — course-service
-- ============================================================
INSERT INTO department (id, department_code, department_name, college_id, description, status) VALUES
(1, 'CS01', '计算机科学系', 1, '计算机科学与技术专业', 1),
(2, 'CS02', '软件工程系', 1, '软件工程专业', 1),
(3, 'CS03', '网络工程系', 1, '网络工程、信息安全专业', 1),
(4, 'MATH01', '数学系', 2, '数学与应用数学专业', 1),
(5, 'MATH02', '统计系', 2, '统计学专业', 1),
(6, 'FL01', '英语系', 3, '英语专业', 1),
(7, 'EM01', '管理系', 4, '工商管理、市场营销专业', 1);

-- ============================================================
-- 3. 专业 (major) — course-service
-- ============================================================
INSERT INTO major (id, major_code, major_name, department_id, description, status) VALUES
(1, 'CS0101', '计算机科学与技术', 1, '计算机科学与技术本科专业', 1),
(2, 'CS0201', '软件工程', 2, '软件工程本科专业', 1),
(3, 'CS0301', '网络工程', 3, '网络工程本科专业', 1),
(4, 'MATH0101', '数学与应用数学', 4, '数学与应用数学本科专业', 1),
(5, 'MATH0201', '统计学', 5, '统计学本科专业', 1),
(6, 'FL0101', '英语', 6, '英语本科专业', 1),
(7, 'EM0101', '工商管理', 7, '工商管理本科专业', 1);

-- ============================================================
-- 4. 班级 (class_info) — course-service
-- ============================================================
INSERT INTO class_info (id, class_code, class_name, college_id, major_id, grade, head_teacher, total_students, status) VALUES
(1, 'CS2101', '计科2101班', 1, 1, '2021级', '张教授', 45, 'normal'),
(2, 'CS2102', '计科2102班', 1, 1, '2021级', '李教授', 45, 'normal'),
(3, 'SE2101', '软工2101班', 1, 2, '2021级', '王老师', 40, 'normal'),
(4, 'NW2101', '网工2101班', 1, 3, '2021级', '刘教授', 40, 'normal'),
(5, 'MA2101', '数学2101班', 2, 4, '2021级', '陈老师', 35, 'normal'),
(6, 'ST2101', '统计2101班', 2, 5, '2021级', '赵老师', 35, 'normal');

-- ============================================================
-- 5. 学期 (semester) — selection-service
-- ============================================================
INSERT INTO semester (id, semester_id, semester_name, start_date, end_date, is_current, status) VALUES
(1, '2024-2025-1', '2024-2025学年第一学期', '2024-09-01', '2025-01-15', 0, 1),
(2, '2024-2025-2', '2024-2025学年第二学期', '2025-02-17', '2025-07-05', 0, 1),
(3, '2025-2026-1', '2025-2026学年第一学期', '2025-09-01', '2026-01-15', 1, 1);

-- ============================================================
-- 6. 管理员 (admin) — user-service
-- 密码 BCrypt 加密：123456
-- ============================================================
INSERT INTO admin (id, username, password, real_name, role, status) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', '系统管理员', 'admin', 1);

-- ============================================================
-- 7. 学生 (student) — student-service
-- 密码 BCrypt 加密：123456
-- ============================================================
INSERT INTO student (id, student_no, name, gender, phone, email, password, college_id, major_id, class_id, enrollment_date, status) VALUES
(1, '2024001', '张三', '男', '13800138001', 'zhangsan@example.com', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 1, 1, '2024-09-01', 1),
(2, '2024002', '李四', '女', '13800138002', 'lisi@example.com', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 2, 3, '2024-09-01', 1),
(3, '2024003', '王五', '男', '13800138003', 'wangwu@example.com', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 1, 2, '2024-09-01', 1),
(4, '2024004', '赵六', '女', '13800138004', 'zhaoliu@example.com', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 3, 4, '2024-09-01', 1),
(5, '2024005', '孙七', '男', '13800138005', 'sunqi@example.com', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 2, 4, 5, '2024-09-01', 1),
(6, '2024006', '周八', '女', '13800138006', 'zhouba@example.com', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 2, 5, 6, '2024-09-01', 1);

-- ============================================================
-- 8. 教师 (teacher) — teacher-service
-- 密码 BCrypt 加密：123456
-- ============================================================
INSERT INTO teacher (id, teacher_no, name, gender, phone, email, password, title, college_id, department_id, hire_date, status) VALUES
(1, 'T001', '张教授', '男', '13900139001', 'zhangprof@example.com', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', '教授', 1, 1, '2020-09-01', 1),
(2, 'T002', '李教授', '女', '13900139002', 'liprof@example.com', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', '副教授', 1, 2, '2021-09-01', 1),
(3, 'T003', '王老师', '男', '13900139003', 'wangteacher@example.com', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', '讲师', 1, 1, '2022-09-01', 1),
(4, 'T004', '刘教授', '女', '13900139004', 'liuprof@example.com', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', '教授', 1, 3, '2019-09-01', 1),
(5, 'T005', '陈老师', '男', '13900139005', 'chenteacher@example.com', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', '讲师', 2, 4, '2023-09-01', 1);

-- ============================================================
-- 9. 课程 (course) — course-service
-- ============================================================
INSERT INTO course (id, course_code, course_name, course_type, credit, total_hours, teacher_id, semester, schedule, classroom, available_slots, selected_count, status, description) VALUES
(1, 'CS101', '数据结构与算法', '专业必修', 4.0, 64, 1, '2025-2026-1', '周一 1-2节', '教学楼301', 60, 0, 1, '本课程介绍常用的数据结构和算法设计方法'),
(2, 'CS102', '操作系统原理', '专业必修', 3.5, 56, 1, '2025-2026-1', '周三 3-4节', '教学楼302', 55, 0, 1, '本课程介绍操作系统的基本原理和实现技术'),
(3, 'CS201', '数据库系统概论', '专业必修', 3.5, 56, 2, '2025-2026-1', '周二 5-6节', '实验楼201', 50, 0, 1, '本课程介绍数据库系统的基本概念和设计方法'),
(4, 'CS301', '软件工程导论', '专业必修', 3.0, 48, 2, '2025-2026-1', '周四 1-2节', '教学楼405', 45, 0, 1, '本课程介绍软件工程的基本原理和开发方法'),
(5, 'CS401', '计算机网络', '专业必修', 3.5, 56, 3, '2025-2026-1', '周五 3-4节', '实验楼302', 50, 0, 1, '本课程介绍计算机网络的基本原理和协议'),
(6, 'MATH101', '高等数学A', '公共必修', 5.0, 80, 5, '2025-2026-1', '周一 3-4节, 周三 1-2节', '教学楼101', 120, 0, 1, '本课程介绍微积分、级数等高等数学基础'),
(7, 'MATH201', '线性代数', '公共必修', 3.0, 48, 5, '2025-2026-1', '周二 1-2节', '教学楼102', 100, 0, 1, '本课程介绍线性代数的基本概念和方法'),
(8, 'CS501', '人工智能导论', '专业选修', 2.5, 40, 3, '2025-2026-1', '周四 5-6节', '实验楼401', 40, 0, 1, '本课程介绍人工智能的基本概念和方法'),
(9, 'CS502', '机器学习基础', '专业选修', 2.5, 40, 1, '2025-2026-1', '周五 5-6节', '实验楼402', 35, 0, 1, '本课程介绍机器学习的基本算法和应用'),
(10, 'FL101', '大学英语', '公共必修', 4.0, 64, null, '2025-2026-1', '周二 3-4节, 周四 3-4节', '教学楼201', 100, 0, 1, '大学英语基础课程');

-- ============================================================
-- 10. 角色 (sys_role) — user-service
-- ============================================================
INSERT INTO sys_role (id, role_name, role_code, description, status) VALUES
(1, '管理员', 'ROLE_ADMIN', '系统管理员', 1),
(2, '教师', 'ROLE_TEACHER', '教师用户', 1),
(3, '学生', 'ROLE_STUDENT', '学生用户', 1);

-- ============================================================
-- 11. 权限 (sys_permission) — user-service
-- ============================================================
INSERT INTO sys_permission (id, permission_name, permission_code, url, method, permission_type, status) VALUES
(1, '用户管理', 'user:manage', '/api/v1/users/**', NULL, 3, 1),
(2, '课程管理', 'course:manage', '/api/v1/courses/**', NULL, 3, 1),
(3, '选课管理', 'selection:manage', '/api/v1/selections/**', NULL, 3, 1),
(4, '成绩管理', 'grade:manage', '/api/v1/grades/**', NULL, 3, 1),
(5, '系统管理', 'system:manage', '/api/v1/system/**', NULL, 3, 1);

-- ============================================================
-- 12. 角色-权限关联 — user-service
-- ============================================================
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5),
(2, 2), (2, 4),
(3, 3);

-- ============================================================
-- 13. 系统配置 (system_config) — 默认初始值
-- ============================================================
INSERT INTO system_config (config_key, config_value, description) VALUES
('system.name', '网上选课系统', '系统名称'),
('system.version', '2.0.0', '系统版本'),
('contact.email', 'admin@example.com', '联系邮箱'),
('login.timeout', '60', '登录超时（分钟）'),
('max.upload.size', '50', '最大上传大小（MB）'),
('system.status', 'open', '选课状态：open-开放，closed-关闭'),
('grade.entry.status', 'open', '成绩录入状态：open-开放，closed-关闭'),
('max.course.selection', '10', '每学期最大选课数量'),
('system.notice', '欢迎使用网上选课系统！', '系统通知公告');

-- ============================================================
-- 14. 系统用户 (sys_user) — user-service（统一登录认证）
-- ============================================================
INSERT INTO sys_user (id, username, password, real_name, user_type, user_code, gender, status) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', '系统管理员', 3, 'ADMIN001', NULL, 1),
(2, '2024001', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', '张三', 1, '2024001', 1, 1),
(3, '2024002', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', '李四', 1, '2024002', 2, 1),
(4, 'T001', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', '张教授', 2, 'T001', 1, 1),
(5, 'T002', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', '李教授', 2, 'T002', 2, 1);

-- ============================================================
-- 15. 用户-角色关联 — user-service
-- ============================================================
INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1),
(2, 3),
(3, 3),
(4, 2),
(5, 2);

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 初始化完成
-- ============================================================
-- 测试账号：
--   管理员：admin / admin123（admin 表硬编码兜底，也支持BCrypt: 123456）
--   学生：2024001 / 123456（张三）
--   教师：T001 / 123456（张教授）
--
-- 表归属微服务一览：
--   user-service（端口8101）：sys_user, sys_role, sys_permission, sys_user_role, sys_role_permission, admin
--   student-service（端口8102）：student
--   teacher-service（端口8103）：teacher
--   course-service（端口8104）：college, department, major, course, class_info, announcement
--   selection-service（端口8105）：course_selection, semester, course_evaluation, system_config
--   公共（web-service）：system_log
