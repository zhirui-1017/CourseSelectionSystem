package org.example.courseselectionsystem.entity;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

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

    @Column(name = "gender", length = 10)
    public String gender;

    @Column(name = "age")
    public Integer age;

    @Column(name = "phone", length = 20)
    public String phone;

    @Column(name = "email", length = 100)
    public String email;

    @Column(name = "password", length = 100)
    public String password;

    @Column(name = "title", length = 50)
    public String title;

    @Column(name = "department_id")
    public Long departmentId;

    @Column(name = "college_id")
    public Long collegeId;

    @Column(name = "status", nullable = false, columnDefinition = "int default 1")
    public Integer status;

    @Column(name = "deleted", nullable = false, columnDefinition = "int default 0")
    public Integer deleted;
}
