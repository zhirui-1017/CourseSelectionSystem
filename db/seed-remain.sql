-- 补充剩余数据：选课记录+评价+用户
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
USE course_selection_system_cloud;

-- 清空可能冲突的数据
TRUNCATE TABLE course_evaluation;
TRUNCATE TABLE course_selection;
DELETE FROM sys_user_role;
DELETE FROM sys_user WHERE id > 1;

-- ========== 选课记录（含remark列） ==========
INSERT INTO course_selection (id, student_id, course_id, semester, status, score, score_level, gpa, daily_grade, lab_grade, exam_grade, remark, selection_time, drop_time, create_time, update_time) VALUES
(1,1,1,'2025-2026-1',2,85.5,'良好',3.6,88,82,84,NULL,NOW(),NULL,NOW(),NOW()),
(2,1,37,'2025-2026-1',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW(),NULL,NOW(),NOW()),
(3,2,3,'2025-2026-1',2,92.0,'优秀',4.2,95,88,91,'学习认真',NOW(),NULL,NOW(),NOW()),
(4,2,6,'2025-2026-1',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW(),NULL,NOW(),NOW()),
(5,3,5,'2025-2026-1',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW(),NULL,NOW(),NOW()),
(6,3,2,'2025-2026-1',2,78.0,'中等',2.8,80,75,77,'需加强练习',NOW(),NULL,NOW(),NOW()),
(7,4,4,'2025-2026-1',2,65.5,'及格',1.6,70,68,62,NULL,NOW(),NULL,NOW(),NOW()),
(8,4,10,'2025-2026-1',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW(),NULL,NOW(),NOW()),
(9,5,7,'2025-2026-1',2,88.0,'良好',3.8,90,85,87,NULL,NOW(),NULL,NOW(),NOW()),
(10,5,13,'2025-2026-1',2,91.5,'优秀',4.2,93,90,91,'成绩优秀',NOW(),NULL,NOW(),NOW()),
(11,6,8,'2025-2026-1',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW(),NULL,NOW(),NOW()),
(12,6,9,'2025-2026-1',2,73.0,'中等',2.3,75,70,72,NULL,NOW(),NULL,NOW(),NOW()),
(13,7,1,'2025-2026-1',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW(),NULL,NOW(),NOW()),
(14,7,11,'2025-2026-1',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW(),NULL,NOW(),NOW()),
(15,8,2,'2025-2026-1',2,82.0,'良好',3.2,85,80,81,NULL,NOW(),NULL,NOW(),NOW()),
(16,8,37,'2025-2026-1',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW(),NULL,NOW(),NOW()),
(17,9,3,'2025-2026-1',2,95.0,'优秀',4.5,96,94,94,'表现突出',NOW(),NULL,NOW(),NOW()),
(18,9,5,'2025-2026-1',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW(),NULL,NOW(),NOW()),
(19,10,4,'2025-2026-1',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW(),NULL,NOW(),NOW()),
(20,10,6,'2025-2026-1',2,76.5,'中等',2.7,78,74,76,NULL,NOW(),NULL,NOW(),NOW()),
(21,11,7,'2025-2026-1',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW(),NULL,NOW(),NOW()),
(22,11,8,'2025-2026-1',2,83.0,'良好',3.3,85,82,82,NULL,NOW(),NULL,NOW(),NOW()),
(23,12,9,'2025-2026-1',2,69.0,'及格',1.9,72,65,68,NULL,NOW(),NULL,NOW(),NOW()),
(24,12,10,'2025-2026-1',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW(),NULL,NOW(),NOW()),
(25,13,1,'2025-2026-1',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW(),NULL,NOW(),NOW()),
(26,13,37,'2025-2026-1',2,87.0,'良好',3.7,89,84,86,NULL,NOW(),NULL,NOW(),NOW()),
(27,14,2,'2025-2026-1',2,93.5,'优秀',4.4,95,92,93,NULL,NOW(),NULL,NOW(),NOW()),
(28,14,3,'2025-2026-1',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW(),NULL,NOW(),NOW()),
(29,15,4,'2025-2026-1',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW(),NULL,NOW(),NOW()),
(30,15,5,'2025-2026-1',2,71.0,'中等',2.1,74,68,70,NULL,NOW(),NULL,NOW(),NOW());

INSERT INTO course_selection (id, student_id, course_id, semester, status, score, score_level, gpa, daily_grade, lab_grade, exam_grade, remark, selection_time, drop_time, create_time, update_time) VALUES
(31,16,6,'2025-2026-1',2,58.0,'不及格',0.0,62,55,56,NULL,NOW(),NULL,NOW(),NOW()),
(32,16,7,'2025-2026-1',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW(),NULL,NOW(),NOW()),
(33,17,8,'2025-2026-1',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW(),NULL,NOW(),NOW()),
(34,17,9,'2025-2026-1',2,90.0,'优秀',4.0,92,88,89,NULL,NOW(),NULL,NOW(),NOW()),
(35,18,10,'2025-2026-1',2,84.5,'良好',3.5,86,82,84,NULL,NOW(),NULL,NOW(),NOW()),
(36,18,11,'2025-2026-1',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW(),NULL,NOW(),NOW()),
(37,19,12,'2025-2026-1',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW(),NULL,NOW(),NOW()),
(38,19,13,'2025-2026-1',2,79.0,'中等',2.9,82,76,78,NULL,NOW(),NULL,NOW(),NOW()),
(39,20,14,'2025-2026-1',2,86.0,'良好',3.6,88,84,85,NULL,NOW(),NULL,NOW(),NOW()),
(40,20,15,'2025-2026-1',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW(),NULL,NOW(),NOW()),
(41,21,16,'2025-2026-1',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW(),NULL,NOW(),NOW()),
(42,21,17,'2025-2026-1',2,74.0,'中等',2.4,76,72,73,NULL,NOW(),NULL,NOW(),NOW()),
(43,22,18,'2025-2026-1',2,81.5,'良好',3.2,84,80,80,NULL,NOW(),NULL,NOW(),NOW()),
(44,22,19,'2025-2026-1',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW(),NULL,NOW(),NOW()),
(45,23,20,'2025-2026-1',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW(),NULL,NOW(),NOW()),
(46,23,21,'2025-2026-1',2,89.0,'良好',3.9,91,86,88,NULL,NOW(),NULL,NOW(),NOW()),
(47,24,22,'2025-2026-1',2,67.5,'及格',1.8,70,64,67,NULL,NOW(),NULL,NOW(),NOW()),
(48,24,23,'2025-2026-1',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW(),NULL,NOW(),NOW()),
(49,25,24,'2025-2026-1',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW(),NULL,NOW(),NOW()),
(50,25,25,'2025-2026-1',2,94.0,'优秀',4.4,96,92,93,NULL,NOW(),NULL,NOW(),NOW()),
(51,26,26,'2025-2026-1',2,77.0,'中等',2.7,80,74,76,NULL,NOW(),NULL,NOW(),NOW()),
(52,26,27,'2025-2026-1',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW(),NULL,NOW(),NOW()),
(53,27,28,'2025-2026-1',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW(),NULL,NOW(),NOW()),
(54,27,29,'2025-2026-1',2,83.0,'良好',3.3,85,82,82,NULL,NOW(),NULL,NOW(),NOW()),
(55,28,30,'2025-2026-1',2,91.0,'优秀',4.1,93,88,90,NULL,NOW(),NULL,NOW(),NOW()),
(56,28,31,'2025-2026-1',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW(),NULL,NOW(),NOW()),
(57,29,32,'2025-2026-1',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW(),NULL,NOW(),NOW()),
(58,29,33,'2025-2026-1',2,72.5,'中等',2.3,75,70,71,NULL,NOW(),NULL,NOW(),NOW()),
(59,30,34,'2025-2026-1',2,88.5,'良好',3.9,90,86,88,NULL,NOW(),NULL,NOW(),NOW()),
(60,30,35,'2025-2026-1',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW(),NULL,NOW(),NOW()),
(61,31,36,'2025-2026-1',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW(),NULL,NOW(),NOW()),
(62,31,38,'2025-2026-1',2,75.0,'中等',2.5,78,72,74,NULL,NOW(),NULL,NOW(),NOW()),
(63,32,39,'2025-2026-1',2,62.0,'及格',1.2,65,58,61,NULL,NOW(),NULL,NOW(),NOW()),
(64,32,40,'2025-2026-1',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW(),NULL,NOW(),NOW()),
(65,33,1,'2025-2026-1',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW(),NULL,NOW(),NOW()),
(66,33,2,'2025-2026-1',2,96.0,'优秀',4.6,97,94,95,NULL,NOW(),NULL,NOW(),NOW()),
(67,34,3,'2025-2026-1',2,80.0,'良好',3.0,82,78,79,NULL,NOW(),NULL,NOW(),NOW()),
(68,34,4,'2025-2026-1',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW(),NULL,NOW(),NOW()),
(69,35,5,'2025-2026-1',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW(),NULL,NOW(),NOW()),
(70,35,6,'2025-2026-1',2,70.0,'中等',2.0,72,68,69,NULL,NOW(),NULL,NOW(),NOW()),
(71,36,7,'2025-2026-1',2,85.0,'良好',3.5,88,82,84,NULL,NOW(),NULL,NOW(),NOW()),
(72,36,8,'2025-2026-1',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW(),NULL,NOW(),NOW()),
(73,37,9,'2025-2026-1',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW(),NULL,NOW(),NOW()),
(74,37,10,'2025-2026-1',2,78.5,'中等',2.9,80,76,78,NULL,NOW(),NULL,NOW(),NOW()),
(75,38,11,'2025-2026-1',2,92.5,'优秀',4.3,94,90,92,NULL,NOW(),NULL,NOW(),NOW()),
(76,38,12,'2025-2026-1',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW(),NULL,NOW(),NOW()),
(77,39,13,'2025-2026-1',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW(),NULL,NOW(),NOW()),
(78,39,14,'2025-2026-1',2,66.0,'及格',1.6,70,62,64,NULL,NOW(),NULL,NOW(),NOW()),
(79,40,15,'2025-2026-1',2,87.0,'良好',3.7,90,84,86,NULL,NOW(),NULL,NOW(),NOW()),
(80,40,16,'2025-2026-1',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW(),NULL,NOW(),NOW());

-- ========== 课程评价（80条） ==========
INSERT INTO course_evaluation (id, student_id, course_id, semester, rating, comment, evaluation_date, create_time, update_time) VALUES
(1,1,1,'2025-2026-1',5,'数据结构课程内容充实，老师讲解清晰，受益匪浅。',NOW(),NOW(),NOW()),
(2,2,3,'2025-2026-1',4,'数据库课程理论与实践结合得很好。',NOW(),NOW(),NOW()),
(3,3,2,'2025-2026-1',3,'操作系统原理课程内容较深，需要更多练习。',NOW(),NOW(),NOW()),
(4,4,4,'2025-2026-1',4,'软件工程导论案例丰富，实用性强。',NOW(),NOW(),NOW()),
(5,5,7,'2025-2026-1',5,'线性代数老师讲课生动有趣。',NOW(),NOW(),NOW()),
(6,5,13,'2025-2026-1',5,'复变函数课程难度适中，收获很大。',NOW(),NOW(),NOW()),
(7,6,9,'2025-2026-1',3,'人工智能导论内容前沿，建议增加实践环节。',NOW(),NOW(),NOW()),
(8,8,2,'2025-2026-1',4,'操作系统原理课程设计合理。',NOW(),NOW(),NOW()),
(9,9,3,'2025-2026-1',5,'数据库课程实验安排合理，动手能力提升明显。',NOW(),NOW(),NOW()),
(10,10,6,'2025-2026-1',3,'高等数学A进度偏快，希望能有更多习题课。',NOW(),NOW(),NOW()),
(11,12,9,'2025-2026-1',4,'人工智能导论拓宽了视野。',NOW(),NOW(),NOW()),
(12,13,37,'2025-2026-1',5,'算法设计与分析课程非常精彩。',NOW(),NOW(),NOW()),
(13,14,2,'2025-2026-1',5,'操作系统原理老师经验丰富。',NOW(),NOW(),NOW()),
(14,15,5,'2025-2026-1',4,'计算机网络课程实用性强。',NOW(),NOW(),NOW()),
(15,16,6,'2025-2026-1',2,'高等数学A需要更多辅导资源。',NOW(),NOW(),NOW()),
(16,17,9,'2025-2026-1',5,'人工智能导论课程非常有趣。',NOW(),NOW(),NOW()),
(17,18,10,'2025-2026-1',4,'大学英语课程内容丰富。',NOW(),NOW(),NOW()),
(18,19,13,'2025-2026-1',4,'复变函数需要更多例题讲解。',NOW(),NOW(),NOW()),
(19,20,14,'2025-2026-1',4,'实变函数课程设计合理。',NOW(),NOW(),NOW()),
(20,22,18,'2025-2026-1',4,'微观经济学案例丰富。',NOW(),NOW(),NOW()),
(21,23,21,'2025-2026-1',5,'人力资源管理课程老师讲课生动。',NOW(),NOW(),NOW()),
(22,24,22,'2025-2026-1',3,'大学生创新创业课程实用性强。',NOW(),NOW(),NOW()),
(23,25,25,'2025-2026-1',5,'嵌入式系统课程动手实践丰富。',NOW(),NOW(),NOW()),
(24,26,26,'2025-2026-1',4,'数据挖掘课程内容前沿。',NOW(),NOW(),NOW()),
(25,27,29,'2025-2026-1',4,'天文学基础拓宽了知识面。',NOW(),NOW(),NOW()),
(26,28,30,'2025-2026-1',5,'商务英语课程实用性强。',NOW(),NOW(),NOW()),
(27,29,33,'2025-2026-1',3,'国际贸易实务需要更多案例分析。',NOW(),NOW(),NOW()),
(28,30,34,'2025-2026-1',5,'财务管理课程清晰易懂。',NOW(),NOW(),NOW()),
(29,31,38,'2025-2026-1',3,'数字逻辑课程有一定难度。',NOW(),NOW(),NOW()),
(30,32,39,'2025-2026-1',4,'数值分析课程内容充实。',NOW(),NOW(),NOW()),
(31,33,2,'2025-2026-1',5,'操作系统原理老师教学水平高。',NOW(),NOW(),NOW()),
(32,34,3,'2025-2026-1',4,'数据库系统概论实验设计好。',NOW(),NOW(),NOW()),
(33,35,6,'2025-2026-1',4,'高等数学A需要更多习题。',NOW(),NOW(),NOW()),
(34,36,7,'2025-2026-1',4,'线性代数课程安排合理。',NOW(),NOW(),NOW()),
(35,37,10,'2025-2026-1',4,'大学英语II课程内容丰富。',NOW(),NOW(),NOW()),
(36,38,11,'2025-2026-1',5,'软件项目管理课程非常实用。',NOW(),NOW(),NOW()),
(37,39,14,'2025-2026-1',3,'实变函数课程难度偏大。',NOW(),NOW(),NOW()),
(38,40,15,'2025-2026-1',4,'泛函分析课程设计合理。',NOW(),NOW(),NOW()),
(39,42,19,'2025-2026-1',4,'宏观经济学案例丰富。',NOW(),NOW(),NOW()),
(40,43,22,'2025-2026-1',5,'大学生创新创业课程启发很大。',NOW(),NOW(),NOW()),
(41,44,23,'2025-2026-1',3,'分布式系统课程有一定挑战性。',NOW(),NOW(),NOW()),
(42,45,26,'2025-2026-1',5,'数据挖掘老师讲解透彻。',NOW(),NOW(),NOW()),
(43,46,27,'2025-2026-1',3,'Java企业级开发内容较多。',NOW(),NOW(),NOW()),
(44,47,30,'2025-2026-1',4,'商务英语课程实用性强。',NOW(),NOW(),NOW()),
(45,48,31,'2025-2026-1',5,'体育IV课程活动丰富。',NOW(),NOW(),NOW()),
(46,49,34,'2025-2026-1',4,'财务管理老师讲课清晰。',NOW(),NOW(),NOW()),
(47,50,35,'2025-2026-1',5,'社交礼仪课程非常实用。',NOW(),NOW(),NOW()),
(48,51,2,'2025-2026-1',4,'操作系统原理需加强实践。',NOW(),NOW(),NOW()),
(49,52,37,'2025-2026-1',4,'算法设计与分析课程有挑战。',NOW(),NOW(),NOW()),
(50,53,4,'2025-2026-1',5,'软件工程导论老师经验丰富。',NOW(),NOW(),NOW()),
(51,54,5,'2025-2026-1',3,'计算机网络课程内容较多。',NOW(),NOW(),NOW()),
(52,55,8,'2025-2026-1',4,'人工智能导论拓宽视野。',NOW(),NOW(),NOW()),
(53,56,9,'2025-2026-1',4,'操作系统原理课程安排合理。',NOW(),NOW(),NOW()),
(54,57,12,'2025-2026-1',3,'人机交互设计很有创意。',NOW(),NOW(),NOW()),
(55,58,13,'2025-2026-1',5,'复变函数老师教学水平高。',NOW(),NOW(),NOW()),
(56,59,16,'2025-2026-1',4,'高级英语课程内容丰富。',NOW(),NOW(),NOW()),
(57,60,17,'2025-2026-1',5,'形势与政策课程紧跟时事。',NOW(),NOW(),NOW()),
(58,61,20,'2025-2026-1',4,'市场营销学课程实用。',NOW(),NOW(),NOW()),
(59,62,21,'2025-2026-1',5,'人力资源管理老师讲课生动。',NOW(),NOW(),NOW()),
(60,63,24,'2025-2026-1',3,'计算机图形学有一定难度。',NOW(),NOW(),NOW()),
(61,64,25,'2025-2026-1',4,'嵌入式系统实践丰富。',NOW(),NOW(),NOW()),
(62,65,28,'2025-2026-1',5,'运筹学课程非常实用。',NOW(),NOW(),NOW()),
(63,66,29,'2025-2026-1',4,'天文学基础有趣味性。',NOW(),NOW(),NOW()),
(64,67,32,'2025-2026-1',4,'思政课老师讲解深入浅出。',NOW(),NOW(),NOW()),
(65,68,33,'2025-2026-1',5,'国际贸易实务案例丰富。',NOW(),NOW(),NOW()),
(66,69,36,'2025-2026-1',4,'Web安全课程内容前沿。',NOW(),NOW(),NOW()),
(67,70,37,'2025-2026-1',5,'算法设计与分析非常精彩。',NOW(),NOW(),NOW()),
(68,71,40,'2025-2026-1',3,'偏微分方程课程难度较大。',NOW(),NOW(),NOW()),
(69,72,1,'2025-2026-1',5,'数据结构与算法老师讲解清晰。',NOW(),NOW(),NOW()),
(70,73,4,'2025-2026-1',4,'软件工程导论课程实用。',NOW(),NOW(),NOW()),
(71,74,5,'2025-2026-1',3,'计算机网络内容较多。',NOW(),NOW(),NOW()),
(72,75,8,'2025-2026-1',4,'人工智能导论拓宽视野。',NOW(),NOW(),NOW()),
(73,76,9,'2025-2026-1',4,'操作系统原理课程设计好。',NOW(),NOW(),NOW()),
(74,77,12,'2025-2026-1',5,'人机交互设计课程有创意。',NOW(),NOW(),NOW()),
(75,78,13,'2025-2026-1',4,'复变函数需更多习题。',NOW(),NOW(),NOW()),
(76,79,16,'2025-2026-1',4,'高级英语课程内容丰富。',NOW(),NOW(),NOW()),
(77,80,17,'2025-2026-1',5,'形势与政策紧跟时代。',NOW(),NOW(),NOW()),
(78,81,20,'2025-2026-1',3,'市场营销学需更多案例。',NOW(),NOW(),NOW()),
(79,82,21,'2025-2026-1',5,'人力资源管理课程生动。',NOW(),NOW(),NOW()),
(80,83,24,'2025-2026-1',4,'计算机图形学有挑战性。',NOW(),NOW(),NOW());

-- ========== 更新课程已选人数 ==========
UPDATE course SET selected_count = (
  SELECT COUNT(*) FROM course_selection WHERE course_id = course.id AND status = 0
);

-- ========== 用户数据 ==========
INSERT INTO sys_user (id, username, password, real_name, user_type, user_code, gender, status, create_time, update_time)
SELECT id, username, password, real_name, user_type, user_code, gender, status, create_time, update_time
FROM (
  SELECT 1 as id,'admin' as username,'$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W' as password,'系统管理员' as real_name,3 as user_type,'ADMIN001' as user_code,NULL as gender,1 as status,NOW() as create_time,NOW() as update_time
  UNION ALL
  SELECT id+1, teacher_no, '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', name, 2, teacher_no, CASE WHEN gender='男' THEN 1 ELSE 2 END, 1, NOW(), NOW()
  FROM teacher
) t;

INSERT INTO sys_user (id, username, password, real_name, user_type, user_code, gender, status, create_time, update_time)
SELECT id+16, student_no, '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W', name, 1, student_no, CASE WHEN gender='男' THEN 1 ELSE 2 END, 1, NOW(), NOW()
FROM student;

INSERT INTO sys_user_role VALUES(1,1);
INSERT INTO sys_user_role SELECT id, 2 FROM sys_user WHERE user_type = 2;
INSERT INTO sys_user_role SELECT id, 3 FROM sys_user WHERE user_type = 1;

SET FOREIGN_KEY_CHECKS = 1;
