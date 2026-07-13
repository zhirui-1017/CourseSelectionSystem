package org.example.courseselectionsystem.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JWT Token 载荷 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtPayload {

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 用户角色 */
    private String role;
}
