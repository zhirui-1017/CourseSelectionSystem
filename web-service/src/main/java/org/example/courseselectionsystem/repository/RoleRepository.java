package org.example.courseselectionsystem.repository;

import org.example.courseselectionsystem.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 角色数据访问层
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * 根据角色编码查询角色
     * @param roleCode 角色编码
     * @return 角色对象
     */
    Optional<Role> findByRoleCode(String roleCode);

    /**
     * 根据角色名称查询角色
     * @param roleName 角色名称
     * @return 角色对象
     */
    Optional<Role> findByRoleName(String roleName);

    default Role findByName(String name) {
        return findByRoleName(name).orElse(null);
    }

    default Role findByCode(String code) {
        return findByRoleCode(code).orElse(null);
    }

    /**
     * 查询启用状态的角色
     * @param status 状态：1-启用
     * @return 角色列表
     */
    List<Role> findByStatus(Integer status);

    List<Role> findByStatusOrderByIdDesc(Integer status);
}
