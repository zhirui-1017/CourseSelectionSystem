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
@RequestMapping("/api/v1/semesters")
public class SemesterController {

    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "id", "id",
            "semesterId", "semester_id",
            "semesterName", "semester_name",
            "startDate", "start_date",
            "endDate", "end_date",
            "isCurrent", "is_current",
            "status", "status",
            "createdAt", "created_at"
    );

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public SemesterController(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping({"", "/list"})
    public Result<PageResult<Map<String, Object>>> list(@RequestParam Map<String, String> params) {
        int pageNum = positiveInt(params.get("pageNum"), 1);
        int pageSize = Math.min(positiveInt(params.get("pageSize"), 20), 100);
        MapSqlParameterSource source = new MapSqlParameterSource()
                .addValue("offset", (pageNum - 1) * pageSize)
                .addValue("pageSize", pageSize);
        String where = semesterWhere(params, source);
        String orderBy = orderBy(params.get("orderByColumn"), params.get("isAsc"), "start_date desc");
        long total = jdbcTemplate.queryForObject("select count(*) from semester" + where, source, Long.class);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select id,
                       semester_id semesterId,
                       semester_name semesterName,
                       start_date startDate,
                       end_date endDate,
                       is_current isCurrent,
                       status,
                       created_at createdAt,
                       updated_at updatedAt
                  from semester
                """ + where + " order by " + orderBy + " limit :pageSize offset :offset", source);
        return Result.success(new PageResult<>(pageNum, pageSize, total, rows));
    }

    @GetMapping("/current")
    public Result<Map<String, Object>> current() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select id,
                       semester_id semesterId,
                       semester_name semesterName,
                       start_date startDate,
                       end_date endDate,
                       is_current isCurrent,
                       status,
                       created_at createdAt,
                       updated_at updatedAt
                  from semester
                 where is_current = 1
                 order by start_date desc
                 limit 1
                """, new MapSqlParameterSource());
        return rows.isEmpty() ? Result.notFound("当前学期未设置") : Result.success(rows.get(0));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> get(@PathVariable Long id) {
        return Result.success(requireSemester(id));
    }

    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        MapSqlParameterSource source = semesterParams(body);
        if (Objects.equals(source.getValue("isCurrent"), 1)) {
            jdbcTemplate.update("update semester set is_current = 0", new MapSqlParameterSource());
        }
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update("""
                insert into semester
                    (semester_id, semester_name, start_date, end_date, is_current, status)
                values
                    (:semesterId, :semesterName, :startDate, :endDate, :isCurrent, :status)
                """, source, keyHolder, new String[]{"id"});
        Number key = Objects.requireNonNull(keyHolder.getKey(), "semester id");
        return Result.success("学期创建成功", requireSemester(key.longValue()));
    }

    @PutMapping("/{id}")
    public Result<Map<String, Object>> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        requireSemester(id);
        MapSqlParameterSource source = semesterParams(body).addValue("id", id);
        if (Objects.equals(source.getValue("isCurrent"), 1)) {
            jdbcTemplate.update("update semester set is_current = 0 where id <> :id", new MapSqlParameterSource("id", id));
        }
        jdbcTemplate.update("""
                update semester
                   set semester_id = :semesterId,
                       semester_name = :semesterName,
                       start_date = :startDate,
                       end_date = :endDate,
                       is_current = :isCurrent,
                       status = :status
                 where id = :id
                """, source);
        return Result.success("学期更新成功", requireSemester(id));
    }

    @PutMapping("/{id}/current")
    public Result<Map<String, Object>> markCurrent(@PathVariable Long id) {
        requireSemester(id);
        jdbcTemplate.update("update semester set is_current = 0", new MapSqlParameterSource());
        jdbcTemplate.update("update semester set is_current = 1, status = 1 where id = :id", new MapSqlParameterSource("id", id));
        return Result.success("当前学期已更新", requireSemester(id));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        int affected = jdbcTemplate.update("delete from semester where id = :id", new MapSqlParameterSource("id", id));
        return affected > 0 ? Result.success(true) : Result.notFound("学期不存在");
    }

    private Map<String, Object> requireSemester(Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select id,
                       semester_id semesterId,
                       semester_name semesterName,
                       start_date startDate,
                       end_date endDate,
                       is_current isCurrent,
                       status,
                       created_at createdAt,
                       updated_at updatedAt
                  from semester
                 where id = :id
                """, new MapSqlParameterSource("id", id));
        if (rows.isEmpty()) {
            throw new BusinessException(Result.NOT_FOUND, "学期不存在");
        }
        return rows.get(0);
    }

    private String semesterWhere(Map<String, String> params, MapSqlParameterSource source) {
        List<String> filters = new ArrayList<>();
        String keyword = text(params.get("keyword"));
        if (keyword != null) {
            filters.add("(semester_id like :keyword or semester_name like :keyword)");
            source.addValue("keyword", "%" + keyword + "%");
        }
        Integer status = intValue(params.get("status"), null);
        if (status != null) {
            filters.add("status = :status");
            source.addValue("status", status);
        }
        Integer isCurrent = intValue(params.get("isCurrent"), null);
        if (isCurrent != null) {
            filters.add("is_current = :isCurrent");
            source.addValue("isCurrent", isCurrent);
        }
        return filters.isEmpty() ? "" : " where " + String.join(" and ", filters);
    }

    private MapSqlParameterSource semesterParams(Map<String, Object> body) {
        return new MapSqlParameterSource()
                .addValue("semesterId", requiredText(body.get("semesterId"), "学期标识不能为空"))
                .addValue("semesterName", requiredText(body.get("semesterName"), "学期名称不能为空"))
                .addValue("startDate", requiredText(body.get("startDate"), "开始日期不能为空"))
                .addValue("endDate", requiredText(body.get("endDate"), "结束日期不能为空"))
                .addValue("isCurrent", booleanValue(body.get("isCurrent")) ? 1 : 0)
                .addValue("status", intValue(body.get("status"), 1));
    }

    private String orderBy(String field, String isAsc, String defaultOrder) {
        String normalized = text(field);
        String column = normalized == null ? null : SORT_COLUMNS.get(normalized);
        if (column == null) {
            return defaultOrder;
        }
        String direction = "false".equalsIgnoreCase(isAsc) || "desc".equalsIgnoreCase(isAsc) ? "desc" : "asc";
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
