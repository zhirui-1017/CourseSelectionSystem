package org.example.courseselectionsystem.service;

import org.example.courseselectionsystem.entity.Major;
import org.example.courseselectionsystem.vo.PageRequest;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 专业服务接口
 */
public interface MajorService {

    /**
     * 添加专业
     * @param major 专业对象
     * @return 保存后的专业对象
     */
    Major addMajor(Major major);

    /**
     * 更新专业信息
     * @param major 专业对象
     * @return 更新后的专业对象
     */
    Major updateMajor(Major major);

    /**
     * 根据ID获取专业信息
     * @param majorId 专业ID
     * @return 专业对象
     */
    Major getMajorById(Long majorId);

    /**
     * 根据专业编号获取专业
     * @param majorCode 专业编号
     * @return 专业对象
     */
    Major getMajorByCode(String majorCode);

    /**
     * 删除专业
     * @param majorId 专业ID
     * @return 删除结果
     */
    boolean deleteMajor(Long majorId);

    /**
     * 批量删除专业
     * @param majorIds 专业ID列表
     * @return 删除结果
     */
    boolean batchDeleteMajors(List<Long> majorIds);

    /**
     * 分页查询专业列表
     * @param pageRequest 分页请求参数
     * @param majorName 专业名称
     * @param majorCode 专业编号
     * @param departmentId 学院ID
     * @param status 状态
     * @return 专业分页列表
     */
    Page<Major> getMajorList(PageRequest pageRequest, String majorName, String majorCode, Long departmentId, Integer status);

    /**
     * 根据学院ID获取专业列表
     * @param departmentId 学院ID
     * @return 专业列表
     */
    List<Major> getMajorsByDepartmentId(Long departmentId);

    /**
     * 根据学院ID获取启用的专业列表
     * @param departmentId 学院ID
     * @return 专业列表
     */
    List<Major> getActiveMajorsByDepartmentId(Long departmentId);

    /**
     * 获取所有启用的专业
     * @return 专业列表
     */
    List<Major> getActiveMajors();

    /**
     * 启用/禁用专业
     * @param majorId 专业ID
     * @param status 状态：1-启用，2-禁用
     * @return 操作结果
     */
    boolean changeMajorStatus(Long majorId, Integer status);
}
