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

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    Optional<Course> findByCourseCode(String courseCode);

    Page<Course> findByCourseNameContaining(String courseName, Pageable pageable);

    Page<Course> findByTeacherId(Long teacherId, Pageable pageable);

    List<Course> findByTeacherId(Long teacherId);

    Page<Course> findByCourseType(String courseType, Pageable pageable);

    List<Course> findByStatus(Integer status);

    @Query("SELECT c FROM Course c WHERE 1=1 " +
            "AND (:courseName IS NULL OR c.courseName LIKE CONCAT('%', :courseName, '%')) " +
            "AND (:courseCode IS NULL OR c.courseCode LIKE CONCAT('%', :courseCode, '%')) " +
            "AND (:teacherId IS NULL OR c.teacherId = :teacherId) " +
            "AND (:courseType IS NULL OR c.courseType = :courseType) " +
            "AND (:status IS NULL OR c.status = :status)")
    Page<Course> findCourses(@Param("courseName") String courseName,
                             @Param("courseCode") String courseCode,
                             @Param("teacherId") Long teacherId,
                             @Param("courseType") String courseType,
                             @Param("status") Integer status,
                             Pageable pageable);

    @Query("SELECT c FROM Course c WHERE 1=1 " +
            "AND (:keyword IS NULL OR c.courseName LIKE CONCAT('%', :keyword, '%') " +
            "OR c.courseCode LIKE CONCAT('%', :keyword, '%') " +
            "OR c.courseType LIKE CONCAT('%', :keyword, '%')) " +
            "AND (:courseType IS NULL OR c.courseType = :courseType) " +
            "AND (:credit IS NULL OR c.credit = :credit) " +
            "AND (:status IS NULL OR c.status = :status)")
    List<Course> searchCourses(@Param("keyword") String keyword,
                               @Param("courseType") String courseType,
                               @Param("credit") Double credit,
                               @Param("status") Integer status);
}
