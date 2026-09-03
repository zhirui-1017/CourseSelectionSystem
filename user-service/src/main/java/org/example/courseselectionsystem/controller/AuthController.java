package org.example.courseselectionsystem.controller;

import org.example.courseselectionsystem.auth.AuthConstants;
import org.example.courseselectionsystem.auth.JwtUtil;
import org.example.courseselectionsystem.common.Constants;
import org.example.courseselectionsystem.common.Result;
import org.example.courseselectionsystem.entity.User;
import org.example.courseselectionsystem.exception.BusinessException;
import org.example.courseselectionsystem.service.UserService;
import org.example.courseselectionsystem.vo.LoginRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 认证控制器 —— 提供 JWT 登录/登出接口
 * <p>
 * 对应路径: /api/v1/auth/**
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private static final String CAPTCHA_KEY_PREFIX = "captcha:";

    @Autowired
    private UserService userService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 用户登录（返回 JWT Token）。登录前先做 Redis 验证码校验（一次性）。
     *
     * @param loginRequest 登录请求（username + password + captchaId/captchaCode）
     * @return { token, user }
     */
    @PostMapping("/login")
    public Result login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("JWT 登录请求: username={}", loginRequest.getUsername());
        verifyCaptcha(loginRequest);
        Map<String, Object> loginResult = userService.login(loginRequest);
        return Result.success(loginResult);
    }

    /**
     * 生成登录验证码：随机码存入 Redis（TTL 2 分钟），图片以 base64 返回前端展示。
     *
     * @return { captchaId, image(data:image/png;base64,...) }
     */
    @GetMapping("/captcha")
    public Result captcha() {
        String code = randomCaptchaCode(4);
        String captchaId = UUID.randomUUID().toString();
        stringRedisTemplate.opsForValue().set(CAPTCHA_KEY_PREFIX + captchaId, code, Duration.ofMinutes(2));
        Map<String, Object> data = new HashMap<>();
        data.put("captchaId", captchaId);
        data.put("image", renderCaptchaImage(code));
        return Result.success(data);
    }

    /**
     * Redis 验证码校验：取后即删（一次性）。
     * 兼容：未携带 captchaId 的旧请求/自动化调用不强制校验。
     */
    private void verifyCaptcha(LoginRequest loginRequest) {
        String captchaId = loginRequest.getCaptchaId();
        if (captchaId == null || captchaId.isBlank()) {
            return;
        }
        String key = CAPTCHA_KEY_PREFIX + captchaId;
        String stored = stringRedisTemplate.opsForValue().get(key);
        if (stored == null) {
            throw new BusinessException(Result.PARAM_ERROR, "验证码已过期，请刷新后重试");
        }
        String input = loginRequest.getCaptchaCode() == null ? "" : loginRequest.getCaptchaCode().trim();
        if (!stored.equalsIgnoreCase(input)) {
            throw new BusinessException(Result.PARAM_ERROR, "验证码错误");
        }
        stringRedisTemplate.delete(key);
    }

    private static String randomCaptchaCode(int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(ThreadLocalRandom.current().nextInt(chars.length())));
        }
        return sb.toString();
    }

    private static String renderCaptchaImage(String code) {
        int width = 120;
        int height = 42;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(246, 248, 250));
        g.fillRect(0, 0, width, height);
        g.setFont(new Font("Serif", Font.BOLD, 26));
        Random random = new Random();
        for (int i = 0; i < code.length(); i++) {
            g.setColor(new Color(random.nextInt(110), random.nextInt(110), random.nextInt(150)));
            g.drawString(String.valueOf(code.charAt(i)), 12 + i * 26 + random.nextInt(4), 26 + random.nextInt(8));
        }
        for (int i = 0; i < 5; i++) {
            g.setColor(new Color(160 + random.nextInt(80), 160 + random.nextInt(80), 160 + random.nextInt(80)));
            g.drawLine(random.nextInt(width), random.nextInt(height), random.nextInt(width), random.nextInt(height));
        }
        g.dispose();
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", bos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bos.toByteArray());
        } catch (IOException e) {
            throw new BusinessException(Result.FAIL, "验证码生成失败");
        }
    }

    /**
     * 用户登出（JWT 模式下由前端清除 Token 即可，后端提供空实现）
     */
    @PostMapping("/logout")
    public Result logout() {
        return Result.success("已登出");
    }

    /**
     * 获取当前登录用户信息
     * <p>
     * Gateway 在 JWT 认证通过后，会将 userId、username、role 通过 Header 传递下来。
     * 此接口从 Header 中读取这些信息并返回给前端。
     */
    @GetMapping("/current-user")
    public Result getCurrentUser(HttpServletRequest request) {
        String userId = request.getHeader(AuthConstants.HEADER_USER_ID);
        String username = request.getHeader(AuthConstants.HEADER_USERNAME);
        String role = request.getHeader(AuthConstants.HEADER_ROLE);

        // 如果 Header 中没有用户信息（可能是直连访问而非通过网关），
        // 尝试从 JWT Token 中解析
        if (userId == null || userId.isEmpty()) {
            String authHeader = request.getHeader(AuthConstants.HEADER_AUTHORIZATION);
            String token = JwtUtil.extractToken(authHeader);
            if (token != null && !token.isEmpty()) {
                var payload = JwtUtil.parseToken(token);
                if (payload != null) {
                    userId = String.valueOf(payload.getUserId());
                    username = payload.getUsername();
                    role = payload.getRole();
                }
            }
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", userId != null ? Long.valueOf(userId) : null);
        userInfo.put("username", username);
        userInfo.put("role", role);

        log.debug("getCurrentUser: userId={}, username={}, role={}", userId, username, role);
        return Result.success(userInfo);
    }
}
