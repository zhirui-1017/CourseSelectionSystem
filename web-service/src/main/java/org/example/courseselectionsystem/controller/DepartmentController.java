package org.example.courseselectionsystem.controller;

import org.example.courseselectionsystem.common.Result;
import org.example.courseselectionsystem.entity.Department;
import org.example.courseselectionsystem.service.DepartmentService;
import org.example.courseselectionsystem.vo.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 学院控制器
 */
@RestController
@RequestMapping("/api/v1/departments")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    /**
     * 添加学院
     * @param department 学院信息
     * @return 保存后的学院信息
     */
    @PostMapping
    public Result addDepartment(@RequestBody Department department) {
        Department savedDepartment = departmentService.addDepartment(department);
        return Result.success(savedDepartment);
    }

    /**
     * 更新学院信息
     * @param departmentId 学院ID
     * @param department 学院信息
     * @return 更新后的学院信息
     */
    @PutMapping("/{departmentId}")
    public Result updateDepartment(@PathVariable Long departmentId, @RequestBody Department department) {
        department.setId(departmentId);
        Department updatedDepartment = departmentService.updateDepartment(department);
        return Result.success(updatedDepartment);
    }

    /**
     * 根据ID获取学院信息
     * @param departmentId 学院ID
     * @return 学院信息
     */
    @GetMapping("/{departmentId}")
    public Result getDepartmentById(@PathVariable Long departmentId) {
        Department department = departmentService.getDepartmentById(departmentId);
        return Result.success(department);
    }

    /**
     * 根据学院编号获取学院
     * @param departmentCode 学院编号
     * @return 学院信息
     */
    @GetMapping("/code/{departmentCode}")
    public Result getDepartmentByCode(@PathVariable String departmentCode) {
        Department department = departmentService.getDepartmentByCode(departmentCode);
        return Result.success(department);
    }

    /**
     * 删除学院
     * @param departmentId 学院ID
     * @return 删除结果
     */
    @DeleteMapping("/{departmentId}")
    public Result deleteDepartment(@PathVariable Long departmentId) {
        boolean result = departmentService.deleteDepartment(departmentId);
        return Result.success(result);
    }

    /**
     * 批量删除学院
     * @param departmentIds 学院ID列表
     * @return 删除结果
     */
    @DeleteMapping("/batch")
    public Result batchDeleteDepartments(@RequestBody Long[] departmentIds) {
        boolean result = departmentService.batchDeleteDepartments(Arrays.asList(departmentIds));
        return Result.success(result);
    }

    /**
     * 获取学院列表
     * @param pageRequest 分页请求参数
     * @param departmentName 学院名称
     * @param departmentCode 学院编号
     * @param status 状态
     * @return 学院列表
     */
    @GetMapping("/list")
    public Result getDepartmentList(PageRequest pageRequest,
                                  @RequestParam(required = false) String departmentName,
                                  @RequestParam(required = false) String departmentCode,
                                  @RequestParam(required = false) Integer status) {
        Page<Department> departmentPage = departmentService.getDepartmentList(pageRequest, departmentName, departmentCode, status);
        return Result.success(departmentPage);
    }

    /**
     * 修改学院状态
     * @param departmentId 学院ID
     * @param status 状态
     * @return 修改结果
     */
    @PutMapping("/{departmentId}/status")
    public Result changeDepartmentStatus(@PathVariable Long departmentId, @RequestParam Integer status) {
        boolean result = departmentService.changeDepartmentStatus(departmentId, status);
        return Result.success(result);
    }

    /**
     * 获取所有启用的学院
     * @return 学院列表
     */
    @GetMapping("/active")
    public Result getActiveDepartments() {
        List<Department> departments = departmentService.getActiveDepartments();
        return Result.success(departments);
    }
}
