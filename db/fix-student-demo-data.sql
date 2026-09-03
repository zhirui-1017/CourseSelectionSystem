-- 修复学生端数据：评价表补列 + 为演示学生(1,2,3)补选课/成绩/评价
USE course_selection_system_cloud;

-- 1) course_evaluation 缺少代码所需的列
ALTER TABLE course_evaluation
    ADD COLUMN score DECIMAL(5,1) NULL AFTER rating,
    ADD COLUMN is_anonymous TINYINT NOT NULL DEFAULT 0 AFTER score;

-- 2) 学生1(张三)：增加进行中课程 2,3,5,6,8
INSERT INTO course_selection (student_id, course_id, semester, status, selection_time)
SELECT s, c, '2025-2026-1', 1, NOW()
FROM (SELECT 1 s, 2 c UNION SELECT 1,3 UNION SELECT 1,5 UNION SELECT 1,6 UNION SELECT 1,8) t
WHERE NOT EXISTS (SELECT 1 FROM course_selection x WHERE x.student_id=t.s AND x.course_id=t.c);

-- 3) 学生1(张三)：增加已结课成绩 4,9
INSERT INTO course_selection (student_id, course_id, semester, status, score, score_level, gpa, selection_time)
SELECT s, c, '2025-2026-1', 2, sc, lv, gp, NOW()
FROM (SELECT 1 s,4 c,82.0 sc,'良好' lv,3.4 gp UNION SELECT 1,9,91.0,'优秀',4.0) t
WHERE NOT EXISTS (SELECT 1 FROM course_selection x WHERE x.student_id=t.s AND x.course_id=t.c);

-- 4) 学生2(李四)：进行中 1,4,7；已结课 9
INSERT INTO course_selection (student_id, course_id, semester, status, selection_time)
SELECT s, c, '2025-2026-1', 1, NOW()
FROM (SELECT 2 s,1 c UNION SELECT 2,4 UNION SELECT 2,7) t
WHERE NOT EXISTS (SELECT 1 FROM course_selection x WHERE x.student_id=t.s AND x.course_id=t.c);
INSERT INTO course_selection (student_id, course_id, semester, status, score, score_level, gpa, selection_time)
SELECT s, c, '2025-2026-1', 2, 88.0, '良好', 3.8, NOW()
FROM (SELECT 2 s,9 c) t
WHERE NOT EXISTS (SELECT 1 FROM course_selection x WHERE x.student_id=t.s AND x.course_id=t.c);

-- 5) 学生3(王五)：进行中 3,7,10；已结课 4
INSERT INTO course_selection (student_id, course_id, semester, status, selection_time)
SELECT s, c, '2025-2026-1', 1, NOW()
FROM (SELECT 3 s,3 c UNION SELECT 3,7 UNION SELECT 3,10) t
WHERE NOT EXISTS (SELECT 1 FROM course_selection x WHERE x.student_id=t.s AND x.course_id=t.c);
INSERT INTO course_selection (student_id, course_id, semester, status, score, score_level, gpa, selection_time)
SELECT s, c, '2025-2026-1', 2, 79.0, '中等', 3.0, NOW()
FROM (SELECT 3 s,4 c) t
WHERE NOT EXISTS (SELECT 1 FROM course_selection x WHERE x.student_id=t.s AND x.course_id=t.c);

-- 6) 学生评价（对应已结课，score 与 rating 同一量纲 1-5）
INSERT INTO course_evaluation (student_id, course_id, semester, rating, score, is_anonymous, comment, evaluation_date)
SELECT s, c, '2025-2026-1', r, r, 0, cm, NOW()
FROM (SELECT 1 s,4 c,4 r,'老师授课清晰，收获很大' cm UNION SELECT 1,9,5,'课程内容前沿，讲解生动' UNION SELECT 2,9,4,'内容丰富，作业适中' UNION SELECT 3,4,4,'案例详实，很有帮助') t
WHERE NOT EXISTS (SELECT 1 FROM course_evaluation x WHERE x.student_id=t.s AND x.course_id=t.c);

-- 7) 给三位演示学生补几条系统消息（若缺失）
INSERT INTO message_notification (recipient_id, recipient_type, title, content, message_type, is_read)
SELECT s, 2, '新学期选课提示', '新学期选课已开放，请及时完成选课并查看课表。', 'system', 0
FROM (SELECT 1 s UNION SELECT 2 UNION SELECT 3) t
WHERE NOT EXISTS (SELECT 1 FROM message_notification m WHERE m.recipient_id=t.s AND m.recipient_type=2 AND m.title='新学期选课提示');
