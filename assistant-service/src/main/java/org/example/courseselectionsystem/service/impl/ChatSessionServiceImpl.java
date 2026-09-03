package org.example.courseselectionsystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.courseselectionsystem.entity.ChatMessage;
import org.example.courseselectionsystem.entity.ChatSession;
import org.example.courseselectionsystem.mapper.ChatMessageMapper;
import org.example.courseselectionsystem.mapper.ChatSessionMapper;
import org.example.courseselectionsystem.service.ChatSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * AI 助手聊天会话服务实现
 */
@Service
public class ChatSessionServiceImpl extends ServiceImpl<ChatSessionMapper, ChatSession> implements ChatSessionService {

    private static final Logger log = LoggerFactory.getLogger(ChatSessionServiceImpl.class);

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Override
    public List<ChatSession> getUserSessions(Long userId, String userRole) {
        return getBaseMapper().selectByUserIdAndRole(userId, userRole);
    }

    @Override
    @Transactional
    public ChatSession createSession(Long userId, String userRole) {
        ChatSession session = new ChatSession();
        session.setSessionUid(UUID.randomUUID().toString().replace("-", ""));
        session.setUserId(userId);
        session.setUserRole(userRole);
        session.setTitle("新对话");
        session.setMessageCount(0);
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        save(session);
        log.info("Created new chat session: uid={}, userId={}, role={}", session.getSessionUid(), userId, userRole);
        return session;
    }

    @Override
    public ChatSession getBySessionUid(String sessionUid) {
        return getBaseMapper().selectBySessionUid(sessionUid);
    }

    @Override
    @Transactional
    public void deleteSession(String sessionUid) {
        ChatSession session = getBaseMapper().selectBySessionUid(sessionUid);
        if (session != null) {
            chatMessageMapper.deleteBySessionId(session.getId());
            removeById(session.getId());
            log.info("Deleted chat session: uid={}", sessionUid);
        }
    }

    @Override
    @Transactional
    public void clearSessionMessages(Long sessionId) {
        chatMessageMapper.deleteBySessionId(sessionId);
        // 更新会话消息计数
        ChatSession session = getById(sessionId);
        if (session != null) {
            session.setMessageCount(0);
            session.setUpdatedAt(LocalDateTime.now());
            updateById(session);
        }
        log.info("Cleared messages for session: id={}", sessionId);
    }

    @Override
    public ChatMessage saveMessage(Long sessionId, String role, String content) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        chatMessageMapper.insert(message);
        return message;
    }

    @Override
    public List<ChatMessage> getSessionMessages(Long sessionId) {
        return chatMessageMapper.selectBySessionId(sessionId);
    }

    @Override
    @Transactional
    public void saveMessagePair(Long sessionId, String userMessage, String assistantMessage) {
        saveMessage(sessionId, "user", userMessage);
        saveMessage(sessionId, "assistant", assistantMessage);
        
        // 更新会话标题和消息计数
        ChatSession session = getById(sessionId);
        if (session != null) {
            int count = chatMessageMapper.countBySessionId(sessionId);
            session.setMessageCount(count);
            session.setUpdatedAt(LocalDateTime.now());
            
            // 如果是第一条消息，用用户消息的前30个字符作为标题
            if ("新对话".equals(session.getTitle()) || count <= 2) {
                String title = userMessage.length() > 30 
                    ? userMessage.substring(0, 30) + "..." 
                    : userMessage;
                session.setTitle(title);
            }
            
            updateById(session);
        }
    }

    @Override
    public int getMessageCount(Long sessionId) {
        Integer count = chatMessageMapper.countBySessionId(sessionId);
        return count != null ? count : 0;
    }
}
