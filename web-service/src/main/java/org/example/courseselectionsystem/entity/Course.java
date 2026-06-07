package org.example.courseselectionsystem.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

/**
 * 课程实体类
 */
@Data
@Entity
@Table(name = "course")
public class Course {
    /**
     * 课程ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 课程编号
     */
    @Column(name = "course_code", length = 20, nullable = false, unique = true)
    private String courseCode;

    /**
     * 课程名称
     */
    @Column(name = "course_name", length = 100, nullable = false)
    private String courseName;

    /**
     * 课程类型：1-必修课，2-选修课，3-通识课
     */
    @Column(name = "course_type", nullable = false)
    private Integer courseType;

    /**
     * 学分
     */
    @Column(name = "credit", nullable = false)
    private Double credit;

    /**
     * 总课时
     */
    @Column(name = "total_hours", nullable = false)
    private Integer totalHours;

    /**
     * 授课教师ID
     */
    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    /**
     * 授课教师名称
     */
    @Column(name = "teacher_name", length = 50, nullable = false)
    private String teacherName;

    /**
     * 开课学院ID
     */
    @Column(name = "department_id", nullable = false)
    private Long departmentId;

    /**
     * 开课学期
     */
    @Column(name = "semester", length = 20, nullable = false)
    private String semester;

    /**
     * 上课时间
     */
    @Column(name = "class_time", length = 200)
    private String classTime;

    /**
     * 上课地点
     */
    @Column(name = "location", length = 100)
    private String location;

    /**
     * 总人数上限
     */
    @Column(name = "max_students", nullable = false)
    private Integer maxStudents;

    /**
     * 当前已选人数
     */
    @Column(name = "current_students", columnDefinition = "int default 0")
    private Integer currentStudents;

    /**
     * 课程状态：0-未开始，1-进行中，2-已结束
     */
    @Column(name = "status", nullable = false, columnDefinition = "tinyint default 0")
    private Integer status;

    /**
     * 课程简介
     */
    @Column(name = "description", length = 500)
    private String description;

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
     * 课程与选课记录的一对多关系
     */
    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<CourseSelection> courseSelections;

    /**
     * 在创建实体时设置默认时间
     */
    @PrePersist
    protected void onCreate() {
        this.createTime = new Date();
        this.updateTime = new Date();
        this.currentStudents = 0;
    }

    /**
     * 在更新实体时更新时间
     */
    @PreUpdate
    protected void onUpdate() {
        this.updateTime = new Date();
    }

    public Integer getMaxCapacity() {
        return maxStudents;
    }

    public void setMaxCapacity(Integer maxCapacity) {
        this.maxStudents = maxCapacity;
    }

    public String getClassroom() {
        return location;
    }

    public void setClassroom(String classroom) {
        this.location = classroom;
    }

    public String getSchedule() {
        return classTime;
    }

    public void setSchedule(String schedule) {
        this.classTime = schedule;
    }
}
