package org.example.courseselectionsystem.repository;

import org.example.courseselectionsystem.entity.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 学院数据访问层
 */
@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    /**
     * 根据学院编号查询学院
     * @param departmentCode 学院编号
     * @return 学院对象
     */
    Department findByDepartmentCode(String departmentCode);

    /**
     * 根据学院名称查询学院
     * @param departmentName 学院名称
     * @return 学院对象
     */
    Department findByDepartmentName(String departmentName);

    /**
     * 查询启用状态的学院
     * @param status 状态：1-启用
     * @return 学院列表
     */
    List<Department> findByStatusOrderByIdAsc(Integer status);

    @Query("SELECT d FROM Department d WHERE 1=1 " +
            "AND (:departmentName IS NULL OR d.departmentName LIKE CONCAT('%', :departmentName, '%')) " +
            "AND (:departmentCode IS NULL OR d.departmentCode LIKE CONCAT('%', :departmentCode, '%')) " +
            "AND (:status IS NULL OR d.status = :status)")
    Page<Department> findDepartments(@Param("departmentName") String departmentName,
                                     @Param("departmentCode") String departmentCode,
                                     @Param("status") Integer status,
                                     Pageable pageable);
}
