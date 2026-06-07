package org.example.courseselectionsystem.controller;

import org.example.courseselectionsystem.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 权限控制器
 * 处理权限相关的API请求
 */
@RestController
@RequestMapping("/api/v1/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    @Autowired
    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    /**
     * 添加权限
     * @param permission 权限信息
     * @return 操作结果
     */
    @PostMapping
    public Map<String, Object> addPermission(@RequestBody Map<String, Object> permission) {
        return permissionService.addPermission(permission);
    }

    /**
     * 更新权限信息
     * @param id 权限ID
     * @param permission 权限信息
     * @return 操作结果
     */
    @PutMapping("/{id}")
    public Map<String, Object> updatePermission(@PathVariable Long id, @RequestBody Map<String, Object> permission) {
        return permissionService.updatePermission(id, permission);
    }

    /**
     * 删除权限
     * @param id 权限ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> deletePermission(@PathVariable Long id) {
        return permissionService.deletePermission(id);
    }

    /**
     * 获取权限详情
     * @param id 权限ID
     * @return 权限信息
     */
    @GetMapping("/{id}")
    public Map<String, Object> getPermissionById(@PathVariable Long id) {
        return permissionService.getPermissionById(id);
    }

    /**
     * 获取所有权限列表
     * @return 权限列表
     */
    @GetMapping
    public List<Map<String, Object>> getAllPermissions() {
        return permissionService.getAllPermissions();
    }

    /**
     * 分页查询权限列表
     * @param page 页码
     * @param size 每页大小
     * @param name 权限名称(可选)
     * @param code 权限编码(可选)
     * @return 分页权限列表
     */
    @GetMapping("/page")
    public Map<String, Object> getPermissionsByPage(@RequestParam(defaultValue = "1") Integer page,
                                               @RequestParam(defaultValue = "10") Integer size,
                                               @RequestParam(required = false) String name,
                                               @RequestParam(required = false) String code) {
        return permissionService.getPermissionsByPage(page, size, name, code);
    }

    /**
     * 获取权限树形结构
     * @return 权限树
     */
    @GetMapping("/tree")
    public List<Map<String, Object>> getPermissionTree() {
        return permissionService.getPermissionTree();
    }

    /**
     * 更新权限状态
     * @param id 权限ID
     * @param status 状态值
     * @return 操作结果
     */
    @PutMapping("/{id}/status")
    public Map<String, Object> updatePermissionStatus(@PathVariable Long id, @RequestParam Integer status) {
        return permissionService.updatePermissionStatus(id, status);
    }
}
