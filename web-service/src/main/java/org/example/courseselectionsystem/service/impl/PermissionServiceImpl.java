package org.example.courseselectionsystem.service.impl;

import org.example.courseselectionsystem.entity.Permission;
import org.example.courseselectionsystem.repository.PermissionRepository;
import org.example.courseselectionsystem.service.PermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 权限服务实现类
 * 实现权限相关的业务逻辑
 */
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
        logger.info("添加权限: {}", permissionMap);
        
        // 参数验证
        if (permissionMap.get("name") == null || permissionMap.get("name").toString().trim().isEmpty()) {
            return Map.of("code", 400, "message", "权限名称不能为空", "data", null);
        }
        if (permissionMap.get("code") == null || permissionMap.get("code").toString().trim().isEmpty()) {
            return Map.of("code", 400, "message", "权限编码不能为空", "data", null);
        }
        
        String name = permissionMap.get("name").toString().trim();
        String code = permissionMap.get("code").toString().trim();
        
        // 检查权限名称是否已存在
        if (permissionRepository.existsByName(name)) {
            return Map.of("code", 400, "message", "权限名称已存在", "data", null);
        }
        
        // 检查权限编码是否已存在
        if (permissionRepository.existsByCode(code)) {
            return Map.of("code", 400, "message", "权限编码已存在", "data", null);
        }
        
        try {
            // 创建权限对象
            Permission permission = new Permission();
            permission.setName(name);
            permission.setCode(code);
            permission.setDescription(permissionMap.get("description") != null ? 
                    permissionMap.get("description").toString() : "");
            permission.setUrl(permissionMap.get("url") != null ? permissionMap.get("url").toString() : "");
            permission.setMethod(permissionMap.get("method") != null ? 
                    permissionMap.get("method").toString() : "");
            
            // 设置父权限ID（如果有）
            if (permissionMap.get("parentId") != null) {
                try {
                    Long parentId = Long.parseLong(permissionMap.get("parentId").toString());
                    permission.setParentId(parentId);
                } catch (NumberFormatException e) {
                    return Map.of("code", 400, "message", "无效的父权限ID", "data", null);
                }
            }
            
            Permission savedPermission = permissionRepository.save(permission);
            logger.info("添加权限成功，ID: {}", savedPermission.getId());
            return Map.of("code", 200, "message", "添加权限成功", "data", convertToMap(savedPermission));
        } catch (Exception e) {
            logger.error("添加权限失败", e);
            return Map.of("code", 500, "message", "添加权限失败，请重试", "data", null);
        }
    }

    @Override
    @Transactional
    public Map<String, Object> updatePermission(Long id, Map<String, Object> permissionMap) {
        logger.info("更新权限，ID: {}, 数据: {}", id, permissionMap);
        
        // 检查权限是否存在
        Optional<Permission> optionalPermission = permissionRepository.findById(id);
        if (!optionalPermission.isPresent()) {
            return Map.of("code", 404, "message", "权限不存在", "data", null);
        }
        
        try {
            Permission permission = optionalPermission.get();
            
            // 更新权限名称
            if (permissionMap.containsKey("name")) {
                String name = permissionMap.get("name").toString().trim();
                if (name.isEmpty()) {
                    return Map.of("code", 400, "message", "权限名称不能为空", "data", null);
                }
                
                // 检查新名称是否与其他权限重复
                if (!permission.getName().equals(name) && permissionRepository.existsByName(name)) {
                    return Map.of("code", 400, "message", "权限名称已存在", "data", null);
                }
                
                permission.setName(name);
            }
            
            // 更新权限编码
            if (permissionMap.containsKey("code")) {
                String code = permissionMap.get("code").toString().trim();
                if (code.isEmpty()) {
                    return Map.of("code", 400, "message", "权限编码不能为空", "data", null);
                }
                
                // 检查新编码是否与其他权限重复
                if (!permission.getCode().equals(code) && permissionRepository.existsByCode(code)) {
                    return Map.of("code", 400, "message", "权限编码已存在", "data", null);
                }
                
                permission.setCode(code);
            }
            
            // 更新其他字段
            if (permissionMap.containsKey("description")) {
                permission.setDescription(permissionMap.get("description").toString());
            }
            if (permissionMap.containsKey("url")) {
                permission.setUrl(permissionMap.get("url").toString());
            }
            if (permissionMap.containsKey("method")) {
                permission.setMethod(permissionMap.get("method").toString());
            }
            if (permissionMap.containsKey("parentId")) {
                try {
                    Long parentId = Long.parseLong(permissionMap.get("parentId").toString());
                    permission.setParentId(parentId);
                } catch (NumberFormatException e) {
                    return Map.of("code", 400, "message", "无效的父权限ID", "data", null);
                }
            }
            
            Permission updatedPermission = permissionRepository.save(permission);
            logger.info("更新权限成功，ID: {}", id);
            return Map.of("code", 200, "message", "更新权限成功", "data", convertToMap(updatedPermission));
        } catch (Exception e) {
            logger.error("更新权限失败，ID: {}", id, e);
            return Map.of("code", 500, "message", "更新权限失败，请重试", "data", null);
        }
    }

    @Override
    @Transactional
    public Map<String, Object> deletePermission(Long id) {
        logger.info("删除权限，ID: {}", id);
        
        // 检查权限是否存在
        if (!permissionRepository.existsById(id)) {
            return Map.of("code", 404, "message", "权限不存在", "data", null);
        }
        
        // 检查是否有子权限
        if (permissionRepository.existsByParentId(id)) {
            return Map.of("code", 400, "message", "该权限下存在子权限，无法删除", "data", null);
        }
        
        try {
            permissionRepository.deleteById(id);
            logger.info("删除权限成功，ID: {}", id);
            return Map.of("code", 200, "message", "删除权限成功", "data", null);
        } catch (Exception e) {
            logger.error("删除权限失败，ID: {}", id, e);
            return Map.of("code", 500, "message", "删除权限失败，可能存在关联数据", "data", null);
        }
    }

    @Override
    public Map<String, Object> getPermissionById(Long id) {
        logger.info("获取权限详情，ID: {}", id);
        
        Optional<Permission> optionalPermission = permissionRepository.findById(id);
        if (!optionalPermission.isPresent()) {
            return Map.of("code", 404, "message", "权限不存在", "data", null);
        }
        
        Permission permission = optionalPermission.get();
        return Map.of("code", 200, "message", "获取权限详情成功", "data", convertToMap(permission));
    }

    @Override
    public List<Map<String, Object>> getAllPermissions() {
        logger.info("获取所有权限列表");
        
        List<Permission> permissions = permissionRepository.findAll();
        return permissions.stream()
                .map(this::convertToMap)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getPermissionsByPage(Integer page, Integer size, 
                                                    String name, String code, 
                                                    Long parentId) {
        logger.info("分页查询权限列表，页码: {}, 每页大小: {}, 名称: {}, 编码: {}, 父权限ID: {}", 
                page, size, name, code, parentId);
        
        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 10;
        
        try {
            // 构建查询
            List<Permission> permissions;
            long total;
            
            // 根据条件组合查询
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
            
            // 转换结果
            List<Map<String, Object>> permissionMaps = permissions.stream()
                    .map(this::convertToMap)
                    .collect(Collectors.toList());
            
            // 构建分页结果
            long pages = total % size == 0 ? total / size : total / size + 1;
            boolean hasNext = page < pages;
            boolean hasPrevious = page > 1;
            
            return Map.of(
                    "code", 200,
                    "message", "查询成功",
                    "data", Map.of(
                            "items", permissionMaps,
                            "total", total,
                            "page", page,
                            "size", size,
                            "pages", pages,
                            "hasNext", hasNext,
                            "hasPrevious", hasPrevious
                    )
            );
        } catch (Exception e) {
            logger.error("分页查询权限列表失败", e);
            return Map.of("code", 500, "message", "查询失败，请重试", "data", null);
        }
    }

    @Override
    public List<Map<String, Object>> getPermissionsByParentId(Long parentId) {
        logger.info("获取父权限下的子权限列表，父权限ID: {}", parentId);
        
        List<Permission> permissions = permissionRepository.findByParentId(parentId);
        return permissions.stream()
                .map(this::convertToMap)
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getTopLevelPermissions() {
        logger.info("获取顶级权限列表");
        
        List<Permission> permissions = permissionRepository.findByParentIdIsNull();
        return permissions.stream()
                .map(this::convertToMap)
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getPermissionsByCodes(List<String> codes) {
        logger.info("根据权限编码列表获取权限");
        
        if (codes == null || codes.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<Permission> permissions = permissionRepository.findByCodeIn(codes);
        return permissions.stream()
                .map(this::convertToMap)
                .collect(Collectors.toList());
    }
    
    /**
     * 将Permission对象转换为Map
     * @param permission 权限对象
     * @return 转换后的Map
     */
    private Map<String, Object> convertToMap(Permission permission) {
        return Map.of(
                "id", permission.getId(),
                "name", permission.getName(),
                "code", permission.getCode(),
                "description", permission.getDescription(),
                "url", permission.getUrl(),
                "method", permission.getMethod(),
                "parentId", permission.getParentId(),
                "createTime", permission.getCreateTime(),
                "updateTime", permission.getUpdateTime()
        );
    }
}
