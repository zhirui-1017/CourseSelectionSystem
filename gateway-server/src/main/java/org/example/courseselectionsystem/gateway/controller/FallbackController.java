package org.example.courseselectionsystem.gateway.controller;

import org.example.courseselectionsystem.common.Result;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FallbackController {
    @RequestMapping("/fallback")
    public Result<Void> fallback() {
        return Result.fail(503, "服务暂时不可用，请稍后重试");
    }
}
