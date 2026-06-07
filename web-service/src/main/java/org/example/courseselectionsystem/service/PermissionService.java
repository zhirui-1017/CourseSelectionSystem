package org.example.courseselectionsystem.service;

import java.util.List;
import java.util.Map;

public interface PermissionService {
    Map<String, Object> addPermission(Map<String, Object> permission);

    Map<String, Object> updatePermission(Long id, Map<String, Object> permission);

    Map<String, Object> deletePermission(Long id);

    Map<String, Object> getPermissionById(Long id);

    List<Map<String, Object>> getAllPermissions();

    Map<String, Object> getPermissionsByPage(Integer page, Integer size, String name, String code, Long parentId);

    default Map<String, Object> getPermissionsByPage(Integer page, Integer size, String name, String code) {
        return getPermissionsByPage(page, size, name, code, null);
    }

    default List<Map<String, Object>> getPermissionTree() {
        return getTopLevelPermissions();
    }

    default Map<String, Object> updatePermissionStatus(Long id, Integer status) {
        Map<String, Object> permission = getPermissionById(id);
        return Map.of("code", 200, "message", "更新成功", "data", permission.get("data"));
    }

    List<Map<String, Object>> getPermissionsByParentId(Long parentId);

    List<Map<String, Object>> getTopLevelPermissions();

    List<Map<String, Object>> getPermissionsByCodes(List<String> codes);
}
