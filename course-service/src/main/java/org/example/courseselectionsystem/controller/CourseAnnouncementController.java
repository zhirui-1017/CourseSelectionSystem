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
@RequestMapping("/api/v1/course-announcements")
public class CourseAnnouncementController {

    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "id", "a.id",
            "courseId", "a.course_id",
            "title", "a.title",
            "publishTime", "a.publish_time",
            "createdAt", "a.created_at"
    );

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public CourseAnnouncementController(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping({"", "/list"})
    public Result<PageResult<Map<String, Object>>> list(@RequestParam Map<String, String> params) {
        int pageNum = positiveInt(params.get("pageNum"), 1);
        int pageSize = Math.min(positiveInt(params.get("pageSize"), 10), 100);
        MapSqlParameterSource source = new MapSqlParameterSource()
                .addValue("offset", (pageNum - 1) * pageSize)
                .addValue("pageSize", pageSize);
        String where = announcementWhere(params, source);
        String orderBy = orderBy(params.get("orderByColumn"), params.get("isAsc"));
        long total = jdbcTemplate.queryForObject("select count(*) from course_announcement a " + where, source, Long.class);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select a.id,
                       a.course_id courseId,
                       c.course_code courseCode,
                       c.course_name courseName,
                       a.title,
                       a.content,
                       a.publish_time publishTime,
                       a.created_by createdBy,
                       t.name createdByName,
                       a.created_at createdAt,
                       a.updated_at updatedAt
                  from course_announcement a
                  join course c on c.id = a.course_id
                  left join teacher t on t.id = a.created_by
                """ + where + " order by " + orderBy + " limit :pageSize offset :offset", source);
        return Result.success(new PageResult<>(pageNum, pageSize, total, rows));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> get(@PathVariable Long id) {
        return Result.success(requireAnnouncement(id));
    }

    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        MapSqlParameterSource source = announcementParams(body);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update("""
                insert into course_announcement (course_id, title, content, created_by)
                values (:courseId, :title, :content, :createdBy)
                """, source, keyHolder, new String[]{"id"});
        Number key = Objects.requireNonNull(keyHolder.getKey(), "announcement id");
        return Result.success("公告创建成功", requireAnnouncement(key.longValue()));
    }

    @PutMapping("/{id}")
    public Result<Map<String, Object>> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        requireAnnouncement(id);
        MapSqlParameterSource source = announcementParams(body).addValue("id", id);
        jdbcTemplate.update("""
                update course_announcement
                   set course_id = :courseId,
                       title = :title,
                       content = :content,
                       created_by = :createdBy
                 where id = :id
                """, source);
        return Result.success("公告更新成功", requireAnnouncement(id));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        int affected = jdbcTemplate.update("delete from course_announcement where id = :id", new MapSqlParameterSource("id", id));
        return affected > 0 ? Result.success(true) : Result.notFound("公告不存在");
    }

    private Map<String, Object> requireAnnouncement(Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select a.id,
                       a.course_id courseId,
                       c.course_code courseCode,
                       c.course_name courseName,
                       a.title,
                       a.content,
                       a.publish_time publishTime,
                       a.created_by createdBy,
                       t.name createdByName,
                       a.created_at createdAt,
                       a.updated_at updatedAt
                  from course_announcement a
                  join course c on c.id = a.course_id
                  left join teacher t on t.id = a.created_by
                 where a.id = :id
                """, new MapSqlParameterSource("id", id));
        if (rows.isEmpty()) {
            throw new BusinessException(Result.NOT_FOUND, "公告不存在");
        }
        return rows.get(0);
    }

    private String announcementWhere(Map<String, String> params, MapSqlParameterSource source) {
        List<String> filters = new ArrayList<>();
        Long courseId = longValue(params.get("courseId"));
        if (courseId != null) {
            filters.add("a.course_id = :courseId");
            source.addValue("courseId", courseId);
        }
        Long createdBy = longValue(params.get("createdBy"));
        if (createdBy != null) {
            filters.add("a.created_by = :createdBy");
            source.addValue("createdBy", createdBy);
        }
        String keyword = text(params.get("keyword"));
        if (keyword != null) {
            filters.add("(a.title like :keyword or a.content like :keyword or c.course_name like :keyword)");
            source.addValue("keyword", "%" + keyword + "%");
        }
        return filters.isEmpty() ? "" : " where " + String.join(" and ", filters);
    }

    private MapSqlParameterSource announcementParams(Map<String, Object> body) {
        return new MapSqlParameterSource()
                .addValue("courseId", requiredLong(body.get("courseId"), "课程不能为空"))
                .addValue("title", requiredText(body.get("title"), "公告标题不能为空"))
                .addValue("content", requiredText(body.get("content"), "公告内容不能为空"))
                .addValue("createdBy", requiredLong(body.get("createdBy"), "发布教师不能为空"));
    }

    private String orderBy(String field, String isAsc) {
        String normalized = text(field);
        String column = normalized == null ? "a.publish_time" : SORT_COLUMNS.getOrDefault(normalized, "a.publish_time");
        String direction = "true".equalsIgnoreCase(isAsc) || "asc".equalsIgnoreCase(isAsc) ? "asc" : "desc";
        return column + " " + direction;
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

    private String requiredText(Object value, String message) {
        String text = text(value);
        if (text == null) {
            throw new BusinessException(Result.PARAM_ERROR, message);
        }
        return text;
    }

    private String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return StringUtils.hasText(text) ? text : null;
    }
}
