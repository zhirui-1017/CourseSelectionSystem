package org.example.courseselectionsystem.controller;

import org.example.courseselectionsystem.common.Result;
import org.example.courseselectionsystem.exception.BusinessException;
import org.example.courseselectionsystem.vo.PageResult;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/grades")
public class GradeController {

    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "id", "cs.id",
            "studentNo", "s.student_no",
            "studentName", "s.name",
            "courseCode", "c.course_code",
            "courseName", "c.course_name",
            "score", "cs.score",
            "selectionTime", "cs.selection_time",
            "updatedAt", "cs.updated_at"
    );

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public GradeController(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping({"", "/list"})
    public Result<PageResult<Map<String, Object>>> list(@RequestParam Map<String, String> params) {
        int pageNum = positiveInt(params.get("pageNum"), 1);
        int pageSize = Math.min(positiveInt(params.get("pageSize"), 10), 100);
        MapSqlParameterSource source = new MapSqlParameterSource()
                .addValue("offset", (pageNum - 1) * pageSize)
                .addValue("pageSize", pageSize);
        String where = gradeWhere(params, source);
        String orderBy = orderBy(params.get("orderByColumn"), params.get("isAsc"));
        long total = jdbcTemplate.queryForObject("""
                select count(*)
                  from course_selection cs
                  join student s on s.id = cs.student_id
                  join course c on c.id = cs.course_id
                  left join teacher t on t.id = c.teacher_id
                """ + where, source, Long.class);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(gradeSelectSql() + where
                + " order by " + orderBy + " limit :pageSize offset :offset", source);
        return Result.success(new PageResult<>(pageNum, pageSize, total, rows));
    }

    @GetMapping("/{selectionId}")
    public Result<Map<String, Object>> get(@PathVariable Long selectionId) {
        return Result.success(requireGrade(selectionId));
    }

    @PostMapping
    public Result<Map<String, Object>> createOrUpdate(@RequestBody Map<String, Object> body) {
        Long selectionId = longValue(body.get("selectionId"));
        if (selectionId == null) {
            selectionId = findOrCreateSelection(requiredLong(body.get("studentId"), "学生不能为空"),
                    requiredLong(body.get("courseId"), "课程不能为空"));
        }
        return update(selectionId, body);
    }

    @PutMapping("/{selectionId}")
    public Result<Map<String, Object>> update(@PathVariable Long selectionId, @RequestBody Map<String, Object> body) {
        requireGrade(selectionId);
        MapSqlParameterSource source = gradeParams(body).addValue("selectionId", selectionId);
        jdbcTemplate.update("""
                update course_selection
                   set daily_grade = :dailyGrade,
                       lab_grade = :labGrade,
                       exam_grade = :examGrade,
                       score = :score,
                       remark = :remark,
                       updated_at = current_timestamp
                 where id = :selectionId
                """, source);
        return Result.success("成绩保存成功", requireGrade(selectionId));
    }

    @DeleteMapping("/{selectionId}")
    public Result<Map<String, Object>> clear(@PathVariable Long selectionId) {
        requireGrade(selectionId);
        jdbcTemplate.update("""
                update course_selection
                   set daily_grade = null,
                       lab_grade = null,
                       exam_grade = null,
                       score = null,
                       remark = null,
                       updated_at = current_timestamp
                 where id = :selectionId
                """, new MapSqlParameterSource("selectionId", selectionId));
        return Result.success("成绩已清空", requireGrade(selectionId));
    }

    private Long findOrCreateSelection(Long studentId, Long courseId) {
        List<Long> existingIds = jdbcTemplate.queryForList("""
                select id from course_selection where student_id = :studentId and course_id = :courseId limit 1
                """, new MapSqlParameterSource().addValue("studentId", studentId).addValue("courseId", courseId), Long.class);
        if (!existingIds.isEmpty()) {
            return existingIds.get(0);
        }
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update("""
                insert into course_selection (student_id, course_id, status, selection_time)
                values (:studentId, :courseId, 1, current_timestamp)
                """, new MapSqlParameterSource().addValue("studentId", studentId).addValue("courseId", courseId),
                keyHolder, new String[]{"id"});
        Number key = Objects.requireNonNull(keyHolder.getKey(), "selection id");
        return key.longValue();
    }

    private Map<String, Object> requireGrade(Long selectionId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(gradeSelectSql() + " where cs.id = :selectionId",
                new MapSqlParameterSource("selectionId", selectionId));
        if (rows.isEmpty()) {
            throw new BusinessException(Result.NOT_FOUND, "成绩记录不存在");
        }
        return rows.get(0);
    }

    private String gradeSelectSql() {
        return """
                select cs.id selectionId,
                       cs.student_id studentId,
                       s.student_no studentNo,
                       s.name studentName,
                       s.class_name className,
                       cs.course_id courseId,
                       c.course_code courseCode,
                       c.course_name courseName,
                       c.course_type courseType,
                       c.credit,
                       t.name teacherName,
                       cs.daily_grade dailyGrade,
                       cs.lab_grade labGrade,
                       cs.exam_grade examGrade,
                       cs.score,
                       cs.remark,
                       cs.status,
                       cs.selection_time selectionTime,
                       cs.updated_at updatedAt
                  from course_selection cs
                  join student s on s.id = cs.student_id
                  join course c on c.id = cs.course_id
                  left join teacher t on t.id = c.teacher_id
                """;
    }

    private String gradeWhere(Map<String, String> params, MapSqlParameterSource source) {
        List<String> filters = new ArrayList<>();
        addLongFilter(filters, source, "studentId", "cs.student_id", params.get("studentId"));
        addLongFilter(filters, source, "courseId", "cs.course_id", params.get("courseId"));
        addIntFilter(filters, source, "status", "cs.status", params.get("status"));
        String className = text(params.get("className"));
        if (className != null && !"all".equalsIgnoreCase(className)) {
            filters.add("s.class_name = :className");
            source.addValue("className", className);
        }
        String graded = text(params.get("graded"));
        if ("true".equalsIgnoreCase(graded)) {
            filters.add("cs.score is not null");
        } else if ("false".equalsIgnoreCase(graded)) {
            filters.add("cs.score is null");
        }
        String keyword = text(params.get("keyword"));
        if (keyword != null) {
            filters.add("(s.student_no like :keyword or s.name like :keyword or c.course_code like :keyword or c.course_name like :keyword)");
            source.addValue("keyword", "%" + keyword + "%");
        }
        return filters.isEmpty() ? "" : " where " + String.join(" and ", filters);
    }

    private MapSqlParameterSource gradeParams(Map<String, Object> body) {
        Double dailyGrade = gradeValue(body.get("dailyGrade"));
        Double labGrade = gradeValue(body.get("labGrade"));
        Double examGrade = gradeValue(body.get("examGrade"));
        Double score = gradeValue(body.get("score"));
        if (score == null && (dailyGrade != null || labGrade != null || examGrade != null)) {
            score = Math.round(((dailyGrade == null ? 0D : dailyGrade) * 0.4D
                    + (labGrade == null ? 0D : labGrade) * 0.2D
                    + (examGrade == null ? 0D : examGrade) * 0.4D) * 10D) / 10D;
        }
        return new MapSqlParameterSource()
                .addValue("dailyGrade", dailyGrade)
                .addValue("labGrade", labGrade)
                .addValue("examGrade", examGrade)
                .addValue("score", score)
                .addValue("remark", text(body.get("remark")));
    }

    private void addLongFilter(List<String> filters, MapSqlParameterSource source, String name, String column, String value) {
        Long parsed = longValue(value);
        if (parsed != null) {
            filters.add(column + " = :" + name);
            source.addValue(name, parsed);
        }
    }

    private void addIntFilter(List<String> filters, MapSqlParameterSource source, String name, String column, String value) {
        Integer parsed = intValue(value, null);
        if (parsed != null) {
            filters.add(column + " = :" + name);
            source.addValue(name, parsed);
        }
    }

    private String orderBy(String field, String isAsc) {
        String normalized = text(field);
        String column = normalized == null ? "cs.id" : SORT_COLUMNS.getOrDefault(normalized, "cs.id");
        String direction = "true".equalsIgnoreCase(isAsc) || "asc".equalsIgnoreCase(isAsc) ? "asc" : "desc";
        return column + " " + direction;
    }

    private Double gradeValue(Object value) {
        String text = text(value);
        if (text == null) {
            return null;
        }
        double number = Double.parseDouble(text);
        if (number < 0D || number > 100D) {
            throw new BusinessException(Result.PARAM_ERROR, "成绩必须在0到100之间");
        }
        return number;
    }

    private int positiveInt(String value, int defaultValue) {
        Integer parsed = intValue(value, defaultValue);
        return parsed == null || parsed < 1 ? defaultValue : parsed;
    }

    private Integer intValue(Object value, Integer defaultValue) {
        String text = text(value);
        if (text == null || "all".equalsIgnoreCase(text)) {
            return defaultValue;
        }
        return Integer.parseInt(text);
    }

    private Long longValue(Object value) {
        String text = text(value);
        if (text == null || "all".equalsIgnoreCase(text)) {
            return null;
        }
        return Long.parseLong(text);
    }

    private Long requiredLong(Object value, String message) {
        Long parsed = longValue(value);
        if (parsed == null) {
            throw new BusinessException(Result.PARAM_ERROR, message);
        }
        return parsed;
    }

    private String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return StringUtils.hasText(text) ? text : null;
    }
}
