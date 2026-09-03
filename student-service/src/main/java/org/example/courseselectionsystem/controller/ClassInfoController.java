package org.example.courseselectionsystem.controller;

import org.example.courseselectionsystem.common.Result;
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
@RequestMapping("/api/v1/classes")
public class ClassInfoController {

    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "id", "c.id",
            "classCode", "c.class_code",
            "className", "c.class_name",
            "grade", "c.grade",
            "studentCount", "c.total_students",
            "status", "c.status",
            "createTime", "c.create_time",
            "updatedAt", "c.update_time"
    );

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ClassInfoController(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping({"", "/list"})
    public Result<PageResult<Map<String, Object>>> list(@RequestParam Map<String, String> params) {
        int pageNum = positiveInt(params.get("pageNum"), 1);
        int pageSize = Math.min(positiveInt(params.get("pageSize"), 10), 1000);
        MapSqlParameterSource source = new MapSqlParameterSource()
                .addValue("offset", (pageNum - 1) * pageSize)
                .addValue("pageSize", pageSize);
        String where = classWhere(params, source);
        String orderBy = orderBy(params.get("orderByColumn"), params.get("isAsc"), "c.id");

        long total = jdbcTemplate.queryForObject("select count(*) from class_info c left join college co on co.id = c.college_id " + where, source, Long.class);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select c.id,
                       c.class_code classCode,
                       c.class_name className,
                       c.college_id collegeId,
                       co.college_name collegeName,
                       c.major_id majorId,
                       m.major_name majorName,
                       c.grade,
                       c.head_teacher headTeacherName,
                       c.total_students studentCount,
                       c.monitor_id monitorId,
                       mon.name monitorName,
                       c.contact_phone contactPhone,
                       c.create_time createTime,
                       c.status,
                       c.update_time updatedAt
                  from class_info c
                  left join college co on co.id = c.college_id
                  left join major m on m.id = c.major_id
                  left join student mon on mon.id = c.monitor_id
                """ + where + " order by " + orderBy + " limit :pageSize offset :offset", source);
        return Result.success(new PageResult<>(pageNum, pageSize, total, rows));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> get(@PathVariable Long id) {
        return Result.success(requireClass(id));
    }

    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        MapSqlParameterSource source = classParams(body);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update("""
                insert into class_info
                    (class_code, class_name, college_id, major_id, grade, head_teacher,
                     total_students, monitor_id, contact_phone, status)
                values
                    (:classCode, :className, :collegeId, :majorId, :grade, :headTeacherId,
                     :studentCount, :monitorId, :contactPhone, :status)
                """, source, keyHolder, new String[]{"id"});
        Number key = Objects.requireNonNull(keyHolder.getKey(), "class id");
        return Result.success("班级创建成功", requireClass(key.longValue()));
    }

    @PutMapping("/{id}")
    public Result<Map<String, Object>> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        requireClass(id);
        MapSqlParameterSource source = classParams(body).addValue("id", id);
        jdbcTemplate.update("""
                update class_info
                   set class_code = :classCode,
                       class_name = :className,
                       college_id = :collegeId,
                       major_id = :majorId,
                       grade = :grade,
                       head_teacher = :headTeacherId,
                       monitor_id = :monitorId,
                       total_students = :studentCount,
                       contact_phone = :contactPhone,
                       status = :status
                 where id = :id
                """, source);
        return Result.success("班级更新成功", requireClass(id));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        int affected = jdbcTemplate.update("delete from class_info where id = :id", new MapSqlParameterSource("id", id));
        return affected > 0 ? Result.success(true) : Result.notFound("班级不存在");
    }

    @DeleteMapping("/batch")
    public Result<Boolean> batchDelete(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.fail("班级ID列表不能为空");
        }
        jdbcTemplate.update("delete from class_info where id in (:ids)", new MapSqlParameterSource("ids", ids));
        return Result.success(true);
    }

    @GetMapping("/{id}/students")
    public Result<List<Map<String, Object>>> students(@PathVariable Long id) {
        Map<String, Object> classInfo = requireClass(id);
        MapSqlParameterSource source = new MapSqlParameterSource()
                .addValue("id", id);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select s.id,
                       s.student_no studentNo,
                       s.name,
                       s.gender,
                       s.phone,
                       s.email,
                       s.status,
                       s.create_time createdAt
                  from student s
                 where s.class_id = :id
                 order by s.student_no
                """, source);
        return Result.success(rows);
    }

    @GetMapping("/{id}/courses")
    public Result<List<Map<String, Object>>> courses(@PathVariable Long id) {
        Map<String, Object> classInfo = requireClass(id);
        MapSqlParameterSource source = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("className", classInfo.get("className"));
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select distinct c.id,
                       c.course_code courseCode,
                       c.course_name courseName,
                       c.credit,
                       c.course_type courseType,
                       c.schedule,
                       c.classroom,
                       c.status,
                       t.name teacherName
                  from student s
                  join course_selection cs on cs.student_id = s.id and cs.status = 1
                  join course c on c.id = cs.course_id
                  left join teacher t on t.id = c.teacher_id
                 where s.class_id = :id
                 order by c.course_code
                """, source);
        return Result.success(rows);
    }

    private Map<String, Object> requireClass(Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select c.id,
                       c.class_code classCode,
                       c.class_name className,
                       c.college_id collegeId,
                       co.college_name collegeName,
                       c.major_id majorId,
                       m.major_name majorName,
                       c.grade,
                       c.head_teacher headTeacherName,
                       c.total_students studentCount,
                       c.monitor_id monitorId,
                       mon.name monitorName,
                       c.contact_phone contactPhone,
                       c.create_time createTime,
                       c.status,
                       c.update_time updatedAt
                  from class_info c
                  left join college co on co.id = c.college_id
                  left join major m on m.id = c.major_id
                  left join student mon on mon.id = c.monitor_id
                 where c.id = :id
                """, new MapSqlParameterSource("id", id));
        if (rows.isEmpty()) {
            throw new org.example.courseselectionsystem.exception.BusinessException(Result.NOT_FOUND, "班级不存在");
        }
        return rows.get(0);
    }

    private String classWhere(Map<String, String> params, MapSqlParameterSource source) {
        List<String> filters = new ArrayList<>();
        String keyword = text(params.get("keyword"));
        if (keyword != null) {
            filters.add("(c.class_code like :keyword or c.class_name like :keyword or c.grade like :keyword or co.college_name like :keyword)");
            source.addValue("keyword", "%" + keyword + "%");
        }
        addLongFilter(filters, source, "collegeId", "c.college_id", params.get("collegeId"));
        addLongFilter(filters, source, "majorId", "c.major_id", params.get("majorId"));
        addIntFilter(filters, source, "status", "c.status", params.get("status"));
        String grade = text(params.get("grade"));
        if (grade != null) {
            filters.add("c.grade = :grade");
            source.addValue("grade", grade);
        }
        return filters.isEmpty() ? "" : " where " + String.join(" and ", filters);
    }

    private MapSqlParameterSource classParams(Map<String, Object> body) {
        String classCode = requiredText(body.get("classCode"), "班级编号不能为空");
        String className = requiredText(body.get("className"), "班级名称不能为空");
        Long collegeId = requiredLong(body.get("collegeId"), "学院不能为空");
        Long majorId = requiredLong(body.get("majorId"), "专业不能为空");
        String grade = requiredText(body.get("grade"), "年级不能为空");
        // head_teacher 列存教师姓名；优先 headTeacherName，兼容 headTeacherId
        String headTeacher = text(body.get("headTeacherName"));
        if (headTeacher == null) {
            Long htId = longValue(body.get("headTeacherId"));
            headTeacher = htId == null ? null : String.valueOf(htId);
        }
        // monitor_id 列存学生 ID；优先 monitorId，兼容 monitorName 按姓名查询
        Long monitorId = longValue(body.get("monitorId"));
        if (monitorId == null) {
            String monitorName = text(body.get("monitorName"));
            if (monitorName != null) {
                monitorId = jdbcTemplate.query("select id from student where name = :name order by id limit 1",
                        new MapSqlParameterSource("name", monitorName),
                        rs -> rs.next() ? rs.getLong(1) : null);
            }
        }
        return new MapSqlParameterSource()
                .addValue("classCode", classCode)
                .addValue("className", className)
                .addValue("collegeId", collegeId)
                .addValue("majorId", majorId)
                .addValue("grade", grade)
                .addValue("headTeacherId", headTeacher)
                .addValue("monitorId", monitorId)
                .addValue("contactPhone", text(body.get("contactPhone")))
                .addValue("studentCount", intValue(body.get("studentCount"), 0))
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

    private String orderBy(String field, String isAsc, String defaultColumn) {
        String normalized = text(field);
        String column = normalized == null ? defaultColumn : SORT_COLUMNS.getOrDefault(normalized, defaultColumn);
        String direction = "false".equalsIgnoreCase(isAsc) || "desc".equalsIgnoreCase(isAsc) ? "desc" : "asc";
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
            throw new org.example.courseselectionsystem.exception.BusinessException(Result.PARAM_ERROR, message);
        }
        return parsed;
    }

    private String requiredText(Object value, String message) {
        String text = text(value);
        if (text == null) {
            throw new org.example.courseselectionsystem.exception.BusinessException(Result.PARAM_ERROR, message);
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
