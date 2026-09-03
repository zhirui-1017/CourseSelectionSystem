package org.example.courseselectionsystem.ai.tool;

import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * AI 助手数据工具（微服务版）
 * 说明：旧单体版工具依赖整套业务 Service；微服务版改为本服务直接用 JdbcTemplate
 * 连接同一业务库（course_selection_system_cloud），按角色提供查询能力。
 */
@Component
public class AssistantDataTools {

    private static final Logger log = LoggerFactory.getLogger(AssistantDataTools.class);

    private final JdbcTemplate jdbcTemplate;

    public AssistantDataTools(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Tool("获取当前日期时间（今天的日期、星期几、当前时间）")
    public String getCurrentDateTime() {
        LocalDateTime now = LocalDateTime.now();
        String week = "星期" + "一二三四五六日".charAt(now.getDayOfWeek().getValue() - 1);
        return now.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")) + "，" + week + "，" + now.toLocalTime().toString();
    }

    @Tool("按关键词搜索启用中的课程，关键词可为课程名/课程编号/课程类型")
    public String searchCourses(String keyword) {
        String kw = (keyword == null ? "" : keyword.trim());
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                    SELECT c.course_code courseCode, c.course_name courseName, c.course_type courseType,
                           c.credit, c.schedule, c.classroom, c.available_slots capacity, c.selected_count selectedCount,
                           t.name teacherName
                      FROM course c LEFT JOIN teacher t ON t.id = c.teacher_id
                     WHERE c.status = 1
                       AND (:kw = '' OR c.course_name LIKE CONCAT('%',:kw,'%')
                            OR c.course_code LIKE CONCAT('%',:kw,'%')
                            OR c.course_type LIKE CONCAT('%',:kw,'%'))
                     ORDER BY c.id LIMIT 10
                    """, Map.of("kw", kw));
            return rows.isEmpty() ? "暂无匹配课程" : formatRows(rows);
        } catch (Exception e) {
            log.warn("searchCourses error", e);
            return "查询课程失败：" + e.getMessage();
        }
    }

    @Tool("查询某学生的课表（当前学期已选课程的上课时间/地点）")
    public String getStudentSchedule(Long studentId) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                    SELECT c.course_name courseName, c.course_code courseCode, c.schedule, c.classroom, c.credit
                      FROM course_selection cs JOIN course c ON c.id = cs.course_id
                     WHERE cs.student_id = ? AND cs.status = 1 ORDER BY c.schedule
                    """, studentId);
            return rows.isEmpty() ? "该学生暂无课表数据" : formatRows(rows);
        } catch (Exception e) {
            return "查询课表失败：" + e.getMessage();
        }
    }

    @Tool("查询某学生的各科成绩与绩点")
    public String getStudentGrades(Long studentId) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                    SELECT c.course_name courseName, c.course_code courseCode, c.credit,
                           cs.score, cs.score_level scoreLevel, cs.gpa
                      FROM course_selection cs JOIN course c ON c.id = cs.course_id
                     WHERE cs.student_id = ? AND cs.status IN (2,3) AND cs.score IS NOT NULL
                     ORDER BY cs.id
                    """, studentId);
            return rows.isEmpty() ? "暂无成绩数据" : formatRows(rows);
        } catch (Exception e) {
            return "查询成绩失败：" + e.getMessage();
        }
    }

    @Tool("查询某教师所授课程及选课人数")
    public String getTeacherCourses(Long teacherId) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                    SELECT c.course_code courseCode, c.course_name courseName, c.schedule, c.classroom,
                           c.selected_count selectedCount, c.available_slots capacity
                      FROM course c WHERE c.teacher_id = ? AND c.status = 1 ORDER BY c.id
                    """, teacherId);
            return rows.isEmpty() ? "该教师暂无授课课程" : formatRows(rows);
        } catch (Exception e) {
            return "查询授课课程失败：" + e.getMessage();
        }
    }

    @Tool("查询某门课程的选课学生名单")
    public String getCourseStudents(Long courseId) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                    SELECT s.student_no studentNo, s.name studentName, s.class_name className,
                           cs.score, cs.status
                      FROM course_selection cs JOIN student s ON s.id = cs.student_id
                     WHERE cs.course_id = ? AND cs.status = 1 ORDER BY s.student_no
                    """, courseId);
            return rows.isEmpty() ? "该课程暂无学生" : formatRows(rows);
        } catch (Exception e) {
            return "查询课程学生失败：" + e.getMessage();
        }
    }

    @Tool("获取系统运营统计（学生/教师/课程/选课/评价数量，当前学期）")
    public String getSystemStats() {
        try {
            Long students = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM student", Long.class);
            Long teachers = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM teacher", Long.class);
            Long courses = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM course", Long.class);
            Long selections = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM course_selection", Long.class);
            Long evaluations = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM course_evaluation", Long.class);
            return String.format(
                    "学生总数：%d；教师总数：%d；课程总数：%d；选课记录数：%d；课程评价数：%d",
                    nvl(students), nvl(teachers), nvl(courses), nvl(selections), nvl(evaluations));
        } catch (Exception e) {
            return "统计失败：" + e.getMessage();
        }
    }

    private static long nvl(Long v) {
        return v == null ? 0L : v;
    }

    private static String formatRows(List<Map<String, Object>> rows) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> row : rows) {
            List<String> cells = new java.util.ArrayList<>();
            for (Map.Entry<String, Object> e : row.entrySet()) {
                cells.add(e.getKey() + "=" + String.valueOf(e.getValue()));
            }
            sb.append("- ").append(String.join(", ", cells)).append("\n");
        }
        return sb.toString().trim();
    }
}
