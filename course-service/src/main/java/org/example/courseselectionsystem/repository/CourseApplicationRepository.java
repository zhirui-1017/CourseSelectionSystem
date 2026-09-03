package org.example.courseselectionsystem.repository;

import org.example.courseselectionsystem.entity.CourseApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseApplicationRepository extends JpaRepository<CourseApplication, Long> {

    /** 按审批状态分页查询（0 待审批） */
    Page<CourseApplication> findByStatus(Integer status, Pageable pageable);

    /** 按申请人分页查询（教师查看自己的申请） */
    Page<CourseApplication> findByApplicantId(Long applicantId, Pageable pageable);
}
