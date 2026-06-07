package org.example.courseselectionsystem.entity;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

/**
 * 学生实体类
 */
@Data
@Entity
@Table(name = "student")
public class Student implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 学生ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /**
     * 学号
     */
    @Column(name = "student_no", unique = true, nullable = false, length = 20)
    public String studentNo;

    /**
     * 姓名
     */
    @Column(name = "name", nullable = false, length = 50)
    public String name;

    /**
     * 性别
     */
    @Column(name = "gender", length = 10)
    public String gender;

    /**
     * 年龄
     */
    @Column(name = "age")
    public Integer age;

    /**
     * 联系电话
     */
    @Column(name = "phone", length = 20)
    public String phone;

    /**
     * 邮箱
     */
    @Column(name = "email", length = 100)
    public String email;

    @Column(name = "password", length = 100)
    public String password;

    /**
     * 专业ID
     */
    @Column(name = "major_id", nullable = false)
    public Long majorId;

    /**
     * 系部ID
     */
    @Column(name = "department_id", nullable = false)
    public Long departmentId;

    /**
     * 学院ID
     */
    @Column(name = "college_id", nullable = false)
    public Long collegeId;

    /**
     * 年级
     */
    @Column(name = "grade", length = 20)
    public String grade;

    /**
     * 班级
     */
    @Column(name = "class_name", length = 50)
    public String className;

    /**
     * 状态 0-禁用 1-启用
     */
    @Column(name = "status", nullable = false, columnDefinition = "int default 1")
    public Integer status;

    /**
     * 逻辑删除 0-未删除 1-已删除
     */
    @Column(name = "deleted", nullable = false, columnDefinition = "int default 0")
    public Integer deleted;
}
