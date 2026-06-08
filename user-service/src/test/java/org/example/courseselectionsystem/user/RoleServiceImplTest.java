package org.example.courseselectionsystem.user;

import org.example.courseselectionsystem.entity.Role;
import org.example.courseselectionsystem.repository.RoleRepository;
import org.example.courseselectionsystem.service.impl.RoleServiceImpl;
import org.example.courseselectionsystem.vo.PageRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    @Test
    void getRoleListFiltersByNameCodeAndStatusBeforePaging() {
        RoleServiceImpl service = newService();
        when(roleRepository.findAll(any(Sort.class))).thenReturn(List.of(
                role(1L, "student", "Student", 1),
                role(2L, "teacher", "Teacher", 0),
                role(3L, "admin", "Admin", 1),
                role(4L, "assistant_admin", "Assistant Admin", 1)
        ));
        PageRequest request = new PageRequest();
        request.setPageNum(1);
        request.setPageSize(1);
        request.setOrderByColumn("name");

        Page<Role> page = service.getRoleList(request, "admin", "admin", 1);

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(Role::getCode).containsExactly("admin");
    }

    @Test
    void updateRoleKeepsMissingFieldsFromExistingRole() {
        RoleServiceImpl service = newService();
        Role existing = role(7L, "teacher", "Teacher", 1);
        existing.setDescription("Original");
        Role patch = new Role();
        patch.setId(7L);
        patch.setName("Teacher Updated");
        patch.setCode("teacher_updated");
        when(roleRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(roleRepository.findByName("Teacher Updated")).thenReturn(null);
        when(roleRepository.findByCode("teacher_updated")).thenReturn(null);
        when(roleRepository.save(existing)).thenReturn(existing);

        Role updated = service.updateRole(patch);

        assertThat(updated.getName()).isEqualTo("Teacher Updated");
        assertThat(updated.getCode()).isEqualTo("teacher_updated");
        assertThat(updated.getDescription()).isEqualTo("Original");
        assertThat(updated.getStatus()).isEqualTo(1);
        verify(roleRepository).save(existing);
    }

    @Test
    void addRoleDefaultsStatusToEnabled() {
        RoleServiceImpl service = newService();
        Role role = role(null, "auditor", "Auditor", null);
        when(roleRepository.findByName("Auditor")).thenReturn(null);
        when(roleRepository.findByCode("auditor")).thenReturn(null);
        when(roleRepository.save(role)).thenReturn(role);

        service.addRole(role);

        assertThat(role.getStatus()).isEqualTo(1);
        verify(roleRepository).save(eq(role));
    }

    private RoleServiceImpl newService() {
        RoleServiceImpl service = new RoleServiceImpl();
        ReflectionTestUtils.setField(service, "roleRepository", roleRepository);
        return service;
    }

    private Role role(Long id, String code, String name, Integer status) {
        Role role = new Role();
        role.setId(id);
        role.setCode(code);
        role.setName(name);
        role.setStatus(status);
        return role;
    }
}
