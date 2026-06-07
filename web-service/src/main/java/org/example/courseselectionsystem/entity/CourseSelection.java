package org.example.courseselectionsystem.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

/**
 * 选课记录实体类
 */
@Data
@Entity
@Table(name = "course_selection")
public class CourseSelection {
    /**
     * 选课记录ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 学生ID
     */
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    /**
     * 学生名称
     */
    @Column(name = "student_name", length = 50, nullable = false)
    private String studentName;

    /**
     * 学号
     */
    @Column(name = "student_code", length = 20, nullable = false)
    private String studentCode;

    /**
     * 课程ID
     */
    @Column(name = "course_id", nullable = false)
    private Long courseId;

    /**
     * 课程名称
     */
    @Column(name = "course_name", length = 100, nullable = false)
    private String courseName;

    /**
     * 课程编号
     */
    @Column(name = "course_code", length = 20, nullable = false)
    private String courseCode;

    @Column(name = "teacher_name", length = 50)
    private String teacherName;

    @Column(name = "semester", length = 50)
    private String semester;

    /**
     * 学分
     */
    @Column(name = "credit", nullable = false)
    private Double credit;

    /**
     * 选课状态：1-正常，2-退课，3-候补
     */
    @Column(name = "status", nullable = false, columnDefinition = "tinyint default 1")
    private Integer status;

    /**
     * 成绩
     */
    @Column(name = "score")
    private Double score;

    /**
     * 成绩等级
     */
    @Column(name = "score_level", length = 10)
    private String scoreLevel;

    /**
     * 选课时间
     */
    @Column(name = "selection_time", nullable = false, updatable = false)
    private Date selectionTime;

    /**
     * 退课时间
     */
    @Column(name = "drop_time")
    private Date dropTime;

    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false, updatable = false)
    private Date createTime;

    /**
     * 更新时间
     */
    @Column(name = "update_time", nullable = false)
    private Date updateTime;

    /**
     * 乐观锁版本号
     */
    @Version
    @Column(name = "version", nullable = false, columnDefinition = "int default 0")
    private Integer version;

    /**
     * 多对一关联课程
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", insertable = false, updatable = false)
    private Course course;

    /**
     * 在创建实体时设置默认时间
     */
    @PrePersist
    protected void onCreate() {
        this.createTime = new Date();
        this.updateTime = new Date();
        this.selectionTime = new Date();
    }

    /**
     * 在更新实体时更新时间
     */
    @PreUpdate
    protected void onUpdate() {
        this.updateTime = new Date();
    }
}
