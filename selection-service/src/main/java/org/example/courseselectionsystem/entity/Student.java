package org.example.courseselectionsystem.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "student")
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_no", length = 20)
    private String studentNo;

    @Column(name = "name", length = 50)
    private String name;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "major_id")
    private Long majorId;

    @Column(name = "college_id")
    private Long collegeId;

    @Column(name = "class_name", length = 50)
    private String className;

    @Column(name = "status")
    private Integer status;
}
