package org.example.courseselectionsystem.controller;

import org.example.courseselectionsystem.common.Result;
import org.example.courseselectionsystem.entity.Student;
import org.example.courseselectionsystem.service.StudentService;
import org.example.courseselectionsystem.vo.PageRequest;
import org.example.courseselectionsystem.vo.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * 学生控制器
 * 处理学生相关的HTTP请求
 */
@Controller
@RequestMapping("/student")
public class StudentController {

    @Autowired
    private StudentService studentService;

    /**
     * 跳转到学生首页
     */
    @GetMapping("/index")
    public String index(Model model, HttpSession session) {
        // 获取当前登录学生信息
        Object user = session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        // 如果session中存储的是Student对象
        if (user instanceof Student) {
            model.addAttribute("student", user);
        }
        
        return "redirect:/student/index.html";
    }

    /**
     * 跳转到学生个人信息页面
     */
    @GetMapping("/profile")
    public String profile(Model model, HttpSession session) {
        // 从会话中获取学生ID
        Long studentId = (Long) session.getAttribute("userId");
        if (studentId == null) {
            return "redirect:/login";
        }
        
        try {
            // 获取学生详情
            Student student = studentService.getStudentById(studentId);
            model.addAttribute("student", student);
            return "student/profile";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    /**
     * 更新学生个人信息
     */
    @PostMapping("/updateProfile")
    @ResponseBody
    public Result<?> updateProfile(@RequestBody Student student, HttpSession session) {
        try {
            // 从会话中获取学生ID
            Long studentId = (Long) session.getAttribute("userId");
            if (studentId == null) {
                return Result.error(Result.NOT_LOGIN, "用户未登录");
            }
            
            // 确保只能修改自己的信息
            student.setId(studentId);
            
            // 调用服务层更新信息
            boolean success = studentService.updateStudent(student);
            if (success) {
                // 更新会话中的用户信息
                Student updatedStudent = studentService.getStudentById(studentId);
                session.setAttribute("user", updatedStudent);
                
                return Result.success("个人信息更新成功");
            } else {
                return Result.error(Result.FAIL, "个人信息更新失败");
            }
        } catch (Exception e) {
            return Result.error(Result.FAIL, e.getMessage());
        }
    }

    /**
     * 修改密码
     */
    @PostMapping("/changePassword")
    @ResponseBody
    public Result<?> changePassword(@RequestParam String oldPassword, 
                                     @RequestParam String newPassword, 
                                     HttpSession session) {
        try {
            // 从会话中获取学生ID
            Long studentId = (Long) session.getAttribute("userId");
            if (studentId == null) {
                return Result.error(Result.NOT_LOGIN, "用户未登录");
            }
            
            // 调用服务层修改密码
            boolean success = studentService.changePassword(studentId, oldPassword, newPassword);
            if (success) {
                return Result.success("密码修改成功");
            } else {
                return Result.error(Result.FAIL, "密码修改失败");
            }
        } catch (Exception e) {
            return Result.error(Result.FAIL, e.getMessage());
        }
    }

    /**
     * 管理员页面 - 学生列表
     */
    @GetMapping("/list")
    public String studentList(Model model, 
                           @RequestParam(defaultValue = "1") Integer pageNum,
                           @RequestParam(defaultValue = "10") Integer pageSize,
                           @RequestParam(required = false) String studentName,
                           @RequestParam(required = false) String studentId,
                           @RequestParam(required = false) Long departmentId,
                           @RequestParam(required = false) Long majorId) {
        // 创建分页请求参数
        PageRequest pageRequest = new PageRequest();
        pageRequest.setPageNum(pageNum);
        pageRequest.setPageSize(pageSize);
        
        // 创建查询条件
        Map<String, Object> params = new HashMap<>();
        if (studentName != null && !studentName.isEmpty()) {
            params.put("studentName", studentName);
        }
        if (studentId != null && !studentId.isEmpty()) {
            params.put("studentId", studentId);
        }
        if (departmentId != null) {
            params.put("departmentId", departmentId);
        }
        if (majorId != null) {
            params.put("majorId", majorId);
        }
        pageRequest.setParams(params);
        
        try {
            // 获取学生列表（分页）
            PageResult<Student> pageResult = studentService.getStudentsByPage(pageRequest);
            
            model.addAttribute("pageResult", pageResult);
            model.addAttribute("studentName", studentName);
            model.addAttribute("studentId", studentId);
            model.addAttribute("departmentId", departmentId);
            model.addAttribute("majorId", majorId);
            
            return "student/list";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    /**
     * 管理员页面 - 添加学生
     */
    @PostMapping("/add")
    @ResponseBody
    public Result<?> addStudent(@RequestBody Student student) {
        try {
            boolean success = studentService.addStudent(student);
            if (success) {
                return Result.success("学生添加成功");
            } else {
                return Result.error(Result.FAIL, "学生添加失败");
            }
        } catch (Exception e) {
            return Result.error(Result.FAIL, e.getMessage());
        }
    }

    /**
     * 管理员页面 - 更新学生信息
     */
    @PostMapping("/update")
    @ResponseBody
    public Result<?> updateStudent(@RequestBody Student student) {
        try {
            boolean success = studentService.updateStudent(student);
            if (success) {
                return Result.success("学生信息更新成功");
            } else {
                return Result.error(Result.FAIL, "学生信息更新失败");
            }
        } catch (Exception e) {
            return Result.error(Result.FAIL, e.getMessage());
        }
    }

    /**
     * 管理员页面 - 删除学生
     */
    @PostMapping("/delete")
    @ResponseBody
    public Result<?> deleteStudent(@RequestParam Long studentId) {
        try {
            boolean success = studentService.deleteStudent(studentId);
            if (success) {
                return Result.success("学生删除成功");
            } else {
                return Result.error(Result.FAIL, "学生删除失败");
            }
        } catch (Exception e) {
            return Result.error(Result.FAIL, e.getMessage());
        }
    }

    /**
     * 管理员页面 - 批量删除学生
     */
    @PostMapping("/batchDelete")
    @ResponseBody
    public Result<?> batchDeleteStudents(@RequestBody Long[] studentIds) {
        try {
            int count = studentService.batchDeleteStudents(studentIds);
            return Result.success("批量删除成功，共删除 " + count + " 个学生");
        } catch (Exception e) {
            return Result.error(Result.FAIL, e.getMessage());
        }
    }

    /**
     * 管理员页面 - 获取学生详情
     */
    @GetMapping("/getStudentById")
    @ResponseBody
    public Result<Student> getStudentById(@RequestParam Long studentId) {
        try {
            Student student = studentService.getStudentById(studentId);
            return Result.success(student);
        } catch (Exception e) {
            return Result.error(Result.FAIL, e.getMessage());
        }
    }

    /**
     * 重置学生密码
     */
    @PostMapping("/resetPassword")
    @ResponseBody
    public Result<?> resetPassword(@RequestParam Long studentId) {
        try {
            boolean success = studentService.resetPassword(studentId);
            if (success) {
                return Result.success("密码重置成功");
            } else {
                return Result.error(Result.FAIL, "密码重置失败");
            }
        } catch (Exception e) {
            return Result.error(Result.FAIL, e.getMessage());
        }
    }
}
