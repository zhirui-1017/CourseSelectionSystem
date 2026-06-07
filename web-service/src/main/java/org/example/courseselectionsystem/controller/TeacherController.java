package org.example.courseselectionsystem.controller;

import org.example.courseselectionsystem.common.Result;
import org.example.courseselectionsystem.entity.Course;
import org.example.courseselectionsystem.entity.CourseSelection;
import org.example.courseselectionsystem.entity.Student;
import org.example.courseselectionsystem.entity.Teacher;
import org.example.courseselectionsystem.exception.BusinessException;
import org.example.courseselectionsystem.repository.CourseSelectionRepository;
import org.example.courseselectionsystem.service.CourseService;
import org.example.courseselectionsystem.service.StudentService;
import org.example.courseselectionsystem.service.TeacherService;
import org.example.courseselectionsystem.vo.PageRequest;
import org.example.courseselectionsystem.vo.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 教师控制器
 * 处理教师相关的HTTP请求
 */
@Controller
@RequestMapping("/teacher")
public class TeacherController {

    @Autowired
    private TeacherService teacherService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private CourseSelectionRepository courseSelectionRepository;

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
        
        return "redirect:/teacher/index.html";
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

    @GetMapping("/current")
    @ResponseBody
    public Result<?> currentTeacher(HttpSession session) {
        try {
            Long teacherId = requireTeacherId(session);
            return Result.success(teacherService.getTeacherById(teacherId));
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            return Result.error(Result.FAIL, e.getMessage());
        }
    }

    @GetMapping("/myCourses")
    @ResponseBody
    public Result<?> myCourses(HttpSession session) {
        try {
            Long teacherId = requireTeacherId(session);
            List<Course> courses = courseService.getCoursesByTeacher(teacherId);
            return Result.success(courses == null ? Collections.emptyList() : courses);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            return Result.error(Result.FAIL, e.getMessage());
        }
    }

    @GetMapping("/courseStudents")
    @ResponseBody
    public Result<?> courseStudents(@RequestParam Long courseId,
                                    @RequestParam(required = false) Integer status,
                                    HttpSession session) {
        try {
            Long teacherId = requireTeacherId(session);
            Course course = requireOwnedCourse(courseId, teacherId);
            List<CourseSelection> selections = status == null
                    ? courseSelectionRepository.findByCourseId(courseId)
                    : courseSelectionRepository.findByCourseIdAndStatus(courseId, status);

            List<Map<String, Object>> rows = selections.stream()
                    .map(selection -> toStudentRow(selection, course))
                    .collect(Collectors.toList());
            return Result.success(rows);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            return Result.error(Result.FAIL, e.getMessage());
        }
    }

    @GetMapping("/dashboard")
    @ResponseBody
    public Result<?> dashboard(HttpSession session) {
        try {
            Long teacherId = requireTeacherId(session);
            List<Course> courses = courseService.getCoursesByTeacher(teacherId);
            if (courses == null) {
                courses = Collections.emptyList();
            }

            long selectedCount = 0L;
            long gradedCount = 0L;
            long waitingCount = 0L;
            double scoreTotal = 0D;
            long scoreCount = 0L;
            List<Map<String, Object>> recentSelections = new ArrayList<>();

            for (Course course : courses) {
                List<CourseSelection> selections = courseSelectionRepository.findByCourseId(course.getId());
                for (CourseSelection selection : selections) {
                    if (Objects.equals(selection.getStatus(), 1)) {
                        selectedCount++;
                        if (selection.getScore() != null) {
                            gradedCount++;
                            scoreTotal += selection.getScore();
                            scoreCount++;
                        }
                        recentSelections.add(toStudentRow(selection, course));
                    } else if (Objects.equals(selection.getStatus(), 3)) {
                        waitingCount++;
                    }
                }
            }

            recentSelections.sort((left, right) -> {
                Date leftTime = (Date) left.get("selectionTime");
                Date rightTime = (Date) right.get("selectionTime");
                if (leftTime == null && rightTime == null) {
                    return 0;
                }
                if (leftTime == null) {
                    return 1;
                }
                if (rightTime == null) {
                    return -1;
                }
                return rightTime.compareTo(leftTime);
            });
            if (recentSelections.size() > 8) {
                recentSelections = recentSelections.subList(0, 8);
            }

            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("courseCount", courses.size());
            stats.put("studentCount", selectedCount);
            stats.put("gradedCount", gradedCount);
            stats.put("waitingCount", waitingCount);
            stats.put("averageScore", scoreCount == 0 ? null : Math.round((scoreTotal / scoreCount) * 10D) / 10D);
            stats.put("courses", courses);
            stats.put("recentSelections", recentSelections);
            return Result.success(stats);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            return Result.error(Result.FAIL, e.getMessage());
        }
    }

    @PostMapping("/updateGrade")
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public Result<?> updateGrade(@RequestBody Map<String, Object> gradeInfo, HttpSession session) {
        try {
            Long teacherId = requireTeacherId(session);
            Long selectionId = longValue(gradeInfo.get("selectionId"));
            if (selectionId == null) {
                return Result.error(Result.PARAM_ERROR, "selectionId不能为空");
            }

            CourseSelection selection = courseSelectionRepository.findById(selectionId)
                    .orElseThrow(() -> new BusinessException(Result.NOT_FOUND, "选课记录不存在"));
            requireOwnedCourse(selection.getCourseId(), teacherId);

            Double dailyGrade = gradeValue(gradeInfo.get("dailyGrade"), "平时成绩");
            Double labGrade = gradeValue(gradeInfo.get("labGrade"), "实验成绩");
            Double examGrade = gradeValue(gradeInfo.get("examGrade"), "考试成绩");
            Double score = gradeValue(gradeInfo.get("score"), "总评成绩");

            if (score == null) {
                score = calculateScore(dailyGrade, labGrade, examGrade);
            }

            selection.setDailyGrade(dailyGrade);
            selection.setLabGrade(labGrade);
            selection.setExamGrade(examGrade);
            selection.setScore(score);
            selection.setRemark(stringValue(gradeInfo.get("remark")));
            selection.setUpdateTime(new Date());

            CourseSelection saved = courseSelectionRepository.save(selection);
            return Result.success(toStudentRow(saved, courseService.getCourseById(saved.getCourseId())));
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            return Result.error(Result.FAIL, e.getMessage());
        }
    }

    private Long requireTeacherId(HttpSession session) {
        if (session == null || !"teacher".equals(session.getAttribute("role"))) {
            throw new BusinessException(Result.NOT_LOGIN, "请先以教师身份登录");
        }
        Object userId = session.getAttribute("userId");
        Long teacherId = longValue(userId);
        if (teacherId == null) {
            throw new BusinessException(Result.NOT_LOGIN, "教师登录信息已失效，请重新登录");
        }
        return teacherId;
    }

    private Course requireOwnedCourse(Long courseId, Long teacherId) {
        if (courseId == null) {
            throw new BusinessException(Result.PARAM_ERROR, "课程ID不能为空");
        }
        Course course = courseService.getCourseById(courseId);
        if (!Objects.equals(course.getTeacherId(), teacherId)) {
            throw new BusinessException(403, "无权访问该课程");
        }
        return course;
    }

    private Map<String, Object> toStudentRow(CourseSelection selection, Course course) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("selectionId", selection.getId());
        row.put("courseId", selection.getCourseId());
        row.put("courseCode", course.getCourseCode());
        row.put("courseName", course.getCourseName());
        row.put("studentId", selection.getStudentId());
        row.put("selectionTime", selection.getSelectionTime());
        row.put("dailyGrade", selection.getDailyGrade());
        row.put("labGrade", selection.getLabGrade());
        row.put("examGrade", selection.getExamGrade());
        row.put("score", selection.getScore());
        row.put("scoreLevel", scoreLevel(selection.getScore()));
        row.put("remark", selection.getRemark());
        row.put("status", selection.getStatus());
        row.put("statusText", selectionStatus(selection.getStatus()));

        try {
            Student student = studentService.getStudentById(selection.getStudentId());
            row.put("studentNo", student.getStudentNo());
            row.put("studentName", student.getName());
            row.put("gender", student.getGender());
            row.put("majorId", student.getMajorId());
            row.put("collegeId", student.getCollegeId());
            row.put("className", student.getClassName());
            row.put("phone", student.getPhone());
            row.put("email", student.getEmail());
        } catch (Exception ignored) {
            row.put("studentNo", selection.getStudentId());
            row.put("studentName", "学生" + selection.getStudentId());
        }
        return row;
    }

    private Double calculateScore(Double dailyGrade, Double labGrade, Double examGrade) {
        if (dailyGrade == null && labGrade == null && examGrade == null) {
            return null;
        }
        double daily = dailyGrade == null ? 0D : dailyGrade;
        double lab = labGrade == null ? 0D : labGrade;
        double exam = examGrade == null ? 0D : examGrade;
        return Math.round((daily * 0.4D + lab * 0.2D + exam * 0.4D) * 10D) / 10D;
    }

    private String scoreLevel(Double score) {
        if (score == null) {
            return "未录入";
        }
        if (score >= 90D) {
            return "优秀";
        }
        if (score >= 80D) {
            return "良好";
        }
        if (score >= 70D) {
            return "中等";
        }
        if (score >= 60D) {
            return "及格";
        }
        return "不及格";
    }

    private String selectionStatus(Integer status) {
        if (Objects.equals(status, 1)) {
            return "已选";
        }
        if (Objects.equals(status, 2)) {
            return "已退课";
        }
        if (Objects.equals(status, 3)) {
            return "候补";
        }
        return "未知";
    }

    private Double gradeValue(Object value, String label) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        try {
            double number = Double.parseDouble(String.valueOf(value));
            if (number < 0D || number > 100D) {
                throw new BusinessException(Result.PARAM_ERROR, label + "必须在0到100之间");
            }
            return number;
        } catch (NumberFormatException e) {
            throw new BusinessException(Result.PARAM_ERROR, label + "必须是数字");
        }
    }

    private Long longValue(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }
}
