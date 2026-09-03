package org.example.courseselectionsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.courseselectionsystem.entity.ChatMessage;
import org.example.courseselectionsystem.entity.ChatSession;

import java.util.List;
import java.util.Map;

/**
 * AI 助手聊天会话服务接口
 */
public interface ChatSessionService extends IService<ChatSession> {
    
    /**
     * 获取用户的所有会话列表
     */
    List<ChatSession> getUserSessions(Long userId, String userRole);
    
    /**
     * 创建新会话
     */
    ChatSession createSession(Long userId, String userRole);
    
    /**
     * 根据 sessionUid 获取会话
     */
    ChatSession getBySessionUid(String sessionUid);
    
    /**
     * 删除会话及其所有消息
     */
    void deleteSession(String sessionUid);
    
    /**
     * 清空会话中的所有消息（保留会话）
     */
    void clearSessionMessages(Long sessionId);
    
    /**
     * 保存消息到会话
     */
    ChatMessage saveMessage(Long sessionId, String role, String content);
    
    /**
     * 获取会话的所有消息
     */
    List<ChatMessage> getSessionMessages(Long sessionId);
    
    /**
     * 保存一对问答消息（用户消息 + AI 回复），同时更新会话标题
     */
    void saveMessagePair(Long sessionId, String userMessage, String assistantMessage);
    
    /**
     * 获取消息数量
     */
    int getMessageCount(Long sessionId);
}
