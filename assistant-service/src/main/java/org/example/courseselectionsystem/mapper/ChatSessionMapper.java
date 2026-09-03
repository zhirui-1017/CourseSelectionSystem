package org.example.courseselectionsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.courseselectionsystem.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 聊天会话 Mapper
 */
@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {
    
    List<ChatSession> selectByUserIdAndRole(
        @Param("userId") Long userId, 
        @Param("userRole") String userRole);
    
    ChatSession selectBySessionUid(@Param("sessionUid") String sessionUid);
    
    int updateTitle(
        @Param("id") Long id, 
        @Param("title") String title,
        @Param("messageCount") Integer messageCount);
}
