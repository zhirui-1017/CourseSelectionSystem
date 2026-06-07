package org.example.courseselectionsystem.repository;

import org.example.courseselectionsystem.entity.Major;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 专业数据访问层
 */
@Repository
public interface MajorRepository extends JpaRepository<Major, Long> {

    /**
     * 根据专业编号查询专业
     * @param majorCode 专业编号
     * @return 专业对象
     */
    Major findByMajorCode(String majorCode);

    /**
     * 根据专业名称查询专业
     * @param majorName 专业名称
     * @return 专业对象
     */
    Major findByMajorName(String majorName);

    /**
     * 根据学院ID查询专业
     * @param departmentId 学院ID
     * @return 专业列表
     */
    List<Major> findByDepartmentIdOrderBySortAsc(Long departmentId);

    /**
     * 根据学院ID和状态查询专业
     * @param departmentId 学院ID
     * @param status 状态：1-启用
     * @return 专业列表
     */
    List<Major> findByDepartmentIdAndStatusOrderBySortAsc(Long departmentId, Integer status);

    /**
     * 查询启用状态的专业
     * @param status 状态：1-启用
     * @return 专业列表
     */
    List<Major> findByStatusOrderBySortAsc(Integer status);
}
