package org.example.courseselectionsystem.service;

import org.example.courseselectionsystem.entity.Department;
import org.example.courseselectionsystem.vo.PageRequest;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

/**
 * 学院服务接口
 */
public interface DepartmentService {

    /**
     * 添加学院
     * @param department 学院对象
     * @return 保存后的学院对象
     */
    Department addDepartment(Department department);

    default Department addDepartment(Map<String, Object> deptInfo) {
        Department department = new Department();
        department.setDepartmentName(String.valueOf(deptInfo.getOrDefault("departmentName", "")));
        department.setDepartmentCode(String.valueOf(deptInfo.getOrDefault("departmentCode", "")));
        return addDepartment(department);
    }

    /**
     * 更新学院信息
     * @param department 学院对象
     * @return 更新后的学院对象
     */
    Department updateDepartment(Department department);

    default Department updateDepartment(Map<String, Object> deptInfo) {
        Department department = new Department();
        Object id = deptInfo.get("id");
        if (id != null) {
            department.setId(Long.valueOf(String.valueOf(id)));
        }
        department.setDepartmentName(String.valueOf(deptInfo.getOrDefault("departmentName", "")));
        department.setDepartmentCode(String.valueOf(deptInfo.getOrDefault("departmentCode", "")));
        return updateDepartment(department);
    }

    /**
     * 根据ID获取学院信息
     * @param departmentId 学院ID
     * @return 学院对象
     */
    Department getDepartmentById(Long departmentId);

    /**
     * 根据学院编号获取学院
     * @param departmentCode 学院编号
     * @return 学院对象
     */
    Department getDepartmentByCode(String departmentCode);

    /**
     * 删除学院
     * @param departmentId 学院ID
     * @return 删除结果
     */
    boolean deleteDepartment(Long departmentId);

    default boolean deleteDepartment(String departmentId) {
        return deleteDepartment(Long.valueOf(departmentId));
    }

    /**
     * 批量删除学院
     * @param departmentIds 学院ID列表
     * @return 删除结果
     */
    boolean batchDeleteDepartments(List<Long> departmentIds);

    /**
     * 分页查询学院列表
     * @param pageRequest 分页请求参数
     * @param departmentName 学院名称
     * @param departmentCode 学院编号
     * @param status 状态
     * @return 学院分页列表
     */
    Page<Department> getDepartmentList(PageRequest pageRequest, String departmentName, String departmentCode, Integer status);

    /**
     * 获取所有启用的学院
     * @return 学院列表
     */
    List<Department> getActiveDepartments();

    default List<Department> getAllDepartments() {
        return getActiveDepartments();
    }

    /**
     * 启用/禁用学院
     * @param departmentId 学院ID
     * @param status 状态：1-启用，2-禁用
     * @return 操作结果
     */
    boolean changeDepartmentStatus(Long departmentId, Integer status);
}
