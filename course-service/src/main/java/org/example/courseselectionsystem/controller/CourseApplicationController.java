package org.example.courseselectionsystem.controller;

import org.example.courseselectionsystem.common.Result;
import org.example.courseselectionsystem.entity.CourseApplication;
import org.example.courseselectionsystem.service.CourseApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 课程申请审批接口：教师提交申请，管理员审批。
 */
@RestController
@RequestMapping("/api/v1/course-applications")
public class CourseApplicationController {

    @Autowired
    private CourseApplicationService courseApplicationService;

    /** 提交申请（add/edit/delete） */
    @PostMapping
    public Result<CourseApplication> apply(@RequestBody CourseApplication application) {
        return Result.success(courseApplicationService.apply(application));
    }

    /** 申请详情 */
    @GetMapping("/{id}")
    public Result<CourseApplication> get(@PathVariable Long id) {
        return Result.success(courseApplicationService.getById(id));
    }

    /** 待审批申请分页（管理员审批中心） */
    @GetMapping("/pending")
    public Result<Page<CourseApplication>> pending(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(courseApplicationService.listPending(pageNum, pageSize));
    }

    /** 某教师的申请记录（教师查看自己的申请进度） */
    @GetMapping("/teacher/{applicantId}")
    public Result<Page<CourseApplication>> byTeacher(
            @PathVariable Long applicantId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(courseApplicationService.listByTeacher(applicantId, pageNum, pageSize));
    }

    /** 审批通过 */
    @PutMapping("/{id}/approve")
    public Result<CourseApplication> approve(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long approverId = numberToLong(body.get("approverId"));
        return Result.success("审批通过", courseApplicationService.approve(id, approverId));
    }

    /** 审批驳回 */
    @PutMapping("/{id}/reject")
    public Result<CourseApplication> reject(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long approverId = numberToLong(body.get("approverId"));
        Object comment = body.get("comment");
        return Result.success("已驳回", courseApplicationService.reject(id, approverId,
                comment == null ? null : String.valueOf(comment)));
    }

    private Long numberToLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}
