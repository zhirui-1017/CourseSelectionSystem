package org.example.courseselectionsystem.controller;

import org.example.courseselectionsystem.common.Result;
import org.example.courseselectionsystem.service.AdminService;
import org.example.courseselectionsystem.service.CourseService;
import org.example.courseselectionsystem.service.DepartmentService;
import org.example.courseselectionsystem.service.StudentService;
import org.example.courseselectionsystem.service.TeacherService;
import org.example.courseselectionsystem.vo.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 管理员控制器
 * 处理系统管理相关功能
 */
@Controller
@RequestMapping("/admin")
public class AdminController {
    
    @Resource
    private AdminService adminService;
    
    @Resource
    private StudentService studentService;
    
    @Resource
    private TeacherService teacherService;
    
    @Resource
    private CourseService courseService;
    
    @Resource
    private DepartmentService departmentService;
    
    /**
     * 跳转到管理员首页
     */
    @GetMapping("/index")
    public String toIndex(Model model) {
        // 获取系统统计信息
        Map<String, Object> stats = adminService.getSystemStats();
        model.addAttribute("stats", stats);
        return "redirect:/admin/index.html";
    }
    
    /**
     * 跳转到学生管理页面
     */
    @GetMapping("/studentManagement")
    public String toStudentManagement() {
        return "admin/student_management";
    }
    
    /**
     * 获取学生列表（分页）
     */
    @GetMapping("/students")
    @ResponseBody
    public Result<?> getStudentList(PageRequest pageRequest, @RequestParam Map<String, Object> params) {
        try {
            pageRequest.setParams(params);
            Map<String, Object> result = studentService.getStudentListByPage(pageRequest);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 添加学生
     */
    @PostMapping("/addStudent")
    @ResponseBody
    public Result<?> addStudent(@RequestBody Map<String, Object> studentInfo) {
        try {
            studentService.addStudent(studentInfo);
            return Result.success("学生添加成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 更新学生信息
     */
    @PostMapping("/updateStudent")
    @ResponseBody
    public Result<?> updateStudent(@RequestBody Map<String, Object> studentInfo) {
        try {
            studentService.updateStudent(studentInfo);
            return Result.success("学生信息更新成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 删除学生
     */
    @PostMapping("/deleteStudent")
    @ResponseBody
    public Result<?> deleteStudent(@RequestParam String studentId) {
        try {
            studentService.deleteStudent(studentId);
            return Result.success("学生删除成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 重置学生密码
     */
    @PostMapping("/resetStudentPassword")
    @ResponseBody
    public Result<?> resetStudentPassword(@RequestParam String studentId) {
        try {
            studentService.resetPassword(studentId);
            return Result.success("学生密码重置成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 跳转到教师管理页面
     */
    @GetMapping("/teacherManagement")
    public String toTeacherManagement() {
        return "admin/teacher_management";
    }
    
    /**
     * 获取教师列表（分页）
     */
    @GetMapping("/teachers")
    @ResponseBody
    public Result<?> getTeacherList(PageRequest pageRequest, @RequestParam Map<String, Object> params) {
        try {
            pageRequest.setParams(params);
            Map<String, Object> result = teacherService.getTeacherListByPage(pageRequest);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 添加教师
     */
    @PostMapping("/addTeacher")
    @ResponseBody
    public Result<?> addTeacher(@RequestBody Map<String, Object> teacherInfo) {
        try {
            teacherService.addTeacher(teacherInfo);
            return Result.success("教师添加成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 更新教师信息
     */
    @PostMapping("/updateTeacher")
    @ResponseBody
    public Result<?> updateTeacher(@RequestBody Map<String, Object> teacherInfo) {
        try {
            teacherService.updateTeacher(teacherInfo);
            return Result.success("教师信息更新成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 删除教师
     */
    @PostMapping("/deleteTeacher")
    @ResponseBody
    public Result<?> deleteTeacher(@RequestParam String teacherId) {
        try {
            teacherService.deleteTeacher(teacherId);
            return Result.success("教师删除成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 重置教师密码
     */
    @PostMapping("/resetTeacherPassword")
    @ResponseBody
    public Result<?> resetTeacherPassword(@RequestParam String teacherId) {
        try {
            teacherService.resetPassword(teacherId);
            return Result.success("教师密码重置成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 跳转到课程管理页面
     */
    @GetMapping("/courseManagement")
    public String toCourseManagement() {
        return "admin/course_management";
    }
    
    /**
     * 获取课程列表（分页）
     */
    @GetMapping("/adminCourses")
    @ResponseBody
    public Result<?> getCourseList(PageRequest pageRequest) {
        try {
            Map<String, Object> result = courseService.getCourseListByPage(pageRequest);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 添加课程
     */
    @PostMapping("/addCourse")
    @ResponseBody
    public Result<?> addCourse(@RequestBody Map<String, Object> courseInfo) {
        try {
            courseService.addCourse(courseInfo);
            return Result.success("课程添加成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 更新课程信息
     */
    @PostMapping("/updateCourse")
    @ResponseBody
    public Result<?> updateCourse(@RequestBody Map<String, Object> courseInfo) {
        try {
            courseService.updateCourse(courseInfo);
            return Result.success("课程信息更新成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 删除课程
     */
    @PostMapping("/deleteCourse")
    @ResponseBody
    public Result<?> deleteCourse(@RequestParam String courseId) {
        try {
            courseService.deleteCourse(courseId);
            return Result.success("课程删除成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 跳转到院系管理页面
     */
    @GetMapping("/departmentManagement")
    public String toDepartmentManagement() {
        return "admin/department_management";
    }
    
    /**
     * 获取院系列表
     */
    @GetMapping("/departments")
    @ResponseBody
    public Result<?> getDepartmentList() {
        try {
            return Result.success(departmentService.getAllDepartments());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 添加院系
     */
    @PostMapping("/addDepartment")
    @ResponseBody
    public Result<?> addDepartment(@RequestBody Map<String, Object> deptInfo) {
        try {
            departmentService.addDepartment(deptInfo);
            return Result.success("院系添加成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 更新院系信息
     */
    @PostMapping("/updateDepartment")
    @ResponseBody
    public Result<?> updateDepartment(@RequestBody Map<String, Object> deptInfo) {
        try {
            departmentService.updateDepartment(deptInfo);
            return Result.success("院系信息更新成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 删除院系
     */
    @PostMapping("/deleteDepartment")
    @ResponseBody
    public Result<?> deleteDepartment(@RequestParam String deptId) {
        try {
            departmentService.deleteDepartment(deptId);
            return Result.success("院系删除成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 跳转到选课统计页面
     */
    @GetMapping("/selectionStatistics")
    public String toSelectionStatistics() {
        return "admin/selection_statistics";
    }
    
    /**
     * 获取选课统计数据
     */
    @GetMapping("/stats")
    @ResponseBody
    public Result<?> getSelectionStats() {
        try {
            Map<String, Object> stats = adminService.getSelectionStats();
            return Result.success(stats);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
