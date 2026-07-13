package org.example.courseselectionsystem.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类 —— 所有微服务共享同一套生成/解析逻辑
 */
public final class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    /** 默认密钥（生产环境建议外部化配置） */
    private static final String DEFAULT_SECRET = "CourseSelectionSystemSecretKeyForJwtTokenGeneration2026!@#";

    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(
            DEFAULT_SECRET.getBytes(StandardCharsets.UTF_8));

    private JwtUtil() {
    }

    /**
     * 生成 JWT Token
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param role     角色
     * @return JWT Token 字符串
     */
    public static String generateToken(Long userId, String username, String role) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + AuthConstants.TOKEN_EXPIRE_MS);

        return Jwts.builder()
                .claim(AuthConstants.CLAIM_USER_ID, userId)
                .claim(AuthConstants.CLAIM_USERNAME, username)
                .claim(AuthConstants.CLAIM_ROLE, role)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 从 Bearer Token 字符串中提取纯 token（去除 "Bearer " 前缀）
     */
    public static String extractToken(String bearerToken) {
        if (bearerToken == null || bearerToken.isEmpty()) {
            return null;
        }
        if (bearerToken.startsWith(AuthConstants.TOKEN_PREFIX)) {
            return bearerToken.substring(AuthConstants.TOKEN_PREFIX.length()).trim();
        }
        return bearerToken.trim();
    }

    /**
     * 解析 JWT Token 为 JwtPayload
     *
     * @param token JWT token 字符串（带或不带 Bearer 前缀）
     * @return JwtPayload，解析失败返回 null
     */
    public static JwtPayload parseToken(String token) {
        try {
            String pureToken = extractToken(token);
            if (pureToken == null) {
                return null;
            }
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(pureToken)
                    .getBody();

            JwtPayload payload = new JwtPayload();
            Object userIdObj = claims.get(AuthConstants.CLAIM_USER_ID);
            payload.setUserId(userIdObj instanceof Integer ? ((Integer) userIdObj).longValue() : (Long) userIdObj);
            payload.setUsername((String) claims.get(AuthConstants.CLAIM_USERNAME));
            payload.setRole((String) claims.get(AuthConstants.CLAIM_ROLE));
            return payload;
        } catch (ExpiredJwtException e) {
            log.warn("JWT Token 已过期: {}", e.getMessage());
            return null;
        } catch (UnsupportedJwtException | MalformedJwtException e) {
            log.warn("JWT Token 格式错误: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("JWT Token 解析失败", e);
            return null;
        }
    }

    /**
     * 验证 Token 是否有效
     *
     * @param token JWT token 字符串
     * @return true 有效，false 无效或过期
     */
    public static boolean validateToken(String token) {
        return parseToken(token) != null;
    }
}
