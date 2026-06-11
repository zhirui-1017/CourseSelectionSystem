-- ============================================================
-- 网上选课系统 - 数据库初始化脚本
-- 数据库：course_selection_system
-- 编码：UTF-8
-- 说明：在 MySQL 中先创建数据库再执行本脚本
--   CREATE DATABASE IF NOT EXISTS course_selection_system
--     DEFAULT CHARACTER SET utf8mb4
--     DEFAULT COLLATE utf8mb4_unicode_ci;
--   USE course_selection_system;
--   SOURCE db/init.sql;
-- ============================================================

-- 清空旧数据（按外键依赖顺序）
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE course_selection;
TRUNCATE TABLE course;
TRUNCATE TABLE major;
TRUNCATE TABLE department;
TRUNCATE TABLE college;
TRUNCATE TABLE teacher;
TRUNCATE TABLE student;
TRUNCATE TABLE admin;
TRUNCATE TABLE sys_role;
TRUNCATE TABLE sys_permission;
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 1. 学院 (college)
-- ============================================================
INSERT INTO college (id, college_name, college_code, description, status, created_at, updated_at) VALUES
(1, '计算机与信息技术学院', 'CS', '计算机科学与技术、软件工程等相关专业', 1, NOW(), NOW()),
(2, '数学与统计学院', 'MATH', '数学与应用数学、统计学等相关专业', 1, NOW(), NOW()),
(3, '外国语学院', 'FL', '英语、日语、翻译等相关专业', 1, NOW(), NOW()),
(4, '经济管理学院', 'EM', '经济学、管理学、工商管理等相关专业', 1, NOW(), NOW());

-- ============================================================
-- 2. 系部 (department)
-- ============================================================
INSERT INTO department (id, department_code, department_name, college_id, description, status, created_at, updated_at) VALUES
(1, 'CS01', '计算机科学系', 1, '计算机科学与技术专业', 1, NOW(), NOW()),
(2, 'CS02', '软件工程系', 1, '软件工程专业', 1, NOW(), NOW()),
(3, 'CS03', '网络工程系', 1, '网络工程、信息安全专业', 1, NOW(), NOW()),
(4, 'MATH01', '数学系', 2, '数学与应用数学专业', 1, NOW(), NOW()),
(5, 'MATH02', '统计系', 2, '统计学专业', 1, NOW(), NOW()),
(6, 'FL01', '英语系', 3, '英语专业', 1, NOW(), NOW()),
(7, 'EM01', '管理系', 4, '工商管理、市场营销专业', 1, NOW(), NOW());

-- ============================================================
-- 3. 专业 (major)
-- ============================================================
INSERT INTO major (id, major_code, major_name, department_id, description, status, created_at, updated_at) VALUES
(1, 'CS0101', '计算机科学与技术', 1, '计算机科学与技术本科专业', 1, NOW(), NOW()),
(2, 'CS0201', '软件工程', 2, '软件工程本科专业', 1, NOW(), NOW()),
(3, 'CS0301', '网络工程', 3, '网络工程本科专业', 1, NOW(), NOW()),
(4, 'MATH0101', '数学与应用数学', 4, '数学与应用数学本科专业', 1, NOW(), NOW()),
(5, 'MATH0201', '统计学', 5, '统计学本科专业', 1, NOW(), NOW()),
(6, 'FL0101', '英语', 6, '英语本科专业', 1, NOW(), NOW()),
(7, 'EM0101', '工商管理', 7, '工商管理本科专业', 1, NOW(), NOW());

-- ============================================================
-- 4. 学生 (student)
-- 密码 BCrypt 加密：123456
-- ============================================================
INSERT INTO student (id, student_no, name, gender, phone, email, password, major_id, college_id, class_name, status, created_at, updated_at) VALUES
(1, '2024001', '张三', '男', '13800138001', 'zhangsan@example.com', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 1, '计科2101班', 1, NOW(), NOW()),
(2, '2024002', '李四', '女', '13800138002', 'lisi@example.com', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 2, 1, '软工2101班', 1, NOW(), NOW()),
(3, '2024003', '王五', '男', '13800138003', 'wangwu@example.com', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 1, '计科2102班', 1, NOW(), NOW()),
(4, '2024004', '赵六', '女', '13800138004', 'zhaoliu@example.com', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 3, 1, '网工2101班', 1, NOW(), NOW()),
(5, '2024005', '孙七', '男', '13800138005', 'sunqi@example.com', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 4, 2, '数学2101班', 1, NOW(), NOW()),
(6, '2024006', '周八', '女', '13800138006', 'zhouba@example.com', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 5, 2, '统计2101班', 1, NOW(), NOW());

-- ============================================================
-- 5. 教师 (teacher)
-- 密码 BCrypt 加密：123456
-- ============================================================
INSERT INTO teacher (id, teacher_no, name, gender, phone, email, password, title, department_id, status, created_at, updated_at) VALUES
(1, 'T001', '张教授', '男', '13900139001', 'zhangprof@example.com', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', '教授', 1, 1, NOW(), NOW()),
(2, 'T002', '李教授', '女', '13900139002', 'liprof@example.com', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', '副教授', 2, 1, NOW(), NOW()),
(3, 'T003', '王老师', '男', '13900139003', 'wangteacher@example.com', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', '讲师', 1, 1, NOW(), NOW()),
(4, 'T004', '刘教授', '女', '13900139004', 'liuprof@example.com', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', '教授', 3, 1, NOW(), NOW()),
(5, 'T005', '陈老师', '男', '13900139005', 'chenteacher@example.com', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', '讲师', 4, 1, NOW(), NOW());

-- ============================================================
-- 6. 管理员 (admin)
-- 密码 BCrypt 加密（硬编码也已支持 admin/admin123）
-- ============================================================
INSERT INTO admin (id, username, password, role, status) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 1);

-- ============================================================
-- 7. 课程 (course)
-- ============================================================
INSERT INTO course (id, course_code, course_name, course_type, credit, total_hours, teacher_id, schedule, classroom, available_slots, selected_count, status, description, created_at, updated_at) VALUES
(1, 'CS101', '数据结构与算法', '专业必修', 4.0, 64, 1, '周一 1-2节', '教学楼301', 60, 0, 1, '本课程介绍常用的数据结构和算法设计方法', NOW(), NOW()),
(2, 'CS102', '操作系统原理', '专业必修', 3.5, 56, 1, '周三 3-4节', '教学楼302', 55, 0, 1, '本课程介绍操作系统的基本原理和实现技术', NOW(), NOW()),
(3, 'CS201', '数据库系统概论', '专业必修', 3.5, 56, 2, '周二 5-6节', '实验楼201', 50, 0, 1, '本课程介绍数据库系统的基本概念和设计方法', NOW(), NOW()),
(4, 'CS301', '软件工程导论', '专业必修', 3.0, 48, 2, '周四 1-2节', '教学楼405', 45, 0, 1, '本课程介绍软件工程的基本原理和开发方法', NOW(), NOW()),
(5, 'CS401', '计算机网络', '专业必修', 3.5, 56, 3, '周五 3-4节', '实验楼302', 50, 0, 1, '本课程介绍计算机网络的基本原理和协议', NOW(), NOW()),
(6, 'MATH101', '高等数学A', '公共必修', 5.0, 80, 5, '周一 3-4节, 周三 1-2节', '教学楼101', 120, 0, 1, '本课程介绍微积分、级数等高等数学基础', NOW(), NOW()),
(7, 'MATH201', '线性代数', '公共必修', 3.0, 48, 5, '周二 1-2节', '教学楼102', 100, 0, 1, '本课程介绍线性代数的基本概念和方法', NOW(), NOW()),
(8, 'CS501', '人工智能导论', '专业选修', 2.5, 40, 3, '周四 5-6节', '实验楼401', 40, 0, 1, '本课程介绍人工智能的基本概念和方法', NOW(), NOW()),
(9, 'CS502', '机器学习基础', '专业选修', 2.5, 40, 1, '周五 5-6节', '实验楼402', 35, 0, 1, '本课程介绍机器学习的基本算法和应用', NOW(), NOW()),
(10, 'FL101', '大学英语', '公共必修', 4.0, 64, null, '周二 3-4节, 周四 3-4节', '教学楼201', 100, 0, 1, '大学英语基础课程', NOW(), NOW());

-- ============================================================
-- 8. 角色 (sys_role)
-- ============================================================
INSERT INTO sys_role (id, role_name, role_code, description, status, create_time, update_time) VALUES
(1, '管理员', 'ROLE_ADMIN', '系统管理员', 1, NOW(), NOW()),
(2, '教师', 'ROLE_TEACHER', '教师用户', 1, NOW(), NOW()),
(3, '学生', 'ROLE_STUDENT', '学生用户', 1, NOW(), NOW());

-- ============================================================
-- 9. 权限 (sys_permission)
-- ============================================================
INSERT INTO sys_permission (id, permission_name, permission_code, description, status, create_time, update_time) VALUES
(1, '用户管理', 'user:manage', '用户管理权限', 1, NOW(), NOW()),
(2, '课程管理', 'course:manage', '课程管理权限', 1, NOW(), NOW()),
(3, '选课管理', 'selection:manage', '选课管理权限', 1, NOW(), NOW()),
(4, '成绩管理', 'grade:manage', '成绩管理权限', 1, NOW(), NOW()),
(5, '系统管理', 'system:manage', '系统管理权限', 1, NOW(), NOW());

-- ============================================================
-- 初始化完成
-- ============================================================
-- 测试账号：
--   管理员：admin / admin123（硬编码兜底，也支持BCrypt: 123456）
--   学生：2024001 / 123456（张三）
--   教师：T001 / 123456（张教授）
