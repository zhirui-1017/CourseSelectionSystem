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
@RequestMapping("/api/v1/evaluations")
public class CourseEvaluationController {

    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "id", "e.id",
            "courseId", "e.course_id",
            "studentId", "e.student_id",
            "score", "e.score",
            "evaluationTime", "e.evaluation_time",
            "status", "e.status"
    );

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public CourseEvaluationController(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping({"", "/list"})
    public Result<PageResult<Map<String, Object>>> list(@RequestParam Map<String, String> params) {
        int pageNum = positiveInt(params.get("pageNum"), 1);
        int pageSize = Math.min(positiveInt(params.get("pageSize"), 10), 100);
        MapSqlParameterSource source = new MapSqlParameterSource()
                .addValue("offset", (pageNum - 1) * pageSize)
                .addValue("pageSize", pageSize);
        String where = evaluationWhere(params, source);
        String orderBy = orderBy(params.get("orderByColumn"), params.get("isAsc"));
        long total = jdbcTemplate.queryForObject("select count(*) from course_evaluation e join course c on c.id = e.course_id join student s on s.id = e.student_id " + where,
                source, Long.class);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select e.id,
                       e.course_id courseId,
                       c.course_code courseCode,
                       c.course_name courseName,
                       c.course_type courseType,
                       t.name teacherName,
                       e.student_id studentId,
                       s.student_no studentNo,
                       case when e.is_anonymous = 1 then '匿名学生' else s.name end studentName,
                       e.score,
                       e.content,
                       e.evaluation_time evaluationTime,
                       e.is_anonymous isAnonymous,
                       e.status,
                       e.created_at createdAt,
                       e.updated_at updatedAt
                  from course_evaluation e
                  join course c on c.id = e.course_id
                  left join teacher t on t.id = c.teacher_id
                  join student s on s.id = e.student_id
                """ + where + " order by " + orderBy + " limit :pageSize offset :offset", source);
        return Result.success(new PageResult<>(pageNum, pageSize, total, rows));
    }

    @GetMapping("/student/{studentId}/courses")
    public Result<List<Map<String, Object>>> studentEvaluationCourses(@PathVariable Long studentId,
                                                                      @RequestParam(required = false) Integer status) {
        MapSqlParameterSource source = new MapSqlParameterSource()
                .addValue("studentId", studentId)
                .addValue("status", status == null ? 1 : status);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select cs.id selectionId,
                       c.id courseId,
                       c.course_code courseCode,
                       c.course_name courseName,
                       c.course_type courseType,
                       c.credit,
                       c.schedule,
                       c.classroom,
                       t.name teacherName,
                       cs.selection_time selectionTime,
                       e.id evaluationId,
                       e.score evaluationScore,
                       e.content evaluationContent,
                       e.evaluation_time evaluationTime,
                       e.is_anonymous isAnonymous,
                       e.status evaluationStatus,
                       case when e.id is null then 0 else 1 end evaluated
                  from course_selection cs
                  join course c on c.id = cs.course_id
                  left join teacher t on t.id = c.teacher_id
                  left join course_evaluation e on e.course_id = c.id and e.student_id = cs.student_id
                 where cs.student_id = :studentId and cs.status = :status
                 order by cs.selection_time desc
                """, source);
        return Result.success(rows);
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> get(@PathVariable Long id) {
        return Result.success(requireEvaluation(id));
    }

    @PostMapping
    public Result<Map<String, Object>> createOrUpdate(@RequestBody Map<String, Object> body) {
        Long courseId = requiredLong(body.get("courseId"), "课程不能为空");
        Long studentId = requiredLong(body.get("studentId"), "学生不能为空");
        Long existingId = existingEvaluationId(courseId, studentId);
        if (existingId != null) {
            return update(existingId, body);
        }

        MapSqlParameterSource source = evaluationParams(body)
                .addValue("courseId", courseId)
                .addValue("studentId", studentId);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update("""
                insert into course_evaluation (course_id, student_id, score, content, is_anonymous, status)
                values (:courseId, :studentId, :score, :content, :isAnonymous, :status)
                """, source, keyHolder, new String[]{"id"});
        Number key = Objects.requireNonNull(keyHolder.getKey(), "evaluation id");
        return Result.success("评价提交成功", requireEvaluation(key.longValue()));
    }

    @PutMapping("/{id}")
    public Result<Map<String, Object>> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        requireEvaluation(id);
        MapSqlParameterSource source = evaluationParams(body).addValue("id", id);
        jdbcTemplate.update("""
                update course_evaluation
                   set score = :score,
                       content = :content,
                       is_anonymous = :isAnonymous,
                       status = :status,
                       evaluation_time = current_timestamp
                 where id = :id
                """, source);
        return Result.success("评价更新成功", requireEvaluation(id));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        int affected = jdbcTemplate.update("delete from course_evaluation where id = :id", new MapSqlParameterSource("id", id));
        return affected > 0 ? Result.success(true) : Result.notFound("评价不存在");
    }

    private Map<String, Object> requireEvaluation(Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select e.id,
                       e.course_id courseId,
                       c.course_code courseCode,
                       c.course_name courseName,
                       c.course_type courseType,
                       t.name teacherName,
                       e.student_id studentId,
                       s.student_no studentNo,
                       case when e.is_anonymous = 1 then '匿名学生' else s.name end studentName,
                       e.score,
                       e.content,
                       e.evaluation_time evaluationTime,
                       e.is_anonymous isAnonymous,
                       e.status,
                       e.created_at createdAt,
                       e.updated_at updatedAt
                  from course_evaluation e
                  join course c on c.id = e.course_id
                  left join teacher t on t.id = c.teacher_id
                  join student s on s.id = e.student_id
                 where e.id = :id
                """, new MapSqlParameterSource("id", id));
        if (rows.isEmpty()) {
            throw new BusinessException(Result.NOT_FOUND, "评价不存在");
        }
        return rows.get(0);
    }

    private Long existingEvaluationId(Long courseId, Long studentId) {
        List<Long> rows = jdbcTemplate.queryForList("""
                select id from course_evaluation where course_id = :courseId and student_id = :studentId
                """, new MapSqlParameterSource().addValue("courseId", courseId).addValue("studentId", studentId), Long.class);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private String evaluationWhere(Map<String, String> params, MapSqlParameterSource source) {
        List<String> filters = new ArrayList<>();
        addLongFilter(filters, source, "courseId", "e.course_id", params.get("courseId"));
        addLongFilter(filters, source, "studentId", "e.student_id", params.get("studentId"));
        addIntFilter(filters, source, "status", "e.status", params.get("status"));
        String keyword = text(params.get("keyword"));
        if (keyword != null) {
            filters.add("(c.course_name like :keyword or c.course_code like :keyword or s.name like :keyword or e.content like :keyword)");
            source.addValue("keyword", "%" + keyword + "%");
        }
        return filters.isEmpty() ? "" : " where " + String.join(" and ", filters);
    }

    private MapSqlParameterSource evaluationParams(Map<String, Object> body) {
        int score = intValue(body.get("score"), 0);
        if (score < 1 || score > 5) {
            throw new BusinessException(Result.PARAM_ERROR, "评分必须在1到5之间");
        }
        return new MapSqlParameterSource()
                .addValue("score", score)
                .addValue("content", text(body.get("content")))
                .addValue("isAnonymous", booleanValue(body.get("isAnonymous")) ? 1 : 0)
                .addValue("status", intValue(body.get("status"), 1));
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
        String column = normalized == null ? "e.evaluation_time" : SORT_COLUMNS.getOrDefault(normalized, "e.evaluation_time");
        String direction = "true".equalsIgnoreCase(isAsc) || "asc".equalsIgnoreCase(isAsc) ? "asc" : "desc";
        return column + " " + direction;
    }

    private boolean booleanValue(Object value) {
        String text = text(value);
        return "1".equals(text) || "true".equalsIgnoreCase(text) || "on".equalsIgnoreCase(text);
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
