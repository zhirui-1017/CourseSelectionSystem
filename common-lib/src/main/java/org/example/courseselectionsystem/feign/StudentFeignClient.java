package org.example.courseselectionsystem.feign;

import org.example.courseselectionsystem.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * 学生服务 Feign 客户端
 * <p>
 * 调用方：selection-service（选课列表需要学生姓名）、user-service（用户聚合）
 */
@FeignClient(name = "student-service", path = "/api/v1")
public interface StudentFeignClient {

    @GetMapping("/students/{id}")
    Result<Map<String, Object>> getStudentById(@PathVariable("id") Long id);

    @GetMapping("/students")
    Result<List<Map<String, Object>>> listStudents(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size);

    @GetMapping("/students/count")
    Result<Long> count();
}
