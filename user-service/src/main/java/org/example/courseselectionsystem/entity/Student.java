package org.example.courseselectionsystem.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Data
@Entity
@Table(name = "student")
public class Student implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "student_no", unique = true, nullable = false, length = 20)
    public String studentNo;

    @Column(name = "name", nullable = false, length = 50)
    public String name;

    @Column(name = "gender", nullable = false, length = 10)
    public String gender;

    @Transient
    @TableField(exist = false)
    public Integer age;

    @Column(name = "phone", length = 20)
    public String phone;

    @Column(name = "email", length = 100)
    public String email;

    @Column(name = "password", nullable = false)
    public String password;

    @Column(name = "avatar", length = 200)
    public String avatar;

    @Column(name = "major_id", nullable = false)
    public Long majorId;

    @Transient
    @TableField(exist = false)
    public Long departmentId;

    @Column(name = "college_id", nullable = false)
    public Long collegeId;

    @Transient
    @TableField(exist = false)
    public String grade;

    @Column(name = "class_name", nullable = false, length = 50)
    public String className;

    @Column(name = "status", nullable = false, columnDefinition = "int default 1")
    public Integer status;

    @Transient
    @TableField(exist = false)
    public Integer deleted;

    @Column(name = "create_time", updatable = false)
    public Date createTime;

    @Column(name = "update_time")
    public Date updateTime;

    @Column(name = "class_id")
    public Long classId;
}
