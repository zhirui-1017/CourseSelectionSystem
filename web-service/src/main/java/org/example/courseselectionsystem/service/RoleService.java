package org.example.courseselectionsystem.service;

import org.example.courseselectionsystem.entity.Role;
import org.example.courseselectionsystem.vo.PageRequest;
import org.example.courseselectionsystem.vo.RoleRequest;
import org.example.courseselectionsystem.vo.RoleVO;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 角色服务接口
 * 定义角色相关的服务方法
 */
public interface RoleService {

    /**
     * 添加角色方法
     * @param request 角色请求参数
     * @return 角色ID
     */
    Role addRole(Role role);

    default Long addRole(RoleRequest request) {
        Role role = new Role();
        role.setName(request.getName());
        role.setCode(request.getCode());
        role.setDescription(request.getDescription());
        role.setStatus(request.getStatus());
        return addRole(role).getId();
    }

    default Long addRole(String name, String code) {
        Role role = new Role();
        role.setName(name);
        role.setCode(code);
        role.setStatus(1);
        return addRole(role).getId();
    }

    /**
     * 更新角色方法
     * @param id 角色ID
     * @param request 角色请求参数
     */
    Role updateRole(Role role);

    default void updateRole(Long id, RoleRequest request) {
        Role role = new Role();
        role.setId(id);
        role.setName(request.getName());
        role.setCode(request.getCode());
        role.setDescription(request.getDescription());
        role.setStatus(request.getStatus());
        updateRole(role);
    }

    default void updateRole(Long id, String name, String code) {
        Role role = new Role();
        role.setId(id);
        role.setName(name);
        role.setCode(code);
        updateRole(role);
    }

    /**
     * 删除角色方法
     * @param id 角色ID
     */
    boolean deleteRole(Long id);

    /**
     * 根据ID获取角色方法
     * @param id 角色ID
     * @return 角色信息
     */
    Role getRoleById(Long id);

    /**
     * 获取所有角色方法
     * @return 角色列表
     */
    List<Role> getAllRoles();

    /**
     * 获取角色列表方法
     * @param pageRequest 分页请求参数
     * @return 角色列表
     */
    Page<Role> getRoleList(PageRequest pageRequestParam, String name, String code, Integer status);

    default PageResult<Object> getRoleList(PageRequest pageRequest) {
        Page<Role> page = getRoleList(pageRequest, null, null, null);
        return new SimplePageResult<>(List.copyOf(page.getContent()), page.getTotalElements(), pageRequest.getPageNum(), pageRequest.getPageSize());
    }

    /**
     * 根据角色名称查询角色方法
     * @param name 角色名称
     * @return 角色列表
     */
    Role getRoleByName(String name);

    Role getRoleByCode(String code);

    /**
     * 启用角色方法
     * @param id 角色ID
     */
    default void enableRole(Long id) {
        changeRoleStatus(id, 1);
    }

    /**
     * 禁用角色方法
     * @param id 角色ID
     */
    default void disableRole(Long id) {
        changeRoleStatus(id, 0);
    }

    List<Role> getActiveRoles();

    boolean changeRoleStatus(Long roleId, Integer status);

    boolean batchDeleteRoles(Long[] roleIds);

    /**
     * 分页结果接口
     * @param <T> 数据泛型
     */
    interface PageResult<T> {
        List<T> getItems();
        long getTotal();
        int getPageNum();
        int getPageSize();
    }

    class SimplePageResult<T> implements PageResult<T> {
        private final List<T> items;
        private final long total;
        private final int pageNum;
        private final int pageSize;

        public SimplePageResult(List<T> items, long total, int pageNum, int pageSize) {
            this.items = items;
            this.total = total;
            this.pageNum = pageNum;
            this.pageSize = pageSize;
        }

        public List<T> getItems() {
            return items;
        }

        public long getTotal() {
            return total;
        }

        public int getPageNum() {
            return pageNum;
        }

        public int getPageSize() {
            return pageSize;
        }
    }
}
