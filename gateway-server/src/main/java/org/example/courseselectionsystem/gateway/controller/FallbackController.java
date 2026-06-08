package org.example.courseselectionsystem.gateway.controller;

import org.example.courseselectionsystem.common.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FallbackController {
    @RequestMapping("/fallback")
    public ResponseEntity<Result<Void>> fallback() {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Result.fail(503, "服务暂时不可用，请稍后重试"));
    }
}
