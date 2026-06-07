package org.example.courseselectionsystem.service;

import java.util.LinkedHashMap;
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
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 501);
        result.put("message", "Permission status update is not implemented");
        result.put("data", null);
        return result;
    }

    List<Map<String, Object>> getPermissionsByParentId(Long parentId);

    List<Map<String, Object>> getTopLevelPermissions();

    List<Map<String, Object>> getPermissionsByCodes(List<String> codes);
}
