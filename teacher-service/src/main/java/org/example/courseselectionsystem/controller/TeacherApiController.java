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

import java.util.List;
import java.util.Map;

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

    @GetMapping("/all")
    public Result<List<Teacher>> getAllTeachers() {
        return Result.success(teacherService.getAllTeachers());
    }

    @GetMapping("/list")
    public Result<PageResult<Teacher>> getTeachersByPage(PageRequest pageRequest) {
        return Result.success(teacherService.getTeachersByPage(pageRequest));
    }

    @GetMapping("/page-map")
    public Result<Map<String, Object>> getTeacherListByPage(PageRequest pageRequest) {
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
}
