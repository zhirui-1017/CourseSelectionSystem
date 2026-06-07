package org.example.courseselectionsystem.service.impl;

import org.example.courseselectionsystem.entity.Role;
import org.example.courseselectionsystem.repository.RoleRepository;
import org.example.courseselectionsystem.service.RoleService;
import org.example.courseselectionsystem.vo.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 角色服务实现类
 */
@Service
public class RoleServiceImpl implements RoleService {

    @Autowired
    private RoleRepository roleRepository;

    @Override
    @Transactional
    public Role addRole(Role role) {
        // 验证角色名称是否已存在
        Role existingRole = roleRepository.findByName(role.getName());
        if (existingRole != null) {
            throw new RuntimeException("角色名称已存在");
        }

        // 验证角色编码是否已存在
        existingRole = roleRepository.findByCode(role.getCode());
        if (existingRole != null) {
            throw new RuntimeException("角色编码已存在");
        }

        return roleRepository.save(role);
    }

    @Override
    @Transactional
    public Role updateRole(Role role) {
        // 验证角色是否存在
        Role existingRole = roleRepository.findById(role.getId()).orElse(null);
        if (existingRole == null) {
            throw new RuntimeException("角色不存在");
        }

        // 验证角色名称是否已被其他记录使用
        Role roleWithSameName = roleRepository.findByName(role.getName());
        if (roleWithSameName != null && !roleWithSameName.getId().equals(role.getId())) {
            throw new RuntimeException("角色名称已存在");
        }

        // 验证角色编码是否已被其他记录使用
        Role roleWithSameCode = roleRepository.findByCode(role.getCode());
        if (roleWithSameCode != null && !roleWithSameCode.getId().equals(role.getId())) {
            throw new RuntimeException("角色编码已存在");
        }

        // 更新信息
        existingRole.setName(role.getName());
        existingRole.setCode(role.getCode());
        existingRole.setDescription(role.getDescription());
        existingRole.setStatus(role.getStatus());

        return roleRepository.save(existingRole);
    }

    @Override
    public Role getRoleById(Long roleId) {
        return roleRepository.findById(roleId).orElse(null);
    }

    @Override
    public Role getRoleByName(String roleName) {
        return roleRepository.findByName(roleName);
    }

    @Override
    public Role getRoleByCode(String roleCode) {
        return roleRepository.findByCode(roleCode);
    }

    @Override
    @Transactional
    public boolean deleteRole(Long roleId) {
        Role role = roleRepository.findById(roleId).orElse(null);
        if (role == null) {
            return false;
        }

        roleRepository.delete(role);
        return true;
    }

    @Override
    @Transactional
    public boolean batchDeleteRoles(Long[] roleIds) {
        if (roleIds == null || roleIds.length == 0) {
            return false;
        }

        for (Long roleId : roleIds) {
            deleteRole(roleId);
        }

        return true;
    }

    @Override
    public Page<Role> getRoleList(PageRequest pageRequestParam, String name, String code, Integer status) {
        // 构建排序条件
        Sort sort;
        if (pageRequestParam.getSortField() != null && !pageRequestParam.getSortField().isEmpty()) {
            sort = "asc".equalsIgnoreCase(pageRequestParam.getSortOrder()) 
                ? Sort.by(Sort.Order.asc(pageRequestParam.getSortField())) 
                : Sort.by(Sort.Order.desc(pageRequestParam.getSortField()));
        } else {
            sort = Sort.by(Sort.Order.desc("id"));
        }

        // 构建分页请求
        org.springframework.data.domain.PageRequest pageable =
                org.springframework.data.domain.PageRequest.of(pageRequestParam.getPageNum() - 1, pageRequestParam.getPageSize(), sort);

        // TODO: 根据条件查询角色列表，暂时返回全部列表
        return roleRepository.findAll(pageable);
    }

    @Override
    public List<Role> getAllRoles() {
        return roleRepository.findAll(Sort.by(Sort.Order.desc("id")));
    }

    @Override
    public List<Role> getActiveRoles() {
        return roleRepository.findByStatusOrderByIdDesc(1);
    }

    @Override
    @Transactional
    public boolean changeRoleStatus(Long roleId, Integer status) {
        Role role = roleRepository.findById(roleId).orElse(null);
        if (role == null) {
            return false;
        }

        role.setStatus(status);
        roleRepository.save(role);
        return true;
    }
}
