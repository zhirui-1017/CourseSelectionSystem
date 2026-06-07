package org.example.courseselectionsystem.vo;

import lombok.Data;

@Data
public class RoleRequest {
    private String name;
    private String code;
    private String description;
    private Integer status;
}
