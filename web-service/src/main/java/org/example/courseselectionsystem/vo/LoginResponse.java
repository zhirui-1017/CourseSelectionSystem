package org.example.courseselectionsystem.vo;

import java.io.Serializable;
import java.util.Date;

/**
 * 登录响应类
 * 用于用户登录成功后的返回数据结构
 */
public class LoginResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户姓名
     */
    private String name;

    /**
     * 用户角色
     */
    private String roleName;

    /**
     * 访问令牌
     */
    private String accessToken;

    /**
     * 令牌过期时间
     */
    private Date expiresAt;

    /**
     * 获取用户ID
     * @return 用户ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 设置用户ID
     * @param userId 用户ID
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * 获取用户名
     * @return 用户名
     */
    public String getUsername() {
        return username;
    }

    /**
     * 设置用户名
     * @param username 用户名
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 获取用户姓名
     * @return 用户姓名
     */
    public String getName() {
        return name;
    }

    /**
     * 设置用户姓名
     * @param name 用户姓名
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取用户角色
     * @return 用户角色
     */
    public String getRoleName() {
        return roleName;
    }

    /**
     * 设置用户角色
     * @param roleName 用户角色
     */
    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    /**
     * 获取访问令牌
     * @return 访问令牌
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * 设置访问令牌
     * @param accessToken 访问令牌
     */
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    /**
     * 获取令牌过期时间
     * @return 令牌过期时间
     */
    public Date getExpiresAt() {
        return expiresAt;
    }

    /**
     * 设置令牌过期时间
     * @param expiresAt 令牌过期时间
     */
    public void setExpiresAt(Date expiresAt) {
        this.expiresAt = expiresAt;
    }

    @Override
    public String toString() {
        return "LoginResponse{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", name='" + name + '\'' +
                ", roleName='" + roleName + '\'' +
                ", accessToken='[PROTECTED]'" +
                ", expiresAt=" + expiresAt +
                '}';
    }
}
