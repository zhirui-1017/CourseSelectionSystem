package org.example.courseselectionsystem.controller;

import org.example.courseselectionsystem.common.Result;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 选课趋势统计控制器（供仪表盘展示真实数据）
 * <p>
 * 基于 course_selection.selection_time 统计真实选课趋势，
 * 替代之前写死的月度/每日全 0 假数据。
 */
@RestController
@RequestMapping("/api/v1/course-selections/trend")
public class SelectionTrendController {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public SelectionTrendController(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 近 12 个月的选课数量趋势
     */
    @GetMapping("/month")
    public Result<List<Map<String, Object>>> monthTrend() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select date_format(selection_time, '%Y-%m') ym, count(*) cnt
                  from course_selection
                 where selection_time is not null
                 group by date_format(selection_time, '%Y-%m')
                """, new MapSqlParameterSource());

        Map<String, Long> countByMonth = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Object ym = row.get("ym");
            Object cnt = row.get("cnt");
            if (ym != null) {
                countByMonth.put(String.valueOf(ym), cnt instanceof Number ? ((Number) cnt).longValue() : 0L);
            }
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        YearMonth current = YearMonth.now();
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            YearMonth ym = current.minusMonths(i);
            String key = ym.format(formatter);
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("month", ym.getMonthValue() + "月");
            point.put("yearMonth", key);
            point.put("count", countByMonth.getOrDefault(key, 0L));
            trend.add(point);
        }
        return Result.success(trend);
    }

    /**
     * 近 30 天的选课数量趋势
     */
    @GetMapping("/day")
    public Result<List<Map<String, Object>>> dayTrend() {
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(29);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select date(selection_time) day, count(*) cnt
                  from course_selection
                 where selection_time is not null and date(selection_time) >= :from and date(selection_time) <= :to
                 group by date(selection_time)
                """, new MapSqlParameterSource()
                .addValue("from", from.toString())
                .addValue("to", today.toString()));

        Map<String, Long> countByDay = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Object day = row.get("day");
            Object cnt = row.get("cnt");
            if (day != null) {
                countByDay.put(String.valueOf(day), cnt instanceof Number ? ((Number) cnt).longValue() : 0L);
            }
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            LocalDate day = from.plusDays(i);
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("day", day.getDayOfMonth() + "日");
            point.put("fullDay", day.format(formatter));
            point.put("count", countByDay.getOrDefault(day.format(formatter), 0L));
            trend.add(point);
        }
        return Result.success(trend);
    }
}
