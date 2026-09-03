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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

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

            // 获取真实趋势数据（月度/每日选课量）与近期新增数量
            List<Map<String, Object>> monthlyTrend = buildMonthlyTrend();
            List<Map<String, Object>> dailyTrend = buildDailyTrend();
            long recentStudents = safeGetRecent(() -> studentFeignClient.countRecent(30), "学生");
            long recentTeachers = safeGetRecent(() -> teacherFeignClient.countRecent(30), "教师");
            long recentCourses = safeGetRecent(() -> courseFeignClient.countRecent(30), "课程");

            // 构造统计数据（增长率基于真实数据计算，不再写死）
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalStudents", totalStudents);
            stats.put("totalTeachers", totalTeachers);
            stats.put("totalCourses", totalCourses);
            stats.put("totalSelections", totalSelections);
            stats.put("totalUsers", totalUsers);
            stats.put("studentsGrowthRate", growthRate(recentStudents, totalStudents));
            stats.put("teachersGrowthRate", growthRate(recentTeachers, totalTeachers));
            stats.put("coursesGrowthRate", growthRate(recentCourses, totalCourses));
            stats.put("selectionsGrowthRate", selectionGrowthRate(monthlyTrend));

            // 构造图表数据（真实趋势）
            Map<String, Object> charts = new HashMap<>();
            charts.put("selectionTrend", monthlyTrend);
            charts.put("dailyTrend", dailyTrend);
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

    @FunctionalInterface
    private interface RecentSupplier {
        Result<Long> get();
    }

    private List<Map<String, Object>> safeGetTrend(Supplier<Result<List<Map<String, Object>>>> supplier) {
        try {
            Result<List<Map<String, Object>>> result = supplier.get();
            if (result != null && result.getSuccess() && result.getData() != null) {
                return result.getData();
            }
        } catch (Exception e) {
            logger.warn("获取选课趋势失败：{}", e.getMessage());
        }
        return Collections.emptyList();
    }

    private long safeGetRecent(RecentSupplier supplier, String name) {
        try {
            Result<Long> result = supplier.get();
            if (result != null && result.getSuccess() && result.getData() != null) {
                return result.getData();
            }
        } catch (Exception e) {
            logger.warn("获取{}近30天新增数量失败：{}", name, e.getMessage());
        }
        return 0L;
    }

    /**
     * 近30天新增数量占总数的比例（百分比，保留1位小数），作为真实增长率指标
     */
    private double growthRate(long recent, long total) {
        if (total <= 0) {
            return 0D;
        }
        return Math.round(recent * 1000D / total) / 10D;
    }

    /**
     * 根据真实月度选课趋势计算环比增长率（本月 vs 上月，百分比，保留1位小数）
     */
    private double selectionGrowthRate(List<Map<String, Object>> trend) {
        if (trend == null || trend.size() < 2) {
            return 0D;
        }
        long current = numberValue(trend.get(trend.size() - 1).get("count"));
        long previous = numberValue(trend.get(trend.size() - 2).get("count"));
        if (previous <= 0) {
            return 0D;
        }
        return Math.round((current - previous) * 1000D / previous) / 10D;
    }

    private long numberValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value != null) {
            try {
                return Long.parseLong(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                // 非数字视为 0
            }
        }
        return 0L;
    }

    private List<Map<String, Object>> buildMonthlyTrend() {
        return safeGetTrend(selectionFeignClient::getMonthlyTrend);
    }

    private List<Map<String, Object>> buildDailyTrend() {
        return safeGetTrend(selectionFeignClient::getDailyTrend);
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
