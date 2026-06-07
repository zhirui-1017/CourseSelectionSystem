package org.example.courseselectionsystem.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

/**
 * 权限实体类
 */
@Data
@Entity
@Table(name = "sys_permission")
public class Permission {
    /**
     * 权限ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 权限名称
     */
    @Column(name = "permission_name", length = 50, nullable = false)
    private String permissionName;

    /**
     * 权限编码
     */
    @Column(name = "permission_code", length = 50, nullable = false, unique = true)
    private String permissionCode;

    /**
     * 请求URL
     */
    @Column(name = "url", length = 200)
    private String url;

    /**
     * 请求方法
     */
    @Column(name = "method", length = 10)
    private String method;

    /**
     * 权限类型：1-菜单，2-按钮，3-API
     */
    @Column(name = "permission_type", nullable = false)
    private Integer permissionType;

    /**
     * 父权限ID
     */
    @Column(name = "parent_id")
    private Long parentId;

    /**
     * 图标
     */
    @Column(name = "icon", length = 50)
    private String icon;

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
     * 角色与权限的多对多关系
     */
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToMany(mappedBy = "permissions", fetch = FetchType.LAZY)
    private List<Role> roles;

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

    public String getName() {
        return permissionName;
    }

    public void setName(String name) {
        this.permissionName = name;
    }

    public String getCode() {
        return permissionCode;
    }

    public void setCode(String code) {
        this.permissionCode = code;
    }

    public String getDescription() {
        return null;
    }

    public void setDescription(String description) {
        // 当前实体没有独立描述字段，保留兼容方法以支持旧服务实现。
    }
}
