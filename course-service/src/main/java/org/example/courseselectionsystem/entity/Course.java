package org.example.courseselectionsystem.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

@Data
@Entity
@Table(name = "course")
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_code", length = 20, nullable = false, unique = true)
    private String courseCode;

    @Column(name = "course_name", length = 100, nullable = false)
    private String courseName;

    @Column(name = "course_type", nullable = false, length = 20)
    private String courseType;

    @Column(name = "credit", nullable = false)
    private Double credit;

    @Column(name = "total_hours", nullable = false)
    private Integer totalHours;

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Transient
    private String teacherName;

    @Transient
    private Long departmentId;

    @Transient
    private String semester;

    @Column(name = "schedule", length = 200, nullable = false)
    private String schedule;

    @Column(name = "classroom", length = 50, nullable = false)
    private String classroom;

    @Column(name = "available_slots", nullable = false)
    private Integer availableSlots;

    @Column(name = "selected_count", nullable = false, columnDefinition = "int default 0")
    private Integer selectedCount;

    @Column(name = "status", nullable = false, columnDefinition = "int default 0")
    private Integer status;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "score")
    private Double score;

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
        this.updateTime = now;
        if (this.selectedCount == null) {
            this.selectedCount = 0;
        }
        if (this.status == null) {
            this.status = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateTime = new Date();
    }

    public Integer getMaxStudents() {
        return availableSlots;
    }

    public void setMaxStudents(Integer maxStudents) {
        this.availableSlots = maxStudents;
    }

    public Integer getCurrentStudents() {
        return selectedCount;
    }

    public void setCurrentStudents(Integer currentStudents) {
        this.selectedCount = currentStudents;
    }

    public Integer getMaxCapacity() {
        return availableSlots;
    }

    public void setMaxCapacity(Integer maxCapacity) {
        this.availableSlots = maxCapacity;
    }

    public String getLocation() {
        return classroom;
    }

    public void setLocation(String location) {
        this.classroom = location;
    }

    public String getClassTime() {
        return schedule;
    }

    public void setClassTime(String classTime) {
        this.schedule = classTime;
    }
}
