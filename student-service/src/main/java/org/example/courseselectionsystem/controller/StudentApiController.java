package org.example.courseselectionsystem.controller;

import org.example.courseselectionsystem.common.Result;
import org.example.courseselectionsystem.entity.Student;
import org.example.courseselectionsystem.service.StudentService;
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
@RequestMapping("/api/v1/students")
public class StudentApiController {

    @Autowired
    private StudentService studentService;

    @PostMapping
    public Result<Boolean> addStudent(@RequestBody Student student) {
        return Result.success(studentService.addStudent(student));
    }

    @PostMapping("/from-map")
    public Result<Boolean> addStudentFromMap(@RequestBody Map<String, Object> studentInfo) {
        return Result.success(studentService.addStudent(studentInfo));
    }

    @PutMapping("/{studentId}")
    public Result<Boolean> updateStudent(@PathVariable Long studentId, @RequestBody Student student) {
        student.setId(studentId);
        return Result.success(studentService.updateStudent(student));
    }

    @PutMapping("/{studentId}/from-map")
    public Result<Boolean> updateStudentFromMap(@PathVariable Long studentId, @RequestBody Map<String, Object> studentInfo) {
        studentInfo.put("id", studentId);
        return Result.success(studentService.updateStudent(studentInfo));
    }

    @DeleteMapping("/{studentId}")
    public Result<Boolean> deleteStudent(@PathVariable Long studentId) {
        return Result.success(studentService.deleteStudent(studentId));
    }

    @DeleteMapping("/batch")
    public Result<Integer> batchDeleteStudents(@RequestBody Long[] studentIds) {
        return Result.success(studentService.batchDeleteStudents(studentIds));
    }

    @GetMapping("/{studentId}")
    public Result<Student> getStudentById(@PathVariable Long studentId) {
        return Result.success(studentService.getStudentById(studentId));
    }

    @GetMapping("/student-no/{studentNo}")
    public Result<Student> getStudentByStudentNo(@PathVariable String studentNo) {
        return Result.success(studentService.getStudentByStudentNo(studentNo));
    }

    @GetMapping("/all")
    public Result<List<Student>> getAllStudents() {
        return Result.success(studentService.getAllStudents());
    }

    @GetMapping("/list")
    public Result<PageResult<Student>> getStudentsByPage(PageRequest pageRequest, @RequestParam Map<String, Object> params) {
        pageRequest.setParams(params);
        return Result.success(studentService.getStudentsByPage(pageRequest));
    }

    @GetMapping("/page-map")
    public Result<Map<String, Object>> getStudentListByPage(PageRequest pageRequest, @RequestParam Map<String, Object> params) {
        pageRequest.setParams(params);
        return Result.success(studentService.getStudentListByPage(pageRequest));
    }

    @GetMapping("/major/{majorId}")
    public Result<List<Student>> getStudentsByMajorId(@PathVariable Long majorId) {
        return Result.success(studentService.getStudentsByMajorId(majorId));
    }

    @GetMapping("/department/{departmentId}")
    public Result<List<Student>> getStudentsByDepartmentId(@PathVariable Long departmentId) {
        return Result.success(studentService.getStudentsByDepartmentId(departmentId));
    }

    @GetMapping("/college/{collegeId}")
    public Result<List<Student>> getStudentsByCollegeId(@PathVariable Long collegeId) {
        return Result.success(studentService.getStudentsByCollegeId(collegeId));
    }

    @GetMapping("/search")
    public Result<List<Student>> searchStudentsByName(@RequestParam String name) {
        return Result.success(studentService.searchStudentsByName(name));
    }

    @PutMapping("/{studentId}/reset-password")
    public Result<Boolean> resetPassword(@PathVariable Long studentId) {
        return Result.success(studentService.resetPassword(studentId));
    }

    @PutMapping("/{studentId}/change-password")
    public Result<Boolean> changePassword(@PathVariable Long studentId,
                                          @RequestParam String oldPassword,
                                          @RequestParam String newPassword) {
        return Result.success(studentService.changePassword(studentId, oldPassword, newPassword));
    }
}
