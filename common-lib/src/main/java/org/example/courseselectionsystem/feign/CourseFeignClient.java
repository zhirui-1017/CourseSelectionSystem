package org.example.courseselectionsystem.feign;

import org.example.courseselectionsystem.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * 课程服务 Feign 客户端
 * <p>
 * 调用方：selection-service（选课列表需要课程名称）、web-service（仪表盘统计）
 */
@FeignClient(name = "course-service", path = "/api/v1")
public interface CourseFeignClient {

    @GetMapping("/courses/{id}")
    Result<Map<String, Object>> getCourseById(@PathVariable("id") Long id);

    @GetMapping("/courses")
    Result<List<Map<String, Object>>> listCourses(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size);

    @GetMapping("/colleges")
    Result<List<Map<String, Object>>> listColleges();

    @GetMapping("/courses/count")
    Result<Long> count();
}
