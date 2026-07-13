package org.example.courseselectionsystem.feign;

import org.example.courseselectionsystem.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

/**
 * 教师服务 Feign 客户端
 * <p>
 * 调用方：selection-service（教师查看选课学生）、user-service（用户聚合）
 */
@FeignClient(name = "teacher-service", path = "/api/v1")
public interface TeacherFeignClient {

    @GetMapping("/teachers/{id}")
    Result<Map<String, Object>> getTeacherById(@PathVariable("id") Long id);

    @GetMapping("/teachers")
    Result<List<Map<String, Object>>> listTeachers();

    @GetMapping("/teachers/count")
    Result<Long> count();
}
