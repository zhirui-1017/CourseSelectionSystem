package org.example.courseselectionsystem.feign;

import org.example.courseselectionsystem.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * 用户服务 Feign 客户端
 */
@FeignClient(name = "user-service", path = "/api/v1")
public interface UserFeignClient {

    @GetMapping("/users/{userId}")
    Result<Map<String, Object>> getUserById(@PathVariable("userId") Long userId);

    @GetMapping("/users/count")
    Result<Long> count();
}
