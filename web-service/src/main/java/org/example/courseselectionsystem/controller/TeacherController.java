package org.example.courseselectionsystem.controller;

import org.example.courseselectionsystem.common.Result;
import org.example.courseselectionsystem.entity.Teacher;
import org.example.courseselectionsystem.service.TeacherService;
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
 * 教师控制器
 * 处理教师相关的HTTP请求
 */
@Controller
@RequestMapping("/teacher")
public class TeacherController {

    @Autowired
    private TeacherService teacherService;

    /**
     * 跳转到教师首页
     */
    @GetMapping("/index")
    public String index(Model model, HttpSession session) {
        // 获取当前登录教师信息
        Object user = session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        // 如果session中存储的是Teacher对象
        if (user instanceof Teacher) {
            model.addAttribute("teacher", user);
        }
        
        return "teacher/index";
    }

    /**
     * 跳转到教师个人信息页面
     */
    @GetMapping("/profile")
    public String profile(Model model, HttpSession session) {
        // 从会话中获取教师ID
        Long teacherId = (Long) session.getAttribute("userId");
        if (teacherId == null) {
            return "redirect:/login";
        }
        
        try {
            // 获取教师详情
            Teacher teacher = teacherService.getTeacherById(teacherId);
            model.addAttribute("teacher", teacher);
            return "teacher/profile";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    /**
     * 更新教师个人信息
     */
    @PostMapping("/updateProfile")
    @ResponseBody
    public Result<?> updateProfile(@RequestBody Teacher teacher, HttpSession session) {
        try {
            // 从会话中获取教师ID
            Long teacherId = (Long) session.getAttribute("userId");
            if (teacherId == null) {
                return Result.error(Result.NOT_LOGIN, "用户未登录");
            }
            
            // 确保只能修改自己的信息
            teacher.setId(teacherId);
            
            // 调用服务层更新信息
            boolean success = teacherService.updateTeacher(teacher);
            if (success) {
                // 更新会话中的用户信息
                Teacher updatedTeacher = teacherService.getTeacherById(teacherId);
                session.setAttribute("user", updatedTeacher);
                
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
            // 从会话中获取教师ID
            Long teacherId = (Long) session.getAttribute("userId");
            if (teacherId == null) {
                return Result.error(Result.NOT_LOGIN, "用户未登录");
            }
            
            // 调用服务层修改密码
            boolean success = teacherService.changePassword(teacherId, oldPassword, newPassword);
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
     * 管理员页面 - 教师列表
     */
    @GetMapping("/list")
    public String teacherList(Model model, 
                           @RequestParam(defaultValue = "1") Integer pageNum,
                           @RequestParam(defaultValue = "10") Integer pageSize,
                           @RequestParam(required = false) String teacherName,
                           @RequestParam(required = false) String teacherId,
                           @RequestParam(required = false) Long departmentId) {
        // 创建分页请求参数
        PageRequest pageRequest = new PageRequest();
        pageRequest.setPageNum(pageNum);
        pageRequest.setPageSize(pageSize);
        
        // 创建查询条件
        Map<String, Object> params = new HashMap<>();
        if (teacherName != null && !teacherName.isEmpty()) {
            params.put("teacherName", teacherName);
        }
        if (teacherId != null && !teacherId.isEmpty()) {
            params.put("teacherId", teacherId);
        }
        if (departmentId != null) {
            params.put("departmentId", departmentId);
        }
        pageRequest.setParams(params);
        
        try {
            // 获取教师列表（分页）
            PageResult<Teacher> pageResult = teacherService.getTeachersByPage(pageRequest);
            
            model.addAttribute("pageResult", pageResult);
            model.addAttribute("teacherName", teacherName);
            model.addAttribute("teacherId", teacherId);
            model.addAttribute("departmentId", departmentId);
            
            return "teacher/list";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    /**
     * 管理员页面 - 添加教师
     */
    @PostMapping("/add")
    @ResponseBody
    public Result<?> addTeacher(@RequestBody Teacher teacher) {
        try {
            boolean success = teacherService.addTeacher(teacher);
            if (success) {
                return Result.success("教师添加成功");
            } else {
                return Result.error(Result.FAIL, "教师添加失败");
            }
        } catch (Exception e) {
            return Result.error(Result.FAIL, e.getMessage());
        }
    }

    /**
     * 管理员页面 - 更新教师信息
     */
    @PostMapping("/update")
    @ResponseBody
    public Result<?> updateTeacher(@RequestBody Teacher teacher) {
        try {
            boolean success = teacherService.updateTeacher(teacher);
            if (success) {
                return Result.success("教师信息更新成功");
            } else {
                return Result.error(Result.FAIL, "教师信息更新失败");
            }
        } catch (Exception e) {
            return Result.error(Result.FAIL, e.getMessage());
        }
    }

    /**
     * 管理员页面 - 删除教师
     */
    @PostMapping("/delete")
    @ResponseBody
    public Result<?> deleteTeacher(@RequestParam Long teacherId) {
        try {
            boolean success = teacherService.deleteTeacher(teacherId);
            if (success) {
                return Result.success("教师删除成功");
            } else {
                return Result.error(Result.FAIL, "教师删除失败");
            }
        } catch (Exception e) {
            return Result.error(Result.FAIL, e.getMessage());
        }
    }

    /**
     * 管理员页面 - 批量删除教师
     */
    @PostMapping("/batchDelete")
    @ResponseBody
    public Result<?> batchDeleteTeachers(@RequestBody Long[] teacherIds) {
        try {
            int count = teacherService.batchDeleteTeachers(teacherIds);
            return Result.success("批量删除成功，共删除 " + count + " 个教师");
        } catch (Exception e) {
            return Result.error(Result.FAIL, e.getMessage());
        }
    }

    /**
     * 管理员页面 - 获取教师详情
     */
    @GetMapping("/getTeacherById")
    @ResponseBody
    public Result<Teacher> getTeacherById(@RequestParam Long teacherId) {
        try {
            Teacher teacher = teacherService.getTeacherById(teacherId);
            return Result.success(teacher);
        } catch (Exception e) {
            return Result.error(Result.FAIL, e.getMessage());
        }
    }

    /**
     * 重置教师密码
     */
    @PostMapping("/resetPassword")
    @ResponseBody
    public Result<?> resetPassword(@RequestParam Long teacherId) {
        try {
            boolean success = teacherService.resetPassword(teacherId);
            if (success) {
                return Result.success("密码重置成功");
            } else {
                return Result.error(Result.FAIL, "密码重置失败");
            }
        } catch (Exception e) {
            return Result.error(Result.FAIL, e.getMessage());
        }
    }

    /**
     * 获取所有教师列表（用于下拉选择）
     */
    @GetMapping("/getAllTeachers")
    @ResponseBody
    public Result<?> getAllTeachers() {
        try {
            return Result.success(teacherService.getAllTeachers());
        } catch (Exception e) {
            return Result.error(Result.FAIL, e.getMessage());
        }
    }
}
