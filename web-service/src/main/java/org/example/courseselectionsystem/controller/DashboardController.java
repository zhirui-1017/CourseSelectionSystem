package org.example.courseselectionsystem.controller;

import org.example.courseselectionsystem.common.Result;
import org.example.courseselectionsystem.feign.CourseFeignClient;
import org.example.courseselectionsystem.feign.SelectionFeignClient;
import org.example.courseselectionsystem.feign.StudentFeignClient;
import org.example.courseselectionsystem.feign.TeacherFeignClient;
import org.example.courseselectionsystem.feign.UserFeignClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 仪表盘控制器
 * <p>
 * 通过 Feign 调用各微服务聚合统计数据，使用 Caffeine 缓存优化性能。
 */
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    @Autowired
    private StudentFeignClient studentFeignClient;

    @Autowired
    private TeacherFeignClient teacherFeignClient;

    @Autowired
    private CourseFeignClient courseFeignClient;

    @Autowired
    private SelectionFeignClient selectionFeignClient;

    @Autowired
    private UserFeignClient userFeignClient;

    /**
     * 获取仪表盘统计数据（缓存5分钟，并行查询）
     */
    @GetMapping("/stats")
    @Cacheable(value = "dashboardStats", key = "'dashboard:stats'")
    public Result<?> getDashboardStats() {
        try {
            logger.info("开始获取仪表盘数据（并行查询）");

            // 使用 CompletableFuture 并行执行所有统计查询
            CompletableFuture<Long> studentsFuture = CompletableFuture.supplyAsync(
                    () -> safeGetCount(studentFeignClient::count, "学生"));
            CompletableFuture<Long> teachersFuture = CompletableFuture.supplyAsync(
                    () -> safeGetCount(teacherFeignClient::count, "教师"));
            CompletableFuture<Long> coursesFuture = CompletableFuture.supplyAsync(
                    () -> safeGetCount(courseFeignClient::count, "课程"));
            CompletableFuture<Long> selectionsFuture = CompletableFuture.supplyAsync(
                    () -> safeGetCount(selectionFeignClient::getSelectionCount, "选课"));
            CompletableFuture<Long> usersFuture = CompletableFuture.supplyAsync(
                    () -> safeGetCount(userFeignClient::count, "用户"));

            // 等待所有查询完成（最多10秒超时）
            CompletableFuture.allOf(studentsFuture, teachersFuture, coursesFuture,
                            selectionsFuture, usersFuture)
                    .orTimeout(10, TimeUnit.SECONDS);

            long totalStudents = studentsFuture.get();
            long totalTeachers = teachersFuture.get();
            long totalCourses = coursesFuture.get();
            long totalSelections = selectionsFuture.get();
            long totalUsers = usersFuture.get();

            logger.info("并行查询完成 - 学生:{}, 教师:{}, 课程:{}, 选课:{}, 用户:{}",
                    totalStudents, totalTeachers, totalCourses, totalSelections, totalUsers);

            // 构造统计数据
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalStudents", totalStudents);
            stats.put("totalTeachers", totalTeachers);
            stats.put("totalCourses", totalCourses);
            stats.put("totalSelections", totalSelections);
            stats.put("totalUsers", totalUsers);
            stats.put("studentsGrowthRate", 5.2);
            stats.put("teachersGrowthRate", 2.4);
            stats.put("coursesGrowthRate", 8.7);
            stats.put("selectionsGrowthRate", 12.5);

            // 构造图表数据
            Map<String, Object> charts = new HashMap<>();
            charts.put("selectionTrend", buildMonthlyTrend());
            charts.put("roleDistribution", buildRoleDistribution(totalStudents, totalTeachers,
                    totalUsers - totalStudents - totalTeachers));

            Map<String, Object> result = new HashMap<>();
            result.put("stats", stats);
            result.put("charts", charts);

            return Result.success(result);
        } catch (Exception e) {
            logger.error("获取仪表盘数据失败：{}", e.getMessage(), e);
            return Result.error("获取仪表盘数据失败：" + e.getMessage());
        }
    }

    /**
     * 获取选课趋势数据
     */
    @GetMapping("/selection-trend")
    public Result<?> getSelectionTrend(@RequestParam(value = "viewType", defaultValue = "month") String viewType) {
        switch (viewType) {
            case "month" -> {
                return Result.success(buildMonthlyTrend());
            }
            case "day" -> {
                return Result.success(buildDailyTrend());
            }
            default -> {
                return Result.success(buildMonthlyTrend());
            }
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Result<Map<String, String>> health() {
        return Result.success(Map.of("status", "ok", "service", "dashboard"));
    }

    // ============================================================
    // 内部辅助方法
    // ============================================================

    private long safeGetCount(CountSupplier supplier, String name) {
        try {
            Result<?> result = supplier.get();
            if (result != null && result.getSuccess() && result.getData() != null) {
                Object data = result.getData();
                if (data instanceof Number) {
                    return ((Number) data).longValue();
                }
                if (data instanceof Map) {
                    Object count = ((Map<?, ?>) data).get("count");
                    if (count instanceof Number) {
                        return ((Number) count).longValue();
                    }
                }
            }
            return 0L;
        } catch (Exception e) {
            logger.warn("获取{}总数失败：{}", name, e.getMessage());
            return 0L;
        }
    }

    @FunctionalInterface
    private interface CountSupplier {
        Result<?> get();
    }

    private List<Map<String, Object>> buildMonthlyTrend() {
        List<Map<String, Object>> trend = new ArrayList<>();
        // 默认每个月为0，前端可通过真实数据替换
        for (int i = 1; i <= 12; i++) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("month", i + "月");
            map.put("count", 0L);
            trend.add(map);
        }
        return trend;
    }

    private List<Map<String, Object>> buildDailyTrend() {
        List<Map<String, Object>> trend = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        for (int i = 30; i >= 0; i--) {
            Calendar dayCal = (Calendar) cal.clone();
            dayCal.add(Calendar.DAY_OF_MONTH, -i);
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("day", dayCal.get(Calendar.DAY_OF_MONTH) + "日");
            map.put("count", 0L);
            trend.add(map);
        }
        return trend;
    }

    private List<Map<String, Object>> buildRoleDistribution(long students, long teachers, long admins) {
        long adminCount = Math.max(admins, 1);
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("role", "学生"); s.put("count", students); s.put("color", "#36A2EB"); list.add(s);
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("role", "教师"); t.put("count", teachers); t.put("color", "#FF6384"); list.add(t);
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("role", "管理员"); a.put("count", adminCount); a.put("color", "#4BC0C0"); list.add(a);
        return list;
    }
}
