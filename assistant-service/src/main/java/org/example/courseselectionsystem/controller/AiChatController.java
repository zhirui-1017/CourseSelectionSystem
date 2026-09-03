package org.example.courseselectionsystem.controller;

import dev.langchain4j.service.TokenStream;
import org.example.courseselectionsystem.ai.CourseAssistant;
import org.example.courseselectionsystem.auth.AuthConstants;
import org.example.courseselectionsystem.common.Result;
import org.example.courseselectionsystem.entity.ChatMessage;
import org.example.courseselectionsystem.entity.ChatSession;
import org.example.courseselectionsystem.service.ChatSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * AI 智能助手对话控制器（微服务版）
 * - 身份来源：Gateway JWT 认证后注入的 Header（X-User-Id / X-Role / X-Username）
 * - 支持：会话管理、消息持久化、SSE 流式对话
 * - 路由：Gateway 将 /api/v1/ai/** 转发到 assistant-service
 */
@RestController
@RequestMapping("/api/v1/ai")
public class AiChatController {

    private static final Logger log = LoggerFactory.getLogger(AiChatController.class);

    private final CourseAssistant courseAssistant;
    private final Executor sseExecutor;
    private final ChatSessionService chatSessionService;

    public AiChatController(CourseAssistant courseAssistant,
                            @Qualifier("sseExecutor") Executor sseExecutor,
                            ChatSessionService chatSessionService) {
        this.courseAssistant = courseAssistant;
        this.sseExecutor = sseExecutor;
        this.chatSessionService = chatSessionService;
    }

    private record UserIdentity(Long userId, String username, String role) {}

    private UserIdentity resolveIdentity(HttpServletRequest request) {
        String userId = request.getHeader(AuthConstants.HEADER_USER_ID);
        String role = request.getHeader(AuthConstants.HEADER_ROLE);
        String username = request.getHeader(AuthConstants.HEADER_USERNAME);
        if (userId == null || userId.isEmpty() || role == null || role.isEmpty()) {
            return null;
        }
        return new UserIdentity(Long.valueOf(userId), username, role);
    }

    // ============================================================
    //   会话管理 API
    // ============================================================

    @GetMapping("/sessions")
    public Result<List<ChatSession>> listSessions(HttpServletRequest request) {
        UserIdentity identity = resolveIdentity(request);
        if (identity == null) return Result.error(401, "请先登录");
        return Result.success(chatSessionService.getUserSessions(identity.userId(), identity.role()));
    }

    @PostMapping("/sessions/new")
    public Result<ChatSession> createSession(HttpServletRequest request) {
        UserIdentity identity = resolveIdentity(request);
        if (identity == null) return Result.error(401, "请先登录");
        return Result.success(chatSessionService.createSession(identity.userId(), identity.role()));
    }

    @DeleteMapping("/sessions/{sessionUid}")
    public Result<Void> deleteSession(@PathVariable String sessionUid, HttpServletRequest request) {
        UserIdentity identity = resolveIdentity(request);
        if (identity == null) return Result.error(401, "请先登录");
        ChatSession chatSession = chatSessionService.getBySessionUid(sessionUid);
        if (chatSession == null || !chatSession.getUserId().equals(identity.userId())) {
            return Result.error(403, "无权操作该会话");
        }
        chatSessionService.deleteSession(sessionUid);
        return Result.success("会话已删除", null);
    }

    @GetMapping("/sessions/{sessionUid}/messages")
    public Result<List<ChatMessage>> getSessionMessages(@PathVariable String sessionUid, HttpServletRequest request) {
        UserIdentity identity = resolveIdentity(request);
        if (identity == null) return Result.error(401, "请先登录");
        ChatSession chatSession = chatSessionService.getBySessionUid(sessionUid);
        if (chatSession == null) return Result.error(404, "会话不存在");
        return Result.success(chatSessionService.getSessionMessages(chatSession.getId()));
    }

    @DeleteMapping("/sessions/{sessionUid}/messages")
    public Result<Void> clearSessionMessages(@PathVariable String sessionUid, HttpServletRequest request) {
        UserIdentity identity = resolveIdentity(request);
        if (identity == null) return Result.error(401, "请先登录");
        ChatSession chatSession = chatSessionService.getBySessionUid(sessionUid);
        if (chatSession == null) return Result.error(404, "会话不存在");
        chatSessionService.clearSessionMessages(chatSession.getId());
        return Result.success("会话消息已清空", null);
    }

    // ============================================================
    //   对话 API
    // ============================================================

    @PostMapping("/chat")
    public Result<Map<String, String>> chat(@RequestBody Map<String, String> request, HttpServletRequest servletRequest) {
        String message = request.get("message");
        String sessionUid = request.get("sessionId");
        if (message == null || message.trim().isEmpty()) return Result.error(400, "消息不能为空");

        UserIdentity identity = resolveIdentity(servletRequest);
        if (identity == null) return Result.error(401, "请先登录");

        ChatSession chatSession = getOrCreateSession(identity, sessionUid);
        String memoryId = buildMemoryId(identity, chatSession.getSessionUid());
        String identityInfo = buildIdentityInfo(identity);
        String answer;
        try {
            answer = switch (identity.role()) {
                case "student" -> courseAssistant.chatStudent(memoryId, message, identityInfo);
                case "teacher" -> courseAssistant.chatTeacher(memoryId, message, identityInfo);
                case "admin" -> courseAssistant.chatAdmin(memoryId, message, identityInfo);
                default -> "暂不支持该角色使用 AI 助手。";
            };
        } catch (Exception e) {
            log.error("AI chat error", e);
            return Result.error(500, "AI 助手处理出错：" + e.getMessage());
        }
        chatSessionService.saveMessagePair(chatSession.getId(), message, answer);
        Map<String, String> data = new LinkedHashMap<>();
        data.put("role", identity.role());
        data.put("answer", answer);
        data.put("sessionId", chatSession.getSessionUid());
        return Result.success(data);
    }

    @PostMapping(value = "/chat/stream", produces = "text/event-stream")
    public SseEmitter chatStream(@RequestBody Map<String, String> request, HttpServletRequest servletRequest) {
        SseEmitter emitter = new SseEmitter(300000L);
        String message = request.get("message");
        String sessionUid = request.get("sessionId");
        if (message == null || message.trim().isEmpty()) {
            sendErrorAndComplete(emitter, "消息不能为空");
            return emitter;
        }
        UserIdentity identity = resolveIdentity(servletRequest);
        if (identity == null) {
            sendTokenAndComplete(emitter, "请先登录后再使用 AI 助手。");
            return emitter;
        }

        ChatSession chatSession = getOrCreateSession(identity, sessionUid);
        String memoryId = buildMemoryId(identity, chatSession.getSessionUid());
        String identityInfo = buildIdentityInfo(identity);

        chatSessionService.saveMessage(chatSession.getId(), "user", message);

        final StringBuilder fullResponse = new StringBuilder();
        final Long sessionId = chatSession.getId();
        final String chatSessionUid = chatSession.getSessionUid();

        CompletableFuture.runAsync(() -> {
            try {
                TokenStream tokenStream = switch (identity.role()) {
                    case "student" -> courseAssistant.chatStudentStream(memoryId, message, identityInfo);
                    case "teacher" -> courseAssistant.chatTeacherStream(memoryId, message, identityInfo);
                    case "admin" -> courseAssistant.chatAdminStream(memoryId, message, identityInfo);
                    default -> throw new IllegalStateException("未知角色: " + identity.role());
                };
                tokenStream
                        .onPartialResponse(token -> {
                            fullResponse.append(token);
                            try {
                                emitter.send(SseEmitter.event().data(Map.of("token", token)));
                            } catch (Exception e) {
                                log.error("SSE send token error", e);
                            }
                        })
                        .onCompleteResponse(res -> {
                            try {
                                String aiAnswer = fullResponse.toString();
                                chatSessionService.saveMessage(sessionId, "assistant", aiAnswer);
                                updateSessionTitleIfNeeded(sessionId, message);
                                Map<String, Object> done = new LinkedHashMap<>();
                                done.put("done", true);
                                done.put("sessionId", chatSessionUid);
                                emitter.send(SseEmitter.event().data(done));
                                emitter.complete();
                            } catch (Exception e) {
                                log.error("SSE complete error", e);
                            }
                        })
                        .onError(error -> {
                            log.error("TokenStream error", error);
                            try {
                                String errorText = "\n\n> ⚠️ AI 助手处理出错，请稍后重试。";
                                chatSessionService.saveMessage(sessionId, "assistant", errorText);
                                Map<String, Object> d = new LinkedHashMap<>();
                                d.put("token", errorText);
                                emitter.send(SseEmitter.event().data(d));
                                d.clear();
                                d.put("done", true);
                                d.put("sessionId", chatSessionUid);
                                emitter.send(SseEmitter.event().data(d));
                                emitter.complete();
                            } catch (Exception e) {
                                emitter.completeWithError(e);
                            }
                        })
                        .start();
            } catch (Exception e) {
                log.error("AI stream error", e);
                try {
                    String errorText = "\n\n> ⚠️ AI 服务暂时不可用：" + e.getMessage();
                    chatSessionService.saveMessage(sessionId, "assistant", errorText);
                    Map<String, Object> d = new LinkedHashMap<>();
                    d.put("token", errorText);
                    emitter.send(SseEmitter.event().data(d));
                    d.clear();
                    d.put("done", true);
                    d.put("sessionId", chatSessionUid);
                    emitter.send(SseEmitter.event().data(d));
                    emitter.complete();
                } catch (Exception ex) {
                    emitter.completeWithError(ex);
                }
            }
        }, sseExecutor);

        emitter.onTimeout(() -> log.warn("SSE connection timed out after 5 min"));
        emitter.onError(throwable -> log.error("SSE connection error", throwable));
        emitter.onCompletion(() -> log.info("SSE connection completed"));
        return emitter;
    }

    @GetMapping("/health")
    public Result<Map<String, String>> health() {
        return Result.success(Map.of("status", "ok", "service", "assistant-service"));
    }

    // ============================================================
    //   内部辅助方法
    // ============================================================

    private ChatSession getOrCreateSession(UserIdentity identity, String sessionUid) {
        if (sessionUid != null && !sessionUid.isEmpty()) {
            ChatSession existing = chatSessionService.getBySessionUid(sessionUid);
            if (existing != null) return existing;
        }
        return chatSessionService.createSession(identity.userId(), identity.role());
    }

    private String buildMemoryId(UserIdentity identity, String sessionUid) {
        return identity.userId() + "_" + identity.role() + "_" + sessionUid.substring(0, Math.min(sessionUid.length(), 12));
    }

    private String buildIdentityInfo(UserIdentity identity) {
        return "当前用户ID：" + identity.userId() + "；用户名：" + (identity.username() == null ? "" : identity.username())
                + "；角色：" + identity.role();
    }

    private void updateSessionTitleIfNeeded(Long sessionId, String userMessage) {
        try {
            int count = chatSessionService.getMessageCount(sessionId);
            if (count <= 2) {
                ChatSession s = chatSessionService.getById(sessionId);
                if (s != null && "新对话".equals(s.getTitle())) {
                    String title = userMessage.length() > 30 ? userMessage.substring(0, 30) + "..." : userMessage;
                    s.setTitle(title);
                    s.setMessageCount(count);
                    s.setUpdatedAt(java.time.LocalDateTime.now());
                    chatSessionService.updateById(s);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to update session title", e);
        }
    }

    private static void sendTokenAndComplete(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().data(Map.of("token", message)));
            emitter.send(SseEmitter.event().data(Map.of("done", true)));
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }

    private static void sendErrorAndComplete(SseEmitter emitter, String error) {
        try {
            emitter.send(SseEmitter.event().data(Map.of("error", error)));
            emitter.send(SseEmitter.event().data(Map.of("done", true)));
            emitter.complete();
        } catch (Exception ex) {
            emitter.completeWithError(ex);
        }
    }
}
