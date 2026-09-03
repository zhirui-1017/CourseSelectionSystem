package org.example.courseselectionsystem.repository;

import org.example.courseselectionsystem.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    Optional<Course> findByCourseCode(String courseCode);

    Page<Course> findByCourseNameContaining(String courseName, Pageable pageable);

    Page<Course> findByTeacherId(Long teacherId, Pageable pageable);

    List<Course> findByTeacherId(Long teacherId);

    Page<Course> findByCourseType(String courseType, Pageable pageable);

    List<Course> findByStatus(Integer status);

    long countByCreateTimeGreaterThanEqual(LocalDateTime createTime);

    @Query("SELECT c FROM Course c WHERE 1=1 " +
            "AND ((:courseName IS NULL AND :courseCode IS NULL) " +
            "OR c.courseName LIKE CONCAT('%', :courseName, '%') " +
            "OR c.courseCode LIKE CONCAT('%', :courseCode, '%')) " +
            "AND (:teacherId IS NULL OR c.teacherId = :teacherId) " +
            "AND (:courseType IS NULL OR c.courseType = :courseType) " +
            "AND (:status IS NULL OR c.status = :status)")
    Page<Course> findCourses(@Param("courseName") String courseName,
                             @Param("courseCode") String courseCode,
                             @Param("teacherId") Long teacherId,
                             @Param("courseType") String courseType,
                             @Param("status") Integer status,
                             Pageable pageable);

    @Query(value = "SELECT c.* FROM course c INNER JOIN teacher t ON c.teacher_id = t.id WHERE 1=1 " +
            "AND (:departmentId IS NULL OR t.department_id = :departmentId) " +
            "AND (:keyword IS NULL OR c.course_name LIKE CONCAT('%', :keyword, '%') " +
            "OR c.course_code LIKE CONCAT('%', :keyword, '%') " +
            "OR c.course_type LIKE CONCAT('%', :keyword, '%')) " +
            "AND (:courseType IS NULL OR c.course_type = :courseType) " +
            "AND (:credit IS NULL OR c.credit = :credit) " +
            "AND (:status IS NULL OR c.status = :status)",
            nativeQuery = true)
    List<Course> searchCourses(@Param("keyword") String keyword,
                               @Param("departmentId") Long departmentId,
                               @Param("courseType") String courseType,
                               @Param("credit") Double credit,
                               @Param("status") Integer status);

    /**
     * 根据授课教师所属系部查询指定状态的课程
     * 课程表无 department_id 列，需通过 teacher.department_id 关联过滤
     * @param departmentId 系部ID
     * @param status 课程状态：1-启用
     * @return 课程列表
     */
    @Query(value = "SELECT c.* FROM course c INNER JOIN teacher t ON c.teacher_id = t.id " +
            "WHERE t.department_id = :departmentId AND c.status = :status", nativeQuery = true)
    List<Course> findByDepartmentIdAndStatus(@Param("departmentId") Long departmentId,
                                             @Param("status") Integer status);
}
