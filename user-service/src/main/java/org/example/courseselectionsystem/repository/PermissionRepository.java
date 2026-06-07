package org.example.courseselectionsystem.repository;

import org.example.courseselectionsystem.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 权限数据访问层
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    /**
     * 根据权限编码查询权限
     * @param permissionCode 权限编码
     * @return 权限对象
     */
    Permission findByPermissionCode(String permissionCode);

    default boolean existsByName(String name) {
        return findByPermissionName(name) != null;
    }

    default boolean existsByCode(String code) {
        return findByPermissionCode(code) != null;
    }

    Permission findByPermissionName(String permissionName);

    boolean existsByParentId(Long parentId);

    List<Permission> findByPermissionNameContaining(String name, Pageable pageable);

    default List<Permission> findByNameContaining(String name, Pageable pageable) {
        return findByPermissionNameContaining(name, pageable);
    }

    long countByPermissionNameContaining(String name);

    default long countByNameContaining(String name) {
        return countByPermissionNameContaining(name);
    }

    List<Permission> findByPermissionCodeContaining(String code, Pageable pageable);

    default List<Permission> findByCodeContaining(String code, Pageable pageable) {
        return findByPermissionCodeContaining(code, pageable);
    }

    long countByPermissionCodeContaining(String code);

    default long countByCodeContaining(String code) {
        return countByPermissionCodeContaining(code);
    }

    List<Permission> findByParentId(Long parentId, Pageable pageable);

    List<Permission> findByParentId(Long parentId);

    long countByParentId(Long parentId);

    List<Permission> findByParentIdIsNull();

    List<Permission> findByPermissionCodeIn(List<String> codes);

    default List<Permission> findByCodeIn(List<String> codes) {
        return findByPermissionCodeIn(codes);
    }

    /**
     * 查询启用状态的权限
     * @param status 状态：1-启用
     * @return 权限列表
     */
    List<Permission> findByStatus(Integer status);

    /**
     * 根据权限类型查询权限
     * @param permissionType 权限类型：1-菜单，2-按钮，3-API
     * @return 权限列表
     */
    List<Permission> findByPermissionType(Integer permissionType);

    /**
     * 根据角色ID列表查询权限
     * @param roleIds 角色ID列表
     * @return 权限列表
     */
    @Query(value = "SELECT DISTINCT p.* FROM sys_permission p " +
            "JOIN sys_role_permission rp ON p.id = rp.permission_id " +
            "WHERE rp.role_id IN :roleIds AND p.status = 1", nativeQuery = true)
    List<Permission> findByRoleIds(List<Long> roleIds);

    /**
     * 查询顶级菜单权限
     * @return 权限列表
     */
    List<Permission> findByParentIdIsNullAndPermissionTypeAndStatus(Integer permissionType, Integer status);
}
