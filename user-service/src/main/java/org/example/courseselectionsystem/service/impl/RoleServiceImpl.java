package org.example.courseselectionsystem.service.impl;

import org.example.courseselectionsystem.entity.Role;
import org.example.courseselectionsystem.repository.RoleRepository;
import org.example.courseselectionsystem.service.RoleService;
import org.example.courseselectionsystem.vo.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class RoleServiceImpl implements RoleService {

    @Autowired
    private RoleRepository roleRepository;

    @Override
    @Transactional
    public Role addRole(Role role) {
        try {
            if (role == null || isBlank(role.getName()) || isBlank(role.getCode())) {
                throw new RuntimeException("Role name and code cannot be empty");
            }

            Role existingRole = roleRepository.findByName(role.getName());
            if (existingRole != null) {
                throw new RuntimeException("Role name already exists");
            }

            existingRole = roleRepository.findByCode(role.getCode());
            if (existingRole != null) {
                throw new RuntimeException("Role code already exists");
            }

            if (role.getStatus() == null) {
                role.setStatus(1);
            }

            return roleRepository.save(role);
        } catch (DataAccessException ex) {
            throw new RuntimeException("Role table is not available", ex);
        }
    }

    @Override
    @Transactional
    public Role updateRole(Role role) {
        try {
            if (role == null || role.getId() == null) {
                throw new RuntimeException("Role id cannot be empty");
            }

            Role existingRole = roleRepository.findById(role.getId()).orElse(null);
            if (existingRole == null) {
                throw new RuntimeException("Role does not exist");
            }

            if (!isBlank(role.getName())) {
                Role roleWithSameName = roleRepository.findByName(role.getName());
                if (roleWithSameName != null && !roleWithSameName.getId().equals(role.getId())) {
                    throw new RuntimeException("Role name already exists");
                }
                existingRole.setName(role.getName());
            }

            if (!isBlank(role.getCode())) {
                Role roleWithSameCode = roleRepository.findByCode(role.getCode());
                if (roleWithSameCode != null && !roleWithSameCode.getId().equals(role.getId())) {
                    throw new RuntimeException("Role code already exists");
                }
                existingRole.setCode(role.getCode());
            }

            if (role.getDescription() != null) {
                existingRole.setDescription(role.getDescription());
            }
            if (role.getStatus() != null) {
                existingRole.setStatus(role.getStatus());
            }

            return roleRepository.save(existingRole);
        } catch (DataAccessException ex) {
            throw new RuntimeException("Role table is not available", ex);
        }
    }

    @Override
    public Role getRoleById(Long roleId) {
        try {
            return roleRepository.findById(roleId).orElse(null);
        } catch (DataAccessException ex) {
            return fallbackRoles().stream()
                    .filter(role -> role.getId().equals(roleId))
                    .findFirst()
                    .orElse(null);
        }
    }

    @Override
    public Role getRoleByName(String roleName) {
        try {
            return roleRepository.findByName(roleName);
        } catch (DataAccessException ex) {
            return findFallbackRole(roleName);
        }
    }

    @Override
    public Role getRoleByCode(String roleCode) {
        try {
            return roleRepository.findByCode(roleCode);
        } catch (DataAccessException ex) {
            return findFallbackRole(roleCode);
        }
    }

    @Override
    @Transactional
    public boolean deleteRole(Long roleId) {
        try {
            Role role = roleRepository.findById(roleId).orElse(null);
            if (role == null) {
                return false;
            }

            roleRepository.delete(role);
            return true;
        } catch (DataAccessException ex) {
            return false;
        }
    }

    @Override
    @Transactional
    public boolean batchDeleteRoles(Long[] roleIds) {
        if (roleIds == null || roleIds.length == 0) {
            return false;
        }

        boolean success = true;
        for (Long roleId : roleIds) {
            success = deleteRole(roleId) && success;
        }

        return success;
    }

    @Override
    public Page<Role> getRoleList(PageRequest pageRequestParam, String name, String code, Integer status) {
        PageRequest request = pageRequestParam == null ? new PageRequest() : pageRequestParam;
        int pageNum = request.getPageNum() == null || request.getPageNum() < 1 ? 1 : request.getPageNum();
        int pageSize = request.getPageSize() == null || request.getPageSize() < 1 ? 10 : Math.min(request.getPageSize(), 100);

        Sort sort;
        if (!isBlank(request.getSortField())) {
            String sortField = roleSortProperty(request.getSortField());
            sort = "asc".equalsIgnoreCase(request.getSortOrder())
                    ? Sort.by(Sort.Order.asc(sortField))
                    : Sort.by(Sort.Order.desc(sortField));
        } else {
            sort = Sort.by(Sort.Order.desc("id"));
        }

        org.springframework.data.domain.PageRequest pageable =
                org.springframework.data.domain.PageRequest.of(pageNum - 1, pageSize, sort);

        try {
            return toPage(filterRoles(roleRepository.findAll(sort), name, code, status), pageable);
        } catch (DataAccessException ex) {
            return toPage(filterRoles(fallbackRoles(), name, code, status), pageable);
        }
    }

    @Override
    public List<Role> getAllRoles() {
        try {
            return roleRepository.findAll(Sort.by(Sort.Order.desc("id")));
        } catch (DataAccessException ex) {
            return fallbackRoles();
        }
    }

    @Override
    public List<Role> getActiveRoles() {
        try {
            return roleRepository.findByStatusOrderByIdDesc(1);
        } catch (DataAccessException ex) {
            return fallbackRoles();
        }
    }

    @Override
    @Transactional
    public boolean changeRoleStatus(Long roleId, Integer status) {
        try {
            Role role = roleRepository.findById(roleId).orElse(null);
            if (role == null) {
                return false;
            }

            role.setStatus(status);
            roleRepository.save(role);
            return true;
        } catch (DataAccessException ex) {
            return false;
        }
    }

    private List<Role> fallbackRoles() {
        List<Role> roles = new ArrayList<>();
        roles.add(fallbackRole(1L, "student", "Student"));
        roles.add(fallbackRole(2L, "teacher", "Teacher"));
        roles.add(fallbackRole(3L, "admin", "Admin"));
        return Collections.unmodifiableList(roles);
    }

    private Role fallbackRole(Long id, String code, String name) {
        Role role = new Role();
        role.setId(id);
        role.setCode(code);
        role.setName(name);
        role.setDescription(name + " role");
        role.setStatus(1);
        return role;
    }

    private Role findFallbackRole(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        return fallbackRoles().stream()
                .filter(role -> normalized.equals(role.getCode().toLowerCase(Locale.ROOT))
                        || normalized.equals(role.getName().toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElse(null);
    }

    private List<Role> filterRoles(List<Role> roles, String name, String code, Integer status) {
        return roles.stream()
                .filter(role -> containsIgnoreCase(role.getName(), name))
                .filter(role -> containsIgnoreCase(role.getCode(), code))
                .filter(role -> status == null || Objects.equals(role.getStatus(), status))
                .collect(Collectors.toList());
    }

    private Page<Role> toPage(List<Role> roles, org.springframework.data.domain.PageRequest pageable) {
        int fromIndex = Math.min((int) pageable.getOffset(), roles.size());
        int toIndex = Math.min(fromIndex + pageable.getPageSize(), roles.size());
        return new PageImpl<>(roles.subList(fromIndex, toIndex), pageable, roles.size());
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        if (isBlank(keyword)) {
            return true;
        }
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword.trim().toLowerCase(Locale.ROOT));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String roleSortProperty(String field) {
        String normalized = field.trim();
        if ("name".equalsIgnoreCase(normalized)) {
            return "roleName";
        }
        if ("code".equalsIgnoreCase(normalized)) {
            return "roleCode";
        }
        return normalized;
    }
}
