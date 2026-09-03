package org.example.courseselectionsystem.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

/**
 * 课程申请审批实体：教师对课程的新增/编辑/删除申请，由管理员审批。
 */
@Data
@Entity
@Table(name = "course_application")
public class CourseApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 申请人（教师）ID */
    @Column(name = "applicant_id", nullable = false)
    private Long applicantId;

    @Column(name = "applicant_name", length = 100)
    private String applicantName;

    /** 申请类型：add 新增 / edit 编辑 / delete 删除 */
    @Column(name = "type", length = 20, nullable = false)
    private String type;

    /** 目标课程ID（编辑/删除时使用） */
    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "course_code", length = 30)
    private String courseCode;

    @Column(name = "course_name", length = 100)
    private String courseName;

    /** 申请说明 / 删除理由 */
    @Column(name = "reason", length = 1000)
    private String reason;

    /** 课程内容 JSON 快照（新增/编辑时使用） */
    @Column(name = "payload", columnDefinition = "text")
    private String payload;

    /** 审批状态：0 待审批 1 已通过 2 已驳回 */
    @Column(name = "status", nullable = false, columnDefinition = "int default 0")
    private Integer status;

    @Column(name = "approve_comment", length = 500)
    private String approveComment;

    @Column(name = "approver_id")
    private Long approverId;

    @Column(name = "create_time", updatable = false)
    private Date createTime;

    @Column(name = "update_time")
    private Date updateTime;

    @PrePersist
    protected void onCreate() {
        Date now = new Date();
        if (this.createTime == null) {
            this.createTime = now;
        }
        this.updateTime = now;
        if (this.status == null) {
            this.status = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateTime = new Date();
    }
}
