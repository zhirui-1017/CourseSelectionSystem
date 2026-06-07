package org.example.courseselectionsystem.controller;

import org.example.courseselectionsystem.common.Result;
import org.example.courseselectionsystem.entity.Major;
import org.example.courseselectionsystem.service.MajorService;
import org.example.courseselectionsystem.vo.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 专业控制器
 */
@RestController
@RequestMapping("/api/v1/majors")
public class MajorController {

    @Autowired
    private MajorService majorService;

    /**
     * 添加专业
     * @param major 专业信息
     * @return 保存后的专业信息
     */
    @PostMapping
    public Result addMajor(@RequestBody Major major) {
        Major savedMajor = majorService.addMajor(major);
        return Result.success(savedMajor);
    }

    /**
     * 更新专业信息
     * @param majorId 专业ID
     * @param major 专业信息
     * @return 更新后的专业信息
     */
    @PutMapping("/{majorId}")
    public Result updateMajor(@PathVariable Long majorId, @RequestBody Major major) {
        major.setId(majorId);
        Major updatedMajor = majorService.updateMajor(major);
        return Result.success(updatedMajor);
    }

    /**
     * 根据ID获取专业信息
     * @param majorId 专业ID
     * @return 专业信息
     */
    @GetMapping("/{majorId}")
    public Result getMajorById(@PathVariable Long majorId) {
        Major major = majorService.getMajorById(majorId);
        return Result.success(major);
    }

    /**
     * 根据专业编号获取专业
     * @param majorCode 专业编号
     * @return 专业信息
     */
    @GetMapping("/code/{majorCode}")
    public Result getMajorByCode(@PathVariable String majorCode) {
        Major major = majorService.getMajorByCode(majorCode);
        return Result.success(major);
    }

    /**
     * 删除专业
     * @param majorId 专业ID
     * @return 删除结果
     */
    @DeleteMapping("/{majorId}")
    public Result deleteMajor(@PathVariable Long majorId) {
        boolean result = majorService.deleteMajor(majorId);
        return Result.success(result);
    }

    /**
     * 批量删除专业
     * @param majorIds 专业ID列表
     * @return 删除结果
     */
    @DeleteMapping("/batch")
    public Result batchDeleteMajors(@RequestBody Long[] majorIds) {
        boolean result = majorService.batchDeleteMajors(Arrays.asList(majorIds));
        return Result.success(result);
    }

    /**
     * 获取专业列表
     * @param pageRequest 分页请求参数
     * @param majorName 专业名称
     * @param majorCode 专业编号
     * @param departmentId 学院ID
     * @param status 状态
     * @return 专业列表
     */
    @GetMapping("/list")
    public Result getMajorList(PageRequest pageRequest,
                             @RequestParam(required = false) String majorName,
                             @RequestParam(required = false) String majorCode,
                             @RequestParam(required = false) Long departmentId,
                             @RequestParam(required = false) Integer status) {
        Page<Major> majorPage = majorService.getMajorList(pageRequest, majorName, majorCode, departmentId, status);
        return Result.success(majorPage);
    }

    /**
     * 根据学院ID获取专业列表
     * @param departmentId 学院ID
     * @return 专业列表
     */
    @GetMapping("/department/{departmentId}")
    public Result getMajorsByDepartmentId(@PathVariable Long departmentId) {
        List<Major> majors = majorService.getMajorsByDepartmentId(departmentId);
        return Result.success(majors);
    }

    /**
     * 根据学院ID获取启用的专业列表
     * @param departmentId 学院ID
     * @return 专业列表
     */
    @GetMapping("/department/{departmentId}/active")
    public Result getActiveMajorsByDepartmentId(@PathVariable Long departmentId) {
        List<Major> majors = majorService.getActiveMajorsByDepartmentId(departmentId);
        return Result.success(majors);
    }

    /**
     * 修改专业状态
     * @param majorId 专业ID
     * @param status 状态
     * @return 修改结果
     */
    @PutMapping("/{majorId}/status")
    public Result changeMajorStatus(@PathVariable Long majorId, @RequestParam Integer status) {
        boolean result = majorService.changeMajorStatus(majorId, status);
        return Result.success(result);
    }

    /**
     * 获取所有启用的专业
     * @return 专业列表
     */
    @GetMapping("/active")
    public Result getActiveMajors() {
        List<Major> majors = majorService.getActiveMajors();
        return Result.success(majors);
    }
}
