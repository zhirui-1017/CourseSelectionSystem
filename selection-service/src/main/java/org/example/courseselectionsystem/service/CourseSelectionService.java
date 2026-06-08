package org.example.courseselectionsystem.service;

import org.example.courseselectionsystem.entity.CourseSelection;
import org.example.courseselectionsystem.vo.PageRequest;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

/**
 * 选课服务接口
 */
public interface CourseSelectionService {

    /**
     * 学生选课
     * @param studentId 学生ID
     * @param courseId 课程ID
     * @return 选课结果
     */
    Map<String, Object> selectCourse(Long studentId, Long courseId);

    /**
     * 学生退课
     * @param selectionId 选课记录ID
     * @param studentId 学生ID
     * @return 退课结果
     */
    boolean dropCourse(Long selectionId, Long studentId);

    /**
     * 获取选课记录详情
     * @param selectionId 选课记录ID
     * @return 选课记录对象
     */
    CourseSelection getCourseSelectionById(Long selectionId);

    /**
     * 获取学生选课列表
     * @param studentId 学生ID
     * @param pageRequest 分页请求参数
     * @param semester 学期
     * @param status 状态
     * @return 选课记录分页列表
     */
    Page<CourseSelection> getStudentCourseSelections(Long studentId, PageRequest pageRequest, String semester, Integer status);

    /**
     * 获取课程的选课列表
     * @param courseId 课程ID
     * @param pageRequest 分页请求参数
     * @param status 状态
     * @return 选课记录分页列表
     */
    Page<CourseSelection> getCourseStudentList(Long courseId, PageRequest pageRequest, Integer status);

    /**
     * 统计学生已选课程学分
     * @param studentId 学生ID
     * @param semester 学期
     * @return 已选学分
     */
    Double countSelectedCredits(Long studentId, String semester);

    /**
     * 检查学生是否已选某课程
     * @param studentId 学生ID
     * @param courseId 课程ID
     * @return 是否已选
     */
    boolean isCourseSelected(Long studentId, Long courseId);

    /**
     * 批量选课（管理员功能）
     * @param studentIds 学生ID列表
     * @param courseId 课程ID
     * @return 选课结果
     */
    Map<String, Object> batchSelectCourses(List<Long> studentIds, Long courseId);

    /**
     * 批量退课（管理员功能）
     * @param selectionIds 选课记录ID列表
     * @return 退课结果
     */
    boolean batchDropCourses(List<Long> selectionIds);

    /**
     * 获取学生当前学期选课列表（不包含分页）
     * @param studentId 学生ID
     * @param semester 学期
     * @return 选课记录列表
     */
    List<CourseSelection> getStudentCurrentCourses(Long studentId, String semester);

    /**
     * 计算课程的实际选修人数
     * @param courseId 课程ID
     * @return 选修人数
     */
    long countCourseStudents(Long courseId);

    /**
     * 根据条件查询选课记录
     * @param studentId 学生ID
     * @param courseId 课程ID
     * @param semester 学期
     * @param status 状态
     * @return 选课记录列表
     */
    List<CourseSelection> queryCourseSelections(Long studentId, Long courseId, String semester, Integer status);

    Map<String, Object> getSelectionStats();

    Map<String, Object> updateGrade(Long selectionId, Long teacherId, Map<String, Object> gradeInfo);

    List<Map<String, Object>> getTeacherCourseStudents(Long courseId, Long teacherId, Integer status);

    Map<String, Object> getTeacherDashboard(Long teacherId);
}
