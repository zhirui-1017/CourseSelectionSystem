package org.example.courseselectionsystem.controller;

import org.example.courseselectionsystem.common.Result;
import org.example.courseselectionsystem.service.TeacherService;
import org.example.courseselectionsystem.vo.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 教师 REST API 控制器（兼容前端 /api/v1/teachers/** 调用）
 * 答辩演示用，所有请求统一走 web-service 以保持 Session 一致
 */
@RestController
@RequestMapping("/api/v1/teachers")
public class TeacherApiController {

    @Autowired
    private TeacherService teacherService;

    /**
     * 分页查询教师列表
     */
    @GetMapping("/list")
    public Result<?> list(PageRequest pageRequest, @RequestParam(required = false) Map<String, Object> params) {
        try {
            if (params != null && !params.isEmpty()) {
                pageRequest.setParams(params);
            }
            Map<String, Object> result = teacherService.getTeacherListByPage(pageRequest);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
