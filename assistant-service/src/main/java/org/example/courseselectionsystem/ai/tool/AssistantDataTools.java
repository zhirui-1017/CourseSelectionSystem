package org.example.courseselectionsystem.ai.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AI 助手数据工具（微服务版 · 完整工具集）
 * 旧版单体依赖整套业务 Service；本服务直接以 JdbcTemplate 连接同一业务库 course_selection_system_cloud，
 * 按学生/教师/管理员三类角色提供查询。所有方法均带异常兜底，失败时返回可读错误信息，不中断对话。
 */
@Component
public class AssistantDataTools {

    private static final Logger log = LoggerFactory.getLogger(AssistantDataTools.class);

    private final JdbcTemplate jdbc;

    public AssistantDataTools(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ==================== 通用 ====================

    @Tool("获取当前真实的日期、星期几、时间。问今天是什么日期/星期几/现在几点/今天有什么课等涉及时效问题时必须先调用")
    public String getCurrentDateTime() {
        LocalDateTime now = LocalDateTime.now();
        String week = "星期" + "一二三四五六日".charAt(now.getDayOfWeek().getValue() - 1);
        return now.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")) + "，" + week + "，" + now.toLocalTime().toString();
    }

    // ==================== 学生 ====================

    @Tool("搜索课程：支持按关键词(课程名/编号)、学分范围、课程类型、教师名查询，参数均可选")
    public String searchCourses(@P("关键词(课程名/编号，可选)") String keyword,
                                @P("最低学分(可选)") Double minCredit,
                                @P("最高学分(可选)") Double maxCredit,
                                @P("课程类型(必修课/选修课/通识课/专业课，可选)") String courseType,
                                @P("教师姓名(可选)") String teacherName) {
        try {
            List<Object> args = new ArrayList<>();
            StringBuilder sql = new StringBuilder("""
                    SELECT c.course_code courseCode, c.course_name courseName, c.course_type courseType,
                           c.credit, c.schedule, c.classroom, c.available_slots capacity, c.selected_count selectedCount,
                           t.name teacherName
                      FROM course c LEFT JOIN teacher t ON t.id = c.teacher_id
                     WHERE c.status = 1
                    """);
            if (hasText(keyword)) {
                sql.append(" AND (c.course_name LIKE ? OR c.course_code LIKE ? OR c.course_type LIKE ?)");
                String kw = "%" + keyword.trim() + "%";
                args.add(kw); args.add(kw); args.add(kw);
            }
            if (minCredit != null) { sql.append(" AND c.credit >= ?"); args.add(minCredit); }
            if (maxCredit != null) { sql.append(" AND c.credit <= ?"); args.add(maxCredit); }
            if (hasText(courseType)) { sql.append(" AND c.course_type = ?"); args.add(courseType.trim()); }
            if (hasText(teacherName)) { sql.append(" AND t.name LIKE ?"); args.add("%" + teacherName.trim() + "%"); }
            sql.append(" ORDER BY c.selected_count DESC LIMIT 10");
            List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), args.toArray());
            return rows.isEmpty() ? "暂无匹配课程" : formatRows(rows);
        } catch (Exception e) {
            log.warn("searchCourses error", e);
            return "查询课程失败：" + safe(e);
        }
    }

    @Tool("根据学生已选课程，推荐适合的选修课(按选课热度与评价分排序，未选的)")
    public String recommendCourses(@P("学生ID") Long studentId) {
        try {
            List<Map<String, Object>> rows = jdbc.queryForList("""
                    SELECT c.course_code courseCode, c.course_name courseName, c.credit, c.schedule,
                           c.classroom, c.selected_count selectedCount, t.name teacherName,
                           ROUND(AVG(e.score),1) avgScore
                      FROM course c
                      LEFT JOIN teacher t ON t.id = c.teacher_id
                      LEFT JOIN course_evaluation e ON e.course_id = c.id
                     WHERE c.status = 1 AND c.course_type LIKE '%选修%'
                       AND c.id NOT IN (
                            SELECT course_id FROM course_selection
                             WHERE student_id = ? AND status IN (1,3))
                     GROUP BY c.id, c.course_code, c.course_name, c.credit, c.schedule,
                              c.classroom, c.selected_count, t.name
                     ORDER BY c.selected_count DESC LIMIT 5
                    """, studentId);
            return rows.isEmpty() ? "暂无可推荐的选修课" : formatRows(rows);
        } catch (Exception e) {
            log.warn("recommendCourses error", e);
            return "推荐失败：" + safe(e);
        }
    }

    @Tool("查询学生各科成绩与绩点(含加权平均分、最低分科目)")
    public String getMyGrades(@P("学生ID") Long studentId) {
        try {
            List<Map<String, Object>> rows = jdbc.queryForList("""
                    SELECT c.course_name courseName, c.course_code courseCode, c.credit,
                           cs.score, cs.score_level scoreLevel, cs.gpa
                      FROM course_selection cs JOIN course c ON c.id = cs.course_id
                     WHERE cs.student_id = ? AND cs.score IS NOT NULL
                     ORDER BY cs.id
                    """, studentId);
            if (rows.isEmpty()) return "暂无成绩数据";
            StringBuilder sb = new StringBuilder(formatRows(rows)).append("\n");
            sb.append("加权平均分：" + avgScore(studentId) + "\n");
            sb.append("最低分科目：" + minScoreCourse(studentId));
            return sb.toString();
        } catch (Exception e) {
            log.warn("getMyGrades error", e);
            return "查询成绩失败：" + safe(e);
        }
    }

    @Tool("查询学生课程表安排(已选课程的上课时间与地点)")
    public String getMySchedule(@P("学生ID") Long studentId) {
        try {
            List<Map<String, Object>> rows = jdbc.queryForList("""
                    SELECT c.course_name courseName, c.course_code courseCode, c.schedule, c.classroom, c.credit
                      FROM course_selection cs JOIN course c ON c.id = cs.course_id
                     WHERE cs.student_id = ? AND cs.status = 1 ORDER BY c.schedule
                    """, studentId);
            return rows.isEmpty() ? "暂无课表数据" : formatRows(rows);
        } catch (Exception e) {
            log.warn("getMySchedule error", e);
            return "查询课表失败：" + safe(e);
        }
    }

    @Tool("查询某门课程的学生评价汇总(平均评分与精选评论)")
    public String getCourseEvaluations(@P("课程名称或课程ID") String courseIdentifier) {
        try {
            Long courseId = resolveCourseId(courseIdentifier);
            if (courseId == null) return "未找到该课程";
            Map<String, Object> stat = jdbc.queryForMap("""
                    SELECT COUNT(*) cnt, ROUND(AVG(score),1) avgScore, MIN(score) minScore, MAX(score) maxScore
                      FROM course_evaluation WHERE course_id = ?
                    """, courseId);
            List<Map<String, Object>> comments = jdbc.queryForList("""
                    SELECT score, content FROM course_evaluation
                     WHERE course_id = ? AND content IS NOT NULL AND content <> ''
                     ORDER BY score DESC LIMIT 3
                    """, courseId);
            StringBuilder sb = new StringBuilder();
            sb.append("评价数：").append(stat.get("cnt")).append("；平均分：").append(stat.get("avgScore"))
                    .append("；最低：").append(stat.get("minScore")).append("；最高：").append(stat.get("maxScore"));
            if (!comments.isEmpty()) {
                sb.append("\n精选评论：\n").append(formatRows(comments));
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("getCourseEvaluations error", e);
            return "查询课程评价失败：" + safe(e);
        }
    }

    // ==================== 教师 ====================

    @Tool("获取当前教师所授课程及选课人数")
    public String getTeacherCourses(@P("教师ID") Long teacherId) {
        try {
            List<Map<String, Object>> rows = jdbc.queryForList("""
                    SELECT c.course_code courseCode, c.course_name courseName, c.schedule, c.classroom,
                           c.selected_count selectedCount, c.available_slots capacity
                      FROM course c WHERE c.teacher_id = ? AND c.status = 1 ORDER BY c.id
                    """, teacherId);
            return rows.isEmpty() ? "该教师暂无授课课程" : formatRows(rows);
        } catch (Exception e) {
            return "查询授课课程失败：" + safe(e);
        }
    }

    @Tool("分析指定课程的班级成绩分布：优秀/良好/中等/及格/不及格人数、平均/最高/最低分")
    public String getClassGrades(@P("教师ID") Long teacherId,
                                 @P("课程名称或课程ID") String courseIdentifier) {
        try {
            Long courseId = resolveTeacherCourseId(teacherId, courseIdentifier);
            if (courseId == null) return "未找到该教师名下的课程";
            Map<String, Object> stat = jdbc.queryForMap("""
                    SELECT COUNT(*) total,
                           SUM(score>=90) a, SUM(score>=80 AND score<90) b,
                           SUM(score>=70 AND score<80) c, SUM(score>=60 AND score<70) d,
                           SUM(score<60) f,
                           ROUND(AVG(score),1) avgScore, MAX(score) maxScore, MIN(score) minScore
                      FROM course_selection WHERE course_id = ? AND score IS NOT NULL
                    """, courseId);
            return "成绩分布（共 " + stat.get("total") + " 人）：优秀=" + stat.get("a")
                    + " 良好=" + stat.get("b") + " 中等=" + stat.get("c")
                    + " 及格=" + stat.get("d") + " 不及格=" + stat.get("f")
                    + "；平均分=" + stat.get("avgScore") + " 最高=" + stat.get("maxScore")
                    + " 最低=" + stat.get("minScore");
        } catch (Exception e) {
            log.warn("getClassGrades error", e);
            return "分析成绩分布失败：" + safe(e);
        }
    }

    @Tool("查询某位学生在所有课程中的成绩情况(按学号或姓名)")
    public String getStudentAllGrades(@P("学生学号或姓名") String studentIdentifier) {
        try {
            Long studentId = resolveStudentId(studentIdentifier);
            if (studentId == null) return "未找到该学生";
            List<Map<String, Object>> rows = jdbc.queryForList("""
                    SELECT c.course_name courseName, c.course_code courseCode, c.credit,
                           cs.score, cs.score_level scoreLevel
                      FROM course_selection cs JOIN course c ON c.id = cs.course_id
                     WHERE cs.student_id = ? AND cs.score IS NOT NULL ORDER BY cs.id
                    """, studentId);
            return rows.isEmpty() ? "该学生暂无成绩" : formatRows(rows);
        } catch (Exception e) {
            return "查询学生成绩失败：" + safe(e);
        }
    }

    @Tool("列出某门课程的选课学生(学号/姓名/班级/成绩)")
    public String getCourseStudentList(@P("教师ID") Long teacherId,
                                       @P("课程名称或课程ID") String courseIdentifier) {
        try {
            Long courseId = resolveTeacherCourseId(teacherId, courseIdentifier);
            if (courseId == null) return "未找到该教师名下的课程";
            List<Map<String, Object>> rows = jdbc.queryForList("""
                    SELECT s.student_no studentNo, s.name studentName, s.class_name className,
                           cs.score, cs.status
                      FROM course_selection cs JOIN student s ON s.id = cs.student_id
                     WHERE cs.course_id = ? AND cs.status IN (1,3)
                     ORDER BY s.student_no
                    """, courseId);
            return rows.isEmpty() ? "该课程暂无学生" : formatRows(rows);
        } catch (Exception e) {
            return "查询课程学生失败：" + safe(e);
        }
    }

    @Tool("在教师授课范围内搜索学生，可按姓名/学号/班级/所选课程名搜索")
    public String searchStudents(@P("教师ID") Long teacherId,
                                 @P("关键词(姓名/学号/班级/课程名)") String keyword) {
        try {
            String kw = hasText(keyword) ? "%" + keyword.trim() + "%" : "%%";
            List<Map<String, Object>> rows = jdbc.queryForList("""
                    SELECT DISTINCT s.student_no studentNo, s.name studentName, s.class_name className
                      FROM course_selection cs
                      JOIN student s ON s.id = cs.student_id
                      JOIN course c ON c.id = cs.course_id
                     WHERE c.teacher_id = ? AND cs.status IN (1,3)
                       AND (s.student_no LIKE ? OR s.name LIKE ? OR s.class_name LIKE ? OR c.course_name LIKE ?)
                     ORDER BY s.student_no LIMIT 20
                    """, teacherId, kw, kw, kw, kw);
            return rows.isEmpty() ? "授课范围内暂无匹配学生" : formatRows(rows);
        } catch (Exception e) {
            return "搜索学生失败：" + safe(e);
        }
    }

    // ==================== 管理员 ====================

    @Tool("获取系统运营统计：总览/课程热度/异常 等维度")
    public String getSystemStats(@P("统计维度，如 '总览'、'课程热度'") String dimension) {
        try {
            String dim = dimension == null ? "" : dimension;
            if (dim.contains("热度")) {
                List<Map<String, Object>> rows = jdbc.queryForList("""
                        SELECT c.course_name courseName, c.course_code courseCode, c.selected_count selectedCount
                          FROM course c ORDER BY c.selected_count DESC LIMIT 5
                        """);
                return rows.isEmpty() ? "暂无数据" : "热门课程 TOP5：\n" + formatRows(rows);
            }
            Long students = jdbc.queryForObject("SELECT COUNT(*) FROM student", Long.class);
            Long teachers = jdbc.queryForObject("SELECT COUNT(*) FROM teacher", Long.class);
            Long courses = jdbc.queryForObject("SELECT COUNT(*) FROM course", Long.class);
            Long selections = jdbc.queryForObject("SELECT COUNT(*) FROM course_selection", Long.class);
            Long evaluations = jdbc.queryForObject("SELECT COUNT(*) FROM course_evaluation", Long.class);
            return String.format("学生总数：%d；教师总数：%d；课程总数：%d；选课记录数：%d；课程评价数：%d",
                    nvl(students), nvl(teachers), nvl(courses), nvl(selections), nvl(evaluations));
        } catch (Exception e) {
            log.warn("getSystemStats error", e);
            return "统计失败：" + safe(e);
        }
    }

    @Tool("检测系统异常：无人选的空闲课程、无成绩可录的异常等")
    public String checkAnomalies() {
        try {
            List<Map<String, Object>> empty = jdbc.queryForList("""
                    SELECT course_code courseCode, course_name courseName, selected_count selectedCount
                      FROM course WHERE status = 1 AND selected_count = 0 ORDER BY id LIMIT 10
                    """);
            Long noScore = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM course_selection WHERE score IS NULL AND status = 1", Long.class);
            StringBuilder sb = new StringBuilder();
            sb.append("无人选中的空闲课程数：").append(empty.size());
            if (!empty.isEmpty()) sb.append("\n空闲课程：\n").append(formatRows(empty));
            sb.append("\n暂无成绩的在修选课记录数：").append(nvl(noScore));
            return sb.toString();
        } catch (Exception e) {
            return "异常检测失败：" + safe(e);
        }
    }

    @Tool("获取最近的操作日志")
    public String getRecentLogs(@P("日志数量，默认10") int count) {
        try {
            int n = Math.max(1, Math.min(count <= 0 ? 10 : count, 50));
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT * FROM operation_log ORDER BY id DESC LIMIT " + n);
            return rows.isEmpty() ? "暂无操作日志" : formatRows(rows);
        } catch (Exception e) {
            log.warn("getRecentLogs error", e);
            return "查询操作日志失败：" + safe(e);
        }
    }

    @Tool("获取当前学期信息")
    public String getCurrentSemester() {
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT semester_name semesterName, is_current isCurrent FROM semester WHERE is_current = 1 LIMIT 1");
            return rows.isEmpty() ? "暂无当前学期" : formatRows(rows);
        } catch (Exception e) {
            return "查询学期失败：" + safe(e);
        }
    }

    // ==================== 内部辅助 ====================

    private String avgScore(Long studentId) {
        try {
            Map<String, Object> m = jdbc.queryForMap("""
                    SELECT ROUND(AVG(score),1) a FROM course_selection WHERE student_id=? AND score IS NOT NULL
                    """, studentId);
            return String.valueOf(m.get("a"));
        } catch (Exception e) {
            return "-";
        }
    }

    private String minScoreCourse(Long studentId) {
        try {
            List<Map<String, Object>> rows = jdbc.queryForList("""
                    SELECT c.course_name courseName, cs.score FROM course_selection cs
                      JOIN course c ON c.id = cs.course_id
                     WHERE cs.student_id=? AND cs.score IS NOT NULL ORDER BY cs.score ASC LIMIT 1
                    """, studentId);
            return rows.isEmpty() ? "-" : rows.get(0).get("courseName") + " " + rows.get(0).get("score");
        } catch (Exception e) {
            return "-";
        }
    }

    private Long resolveCourseId(String identifier) {
        if (identifier == null) return null;
        String id = identifier.trim();
        if (id.matches("\\d+")) {
            List<Long> ids = jdbc.queryForList(
                    "SELECT id FROM course WHERE id = ?", Long.class, Long.valueOf(id));
            return ids.isEmpty() ? null : ids.get(0);
        }
        List<Long> ids = jdbc.queryForList(
                "SELECT id FROM course WHERE course_code = ? OR course_name LIKE ? ORDER BY id LIMIT 1",
                Long.class, id, "%" + id + "%");
        return ids.isEmpty() ? null : ids.get(0);
    }

    private Long resolveTeacherCourseId(Long teacherId, String identifier) {
        if (identifier == null || teacherId == null) return null;
        String id = identifier.trim();
        List<Long> ids;
        if (id.matches("\\d+")) {
            ids = jdbc.queryForList(
                    "SELECT id FROM course WHERE teacher_id = ? AND id = ?", Long.class, teacherId, Long.valueOf(id));
        } else {
            ids = jdbc.queryForList(
                    "SELECT id FROM course WHERE teacher_id = ? AND (course_code = ? OR course_name LIKE ?) ORDER BY id LIMIT 1",
                    Long.class, teacherId, id, "%" + id + "%");
        }
        return ids.isEmpty() ? null : ids.get(0);
    }

    private Long resolveStudentId(String identifier) {
        if (identifier == null) return null;
        String id = identifier.trim();
        List<Long> ids;
        if (id.matches("\\d+")) {
            ids = jdbc.queryForList(
                    "SELECT id FROM student WHERE student_no = ? OR id = ? ORDER BY id LIMIT 1",
                    Long.class, id, Long.valueOf(id));
        } else {
            ids = jdbc.queryForList(
                    "SELECT id FROM student WHERE name = ? ORDER BY id LIMIT 1", Long.class, id);
        }
        return ids.isEmpty() ? null : ids.get(0);
    }

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static long nvl(Long v) {
        return v == null ? 0L : v;
    }

    private static String safe(Exception e) {
        String m = e.getMessage();
        return m == null ? e.getClass().getSimpleName() : m;
    }

    private static String formatRows(List<Map<String, Object>> rows) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> row : rows) {
            List<String> cells = new ArrayList<>();
            for (Map.Entry<String, Object> e : row.entrySet()) {
                cells.add(e.getKey() + "=" + String.valueOf(e.getValue()));
            }
            sb.append("- ").append(String.join(", ", cells)).append("\n");
        }
        return sb.toString().trim();
    }
}
