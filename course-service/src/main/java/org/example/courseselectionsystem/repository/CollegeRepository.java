package org.example.courseselectionsystem.repository;

import org.example.courseselectionsystem.entity.College;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CollegeRepository extends JpaRepository<College, Long> {
    Optional<College> findByCode(String code);

    List<College> findByName(String name);

    List<College> findByNameContaining(String name);
}
