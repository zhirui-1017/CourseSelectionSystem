package org.example.courseselectionsystem.service.impl;

import org.example.courseselectionsystem.entity.Permission;
import org.example.courseselectionsystem.repository.PermissionRepository;
import org.example.courseselectionsystem.service.PermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PermissionServiceImpl implements PermissionService {

    private static final Logger logger = LoggerFactory.getLogger(PermissionServiceImpl.class);

    private final PermissionRepository permissionRepository;

    @Autowired
    public PermissionServiceImpl(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Override
    @Transactional
    public Map<String, Object> addPermission(Map<String, Object> permissionMap) {
        logger.info("Add permission: {}", permissionMap);

        if (isBlank(permissionMap.get("name"))) {
            return response(400, "Permission name cannot be empty", null);
        }
        if (isBlank(permissionMap.get("code"))) {
            return response(400, "Permission code cannot be empty", null);
        }

        String name = permissionMap.get("name").toString().trim();
        String code = permissionMap.get("code").toString().trim();

        try {
            if (permissionRepository.existsByName(name)) {
                return response(400, "Permission name already exists", null);
            }
            if (permissionRepository.existsByCode(code)) {
                return response(400, "Permission code already exists", null);
            }

            Permission permission = new Permission();
            permission.setName(name);
            permission.setCode(code);
            permission.setDescription(stringValue(permissionMap.get("description")));
            permission.setUrl(stringValue(permissionMap.get("url")));
            permission.setMethod(stringValue(permissionMap.get("method")));
            permission.setPermissionType(intValue(permissionMap.get("permissionType"), 3));
            permission.setIcon(stringValue(permissionMap.get("icon")));
            permission.setSort(intValue(permissionMap.get("sort"), 0));
            permission.setStatus(intValue(permissionMap.get("status"), 1));

            Object parentId = permissionMap.get("parentId");
            if (parentId != null) {
                try {
                    permission.setParentId(Long.parseLong(parentId.toString()));
                } catch (NumberFormatException ex) {
                    return response(400, "Invalid parent permission id", null);
                }
            }

            Permission savedPermission = permissionRepository.save(permission);
            return response(200, "Permission added successfully", convertToMap(savedPermission));
        } catch (DataAccessException ex) {
            logger.warn("Permission table is not available", ex);
            return response(503, "Permission table is not available", null);
        } catch (Exception ex) {
            logger.error("Add permission failed", ex);
            return response(500, "Add permission failed", null);
        }
    }

    @Override
    @Transactional
    public Map<String, Object> updatePermission(Long id, Map<String, Object> permissionMap) {
        logger.info("Update permission, id: {}, data: {}", id, permissionMap);

        try {
            Optional<Permission> optionalPermission = permissionRepository.findById(id);
            if (!optionalPermission.isPresent()) {
                return response(404, "Permission does not exist", null);
            }

            Permission permission = optionalPermission.get();

            if (permissionMap.containsKey("name")) {
                String name = permissionMap.get("name").toString().trim();
                if (name.isEmpty()) {
                    return response(400, "Permission name cannot be empty", null);
                }
                if (!permission.getName().equals(name) && permissionRepository.existsByName(name)) {
                    return response(400, "Permission name already exists", null);
                }
                permission.setName(name);
            }

            if (permissionMap.containsKey("code")) {
                String code = permissionMap.get("code").toString().trim();
                if (code.isEmpty()) {
                    return response(400, "Permission code cannot be empty", null);
                }
                if (!permission.getCode().equals(code) && permissionRepository.existsByCode(code)) {
                    return response(400, "Permission code already exists", null);
                }
                permission.setCode(code);
            }

            if (permissionMap.containsKey("description")) {
                permission.setDescription(stringValue(permissionMap.get("description")));
            }
            if (permissionMap.containsKey("url")) {
                permission.setUrl(stringValue(permissionMap.get("url")));
            }
            if (permissionMap.containsKey("method")) {
                permission.setMethod(stringValue(permissionMap.get("method")));
            }
            if (permissionMap.containsKey("permissionType")) {
                permission.setPermissionType(intValue(permissionMap.get("permissionType"), permission.getPermissionType()));
            }
            if (permissionMap.containsKey("icon")) {
                permission.setIcon(stringValue(permissionMap.get("icon")));
            }
            if (permissionMap.containsKey("sort")) {
                permission.setSort(intValue(permissionMap.get("sort"), permission.getSort()));
            }
            if (permissionMap.containsKey("status")) {
                permission.setStatus(intValue(permissionMap.get("status"), permission.getStatus()));
            }
            if (permissionMap.containsKey("parentId")) {
                try {
                    permission.setParentId(Long.parseLong(permissionMap.get("parentId").toString()));
                } catch (NumberFormatException ex) {
                    return response(400, "Invalid parent permission id", null);
                }
            }

            Permission updatedPermission = permissionRepository.save(permission);
            return response(200, "Permission updated successfully", convertToMap(updatedPermission));
        } catch (DataAccessException ex) {
            logger.warn("Permission table is not available", ex);
            return response(503, "Permission table is not available", null);
        } catch (Exception ex) {
            logger.error("Update permission failed, id: {}", id, ex);
            return response(500, "Update permission failed", null);
        }
    }

    @Override
    @Transactional
    public Map<String, Object> deletePermission(Long id) {
        logger.info("Delete permission, id: {}", id);

        try {
            if (!permissionRepository.existsById(id)) {
                return response(404, "Permission does not exist", null);
            }
            if (permissionRepository.existsByParentId(id)) {
                return response(400, "Permission has child permissions", null);
            }

            permissionRepository.deleteById(id);
            return response(200, "Permission deleted successfully", null);
        } catch (DataAccessException ex) {
            logger.warn("Permission table is not available", ex);
            return response(503, "Permission table is not available", null);
        } catch (Exception ex) {
            logger.error("Delete permission failed, id: {}", id, ex);
            return response(500, "Delete permission failed", null);
        }
    }

    @Override
    public Map<String, Object> getPermissionById(Long id) {
        logger.info("Get permission detail, id: {}", id);

        try {
            Optional<Permission> optionalPermission = permissionRepository.findById(id);
            if (!optionalPermission.isPresent()) {
                return response(404, "Permission does not exist", null);
            }

            return response(200, "Permission fetched successfully", convertToMap(optionalPermission.get()));
        } catch (DataAccessException ex) {
            logger.warn("Permission table is not available", ex);
            return response(404, "Permission does not exist", null);
        }
    }

    @Override
    public List<Map<String, Object>> getAllPermissions() {
        logger.info("Get all permissions");

        try {
            return permissionRepository.findAll().stream()
                    .map(this::convertToMap)
                    .collect(Collectors.toList());
        } catch (DataAccessException ex) {
            logger.warn("Permission table is not available, returning empty list");
            return Collections.emptyList();
        }
    }

    @Override
    public Map<String, Object> getPermissionsByPage(Integer page, Integer size, String name, String code, Long parentId) {
        logger.info("Query permissions, page: {}, size: {}, name: {}, code: {}, parentId: {}", page, size, name, code, parentId);

        if (page == null || page < 1) {
            page = 1;
        }
        if (size == null || size < 1 || size > 100) {
            size = 10;
        }

        try {
            List<Permission> permissions;
            long total;

            if (name != null && !name.trim().isEmpty()) {
                permissions = permissionRepository.findByNameContaining(name,
                        org.springframework.data.domain.PageRequest.of(page - 1, size));
                total = permissionRepository.countByNameContaining(name);
            } else if (code != null && !code.trim().isEmpty()) {
                permissions = permissionRepository.findByCodeContaining(code,
                        org.springframework.data.domain.PageRequest.of(page - 1, size));
                total = permissionRepository.countByCodeContaining(code);
            } else if (parentId != null) {
                permissions = permissionRepository.findByParentId(parentId,
                        org.springframework.data.domain.PageRequest.of(page - 1, size));
                total = permissionRepository.countByParentId(parentId);
            } else {
                permissions = permissionRepository.findAll(
                        org.springframework.data.domain.PageRequest.of(page - 1, size)).getContent();
                total = permissionRepository.count();
            }

            List<Map<String, Object>> permissionMaps = permissions.stream()
                    .map(this::convertToMap)
                    .collect(Collectors.toList());

            return response(200, "Query successful", pageData(permissionMaps, total, page, size));
        } catch (DataAccessException ex) {
            logger.warn("Permission table is not available, returning empty page");
            return response(200, "Query successful", pageData(Collections.emptyList(), 0, page, size));
        } catch (Exception ex) {
            logger.error("Query permissions failed", ex);
            return response(500, "Query permissions failed", null);
        }
    }

    @Override
    public List<Map<String, Object>> getPermissionsByParentId(Long parentId) {
        logger.info("Get permissions by parent id: {}", parentId);

        try {
            return permissionRepository.findByParentId(parentId).stream()
                    .map(this::convertToMap)
                    .collect(Collectors.toList());
        } catch (DataAccessException ex) {
            logger.warn("Permission table is not available, returning empty list");
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> getTopLevelPermissions() {
        logger.info("Get top-level permissions");

        try {
            return permissionRepository.findByParentIdIsNull().stream()
                    .map(this::convertToMap)
                    .collect(Collectors.toList());
        } catch (DataAccessException ex) {
            logger.warn("Permission table is not available, returning empty tree");
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> getPermissionsByCodes(List<String> codes) {
        logger.info("Get permissions by codes");

        if (codes == null || codes.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            return permissionRepository.findByCodeIn(codes).stream()
                    .map(this::convertToMap)
                    .collect(Collectors.toList());
        } catch (DataAccessException ex) {
            logger.warn("Permission table is not available, returning empty list");
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Map<String, Object> updatePermissionStatus(Long id, Integer status) {
        logger.info("Update permission status, id: {}, status: {}", id, status);

        try {
            Optional<Permission> optionalPermission = permissionRepository.findById(id);
            if (!optionalPermission.isPresent()) {
                return response(404, "Permission does not exist", null);
            }

            Permission permission = optionalPermission.get();
            permission.setStatus(status);
            Permission savedPermission = permissionRepository.save(permission);
            return response(200, "Permission status updated successfully", convertToMap(savedPermission));
        } catch (DataAccessException ex) {
            logger.warn("Permission table is not available", ex);
            return response(503, "Permission table is not available", null);
        } catch (Exception ex) {
            logger.error("Update permission status failed, id: {}", id, ex);
            return response(500, "Update permission status failed", null);
        }
    }

    private Map<String, Object> convertToMap(Permission permission) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", permission.getId());
        result.put("name", permission.getName());
        result.put("code", permission.getCode());
        result.put("description", permission.getDescription());
        result.put("url", permission.getUrl());
        result.put("method", permission.getMethod());
        result.put("permissionType", permission.getPermissionType());
        result.put("parentId", permission.getParentId());
        result.put("icon", permission.getIcon());
        result.put("sort", permission.getSort());
        result.put("status", permission.getStatus());
        result.put("createTime", permission.getCreateTime());
        result.put("updateTime", permission.getUpdateTime());
        return result;
    }

    private Map<String, Object> response(int code, String message, Object data) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", code);
        result.put("message", message);
        result.put("data", data);
        return result;
    }

    private Map<String, Object> pageData(List<Map<String, Object>> items, long total, int page, int size) {
        Map<String, Object> data = new HashMap<>();
        long pages = total % size == 0 ? total / size : total / size + 1;
        data.put("items", items);
        data.put("total", total);
        data.put("page", page);
        data.put("size", size);
        data.put("pages", pages);
        data.put("hasNext", page < pages);
        data.put("hasPrevious", page > 1);
        return data;
    }

    private boolean isBlank(Object value) {
        return value == null || value.toString().trim().isEmpty();
    }

    private String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }

    private Integer intValue(Object value, Integer defaultValue) {
        if (value == null || value.toString().trim().isEmpty()) {
            return defaultValue;
        }
        return Integer.parseInt(value.toString());
    }
}
