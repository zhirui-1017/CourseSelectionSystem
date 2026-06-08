package org.example.courseselectionsystem.controller;

import org.example.courseselectionsystem.common.Result;
import org.example.courseselectionsystem.service.RoleService;
import org.example.courseselectionsystem.vo.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色控制器类
 * 处理角色相关的HTTP请求
 */
@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    @Autowired
    private RoleService roleService;

    /**
     * 添加角色接口
     * @param name 角色名称
     * @param code 角色代码
     * @return 添加结果
     */
    @PostMapping
    public Result<Long> addRole(@RequestParam String name, @RequestParam String code) {
        Long id = roleService.addRole(name, code);
        return Result.success(id);
    }

    /**
     * 更新角色接口
     * @param id 角色ID
     * @param name 角色名称
     * @param code 角色代码
     * @return 更新结果
     */
    @PutMapping("/{id}")
    public Result<Boolean> updateRole(@PathVariable Long id, @RequestParam String name, @RequestParam String code) {
        roleService.updateRole(id, name, code);
        return Result.success(true);
    }

    /**
     * 删除角色接口
     * @param id 角色ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return Result.success(true);
    }

    /**
     * 根据ID获取角色接口
     * @param id 角色ID
     * @return 角色信息
     */
    @GetMapping("/{id}")
    public Result<Object> getRoleById(@PathVariable Long id) {
        Object role = roleService.getRoleById(id);
        return Result.success(role);
    }

    /**
     * 获取所有角色接口
     * @return 角色列表
     */
    @GetMapping("/all")
    public Result<List<Object>> getAllRoles() {
        List<Object> roleList = List.copyOf(roleService.getAllRoles());
        return Result.success(roleList);
    }

    /**
     * 获取角色列表接口
     * @param pageRequest 分页请求参数
     * @return 角色列表
     */
    @GetMapping("/list")
    public Result<PageResult<Object>> getRoleList(PageRequest pageRequest,
                                                  @RequestParam(required = false) String name,
                                                  @RequestParam(required = false) String code,
                                                  @RequestParam(required = false) Integer status) {
        PageRequest request = pageRequest == null ? new PageRequest() : pageRequest;
        org.springframework.data.domain.Page<org.example.courseselectionsystem.entity.Role> rolePage =
                roleService.getRoleList(request, name, code, status);
        return Result.success(new PageResult<>(
                new java.util.ArrayList<>(rolePage.getContent()),
                rolePage.getTotalElements(),
                rolePage.getNumber() + 1,
                rolePage.getSize()
        ));
    }

    /**
     * 根据名称查询角色接口
     * @param name 角色名称
     * @return 角色信息
     */
    @GetMapping("/by-name/{name}")
    public Result<Object> getRoleByName(@PathVariable String name) {
        Object role = roleService.getRoleByName(name);
        return Result.success(role);
    }

    /**
     * 根据代码查询角色接口
     * @param code 角色代码
     * @return 角色信息
     */
    @GetMapping("/by-code/{code}")
    public Result<Object> getRoleByCode(@PathVariable String code) {
        Object role = roleService.getRoleByCode(code);
        return Result.success(role);
    }

    /**
     * 启用角色接口
     * @param id 角色ID
     * @return 启用结果
     */
    @PutMapping("/{id}/enable")
    public Result<Boolean> enableRole(@PathVariable Long id) {
        roleService.enableRole(id);
        return Result.success(true);
    }

    /**
     * 禁用角色接口
     * @param id 角色ID
     * @return 禁用结果
     */
    @PutMapping("/{id}/disable")
    public Result<Boolean> disableRole(@PathVariable Long id) {
        roleService.disableRole(id);
        return Result.success(true);
    }

    /**
     * 分页结果类
     * @param <T> 数据泛型
     */
    public static class PageResult<T> {
        private List<T> items;
        private long total;
        private int pageNum;
        private int pageSize;

        public PageResult(List<T> items, long total, int pageNum, int pageSize) {
            this.items = items;
            this.total = total;
            this.pageNum = pageNum;
            this.pageSize = pageSize;
        }

        public List<T> getItems() {
            return items;
        }

        public void setItems(List<T> items) {
            this.items = items;
        }

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }

        public int getPageNum() {
            return pageNum;
        }

        public void setPageNum(int pageNum) {
            this.pageNum = pageNum;
        }

        public int getPageSize() {
            return pageSize;
        }

        public void setPageSize(int pageSize) {
            this.pageSize = pageSize;
        }
    }
}
