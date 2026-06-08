package org.example.courseselectionsystem.controller;

import org.example.courseselectionsystem.common.Result;
import org.example.courseselectionsystem.entity.Course;
import org.example.courseselectionsystem.service.CourseService;
import org.example.courseselectionsystem.vo.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 课程控制器
 */
@RestController
@RequestMapping("/api/v1/courses")
public class CourseController {

    @Autowired
    private CourseService courseService;

    /**
     * 添加课程
     * @param course 课程信息
     * @return 保存后的课程信息
     */
    @PostMapping
    public Result addCourse(@RequestBody Course course) {
        Course savedCourse = courseService.addCourse(course);
        return Result.success(savedCourse);
    }

    /**
     * 更新课程信息
     * @param courseId 课程ID
     * @param course 课程信息
     * @return 更新后的课程信息
     */
    @PutMapping("/{courseId}")
    public Result updateCourse(@PathVariable Long courseId, @RequestBody Course course) {
        course.setId(courseId);
        Course updatedCourse = courseService.updateCourse(course);
        return Result.success(updatedCourse);
    }

    /**
     * 根据ID获取课程信息
     * @param courseId 课程ID
     * @return 课程信息
     */
    @GetMapping("/{courseId}")
    public Result getCourseById(@PathVariable Long courseId) {
        Course course = courseService.getCourseById(courseId);
        return Result.success(course);
    }

    /**
     * 根据课程编号获取课程
     * @param courseCode 课程编号
     * @return 课程信息
     */
    @GetMapping("/code/{courseCode}")
    public Result getCourseByCode(@PathVariable String courseCode) {
        Course course = courseService.getCourseByCode(courseCode);
        return Result.success(course);
    }

    /**
     * 删除课程
     * @param courseId 课程ID
     * @return 删除结果
     */
    @DeleteMapping("/{courseId}")
    public Result deleteCourse(@PathVariable Long courseId) {
        boolean result = courseService.deleteCourse(courseId);
        return Result.success(result);
    }

    /**
     * 批量删除课程
     * @param courseIds 课程ID列表
     * @return 删除结果
     */
    @DeleteMapping("/batch")
    public Result batchDeleteCourses(@RequestBody List<Long> courseIds) {
        boolean result = courseService.batchDeleteCourses(courseIds);
        return Result.success(result);
    }

    /**
     * 获取课程列表
     * @param pageRequest 分页请求参数
     * @param courseName 课程名称
     * @param courseCode 课程编号
     * @param semester 学期
     * @param teacherName 教师名称
     * @param status 状态
     * @return 课程列表
     */
    @GetMapping("/list")
    public Result getCourseList(PageRequest pageRequest,
                               @RequestParam(required = false) String courseName,
                               @RequestParam(required = false) String courseCode,
                               @RequestParam(required = false) Long teacherId,
                               @RequestParam(required = false) Long departmentId,
                               @RequestParam(required = false) String courseType,
                               @RequestParam(required = false) Integer status) {
        Page<Course> coursePage = courseService.getCourseList(pageRequest, courseName, courseCode, teacherId, departmentId, courseType, status);
        return Result.success(coursePage);
    }

    /**
     * 获取所有启用的课程
     * @return 课程列表
     */
    @GetMapping("/active")
    public Result getActiveCourses() {
        List<Course> courses = courseService.getActiveCourses();
        return Result.success(courses);
    }

    /**
     * 根据学期获取启用的课程
     * @param semester 学期
     * @return 课程列表
     */
    @GetMapping("/active/{semester}")
    public Result getActiveCoursesBySemester(@PathVariable String semester) {
        List<Course> courses = courseService.getActiveCoursesBySemester(semester);
        return Result.success(courses);
    }

    /**
     * 根据学院获取课程
     * @param departmentId 学院ID
     * @return 课程列表
     */
    @GetMapping("/department/{departmentId}")
    public Result getCoursesByDepartment(@PathVariable Long departmentId) {
        List<Course> courses = courseService.getCoursesByDepartment(departmentId);
        return Result.success(courses);
    }

    /**
     * 根据教师获取课程
     * @param teacherId 教师ID
     * @return 课程列表
     */
    @GetMapping("/teacher/{teacherId}")
    public Result getCoursesByTeacher(@PathVariable Long teacherId) {
        List<Course> courses = courseService.getCoursesByTeacher(teacherId);
        return Result.success(courses);
    }

    /**
     * 修改课程状态
     * @param courseId 课程ID
     * @param status 状态
     * @return 修改结果
     */
    @PutMapping("/{courseId}/status")
    public Result changeCourseStatus(@PathVariable Long courseId, @RequestParam Integer status) {
        boolean result = courseService.changeCourseStatus(courseId, status);
        return Result.success(result);
    }

    /**
     * 搜索课程
     * @param keyword 关键字
     * @param departmentId 学院ID
     * @param courseType 课程类型
     * @param credit 学分
     * @return 课程列表
     */
    @GetMapping("/search")
    public Result searchCourses(@RequestParam(required = false) String keyword,
                              @RequestParam(required = false) Long departmentId,
                              @RequestParam(required = false) Integer courseType,
                              @RequestParam(required = false) Integer credit) {
        List<Course> courses = courseService.searchCourses(keyword, departmentId, courseType, credit);
        return Result.success(courses);
    }

    /**
     * 获取课程搜索建议
     * @param keyword 搜索关键词
     * @return 课程搜索建议列表
     */
    @GetMapping("/suggestions")
    public Result getCourseSuggestions(@RequestParam String keyword) {
        List<Course> courses = courseService.getCourseSuggestions(keyword);
        return Result.success(courses);
    }

    /**
     * 获取热门课程
     * @param limit 获取数量
     * @return 热门课程列表
     */
    @GetMapping("/popular")
    public Result getPopularCourses(@RequestParam(defaultValue = "10") Integer limit) {
        List<Course> courses = courseService.getPopularCourses(limit);
        return Result.success(courses);
    }
}
