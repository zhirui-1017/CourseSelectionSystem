package org.example.courseselectionsystem.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

/**
 * 专业实体类
 */
@Data
@Entity
@Table(name = "major")
public class Major {
    /**
     * 专业ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 专业编号
     */
    @Column(name = "major_code", length = 20, nullable = false, unique = true)
    private String majorCode;

    /**
     * 专业名称
     */
    @Column(name = "major_name", length = 100, nullable = false, unique = true)
    private String majorName;

    /**
     * 所属学院ID
     */
    @Column(name = "department_id", nullable = false)
    private Long departmentId;

    /**
     * 所属学院名称
     */
    @Transient
    private String departmentName;

    /**
     * 专业负责人ID
     */
    @Transient
    private Long directorId;

    /**
     * 专业负责人名称
     */
    @Transient
    private String directorName;

    /**
     * 专业层次：1-本科，2-硕士，3-博士
     */
    @Transient
    private Integer level;

    /**
     * 专业简介
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 排序
     */
    @Transient
    private Integer sort;

    /**
     * 状态：0-禁用，1-启用
     */
    @Column(name = "status", nullable = false, columnDefinition = "tinyint default 1")
    private Integer status;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Date createTime;

    /**
     * 更新时间
     */
    @Column(name = "updated_at", nullable = false)
    private Date updateTime;

    /**
     * 乐观锁版本号
     */
    @Transient
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
