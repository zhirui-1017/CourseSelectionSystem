package org.example.courseselectionsystem.auth;

/**
 * JWT 认证相关常量
 */
public final class AuthConstants {

    private AuthConstants() {
    }

    /** Authorization 请求头名称 */
    public static final String HEADER_AUTHORIZATION = "Authorization";

    /** Token 前缀 */
    public static final String TOKEN_PREFIX = "Bearer ";

    /** 传递用户ID的请求头 */
    public static final String HEADER_USER_ID = "X-User-Id";

    /** 传递用户名的请求头 */
    public static final String HEADER_USERNAME = "X-Username";

    /** 传递用户角色的请求头 */
    public static final String HEADER_ROLE = "X-Role";

    /** JWT 中用户ID的 claim key */
    public static final String CLAIM_USER_ID = "userId";

    /** JWT 中用户名的 claim key */
    public static final String CLAIM_USERNAME = "username";

    /** JWT 中用户角色的 claim key */
    public static final String CLAIM_ROLE = "role";

    /** Token 有效期（毫秒），默认 24 小时 */
    public static final long TOKEN_EXPIRE_MS = 24 * 60 * 60 * 1000L;

    /** 登录路径 */
    public static final String LOGIN_PATH = "/api/v1/auth/login";

    /** 注册路径 */
    public static final String REGISTER_PATH = "/api/v1/auth/register";
}
