package org.example.courseselectionsystem.controller;

import org.example.courseselectionsystem.common.Result;
import org.example.courseselectionsystem.component.RedisLock;
import org.example.courseselectionsystem.entity.CourseSelection;
import org.example.courseselectionsystem.exception.BusinessException;
import org.example.courseselectionsystem.service.CourseSelectionService;
import org.example.courseselectionsystem.vo.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 选课控制器
 */
@RestController
@RequestMapping("/api/v1/course-selections")
public class CourseSelectionController {

    @Autowired
    private CourseSelectionService courseSelectionService;

    @Autowired
    private RedisLock redisLock;

    /**
     * 学生选课
     * @param studentId 学生ID
     * @param courseId 课程ID
     * @return 选课结果
     */
    @PostMapping
    public Result selectCourse(@RequestParam Long studentId, @RequestParam Long courseId) {
        if (courseId == null || studentId == null) {
            throw new BusinessException(Result.PARAM_ERROR, "学生ID和课程ID不能为空");
        }
        // === Redis 分布式锁：同一课程并发抢选时串行化“查容量-写选课”，防止超员 ===
        String owner = redisLock.newOwner();
        if (!redisLock.tryLockCourseWithRetry(courseId, owner, 15)) {
            throw new BusinessException(Result.PARAM_ERROR, "该课程当前选课人数过多，请稍后重试");
        }
        try {
            Map<String, Object> result = courseSelectionService.selectCourse(studentId, courseId);
            return Result.success(result);
        } finally {
            redisLock.unlockCourse(courseId, owner);
        }
    }

    /**
     * 学生退课
     * @param selectionId 选课记录ID
     * @param studentId 学生ID
     * @return 退课结果
     */
    @DeleteMapping("/{selectionId}")
    public Result dropCourse(@PathVariable Long selectionId, @RequestParam Long studentId) {
        // === Redis 分布式锁：退课与“候补晋升”串行化，避免名额释放被并发抢占 ===
        String owner = redisLock.newOwner();
        if (!redisLock.tryLockSelection(selectionId, owner)) {
            throw new BusinessException(Result.PARAM_ERROR, "该选课记录正在处理中，请稍后重试");
        }
        try {
            boolean result = courseSelectionService.dropCourse(selectionId, studentId);
            return Result.success("退课成功", result);
        } finally {
            redisLock.unlockSelection(selectionId, owner);
        }
    }

    /**
     * 根据ID获取选课记录
     * @param selectionId 选课记录ID
     * @return 选课记录
     */
    @GetMapping("/{selectionId}")
    public Result getCourseSelectionById(@PathVariable Long selectionId) {
        CourseSelection selection = courseSelectionService.getCourseSelectionById(selectionId);
        return Result.success(selection);
    }

    /**
     * 获取学生的选课列表
     * @param studentId 学生ID
     * @param pageRequest 分页请求参数
     * @param semester 学期
     * @param status 状态
     * @return 选课列表
     */
    @GetMapping("/student/{studentId}")
    public Result getStudentCourseSelections(@PathVariable Long studentId, PageRequest pageRequest,
                                          @RequestParam(required = false) String semester,
                                          @RequestParam(required = false) Integer status) {
        Page<CourseSelection> page = courseSelectionService.getStudentCourseSelections(studentId, pageRequest, semester, status);
        return Result.success(page);
    }

    /**
     * 获取课程的选课学生列表
     * @param courseId 课程ID
     * @param pageRequest 分页请求参数
     * @param status 状态
     * @return 选课学生列表
     */
    @GetMapping("/course/{courseId}")
    public Result getCourseStudentList(@PathVariable Long courseId, PageRequest pageRequest,
                                    @RequestParam(required = false) Integer status) {
        Page<CourseSelection> page = courseSelectionService.getCourseStudentList(courseId, pageRequest, status);
        return Result.success(page);
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
     * 检查学生是否已选某课程
     * @param studentId 学生ID
     * @param courseId 课程ID
     * @return 是否已选
     */
    @GetMapping("/check")
    public Result isCourseSelected(@RequestParam Long studentId, @RequestParam Long courseId) {
        boolean result = courseSelectionService.isCourseSelected(studentId, courseId);
        return Result.success(result);
    }

    /**
     * 批量选课
     * @param batchSelectionInfo 批量选课信息
     * @return 选课结果
     */
    @PostMapping("/batch")
    public Result batchSelectCourses(@RequestBody Map<String, Object> batchSelectionInfo) {
        List<Long> studentIds = toLongList(batchSelectionInfo.get("studentIds"));
        Long courseId = toLong(batchSelectionInfo.get("courseId"));
        Map<String, Object> result = courseSelectionService.batchSelectCourses(studentIds, courseId);
        return Result.success(result);
    }

    /**
     * 将请求体中的值安全转换为 Long（兼容 Integer/Long/字符串等类型）
     * 注意：Jackson 反序列化 JSON 数字时可能生成 Integer，直接 (Long) 强转会抛 ClassCastException
     */
    private static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    /**
     * 将请求体中的值安全转换为 List&lt;Long&gt;（JSON 数字可能被反序列化为 Integer）
     */
    private static List<Long> toLongList(Object value) {
        if (!(value instanceof List<?>)) {
            return null;
        }
        List<Long> ids = new ArrayList<>();
        for (Object item : (List<?>) value) {
            if (item != null) {
                ids.add(toLong(item));
            }
        }
        return ids;
    }
    
    /**
     * 批量退课
     * @param selectionIds 选课记录ID列表
     * @return 退课结果
     */
    @DeleteMapping("/batch")
    public Result batchDropCoursesForStudent(@RequestBody List<Long> selectionIds) {
        boolean result = courseSelectionService.batchDropCourses(selectionIds);
        return Result.success(result);
    }

    /**
     * 批量退课（管理员功能）
     * @param selectionIds 选课记录ID列表
     * @return 退课结果
     */
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
     * 获取全部选课记录总数（供 Feign 跨服务调用）
     * @return 选课总数
     */
    @GetMapping("/count")
    public Result<Map<String, Object>> countAll() {
        long count = courseSelectionService.countAll();
        return Result.success(Map.of("count", count));
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
    public Result queryCourseSelections(@RequestParam(required = false) Long studentId,
                                     @RequestParam(required = false) Long courseId,
                                     @RequestParam(required = false) String semester,
                                     @RequestParam(required = false) Integer status) {
        List<CourseSelection> selections = courseSelectionService.queryCourseSelections(studentId, courseId, semester, status);
        return Result.success(selections);
    }

    /**
     * 获取选课统计信息
     * @return 选课统计信息
     */
    @GetMapping("/stats")
    public Result getSelectionStats() {
        return Result.success(courseSelectionService.getSelectionStats());
    }

    @GetMapping("/teacher/course/{courseId}/students")
    public Result getTeacherCourseStudents(@PathVariable Long courseId,
                                           @RequestParam Long teacherId,
                                           @RequestParam(required = false) Integer status) {
        return Result.success(courseSelectionService.getTeacherCourseStudents(courseId, teacherId, status));
    }

    @GetMapping("/teacher/dashboard")
    public Result getTeacherDashboard(@RequestParam Long teacherId) {
        return Result.success(courseSelectionService.getTeacherDashboard(teacherId));
    }

    @PostMapping("/{selectionId}/grade")
    public Result updateGrade(@PathVariable Long selectionId,
                              @RequestParam Long teacherId,
                              @RequestBody Map<String, Object> gradeInfo) {
        return Result.success(courseSelectionService.updateGrade(selectionId, teacherId, gradeInfo));
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
