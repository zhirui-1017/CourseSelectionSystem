package org.example.courseselectionsystem.auth;

/**
 * ThreadLocal 存储当前请求的用户信息
 * <p>
 * 使用方式：
 * <pre>
 *   // 设置当前用户（由 Filter/Interceptor 自动完成）
 *   UserContext.set(new JwtPayload(1L, "admin", "ROLE_ADMIN"));
 *
 *   // 在 Service 层获取当前用户
 *   JwtPayload user = UserContext.get();
 *   Long userId = user.getUserId();
 * </pre>
 * <p>
 * 注意：使用完后务必调用 {@link #clear()} 清理，避免内存泄漏。
 */
public final class UserContext {

    private static final ThreadLocal<JwtPayload> CONTEXT = new ThreadLocal<>();

    private UserContext() {
    }

    /**
     * 设置当前请求的用户上下文
     */
    public static void set(JwtPayload payload) {
        CONTEXT.set(payload);
    }

    /**
     * 设置当前请求的用户上下文
     */
    public static void set(Long userId, String username, String role) {
        CONTEXT.set(new JwtPayload(userId, username, role));
    }

    /**
     * 获取当前请求的用户上下文
     */
    public static JwtPayload get() {
        return CONTEXT.get();
    }

    /**
     * 获取当前用户ID
     */
    public static Long getUserId() {
        JwtPayload payload = CONTEXT.get();
        return payload != null ? payload.getUserId() : null;
    }

    /**
     * 获取当前用户名
     */
    public static String getUsername() {
        JwtPayload payload = CONTEXT.get();
        return payload != null ? payload.getUsername() : null;
    }

    /**
     * 获取当前用户角色
     */
    public static String getRole() {
        JwtPayload payload = CONTEXT.get();
        return payload != null ? payload.getRole() : null;
    }

    /**
     * 清除当前请求的用户上下文（必须调用）
     */
    public static void clear() {
        CONTEXT.remove();
    }
}
