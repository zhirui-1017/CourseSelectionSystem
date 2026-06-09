package org.example.courseselectionsystem.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

@Data
@Entity
@Table(name = "course_selection")
public class CourseSelection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Transient
    private String studentName;

    @Transient
    private String studentCode;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Transient
    private String courseName;

    @Transient
    private String courseCode;

    @Transient
    private String teacherName;

    @Transient
    private String semester;

    @Transient
    private Double credit;

    @Transient
    private Long teacherId;

    @Transient
    private String courseType;

    @Transient
    private String schedule;

    @Transient
    private String classroom;

    @Column(name = "status", nullable = false, columnDefinition = "int default 1")
    private Integer status;

    @Column(name = "score")
    private Double score;

    @Transient
    private String scoreLevel;

    @Column(name = "daily_grade")
    private Double dailyGrade;

    @Column(name = "lab_grade")
    private Double labGrade;

    @Column(name = "exam_grade")
    private Double examGrade;

    @Column(name = "remark", columnDefinition = "text")
    private String remark;

    @Column(name = "selection_time", nullable = false, updatable = false)
    private Date selectionTime;

    @Column(name = "drop_time")
    private Date dropTime;

    @Column(name = "created_at", updatable = false)
    private Date createTime;

    @Column(name = "updated_at")
    private Date updateTime;

    @PrePersist
    protected void onCreate() {
        Date now = new Date();
        if (this.createTime == null) {
            this.createTime = now;
        }
        if (this.selectionTime == null) {
            this.selectionTime = now;
        }
        this.updateTime = now;
        if (this.status == null) {
            this.status = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateTime = new Date();
    }
}
