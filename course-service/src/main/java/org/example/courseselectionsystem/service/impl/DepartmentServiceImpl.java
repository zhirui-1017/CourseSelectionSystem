package org.example.courseselectionsystem.service.impl;

import org.example.courseselectionsystem.common.Result;
import org.example.courseselectionsystem.entity.Department;
import org.example.courseselectionsystem.exception.BusinessException;
import org.example.courseselectionsystem.repository.DepartmentRepository;
import org.example.courseselectionsystem.service.DepartmentService;
import org.example.courseselectionsystem.vo.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * 学院服务实现类
 */
@Service
public class DepartmentServiceImpl implements DepartmentService {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Department addDepartment(Department department) {
        // 验证学院信息
        if (department.getDepartmentName() == null || department.getDepartmentName().trim().isEmpty()) {
            throw new BusinessException(Result.PARAM_ERROR, "学院名称不能为空");
        }

        // 检查学院编号是否已存在
        if (department.getDepartmentCode() != null) {
            Department existingDepartment = departmentRepository.findByDepartmentCode(department.getDepartmentCode());
            if (existingDepartment != null) {
                throw new BusinessException(Result.PARAM_ERROR, "学院编号已存在");
            }
        }

        // 检查学院名称是否已存在
        Department existingDepartmentByName = departmentRepository.findByDepartmentName(department.getDepartmentName());
        if (existingDepartmentByName != null) {
            throw new BusinessException(Result.PARAM_ERROR, "学院名称已存在");
        }

        // 设置创建时间和更新时间
        department.setCreateTime(new Date());
        department.setUpdateTime(new Date());

        // 如果未设置状态，默认为启用
        if (department.getStatus() == null) {
            department.setStatus(1);
        }

        // 保存学院信息
        return departmentRepository.save(department);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Department updateDepartment(Department department) {
        // 验证学院ID
        if (department.getId() == null) {
            throw new BusinessException(Result.PARAM_ERROR, "学院ID不能为空");
        }

        // 检查学院是否存在
        Department existingDepartment = departmentRepository.findById(department.getId())
                .orElseThrow(() -> new BusinessException(Result.NOT_FOUND, "学院不存在"));

        // 检查学院编号是否与其他学院重复
        if (department.getDepartmentCode() != null && !department.getDepartmentCode().equals(existingDepartment.getDepartmentCode())) {
            Department checkDepartment = departmentRepository.findByDepartmentCode(department.getDepartmentCode());
            if (checkDepartment != null) {
                throw new BusinessException(Result.PARAM_ERROR, "学院编号已存在");
            }
        }

        // 检查学院名称是否与其他学院重复
        if (department.getDepartmentName() != null && !department.getDepartmentName().equals(existingDepartment.getDepartmentName())) {
            Department checkDepartment = departmentRepository.findByDepartmentName(department.getDepartmentName());
            if (checkDepartment != null) {
                throw new BusinessException(Result.PARAM_ERROR, "学院名称已存在");
            }
        }

        // 设置更新时间
        department.setUpdateTime(new Date());

        // 保存更新后的学院信息
        return departmentRepository.save(department);
    }

    @Override
    public Department getDepartmentById(Long departmentId) {
        // 根据ID查询学院
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new BusinessException(Result.NOT_FOUND, "学院不存在"));
    }

    @Override
    public Department getDepartmentByCode(String departmentCode) {
        // 根据学院编号查询学院
        Department department = departmentRepository.findByDepartmentCode(departmentCode);
        if (department == null) {
            throw new BusinessException(Result.NOT_FOUND, "学院不存在");
        }
        return department;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDepartment(Long departmentId) {
        // 检查学院是否存在
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new BusinessException(Result.NOT_FOUND, "学院不存在"));

        // 检查是否有专业关联
        // 这里省略检查逻辑，实际需要根据业务需求实现

        // 删除学院
        departmentRepository.delete(department);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchDeleteDepartments(List<Long> departmentIds) {
        // 批量删除学院
        List<Department> departments = departmentRepository.findAllById(departmentIds);
        if (departments.isEmpty()) {
            throw new BusinessException(Result.NOT_FOUND, "未找到要删除的学院");
        }
        
        // 检查是否有专业关联
        // 这里省略检查逻辑，实际需要根据业务需求实现

        departmentRepository.deleteAll(departments);
        return true;
    }

    @Override
    public Page<Department> getDepartmentList(PageRequest pageRequest, String departmentName, String departmentCode, Integer status) {
        // 构建排序规则
        PageRequest request = pageRequest == null ? new PageRequest() : pageRequest;
        int pageNum = request.getPageNum() == null || request.getPageNum() < 1 ? 1 : request.getPageNum();
        int pageSize = request.getPageSize() == null || request.getPageSize() < 1 ? 10 : Math.min(request.getPageSize(), 100);

        // 构建分页请求
        org.springframework.data.domain.PageRequest springPageRequest =
                org.springframework.data.domain.PageRequest.of(pageNum - 1, pageSize, departmentSort(request));

        return departmentRepository.findDepartments(blankToNull(departmentName), blankToNull(departmentCode), status, springPageRequest);
    }

    @Override
    public List<Department> getActiveDepartments() {
        // 获取所有启用状态的学院
        return departmentRepository.findByStatusOrderBySortAsc(1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean changeDepartmentStatus(Long departmentId, Integer status) {
        // 验证状态值
        if (status != 1 && status != 2) {
            throw new BusinessException(Result.PARAM_ERROR, "无效的状态值");
        }

        // 检查学院是否存在
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new BusinessException(Result.NOT_FOUND, "学院不存在"));

        // 更新状态
        department.setStatus(status);
        department.setUpdateTime(new Date());
        departmentRepository.save(department);
        return true;
    }

    private Sort departmentSort(PageRequest request) {
        String sortField = request.getSortField();
        String property = StringUtils.hasText(sortField) ? departmentSortProperty(sortField) : "id";
        Sort.Direction direction = "desc".equalsIgnoreCase(request.getSortOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC;
        if (!StringUtils.hasText(sortField)) {
            direction = Sort.Direction.DESC;
        }
        return Sort.by(new Sort.Order(direction, property));
    }

    private String departmentSortProperty(String field) {
        String normalized = field.trim();
        if ("code".equalsIgnoreCase(normalized)) {
            return "departmentCode";
        }
        if ("name".equalsIgnoreCase(normalized)) {
            return "departmentName";
        }
        if ("departmentCode".equalsIgnoreCase(normalized)
                || "departmentName".equalsIgnoreCase(normalized)
                || "directorId".equalsIgnoreCase(normalized)
                || "directorName".equalsIgnoreCase(normalized)
                || "sort".equalsIgnoreCase(normalized)
                || "status".equalsIgnoreCase(normalized)
                || "createTime".equalsIgnoreCase(normalized)
                || "updateTime".equalsIgnoreCase(normalized)
                || "id".equalsIgnoreCase(normalized)) {
            return normalized;
        }
        return "id";
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }
}
