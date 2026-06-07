package org.example.courseselectionsystem.repository;

import org.example.courseselectionsystem.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 课程数据访问层
 */
@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    /**
     * 根据课程编号查询课程
     * @param courseCode 课程编号
     * @return 课程对象
     */
    Optional<Course> findByCourseCode(String courseCode);

    /**
     * 根据课程名称模糊查询课程
     * @param courseName 课程名称
     * @param pageable 分页参数
     * @return 课程分页列表
     */
    Page<Course> findByCourseNameContaining(String courseName, Pageable pageable);

    /**
     * 根据教师ID查询课程
     * @param teacherId 教师ID
     * @param pageable 分页参数
     * @return 课程分页列表
     */
    Page<Course> findByTeacherId(Long teacherId, Pageable pageable);

    /**
     * 根据学院ID查询课程
     * @param departmentId 学院ID
     * @param pageable 分页参数
     * @return 课程分页列表
     */
    Page<Course> findByDepartmentId(Long departmentId, Pageable pageable);

    /**
     * 根据学期查询课程
     * @param semester 学期
     * @param pageable 分页参数
     * @return 课程分页列表
     */
    Page<Course> findBySemester(String semester, Pageable pageable);

    /**
     * 根据课程类型查询课程
     * @param courseType 课程类型：1-必修课，2-选修课，3-通识课
     * @param pageable 分页参数
     * @return 课程分页列表
     */
    Page<Course> findByCourseType(Integer courseType, Pageable pageable);

    /**
     * 根据学期和教师ID查询课程
     * @param semester 学期
     * @param teacherId 教师ID
     * @return 课程列表
     */
    List<Course> findBySemesterAndTeacherId(String semester, Long teacherId);

    /**
     * 多条件查询课程
     * @param courseName 课程名称
     * @param courseCode 课程编号
     * @param teacherName 教师名称
     * @param departmentId 学院ID
     * @param courseType 课程类型
     * @param semester 学期
     * @param status 状态
     * @param pageable 分页参数
     * @return 课程分页列表
     */
    @Query("SELECT c FROM Course c WHERE 1=1 " +
            "AND (:courseName IS NULL OR c.courseName LIKE CONCAT('%', :courseName, '%')) " +
            "AND (:courseCode IS NULL OR c.courseCode LIKE CONCAT('%', :courseCode, '%')) " +
            "AND (:teacherName IS NULL OR c.teacherName LIKE CONCAT('%', :teacherName, '%')) " +
            "AND (:departmentId IS NULL OR c.departmentId = :departmentId) " +
            "AND (:courseType IS NULL OR c.courseType = :courseType) " +
            "AND (:semester IS NULL OR c.semester = :semester) " +
            "AND (:status IS NULL OR c.status = :status)")
    Page<Course> findCourses(@Param("courseName") String courseName, 
                             @Param("courseCode") String courseCode, 
                             @Param("teacherName") String teacherName,
                             @Param("departmentId") Long departmentId, 
                             @Param("courseType") Integer courseType,
                             @Param("semester") String semester,
                             @Param("status") Integer status,
                             Pageable pageable);
}