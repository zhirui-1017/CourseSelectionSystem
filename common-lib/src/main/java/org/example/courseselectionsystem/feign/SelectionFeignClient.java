package org.example.courseselectionsystem.feign;

import org.example.courseselectionsystem.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

/**
 * 选课服务 Feign 客户端
 * <p>
 * 调用方：web-service（仪表盘统计）
 */
@FeignClient(name = "selection-service", path = "/api/v1")
public interface SelectionFeignClient {

    @GetMapping("/course-selections/count")
    Result<Map<String, Object>> getSelectionCount();

    @GetMapping("/course-selections/trend/month")
    Result<List<Map<String, Object>>> getMonthlyTrend();

    @GetMapping("/course-selections/trend/day")
    Result<List<Map<String, Object>>> getDailyTrend();
}
