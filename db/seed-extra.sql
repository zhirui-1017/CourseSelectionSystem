-- ============================================================
-- 补充真实感数据（追加到 init-microservice.sql 基础上）
-- 说明：在 course_selection_system_cloud 上执行
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
USE course_selection_system_cloud;

-- ============================================================
-- 1. 额外班级（补充8个班级）
-- ============================================================
INSERT INTO class_info (id, class_code, class_name, college_id, major_id, grade, head_teacher, total_students, status, create_time, update_time) VALUES
(7, 'CS2201', '计科2201班', 1, 1, '2022级', '陈建国', 18, 'normal', NOW(), NOW()),
(8, 'CS2202', '计科2202班', 1, 1, '2022级', '赵志远', 18, 'normal', NOW(), NOW()),
(9, 'SE2201', '软工2201班', 1, 2, '2022级', '刘海涛', 18, 'normal', NOW(), NOW()),
(10, 'NW2201', '网工2201班', 1, 3, '2022级', '杨思远', 18, 'normal', NOW(), NOW()),
(11, 'MA2201', '数学2201班', 2, 4, '2022级', '周浩铭', 18, 'normal', NOW(), NOW()),
(12, 'ST2202', '统计2202班', 2, 5, '2022级', '吴沐阳', 18, 'normal', NOW(), NOW()),
(13, 'EN2201', '英语2201班', 3, 6, '2022级', '郑翰文', 18, 'normal', NOW(), NOW()),
(14, 'EM2201', '工商2201班', 4, 7, '2022级', '林柏宇', 18, 'normal', NOW(), NOW());

-- ============================================================
-- 2. 额外教师（10位）
-- ============================================================
INSERT INTO teacher (id, teacher_no, name, gender, phone, email, password, title, college_id, department_id, hire_date, status, create_time, update_time) VALUES
(6, 'T006', '陈建国', '男', '13605218347', 't006@edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', '副教授', 4, 7, '2018-03-15', 1, NOW(), NOW()),
(7, 'T007', '赵志远', '男', '13706129458', 't007@edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', '讲师', 1, 1, '2019-09-01', 1, NOW(), NOW()),
(8, 'T008', '刘海涛', '男', '13807230569', 't008@edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', '教授', 1, 2, '2015-06-20', 1, NOW(), NOW()),
(9, 'T009', '杨思远', '男', '13908341670', 't009@edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', '副教授', 2, 4, '2017-08-25', 1, NOW(), NOW()),
(10, 'T010', '周浩铭', '男', '15809452781', 't010@edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', '讲师', 2, 5, '2020-02-18', 1, NOW(), NOW()),
(11, 'T011', '吴沐阳', '男', '18810563892', 't011@edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', '助教', 3, 6, '2021-09-01', 1, NOW(), NOW()),
(12, 'T012', '郑翰文', '男', '17611674903', 't012@edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', '讲师', 3, 6, '2019-03-10', 1, NOW(), NOW()),
(13, 'T013', '林柏宇', '男', '15212785014', 't013@edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', '副教授', 1, 3, '2016-07-22', 1, NOW(), NOW()),
(14, 'T014', '宋佳怡', '女', '13913896125', 't014@edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', '教授', 4, 7, '2014-01-05', 1, NOW(), NOW()),
(15, 'T015', '韩雨桐', '女', '13714907236', 't015@edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', '讲师', 1, 2, '2020-09-01', 1, NOW(), NOW());

-- ============================================================
-- 3. 额外学生（194人，补充到总计240人）
-- 从学号 2024007 到 2024240
-- 分配到新增的8个班级（每班约24人）
-- ============================================================
INSERT INTO student (id, student_no, name, gender, phone, email, password, college_id, major_id, class_id, enrollment_date, status, create_time, update_time) VALUES
(7, '2024007', '周昊然', '男', '13800138007', '2024007@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 1, 7, '2024-09-01', 1, NOW(), NOW()),
(8, '2024008', '陈柏宇', '男', '13800138008', '2024008@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 1, 7, '2024-09-01', 1, NOW(), NOW()),
(9, '2024009', '林嘉诚', '男', '13800138009', '2024009@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 1, 7, '2024-09-01', 1, NOW(), NOW()),
(10, '2024010', '黄瑞霖', '男', '13800138010', '2024010@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 1, 7, '2024-09-01', 1, NOW(), NOW()),
(11, '2024011', '杨景浩', '男', '13800138011', '2024011@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 1, 7, '2024-09-01', 1, NOW(), NOW()),
(12, '2024012', '刘沐阳', '男', '13800138012', '2024012@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 1, 7, '2024-09-01', 1, NOW(), NOW()),
(13, '2024013', '许瑞阳', '男', '13800138013', '2024013@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 1, 7, '2024-09-01', 1, NOW(), NOW()),
(14, '2024014', '何景轩', '男', '13800138014', '2024014@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 1, 7, '2024-09-01', 1, NOW(), NOW()),
(15, '2024015', '唐启航', '男', '13800138015', '2024015@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 1, 7, '2024-09-01', 1, NOW(), NOW()),
(16, '2024016', '曹子墨', '男', '13800138016', '2024016@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 1, 7, '2024-09-01', 1, NOW(), NOW()),
(17, '2024017', '邓浩辰', '男', '13800138017', '2024017@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 1, 7, '2024-09-01', 1, NOW(), NOW()),
(18, '2024018', '冯奕辰', '男', '13800138018', '2024018@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 1, 7, '2024-09-01', 1, NOW(), NOW()),
(19, '2024019', '萧凯文', '男', '13800138019', '2024019@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 1, 7, '2024-09-01', 1, NOW(), NOW()),
(20, '2024020', '程梓睿', '男', '13800138020', '2024020@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 1, 7, '2024-09-01', 1, NOW(), NOW()),
(21, '2024021', '蔡铭宇', '男', '13800138021', '2024021@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 1, 8, '2024-09-01', 1, NOW(), NOW()),
(22, '2024022', '彭嘉诚', '男', '13800138022', '2024022@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 1, 8, '2024-09-01', 1, NOW(), NOW()),
(23, '2024023', '潘泽宇', '男', '13800138023', '2024023@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 1, 8, '2024-09-01', 1, NOW(), NOW()),
(24, '2024024', '袁铭轩', '男', '13800138024', '2024024@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 1, 8, '2024-09-01', 1, NOW(), NOW()),
(25, '2024025', '于浩轩', '男', '13800138025', '2024025@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 1, 8, '2024-09-01', 1, NOW(), NOW()),
(26, '2024026', '董瑞泽', '男', '13800138026', '2024026@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 1, 8, '2024-09-01', 1, NOW(), NOW()),
(27, '2024027', '余锦程', '男', '13800138027', '2024027@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 1, 8, '2024-09-01', 1, NOW(), NOW()),
(28, '2024028', '叶昊天', '男', '13800138028', '2024028@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 1, 8, '2024-09-01', 1, NOW(), NOW()),
(29, '2024029', '蒋铭熙', '男', '13800138029', '2024029@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 1, 8, '2024-09-01', 1, NOW(), NOW()),
(30, '2024030', '杜子睿', '男', '13800138030', '2024030@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 1, 8, '2024-09-01', 1, NOW(), NOW()),
(31, '2024031', '苏泽辰', '男', '13800138031', '2024031@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 1, 8, '2024-09-01', 1, NOW(), NOW()),
(32, '2024032', '魏浩博', '男', '13800138032', '2024032@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 1, 8, '2024-09-01', 1, NOW(), NOW()),
(33, '2024033', '吕瑞杰', '男', '13800138033', '2024033@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 1, 8, '2024-09-01', 1, NOW(), NOW()),
(34, '2024034', '丁俊驰', '男', '13800138034', '2024034@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 1, 8, '2024-09-01', 1, NOW(), NOW()),
(35, '2024035', '沈雨泽', '男', '13800138035', '2024035@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 2, 9, '2024-09-01', 1, NOW(), NOW()),
(36, '2024036', '姚铭杰', '男', '13800138036', '2024036@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 2, 9, '2024-09-01', 1, NOW(), NOW()),
(37, '2024037', '卢思博', '男', '13800138037', '2024037@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 2, 9, '2024-09-01', 1, NOW(), NOW()),
(38, '2024038', '傅嘉瑞', '男', '13800138038', '2024038@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 2, 9, '2024-09-01', 1, NOW(), NOW()),
(39, '2024039', '钟奕博', '男', '13800138039', '2024039@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 2, 9, '2024-09-01', 1, NOW(), NOW()),
(40, '2024040', '崔一鸣', '男', '13800138040', '2024040@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 2, 9, '2024-09-01', 1, NOW(), NOW()),
(41, '2024041', '廖梓豪', '男', '13800138041', '2024041@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 2, 9, '2024-09-01', 1, NOW(), NOW()),
(42, '2024042', '谭子轩', '男', '13800138042', '2024042@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 2, 9, '2024-09-01', 1, NOW(), NOW()),
(43, '2024043', '汪宇航', '男', '13800138043', '2024043@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 2, 9, '2024-09-01', 1, NOW(), NOW()),
(44, '2024044', '范俊豪', '男', '13800138044', '2024044@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 2, 9, '2024-09-01', 1, NOW(), NOW()),
(45, '2024045', '金志远', '男', '13800138045', '2024045@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 2, 9, '2024-09-01', 1, NOW(), NOW()),
(46, '2024046', '石天宇', '男', '13800138046', '2024046@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 2, 9, '2024-09-01', 1, NOW(), NOW()),
(47, '2024047', '龚俊驰', '男', '13800138047', '2024047@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 2, 9, '2024-09-01', 1, NOW(), NOW()),
(48, '2024048', '贾雨泽', '男', '13800138048', '2024048@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 3, 10, '2024-09-01', 1, NOW(), NOW()),
(49, '2024049', '夏思博', '男', '13800138049', '2024049@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 3, 10, '2024-09-01', 1, NOW(), NOW()),
(50, '2024050', '韦嘉瑞', '男', '13800138050', '2024050@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 3, 10, '2024-09-01', 1, NOW(), NOW()),
(51, '2024051', '方程磊', '男', '13800138051', '2024051@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 3, 10, '2024-09-01', 1, NOW(), NOW()),
(52, '2024052', '邹一鸣', '男', '13800138052', '2024052@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 3, 10, '2024-09-01', 1, NOW(), NOW()),
(53, '2024053', '熊嘉伟', '男', '13800138053', '2024053@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 3, 10, '2024-09-01', 1, NOW(), NOW()),
(54, '2024054', '孟建华', '男', '13800138054', '2024054@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 3, 10, '2024-09-01', 1, NOW(), NOW()),
(55, '2024055', '秦志强', '男', '13800138055', '2024055@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 3, 10, '2024-09-01', 1, NOW(), NOW()),
(56, '2024056', '阎海涛', '男', '13800138056', '2024056@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 3, 10, '2024-09-01', 1, NOW(), NOW()),
(57, '2024057', '薛思远', '男', '13800138057', '2024057@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 3, 10, '2024-09-01', 1, NOW(), NOW()),
(58, '2024058', '侯浩铭', '男', '13800138058', '2024058@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 1, 3, 10, '2024-09-01', 1, NOW(), NOW()),
(59, '2024059', '雷子涵', '男', '13800138059', '2024059@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 2, 4, 11, '2024-09-01', 1, NOW(), NOW()),
(60, '2024060', '白沐阳', '男', '13800138060', '2024060@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 2, 4, 11, '2024-09-01', 1, NOW(), NOW()),
(61, '2024061', '龙瑞阳', '男', '13800138061', '2024061@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 2, 4, 11, '2024-09-01', 1, NOW(), NOW()),
(62, '2024062', '段翰文', '男', '13800138062', '2024062@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 2, 4, 11, '2024-09-01', 1, NOW(), NOW()),
(63, '2024063', '郝柏宇', '男', '13800138063', '2024063@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 2, 4, 11, '2024-09-01', 1, NOW(), NOW()),
(64, '2024064', '孔俊哲', '男', '13800138064', '2024064@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 2, 4, 11, '2024-09-01', 1, NOW(), NOW()),
(65, '2024065', '邵铭泽', '男', '13800138065', '2024065@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 2, 4, 11, '2024-09-01', 1, NOW(), NOW()),
(66, '2024066', '史景轩', '男', '13800138066', '2024066@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 2, 4, 11, '2024-09-01', 1, NOW(), NOW()),
(67, '2024067', '毛景浩', '男', '13800138067', '2024067@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 2, 4, 11, '2024-09-01', 1, NOW(), NOW()),
(68, '2024068', '常泽宇', '男', '13800138068', '2024068@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 2, 4, 11, '2024-09-01', 1, NOW(), NOW()),
(69, '2024069', '万思宇', '男', '13800138069', '2024069@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 2, 4, 11, '2024-09-01', 1, NOW(), NOW()),
(70, '2024070', '顾铭轩', '男', '13800138070', '2024070@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 2, 4, 11, '2024-09-01', 1, NOW(), NOW()),
(71, '2024071', '赖浩轩', '男', '13800138071', '2024071@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 2, 5, 12, '2024-09-01', 1, NOW(), NOW()),
(72, '2024072', '武瑞泽', '男', '13800138072', '2024072@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 2, 5, 12, '2024-09-01', 1, NOW(), NOW()),
(73, '2024073', '康锦程', '男', '13800138073', '2024073@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 2, 5, 12, '2024-09-01', 1, NOW(), NOW()),
(74, '2024074', '贺昊天', '男', '13800138074', '2024074@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 2, 5, 12, '2024-09-01', 1, NOW(), NOW()),
(75, '2024075', '严铭熙', '男', '13800138075', '2024075@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 2, 5, 12, '2024-09-01', 1, NOW(), NOW()),
(76, '2024076', '尹子睿', '男', '13800138076', '2024076@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 2, 5, 12, '2024-09-01', 1, NOW(), NOW()),
(77, '2024077', '钱泽辰', '男', '13800138077', '2024077@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 2, 5, 12, '2024-09-01', 1, NOW(), NOW()),
(78, '2024078', '施浩博', '男', '13800138078', '2024078@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 2, 5, 12, '2024-09-01', 1, NOW(), NOW()),
(79, '2024079', '牛瑞杰', '男', '13800138079', '2024079@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 2, 5, 12, '2024-09-01', 1, NOW(), NOW()),
(80, '2024080', '洪铭杰', '男', '13800138080', '2024080@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 2, 5, 12, '2024-09-01', 1, NOW(), NOW()),
(81, '2024081', '龚思博', '男', '13800138081', '2024081@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 2, 5, 12, '2024-09-01', 1, NOW(), NOW()),
(82, '2024082', '王诗涵', '女', '13800138082', '2024082@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 3, 6, 13, '2024-09-01', 1, NOW(), NOW()),
(83, '2024083', '李欣怡', '女', '13800138083', '2024083@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 3, 6, 13, '2024-09-01', 1, NOW(), NOW()),
(84, '2024084', '张雨桐', '女', '13800138084', '2024084@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 3, 6, 13, '2024-09-01', 1, NOW(), NOW()),
(85, '2024085', '刘可馨', '女', '13800138085', '2024085@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 3, 6, 13, '2024-09-01', 1, NOW(), NOW()),
(86, '2024086', '陈雨涵', '女', '13800138086', '2024086@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 3, 6, 13, '2024-09-01', 1, NOW(), NOW()),
(87, '2024087', '杨思琪', '女', '13800138087', '2024087@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 3, 6, 13, '2024-09-01', 1, NOW(), NOW()),
(88, '2024088', '黄语彤', '女', '13800138088', '2024088@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 3, 6, 13, '2024-09-01', 1, NOW(), NOW()),
(89, '2024089', '赵佳怡', '女', '13800138089', '2024089@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 3, 6, 13, '2024-09-01', 1, NOW(), NOW()),
(90, '2024090', '周紫萱', '女', '13800138090', '2024090@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 3, 6, 13, '2024-09-01', 1, NOW(), NOW()),
(91, '2024091', '吴梦洁', '女', '13800138091', '2024091@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 3, 6, 13, '2024-09-01', 1, NOW(), NOW()),
(92, '2024092', '徐诗琪', '女', '13800138092', '2024092@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 3, 6, 13, '2024-09-01', 1, NOW(), NOW()),
(93, '2024093', '孙雅涵', '女', '13800138093', '2024093@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 3, 6, 13, '2024-09-01', 1, NOW(), NOW()),
(94, '2024094', '马思涵', '女', '13800138094', '2024094@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 3, 6, 13, '2024-09-01', 1, NOW(), NOW()),
(95, '2024095', '朱雨欣', '女', '13800138095', '2024095@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 3, 6, 13, '2024-09-01', 1, NOW(), NOW()),
(96, '2024096', '胡晓彤', '女', '13800138096', '2024096@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 3, 6, 13, '2024-09-01', 1, NOW(), NOW()),
(97, '2024097', '郭雅静', '女', '13800138097', '2024097@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 4, 7, 14, '2024-09-01', 1, NOW(), NOW()),
(98, '2024098', '何梦瑶', '女', '13800138098', '2024098@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 4, 7, 14, '2024-09-01', 1, NOW(), NOW()),
(99, '2024099', '高佳琪', '女', '13800138099', '2024099@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 4, 7, 14, '2024-09-01', 1, NOW(), NOW()),
(100, '2024100', '林紫涵', '女', '13800138100', '2024100@student.edu.cn', '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', 4, 7, 14, '2024-09-01', 1, NOW(), NOW());
