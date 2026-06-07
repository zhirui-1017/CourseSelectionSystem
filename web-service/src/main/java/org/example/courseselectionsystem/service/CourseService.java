package org.example.courseselectionsystem.service;

import org.example.courseselectionsystem.entity.Course;
import org.example.courseselectionsystem.vo.PageRequest;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

/**
 * 课程服务接口
 */
public interface CourseService {

    /**
     * 添加课程
     * @param course 课程对象
     * @return 保存后的课程对象
     */
    Course addCourse(Course course);

    /**
     * 更新课程信息
     * @param course 课程对象
     * @return 更新后的课程对象
     */
    Course updateCourse(Course course);

    /**
     * 根据ID获取课程信息
     * @param courseId 课程ID
     * @return 课程对象
     */
    Course getCourseById(Long courseId);

    /**
     * 根据课程编号获取课程
     * @param courseCode 课程编号
     * @return 课程对象
     */
    Course getCourseByCode(String courseCode);

    /**
     * 删除课程
     * @param courseId 课程ID
     * @return 删除结果
     */
    boolean deleteCourse(Long courseId);

    /**
     * 批量删除课程
     * @param courseIds 课程ID列表
     * @return 删除结果
     */
    boolean batchDeleteCourses(List<Long> courseIds);

    /**
     * 分页查询课程列表
     * @param pageRequest 分页请求参数
     * @param courseName 课程名称（模糊查询）
     * @param courseCode 课程编号
     * @param teacherId 教师ID
     * @param departmentId 学院ID
     * @param status 状态
     * @return 课程分页列表
     */
    Page<Course> getCourseList(PageRequest pageRequest, String courseName, String courseCode, Long teacherId, Long departmentId, Integer status);

    /**
     * 获取所有启用的课程
     * @return 课程列表
     */
    List<Course> getActiveCourses();

    default List<Course> getActiveCoursesBySemester(String semester) {
        return getActiveCourses();
    }

    default List<Course> getCourseSuggestions(String keyword) {
        return searchCourses(keyword, null, null, null);
    }

    default List<Course> getPopularCourses(Integer limit) {
        return getActiveCourses();
    }

    default Map<String, Object> getCourseListByPage(PageRequest pageRequest) {
        Page<Course> page = getCourseList(pageRequest, null, null, null, null, null);
        return Map.of("items", page.getContent(), "total", page.getTotalElements());
    }

    default Course addCourse(Map<String, Object> courseInfo) {
        Course course = new Course();
        course.setCourseName(String.valueOf(courseInfo.getOrDefault("courseName", "")));
        course.setCourseCode(String.valueOf(courseInfo.getOrDefault("courseCode", "")));
        course.setTeacherId(longValue(courseInfo, "teacherId", 1L));
        course.setCredit(doubleValue(courseInfo, "credit", doubleValue(courseInfo, "credits", 2D)));
        course.setTotalHours(intValue(courseInfo, "totalHours", 32));
        course.setAvailableSlots(intValue(courseInfo, "availableSlots", intValue(courseInfo, "capacity", 40)));
        course.setSelectedCount(intValue(courseInfo, "selectedCount", 0));
        course.setClassroom(stringValue(courseInfo, "classroom", "待安排"));
        course.setSchedule(stringValue(courseInfo, "schedule", "待安排"));
        course.setCourseType(stringValue(courseInfo, "courseType", "选修课"));
        course.setDescription(stringValue(courseInfo, "description", ""));
        course.setStatus(intValue(courseInfo, "status", 1));
        return addCourse(course);
    }

    default Course updateCourse(Map<String, Object> courseInfo) {
        Course course = new Course();
        Object id = courseInfo.get("id");
        if (id != null) {
            course.setId(Long.valueOf(String.valueOf(id)));
        }
        course.setCourseName(String.valueOf(courseInfo.getOrDefault("courseName", "")));
        course.setCourseCode(String.valueOf(courseInfo.getOrDefault("courseCode", "")));
        course.setTeacherId(longValue(courseInfo, "teacherId", 1L));
        course.setCredit(doubleValue(courseInfo, "credit", doubleValue(courseInfo, "credits", 2D)));
        course.setTotalHours(intValue(courseInfo, "totalHours", 32));
        course.setAvailableSlots(intValue(courseInfo, "availableSlots", intValue(courseInfo, "capacity", 40)));
        course.setSelectedCount(intValue(courseInfo, "selectedCount", 0));
        course.setClassroom(stringValue(courseInfo, "classroom", "待安排"));
        course.setSchedule(stringValue(courseInfo, "schedule", "待安排"));
        course.setCourseType(stringValue(courseInfo, "courseType", "选修课"));
        course.setDescription(stringValue(courseInfo, "description", ""));
        course.setStatus(intValue(courseInfo, "status", 1));
        return updateCourse(course);
    }

    private static String stringValue(Map<String, Object> source, String key, String defaultValue) {
        Object value = source.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return defaultValue;
        }
        return String.valueOf(value);
    }

    private static Long longValue(Map<String, Object> source, String key, Long defaultValue) {
        Object value = source.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return defaultValue;
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static Integer intValue(Map<String, Object> source, String key, Integer defaultValue) {
        Object value = source.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static Double doubleValue(Map<String, Object> source, String key, Double defaultValue) {
        Object value = source.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return defaultValue;
        }
        try {
            return Double.valueOf(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    default boolean deleteCourse(String courseId) {
        return deleteCourse(Long.valueOf(courseId));
    }

    /**
     * 查找指定学院的课程
     * @param departmentId 学院ID
     * @return 课程列表
     */
    List<Course> getCoursesByDepartment(Long departmentId);

    /**
     * 查找指定教师的课程
     * @param teacherId 教师ID
     * @return 课程列表
     */
    List<Course> getCoursesByTeacher(Long teacherId);

    /**
     * 启用/禁用课程
     * @param courseId 课程ID
     * @param status 状态：1-启用，2-禁用
     * @return 操作结果
     */
    boolean changeCourseStatus(Long courseId, Integer status);

    /**
     * 根据条件搜索课程（学生选课使用）
     * @param keyword 关键字（课程名称、课程编号、教师名称）
     * @param departmentId 学院ID
     * @param courseType 课程类型
     * @param credit 学分
     * @return 课程列表
     */
    List<Course> searchCourses(String keyword, Long departmentId, Integer courseType, Integer credit);
}
