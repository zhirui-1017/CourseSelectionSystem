package org.example.courseselectionsystem.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

/**
 * 用户实体类
 */
@Data
@Entity
@Table(name = "sys_user")
public class User {
    /**
     * 用户ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /**
     * 用户名
     */
    @Column(name = "username", length = 50, nullable = false, unique = true)
    public String username;

    /**
     * 密码
     */
    @Column(name = "password", length = 100, nullable = false)
    public String password;

    /**
     * 真实姓名
     */
    @Column(name = "real_name", length = 50, nullable = false)
    public String realName;

    /**
     * 用户类型：1-学生，2-教师，3-管理员
     */
    @Column(name = "user_type", nullable = false)
    public Integer userType;

    /**
     * 工号/学号
     */
    @Column(name = "user_code", length = 20, unique = true)
    public String userCode;

    /**
     * 性别：1-男，2-女
     */
    @Column(name = "gender")
    public Integer gender;

    /**
     * 邮箱
     */
    @Column(name = "email", length = 100)
    public String email;

    /**
     * 手机号
     */
    @Column(name = "phone", length = 20)
    public String phone;

    /**
     * 头像
     */
    @Column(name = "avatar", length = 200)
    public String avatar;

    /**
     * 状态：0-禁用，1-启用
     */
    @Column(name = "status", nullable = false, columnDefinition = "tinyint default 1")
    public Integer status;

    /**
     * 所属学院ID
     */
    @Column(name = "department_id")
    public Long departmentId;

    /**
     * 所属专业ID
     */
    @Column(name = "major_id")
    public Long majorId;

    /**
     * 班级
     */
    @Column(name = "class_name", length = 50)
    public String className;

    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false, updatable = false)
    public Date createTime;

    /**
     * 更新时间
     */
    @Column(name = "update_time", nullable = false)
    public Date updateTime;

    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    @Column(name = "deleted", columnDefinition = "tinyint default 0")
    public Integer deleted;

    /**
     * 乐观锁版本号
     */
    @Version
    @Column(name = "version", nullable = false, columnDefinition = "int default 0")
    public Integer version;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "sys_user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    public List<Role> roles;

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
