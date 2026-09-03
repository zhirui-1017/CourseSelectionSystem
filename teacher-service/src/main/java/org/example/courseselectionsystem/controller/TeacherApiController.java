package org.example.courseselectionsystem.controller;

import org.example.courseselectionsystem.common.Result;
import org.example.courseselectionsystem.entity.Teacher;
import org.example.courseselectionsystem.service.TeacherService;
import org.example.courseselectionsystem.vo.PageRequest;
import org.example.courseselectionsystem.vo.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/teachers")
public class TeacherApiController {

    @Autowired
    private TeacherService teacherService;

    @PostMapping
    public Result<Boolean> addTeacher(@RequestBody Teacher teacher) {
        return Result.success(teacherService.addTeacher(teacher));
    }

    @PostMapping("/from-map")
    public Result<Boolean> addTeacherFromMap(@RequestBody Map<String, Object> teacherInfo) {
        return Result.success(teacherService.addTeacher(teacherInfo));
    }

    @PutMapping("/{teacherId}")
    public Result<Boolean> updateTeacher(@PathVariable Long teacherId, @RequestBody Teacher teacher) {
        teacher.setId(teacherId);
        return Result.success(teacherService.updateTeacher(teacher));
    }

    @PutMapping("/{teacherId}/from-map")
    public Result<Boolean> updateTeacherFromMap(@PathVariable Long teacherId, @RequestBody Map<String, Object> teacherInfo) {
        teacherInfo.put("id", teacherId);
        return Result.success(teacherService.updateTeacher(teacherInfo));
    }

    @DeleteMapping("/{teacherId}")
    public Result<Boolean> deleteTeacher(@PathVariable Long teacherId) {
        return Result.success(teacherService.deleteTeacher(teacherId));
    }

    @DeleteMapping("/batch")
    public Result<Integer> batchDeleteTeachers(@RequestBody Long[] teacherIds) {
        return Result.success(teacherService.batchDeleteTeachers(teacherIds));
    }

    @GetMapping("/{teacherId}")
    public Result<Teacher> getTeacherById(@PathVariable Long teacherId) {
        return Result.success(teacherService.getTeacherById(teacherId));
    }

    @GetMapping("/teacher-no/{teacherNo}")
    public Result<Teacher> getTeacherByTeacherNo(@PathVariable String teacherNo) {
        return Result.success(teacherService.getTeacherByTeacherNo(teacherNo));
    }

    /**
     * 获取教师列表（供 Feign 跨服务调用，根路径）
     */
    @GetMapping
    public Result<List<Map<String, Object>>> listTeachers() {
        List<Teacher> teachers = teacherService.getAllTeachers();
        List<Map<String, Object>> list = teachers.stream()
                .map(this::teacherToMap)
                .collect(Collectors.toList());
        return Result.success(list);
    }

    @GetMapping("/all")
    public Result<List<Teacher>> getAllTeachers() {
        return Result.success(teacherService.getAllTeachers());
    }

    @GetMapping("/list")
    public Result<PageResult<Teacher>> getTeachersByPage(PageRequest pageRequest, @RequestParam Map<String, Object> params) {
        pageRequest.setParams(params);
        return Result.success(teacherService.getTeachersByPage(pageRequest));
    }

    @GetMapping("/page-map")
    public Result<Map<String, Object>> getTeacherListByPage(PageRequest pageRequest, @RequestParam Map<String, Object> params) {
        pageRequest.setParams(params);
        return Result.success(teacherService.getTeacherListByPage(pageRequest));
    }

    @GetMapping("/department/{departmentId}")
    public Result<List<Teacher>> getTeachersByDepartmentId(@PathVariable Long departmentId) {
        return Result.success(teacherService.getTeachersByDepartmentId(departmentId));
    }

    @GetMapping("/college/{collegeId}")
    public Result<List<Teacher>> getTeachersByCollegeId(@PathVariable Long collegeId) {
        return Result.success(teacherService.getTeachersByCollegeId(collegeId));
    }

    @GetMapping("/search")
    public Result<List<Teacher>> searchTeachersByName(@RequestParam String name) {
        return Result.success(teacherService.searchTeachersByName(name));
    }

    @GetMapping("/count")
    public Result<Long> count() {
        return Result.success(teacherService.count());
    }

    @GetMapping("/count/recent")
    public Result<Long> countRecent(@RequestParam(defaultValue = "30") int days) {
        return Result.success(teacherService.countRecent(days));
    }

    @PutMapping("/{teacherId}/reset-password")
    public Result<Boolean> resetPassword(@PathVariable Long teacherId) {
        return Result.success(teacherService.resetPassword(teacherId));
    }

    @PutMapping("/{teacherId}/change-password")
    public Result<Boolean> changePassword(@PathVariable Long teacherId,
                                          @RequestParam String oldPassword,
                                          @RequestParam String newPassword) {
        return Result.success(teacherService.changePassword(teacherId, oldPassword, newPassword));
    }

    // ========== 内部辅助方法 ==========

    private Map<String, Object> teacherToMap(Teacher teacher) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", teacher.getId());
        map.put("teacherNo", teacher.getTeacherNo());
        map.put("name", teacher.getName());
        map.put("gender", teacher.getGender());
        map.put("phone", teacher.getPhone());
        map.put("email", teacher.getEmail());
        map.put("title", teacher.getTitle());
        map.put("departmentId", teacher.getDepartmentId());
        map.put("status", teacher.getStatus());
        return map;
    }
}
