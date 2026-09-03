package org.example.courseselectionsystem.repository;

import org.example.courseselectionsystem.entity.College;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CollegeRepository extends JpaRepository<College, Long> {
    Optional<College> findByCode(String code);

    List<College> findByName(String name);

    List<College> findByNameContaining(String name);

    @Query("SELECT c FROM College c WHERE 1=1 " +
            "AND ((:name IS NULL AND :code IS NULL) " +
            "     OR c.name LIKE CONCAT('%', :name, '%') " +
            "     OR c.code LIKE CONCAT('%', :code, '%')) " +
            "AND (:status IS NULL OR c.status = :status)")
    Page<College> findColleges(@Param("name") String name,
                               @Param("code") String code,
                               @Param("status") Integer status,
                               Pageable pageable);
}
