package org.example.courseselectionsystem.controller;

import org.example.courseselectionsystem.auth.UserContext;
import org.example.courseselectionsystem.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI 智能助手控制器
 * <p>
 * 直接通过 HTTP 调用通义千问 API（DashScope 兼容 OpenAI 格式），
 * 支持流式（SSE）对话响应。
 * <p>
 * 方案 C：不依赖 LangChain4j 框架，降低架构复杂度。
 */
@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);

    @Value("${ai.qwen.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;

    @Value("${ai.qwen.api-key:REDACTED_API_KEY}")
    private String apiKey;

    @Value("${ai.qwen.model:qwen-plus}")
    private String model;

    private final ExecutorService sseExecutor = Executors.newFixedThreadPool(4);

    @PostConstruct
    public void init() {
        log.info("AI 助手初始化完成 — baseUrl={}, model={}", baseUrl, model);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Result<Map<String, String>> health() {
        Map<String, String> info = new LinkedHashMap<>();
        info.put("status", "ok");
        info.put("model", model);
        info.put("service", "Qwen AI Assistant");
        return Result.success(info);
    }

    /**
     * SSE 流式对话接口
     * <p>
     * 请求格式：{"message": "你好", "sessionId": "..."}
     * 响应格式：data: {"token": "..."}  ...  data: {"done": true, "sessionId": "..."}
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody Map<String, String> request) {
        SseEmitter emitter = new SseEmitter(300000L); // 5 分钟超时

        String message = request.get("message");
        if (message == null || message.trim().isEmpty()) {
            sendErrorAndComplete(emitter, "消息不能为空");
            return emitter;
        }

        String role = UserContext.getRole();
        String username = UserContext.getUsername();

        log.info("AI SSE stream — role={}, user={}, message={}",
                role, username, message.substring(0, Math.min(message.length(), 50)));

        // 在专用线程池中执行 HTTP 调用
        CompletableFuture.runAsync(() -> {
            try {
                callQwenStreamApi(message, role, username, emitter);
            } catch (Exception e) {
                log.error("AI 流式调用失败", e);
                sendErrorAndComplete(emitter, "AI 服务暂时不可用：" + e.getMessage());
            }
        }, sseExecutor);

        emitter.onTimeout(() -> log.warn("SSE 连接超时"));
        emitter.onError(throwable -> log.error("SSE 连接错误", throwable));
        emitter.onCompletion(() -> log.info("SSE 连接正常完成"));

        return emitter;
    }

    /**
     * 同步对话接口
     */
    @PostMapping("/chat")
    public Result<Map<String, String>> chat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.trim().isEmpty()) {
            return Result.error("消息不能为空");
        }

        String role = UserContext.getRole();
        String username = UserContext.getUsername();

        try {
            String answer = callQwenSyncApi(message, role, username);
            Map<String, String> result = new LinkedHashMap<>();
            result.put("role", role != null ? role : "anonymous");
            result.put("answer", answer);
            return Result.success(result);
        } catch (Exception e) {
            log.error("AI 同步调用失败", e);
            return Result.error("AI 助手处理出错：" + e.getMessage());
        }
    }

    // ============================================================
    // 内部方法
    // ============================================================

    private void callQwenStreamApi(String message, String role, String username, SseEmitter emitter) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(baseUrl + "/chat/completions");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(120000);

            // 构建系统提示词
            String systemPrompt = buildSystemPrompt(role, username);

            // 请求体（启用 stream）
            String requestBody = String.format(
                    "{\"model\":\"%s\",\"messages\":[{\"role\":\"system\",\"content\":\"%s\"},{\"role\":\"user\",\"content\":\"%s\"}],\"stream\":true,\"temperature\":0.7,\"max_tokens\":2000}",
                    model,
                    escapeJson(systemPrompt),
                    escapeJson(message)
            );

            try (OutputStream os = conn.getOutputStream()) {
                os.write(requestBody.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                // 读取错误信息
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder errorBody = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        errorBody.append(line);
                    }
                    log.error("通义千问 API 返回错误 {}: {}", responseCode, errorBody);
                }
                sendErrorAndComplete(emitter, "AI 服务返回错误 " + responseCode);
                return;
            }

            // 读取 SSE 流
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                StringBuilder fullResponse = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6);
                        if ("[DONE]".equals(data)) {
                            // 流结束
                            Map<String, Object> doneData = new LinkedHashMap<>();
                            doneData.put("done", true);
                            emitter.send(SseEmitter.event().data(doneData));
                            break;
                        }
                        // 简单提取 content（不引入 JSON 解析库）
                        String token = extractContent(data);
                        if (token != null && !token.isEmpty()) {
                            fullResponse.append(token);
                            Map<String, Object> tokenData = new LinkedHashMap<>();
                            tokenData.put("token", token);
                            emitter.send(SseEmitter.event().data(tokenData));
                        }
                    }
                }
                log.info("AI 流式响应完成，总长度: {}", fullResponse.length());
            }

            emitter.complete();
        } catch (Exception e) {
            log.error("SSE 流式处理异常", e);
            try {
                sendErrorAndComplete(emitter, "AI 服务暂时不可用");
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String callQwenSyncApi(String message, String role, String username) throws Exception {
        URL url = new URL(baseUrl + "/chat/completions");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(60000);

        String systemPrompt = buildSystemPrompt(role, username);
        String requestBody = String.format(
                "{\"model\":\"%s\",\"messages\":[{\"role\":\"system\",\"content\":\"%s\"},{\"role\":\"user\",\"content\":\"%s\"}],\"stream\":false,\"temperature\":0.7,\"max_tokens\":2000}",
                model,
                escapeJson(systemPrompt),
                escapeJson(message)
        );

        try (OutputStream os = conn.getOutputStream()) {
            os.write(requestBody.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new RuntimeException("API 返回错误: " + responseCode);
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        // 简单解析 JSON（提取 content 字段）
        String json = response.toString();
        int contentIdx = json.indexOf("\"content\":\"");
        if (contentIdx < 0) {
            return "AI 返回格式异常";
        }
        int start = contentIdx + 11;
        int end = json.indexOf("\"", start);
        if (end < 0) {
            return json.substring(start);
        }
        return json.substring(start, end)
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private String buildSystemPrompt(String role, String username) {
        if (role == null) role = "anonymous";
        String userName = username != null ? username : "用户";
        return switch (role) {
            case "student" -> String.format(
                    "你是课程小助手，专门帮助学生解答课程相关的问题。你的回答应该友好、专业、有教育性。" +
                    "当前用户：%s（学生）。请针对学生的需求提供帮助，如选课建议、学习方法、课程安排等。", userName);
            case "teacher" -> String.format(
                    "你是教学辅助助手，帮助教师进行教学管理。你的回答应该专业、实用。" +
                    "当前用户：%s（教师）。请针对教师的需求提供帮助，如课程设计、学生管理、成绩分析等。", userName);
            case "admin" -> String.format(
                    "你是运营分析助手，帮助管理员进行系统运营分析。你的回答应该专业、全面。" +
                    "当前用户：%s（管理员）。请针对管理员的需求提供帮助，如系统统计、异常分析、运营建议等。", userName);
            default -> "你是课程小助手，帮助用户解答课程相关的问题。请友好、专业地提供帮助。";
        };
    }

    private String extractContent(String jsonLine) {
        // 简单解析 SSE 响应中的 content 字段
        // 格式: {"choices":[{"delta":{"content":"xxx"}}]}
        int contentIdx = jsonLine.indexOf("\"content\":\"");
        if (contentIdx < 0) return null;
        int start = contentIdx + 11;
        int end = jsonLine.indexOf("\"", start);
        if (end < 0) return null;
        return jsonLine.substring(start, end)
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private void sendErrorAndComplete(SseEmitter emitter, String error) {
        try {
            Map<String, Object> errData = new LinkedHashMap<>();
            errData.put("token", error);
            emitter.send(SseEmitter.event().data(errData));
            Map<String, Object> doneData = new LinkedHashMap<>();
            doneData.put("done", true);
            emitter.send(SseEmitter.event().data(doneData));
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }
}
