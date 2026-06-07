package org.example.courseselectionsystem.service.impl;

import org.example.courseselectionsystem.common.Result;
import org.example.courseselectionsystem.entity.Major;
import org.example.courseselectionsystem.exception.BusinessException;
import org.example.courseselectionsystem.repository.MajorRepository;
import org.example.courseselectionsystem.service.MajorService;
import org.example.courseselectionsystem.vo.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 专业服务实现类
 */
@Service
public class MajorServiceImpl implements MajorService {

    @Autowired
    private MajorRepository majorRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Major addMajor(Major major) {
        // 验证专业信息
        if (major.getMajorName() == null || major.getMajorName().trim().isEmpty()) {
            throw new BusinessException(Result.PARAM_ERROR, "专业名称不能为空");
        }
        if (major.getDepartmentId() == null) {
            throw new BusinessException(Result.PARAM_ERROR, "所属学院不能为空");
        }

        // 检查专业编号是否已存在
        if (major.getMajorCode() != null) {
            Major existingMajor = majorRepository.findByMajorCode(major.getMajorCode());
            if (existingMajor != null) {
                throw new BusinessException(Result.PARAM_ERROR, "专业编号已存在");
            }
        }

        // 检查专业名称是否已存在
        // 注：同一个学院下专业名称不能重复
        Major existingMajorByName = majorRepository.findByMajorName(major.getMajorName());
        if (existingMajorByName != null && existingMajorByName.getDepartmentId().equals(major.getDepartmentId())) {
            throw new BusinessException(Result.PARAM_ERROR, "同一学院下专业名称不能重复");
        }

        // 设置创建时间和更新时间
        major.setCreateTime(new Date());
        major.setUpdateTime(new Date());

        // 如果未设置状态，默认为启用
        if (major.getStatus() == null) {
            major.setStatus(1);
        }

        // 保存专业信息
        return majorRepository.save(major);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Major updateMajor(Major major) {
        // 验证专业ID
        if (major.getId() == null) {
            throw new BusinessException(Result.PARAM_ERROR, "专业ID不能为空");
        }

        // 检查专业是否存在
        Major existingMajor = majorRepository.findById(major.getId())
                .orElseThrow(() -> new BusinessException(Result.NOT_FOUND, "专业不存在"));

        // 检查专业编号是否与其他专业重复
        if (major.getMajorCode() != null && !major.getMajorCode().equals(existingMajor.getMajorCode())) {
            Major checkMajor = majorRepository.findByMajorCode(major.getMajorCode());
            if (checkMajor != null) {
                throw new BusinessException(Result.PARAM_ERROR, "专业编号已存在");
            }
        }

        // 检查专业名称是否与其他专业重复
        if (major.getMajorName() != null && !major.getMajorName().equals(existingMajor.getMajorName())) {
            Major checkMajor = majorRepository.findByMajorName(major.getMajorName());
            if (checkMajor != null && checkMajor.getDepartmentId().equals(major.getDepartmentId())) {
                throw new BusinessException(Result.PARAM_ERROR, "同一学院下专业名称不能重复");
            }
        }

        // 设置更新时间
        major.setUpdateTime(new Date());

        // 保存更新后的专业信息
        return majorRepository.save(major);
    }

    @Override
    public Major getMajorById(Long majorId) {
        // 根据ID查询专业
        return majorRepository.findById(majorId)
                .orElseThrow(() -> new BusinessException(Result.NOT_FOUND, "专业不存在"));
    }

    @Override
    public Major getMajorByCode(String majorCode) {
        // 根据专业编号查询专业
        Major major = majorRepository.findByMajorCode(majorCode);
        if (major == null) {
            throw new BusinessException(Result.NOT_FOUND, "专业不存在");
        }
        return major;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteMajor(Long majorId) {
        // 检查专业是否存在
        Major major = majorRepository.findById(majorId)
                .orElseThrow(() -> new BusinessException(Result.NOT_FOUND, "专业不存在"));

        // 检查是否有学生关联
        // 这里省略检查逻辑，实际需要根据业务需求实现

        // 删除专业
        majorRepository.delete(major);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchDeleteMajors(List<Long> majorIds) {
        // 批量删除专业
        List<Major> majors = majorRepository.findAllById(majorIds);
        if (majors.isEmpty()) {
            throw new BusinessException(Result.NOT_FOUND, "未找到要删除的专业");
        }
        
        // 检查是否有学生关联
        // 这里省略检查逻辑，实际需要根据业务需求实现

        majorRepository.deleteAll(majors);
        return true;
    }

    @Override
    public Page<Major> getMajorList(PageRequest pageRequestParam, String majorName, String majorCode, Long departmentId, Integer status) {
        // 构建排序规则
        Sort.Direction direction = pageRequestParam.getIsAsc() ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, pageRequestParam.getOrderByColumn());

        // 构建分页请求
        org.springframework.data.domain.PageRequest pageable =
                org.springframework.data.domain.PageRequest.of(pageRequestParam.getPageNum() - 1, pageRequestParam.getPageSize(), sort);

        // 根据查询条件查询专业列表
        // 注意：这里为了简化，直接调用findAll方法，实际应该根据具体条件实现自定义查询
        return majorRepository.findAll(pageable);
    }

    @Override
    public List<Major> getMajorsByDepartmentId(Long departmentId) {
        // 根据学院ID获取专业列表
        return majorRepository.findByDepartmentIdOrderBySortAsc(departmentId);
    }

    @Override
    public List<Major> getActiveMajorsByDepartmentId(Long departmentId) {
        // 根据学院ID获取启用的专业列表
        return majorRepository.findByDepartmentIdAndStatusOrderBySortAsc(departmentId, 1);
    }

    @Override
    public List<Major> getActiveMajors() {
        // 获取所有启用的专业
        return majorRepository.findByStatusOrderBySortAsc(1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean changeMajorStatus(Long majorId, Integer status) {
        // 验证状态值
        if (status != 1 && status != 2) {
            throw new BusinessException(Result.PARAM_ERROR, "无效的状态值");
        }

        // 检查专业是否存在
        Major major = majorRepository.findById(majorId)
                .orElseThrow(() -> new BusinessException(Result.NOT_FOUND, "专业不存在"));

        // 更新状态
        major.setStatus(status);
        major.setUpdateTime(new Date());
        majorRepository.save(major);
        return true;
    }
}
