package org.example.courseselectionsystem.repository;

import org.example.courseselectionsystem.entity.CourseSelection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseSelectionRepository extends JpaRepository<CourseSelection, Long> {
    Optional<CourseSelection> findByStudentIdAndCourseId(Long studentId, Long courseId);

    Page<CourseSelection> findByStudentId(Long studentId, Pageable pageable);

    Page<CourseSelection> findByStudentIdAndStatus(Long studentId, Integer status, Pageable pageable);

    Page<CourseSelection> findByCourseId(Long courseId, Pageable pageable);

    Page<CourseSelection> findByCourseIdAndStatus(Long courseId, Integer status, Pageable pageable);

    List<CourseSelection> findByCourseIdAndStatus(Long courseId, Integer status);

    List<CourseSelection> findByStudentId(Long studentId);

    List<CourseSelection> findByStudentIdAndStatus(Long studentId, Integer status);

    List<CourseSelection> findByCourseId(Long courseId);

    List<CourseSelection> findByStudentIdAndCourseIdAndStatus(Long studentId, Long courseId, Integer status);

    List<CourseSelection> findByCourseIdAndStatusOrderBySelectionTimeAsc(Long courseId, Integer status);

    long countByStudentIdAndStatus(Long studentId, Integer status);

    long countByCourseIdAndStatus(Long courseId, Integer status);
}
