package org.example.courseselectionsystem.controller;

import org.example.courseselectionsystem.common.Result;
import org.example.courseselectionsystem.vo.PageResult;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 课程通知接口：删除课程等系统操作产生的通知，供学生端/教师端/管理员端查看。
 */
@RestController
@RequestMapping("/api/v1/notices")
public class CourseNoticeController {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public CourseNoticeController(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 最近通知（页面顶部横幅） */
    @GetMapping("/recent")
    public Result<List<Map<String, Object>>> recent(@RequestParam(defaultValue = "5") int limit) {
        int n = Math.min(Math.max(1, limit), 50);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select id, title, content, course_id courseId, course_name courseName,
                       publisher_name publisherName, publish_time publishTime
                  from course_notice
                 order by publish_time desc, id desc
                 limit :limit
                """, new MapSqlParameterSource().addValue("limit", n));
        return Result.success(rows);
    }

    /** 通知分页 */
    @GetMapping({"", "/list"})
    public Result<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        int p = Math.max(1, pageNum);
        int s = Math.min(Math.max(1, pageSize), 1000);
        MapSqlParameterSource source = new MapSqlParameterSource()
                .addValue("offset", (p - 1) * s)
                .addValue("pageSize", s);
        long total = jdbcTemplate.queryForObject("select count(*) from course_notice", source, Long.class);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select id, title, content, course_id courseId, course_name courseName,
                       publisher_name publisherName, publish_time publishTime
                  from course_notice
                 order by publish_time desc, id desc
                 limit :pageSize offset :offset
                """, source);
        return Result.success(new PageResult<>(p, s, total, rows));
    }
}
