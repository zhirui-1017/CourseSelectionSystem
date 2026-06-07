package org.example.courseselectionsystem.service.impl;

import org.example.courseselectionsystem.repository.CourseRepository;
import org.example.courseselectionsystem.repository.CourseSelectionRepository;
import org.example.courseselectionsystem.repository.DepartmentRepository;
import org.example.courseselectionsystem.service.AdminService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AdminServiceImpl implements AdminService {
    private final CourseRepository courseRepository;
    private final CourseSelectionRepository courseSelectionRepository;
    private final DepartmentRepository departmentRepository;

    public AdminServiceImpl(CourseRepository courseRepository,
                            CourseSelectionRepository courseSelectionRepository,
                            DepartmentRepository departmentRepository) {
        this.courseRepository = courseRepository;
        this.courseSelectionRepository = courseSelectionRepository;
        this.departmentRepository = departmentRepository;
    }

    @Override
    public Map<String, Object> getSystemStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("courseCount", courseRepository.count());
        stats.put("selectionCount", courseSelectionRepository.count());
        stats.put("departmentCount", departmentRepository.count());
        return stats;
    }

    @Override
    public Map<String, Object> getSelectionStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("selectionCount", courseSelectionRepository.count());
        stats.put("courseCount", courseRepository.count());
        return stats;
    }
}
