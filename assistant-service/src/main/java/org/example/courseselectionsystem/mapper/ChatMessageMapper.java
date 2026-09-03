package org.example.courseselectionsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.courseselectionsystem.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 聊天消息 Mapper
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
    
    List<ChatMessage> selectBySessionId(@Param("sessionId") Long sessionId);
    
    int deleteBySessionId(@Param("sessionId") Long sessionId);
    
    int countBySessionId(@Param("sessionId") Long sessionId);
}
