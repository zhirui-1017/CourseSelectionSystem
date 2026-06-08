package org.example.courseselectionsystem.user;

import org.example.courseselectionsystem.entity.Permission;
import org.example.courseselectionsystem.repository.PermissionRepository;
import org.example.courseselectionsystem.service.impl.PermissionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionServiceImplTest {

    @Mock
    private PermissionRepository permissionRepository;

    @Test
    void addPermissionPersistsStatusTypeIconAndSort() {
        PermissionServiceImpl service = new PermissionServiceImpl(permissionRepository);
        when(permissionRepository.existsByName("Course Menu")).thenReturn(false);
        when(permissionRepository.existsByCode("course:menu")).thenReturn(false);
        when(permissionRepository.save(any(Permission.class))).thenAnswer(invocation -> {
            Permission permission = invocation.getArgument(0);
            permission.setId(5L);
            return permission;
        });

        Map<String, Object> result = service.addPermission(Map.of(
                "name", "Course Menu",
                "code", "course:menu",
                "permissionType", 1,
                "icon", "book",
                "sort", 20,
                "status", 0
        ));

        assertThat(result.get("code")).isEqualTo(200);
        Map<String, Object> data = data(result);
        assertThat(data).containsEntry("permissionType", 1)
                .containsEntry("icon", "book")
                .containsEntry("sort", 20)
                .containsEntry("status", 0);
    }

    @Test
    void updatePermissionStatusSavesPermissionAndReturnsStatus() {
        PermissionServiceImpl service = new PermissionServiceImpl(permissionRepository);
        Permission permission = permission(9L, "user:edit", "User Edit", 1);
        when(permissionRepository.findById(9L)).thenReturn(Optional.of(permission));
        when(permissionRepository.save(permission)).thenReturn(permission);

        Map<String, Object> result = service.updatePermissionStatus(9L, 0);

        assertThat(result.get("code")).isEqualTo(200);
        assertThat(data(result)).containsEntry("status", 0);
        assertThat(permission.getStatus()).isEqualTo(0);
        verify(permissionRepository).save(permission);
    }

    @Test
    void updatePermissionCanPatchStatusAndType() {
        PermissionServiceImpl service = new PermissionServiceImpl(permissionRepository);
        Permission permission = permission(11L, "role:view", "Role View", 1);
        permission.setPermissionType(3);
        permission.setSort(1);
        when(permissionRepository.findById(11L)).thenReturn(Optional.of(permission));
        when(permissionRepository.save(permission)).thenReturn(permission);

        Map<String, Object> result = service.updatePermission(11L, Map.of(
                "status", 0,
                "permissionType", 2,
                "sort", 30
        ));

        assertThat(result.get("code")).isEqualTo(200);
        Map<String, Object> data = data(result);
        assertThat(data).containsEntry("status", 0)
                .containsEntry("permissionType", 2)
                .containsEntry("sort", 30);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> data(Map<String, Object> result) {
        return (Map<String, Object>) result.get("data");
    }

    private Permission permission(Long id, String code, String name, Integer status) {
        Permission permission = new Permission();
        permission.setId(id);
        permission.setCode(code);
        permission.setName(name);
        permission.setPermissionType(3);
        permission.setStatus(status);
        return permission;
    }
}
