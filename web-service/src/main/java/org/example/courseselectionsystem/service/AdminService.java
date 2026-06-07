package org.example.courseselectionsystem.service;

import java.util.Map;

public interface AdminService {
    Map<String, Object> getSystemStats();

    Map<String, Object> getSelectionStats();
}
