package org.example.courseselectionsystem.controller;

import org.example.courseselectionsystem.common.Result;
import org.example.courseselectionsystem.entity.CourseSelection;
import org.example.courseselectionsystem.service.CourseSelectionService;
import org.example.courseselectionsystem.vo.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 选课控制器
 * 处理选课相关的HTTP请求
 */
@RestController
@RequestMapping("/api/v1/selections")
public class SelectionController {

    @Autowired
    private CourseSelectionService courseSelectionService;

    /**
     * 选课接口
     * @param studentId 学生ID
     * @param courseId 课程ID
     * @return 选课结果
     */
    @PostMapping
    public Result selectCourse(@RequestParam Long studentId, @RequestParam Long courseId) {
        Map<String, Object> result = courseSelectionService.selectCourse(studentId, courseId);
        return Result.success(result);
    }

    /**
     * 退课接口
     * @param selectionId 选课记录ID
     * @param studentId 学生ID
     * @return 操作结果
     */
    @DeleteMapping("/{selectionId}")
    public Result dropCourse(@PathVariable Long selectionId, @RequestParam Long studentId) {
        boolean result = courseSelectionService.dropCourse(selectionId, studentId);
        return Result.success("退课成功", result);
    }

    /**
     * 批量选课接口
     * @param batchSelectionInfo 批量选课请求
     * @return 选课结果
     */
    @PostMapping("/batch")
    public Result batchSelectCourses(@RequestBody Map<String, Object> batchSelectionInfo) {
        List<Long> studentIds = (List<Long>) batchSelectionInfo.get("studentIds");
        Long courseId = (Long) batchSelectionInfo.get("courseId");
        Map<String, Object> result = courseSelectionService.batchSelectCourses(studentIds, courseId);
        return Result.success(result);
    }

    /**
     * 获取选课记录详情接口
     * @param selectionId 选课记录ID
     * @return 选课记录
     */
    @GetMapping("/{selectionId}")
    public Result getCourseSelectionById(@PathVariable Long selectionId) {
        CourseSelection selection = courseSelectionService.getCourseSelectionById(selectionId);
        return Result.success(selection);
    }

    /**
     * 获取学生选课列表接口
     * @param studentId 学生ID
     * @param pageRequest 分页请求参数
     * @param semester 学期（可选）
     * @param status 状态（可选）
     * @return 选课记录列表
     */
    @GetMapping("/student/{studentId}")
    public Result getStudentCourseSelections(
            @PathVariable Long studentId,
            PageRequest pageRequest,
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) Integer status) {
        Page<CourseSelection> page = courseSelectionService.getStudentCourseSelections(studentId, pageRequest, semester, status);
        return Result.success(page);
    }

    /**
     * 获取课程选课列表接口
     * @param courseId 课程ID
     * @param pageRequest 分页请求参数
     * @param status 状态（可选）
     * @return 选课记录列表
     */
    @GetMapping("/course/{courseId}")
    public Result getCourseStudentList(
            @PathVariable Long courseId,
            PageRequest pageRequest,
            @RequestParam(required = false) Integer status) {
        Page<CourseSelection> page = courseSelectionService.getCourseStudentList(courseId, pageRequest, status);
        return Result.success(page);
    }

    /**
     * 检查学生是否已选课程接口
     * @param studentId 学生ID
     * @param courseId 课程ID
     * @return 是否已选
     */
    @GetMapping("/check")
    public Result isCourseSelected(@RequestParam Long studentId, @RequestParam Long courseId) {
        boolean isSelected = courseSelectionService.isCourseSelected(studentId, courseId);
        return Result.success(isSelected);
    }
    
    /**
     * 批量退课接口
     * @param selectionIds 选课记录ID列表
     * @return 操作结果
     */
    @DeleteMapping("/batch")
    public Result batchDropCourses(@RequestBody List<Long> selectionIds) {
        boolean result = courseSelectionService.batchDropCourses(selectionIds);
        return Result.success(result);
    }
    
    /**
     * 统计学生已选课程学分
     * @param studentId 学生ID
     * @param semester 学期
     * @return 已选学分
     */
    @GetMapping("/credits/{studentId}")
    public Result countSelectedCredits(@PathVariable Long studentId, @RequestParam String semester) {
        Double credits = courseSelectionService.countSelectedCredits(studentId, semester);
        return Result.success(credits);
    }

    /**
     * 获取学生当前学期选课列表
     * @param studentId 学生ID
     * @param semester 学期
     * @return 选课列表
     */
    @GetMapping("/current/{studentId}")
    public Result getStudentCurrentCourses(@PathVariable Long studentId, @RequestParam String semester) {
        List<CourseSelection> courses = courseSelectionService.getStudentCurrentCourses(studentId, semester);
        return Result.success(courses);
    }
    
    /**
     * 统计课程的实际选修人数
     * @param courseId 课程ID
     * @return 选修人数
     */
    @GetMapping("/count/{courseId}")
    public Result countCourseStudents(@PathVariable Long courseId) {
        long count = courseSelectionService.countCourseStudents(courseId);
        return Result.success(count);
    }
    
    /**
     * 根据条件查询选课记录
     * @param studentId 学生ID
     * @param courseId 课程ID
     * @param semester 学期
     * @param status 状态
     * @return 选课记录列表
     */
    @GetMapping("/query")
    public Result queryCourseSelections(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) Integer status) {
        List<CourseSelection> selections = courseSelectionService.queryCourseSelections(studentId, courseId, semester, status);
        return Result.success(selections);
    }
    
    /**
     * 获取选课记录详情（包含学生和课程信息）
     * @param selectionId 选课记录ID
     * @return 选课记录详情
     */
    @GetMapping("/{selectionId}/details")
    public Result getCourseSelectionDetails(@PathVariable Long selectionId) {
        CourseSelection selection = courseSelectionService.getCourseSelectionById(selectionId);
        return Result.success(selection);
    }
}