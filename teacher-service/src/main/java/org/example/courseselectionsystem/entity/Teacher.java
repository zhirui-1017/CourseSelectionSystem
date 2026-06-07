package org.example.courseselectionsystem.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Data
@Entity
@Table(name = "teacher")
public class Teacher implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "teacher_no", unique = true, nullable = false, length = 20)
    public String teacherNo;

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

    @Column(name = "password", nullable = false, length = 100)
    public String password;

    @Column(name = "avatar", length = 200)
    public String avatar;

    @Column(name = "title", length = 50)
    public String title;

    @Column(name = "department_id", nullable = false)
    public Long departmentId;

    @Transient
    @TableField(exist = false)
    public Long collegeId;

    @Column(name = "status", nullable = false, columnDefinition = "int default 1")
    public Integer status;

    @Transient
    @TableField(exist = false)
    public Integer deleted;

    @Column(name = "created_at", updatable = false)
    public Date createdAt;

    @Column(name = "updated_at")
    public Date updatedAt;
}
