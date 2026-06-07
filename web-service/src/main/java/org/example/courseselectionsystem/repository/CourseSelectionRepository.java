package org.example.courseselectionsystem.repository;

import org.example.courseselectionsystem.entity.CourseSelection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 选课记录数据访问层
 */
@Repository
public interface CourseSelectionRepository extends JpaRepository<CourseSelection, Long> {

    /**
     * 根据学生ID和课程ID查询选课记录
     * @param studentId 学生ID
     * @param courseId 课程ID
     * @return 选课记录
     */
    Optional<CourseSelection> findByStudentIdAndCourseId(Long studentId, Long courseId);

    /**
     * 根据学生ID查询选课记录列表
     * @param studentId 学生ID
     * @param pageable 分页参数
     * @return 选课记录分页列表
     */
    Page<CourseSelection> findByStudentId(Long studentId, Pageable pageable);

    /**
     * 根据学生ID和学期查询选课记录列表
     * @param studentId 学生ID
     * @param semester 学期
     * @param pageable 分页参数
     * @return 选课记录分页列表
     */
    Page<CourseSelection> findByStudentIdAndCourseCodeContaining(Long studentId, String semester, Pageable pageable);

    /**
     * 根据课程ID查询选课记录列表
     * @param courseId 课程ID
     * @param pageable 分页参数
     * @return 选课记录分页列表
     */
    Page<CourseSelection> findByCourseId(Long courseId, Pageable pageable);

    /**
     * 根据课程ID和状态查询选课记录列表
     * @param courseId 课程ID
     * @param status 状态：1-正常，2-退课，3-候补
     * @param pageable 分页参数
     * @return 选课记录分页列表
     */
    Page<CourseSelection> findByCourseIdAndStatus(Long courseId, Integer status, Pageable pageable);

    /**
     * 查询指定学生在指定学期的有效选课记录
     * @param studentId 学生ID
     * @param semester 学期
     * @param status 状态：1-正常
     * @return 选课记录列表
     */
    List<CourseSelection> findByStudentIdAndSemesterAndStatus(Long studentId, String semester, Integer status);

    Page<CourseSelection> findByStudentIdAndSemesterAndStatus(Long studentId, String semester, Integer status, Pageable pageable);

    Page<CourseSelection> findByStudentIdAndSemester(Long studentId, String semester, Pageable pageable);

    Page<CourseSelection> findByStudentIdAndStatus(Long studentId, Integer status, Pageable pageable);

    List<CourseSelection> findByCourseIdAndStatus(Long courseId, Integer status);

    List<CourseSelection> findByStudentId(Long studentId);

    List<CourseSelection> findByCourseId(Long courseId);

    List<CourseSelection> findByStudentIdAndCourseIdAndSemesterAndStatus(Long studentId, Long courseId, String semester, Integer status);

    List<CourseSelection> findByCourseIdAndStatusOrderBySelectionTimeAsc(Long courseId, Integer status);

    /**
     * 根据学生ID和状态统计选课数量
     * @param studentId 学生ID
     * @param status 状态：1-正常
     * @return 选课数量
     */
    long countByStudentIdAndStatus(Long studentId, Integer status);

    /**
     * 根据课程ID和状态统计选课数量
     * @param courseId 课程ID
     * @param status 状态：1-正常
     * @return 选课数量
     */
    long countByCourseIdAndStatus(Long courseId, Integer status);

    /**
     * 查询课程的候补记录，按选课时间升序排序
     * @param courseId 课程ID
     * @param status 状态：3-候补
     * @param limit 限制数量
     * @return 候补记录列表
     */
}
