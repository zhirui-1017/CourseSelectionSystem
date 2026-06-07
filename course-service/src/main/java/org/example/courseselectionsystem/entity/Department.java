package org.example.courseselectionsystem.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

/**
 * 学院实体类
 */
@Data
@Entity
@Table(name = "department")
public class Department {
    /**
     * 学院ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 学院编号
     */
    @Column(name = "department_code", length = 20, nullable = false, unique = true)
    private String departmentCode;

    /**
     * 学院名称
     */
    @Column(name = "department_name", length = 100, nullable = false, unique = true)
    private String departmentName;

    /**
     * 学院负责人ID
     */
    @Column(name = "director_id")
    private Long directorId;

    /**
     * 学院负责人名称
     */
    @Column(name = "director_name", length = 50)
    private String directorName;

    /**
     * 联系电话
     */
    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    /**
     * 学院简介
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 排序
     */
    @Column(name = "sort")
    private Integer sort;

    /**
     * 状态：0-禁用，1-启用
     */
    @Column(name = "status", nullable = false, columnDefinition = "tinyint default 1")
    private Integer status;

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
     * 在创建实体时设置默认时间
     */
    @PrePersist
    protected void onCreate() {
        this.createTime = new Date();
        this.updateTime = new Date();
    }

    /**
     * 在更新实体时更新时间
     */
    @PreUpdate
    protected void onUpdate() {
        this.updateTime = new Date();
    }
}