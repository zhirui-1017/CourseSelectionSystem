package org.example.courseselectionsystem.service;

import org.example.courseselectionsystem.entity.CourseApplication;
import org.springframework.data.domain.Page;

/**
 * 课程申请审批服务：教师提交新增/编辑/删除课程申请，管理员审批通过或驳回。
 */
public interface CourseApplicationService {

    /** 教师提交申请 */
    CourseApplication apply(CourseApplication application);

    /** 申请详情 */
    CourseApplication getById(Long id);

    /** 待审批申请分页（管理员） */
    Page<CourseApplication> listPending(int pageNum, int pageSize);

    /** 某教师的申请记录分页 */
    Page<CourseApplication> listByTeacher(Long applicantId, int pageNum, int pageSize);

    /** 审批通过：新增则建课并开放选课，编辑则更新课程，删除则删除课程（有选课则发通知） */
    CourseApplication approve(Long id, Long approverId);

    /** 审批驳回 */
    CourseApplication reject(Long id, Long approverId, String comment);
}
