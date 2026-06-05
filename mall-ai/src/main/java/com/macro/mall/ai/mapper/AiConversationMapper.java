package com.macro.mall.ai.mapper;

import com.macro.mall.ai.model.AiConversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AiConversationMapper {

    int insert(AiConversation conv);

    AiConversation selectByConvId(@Param("convId") String convId);

    List<AiConversation> selectByUserId(@Param("userId") Long userId,
                                        @Param("limit") int limit,
                                        @Param("offset") int offset);

    int updateLastActive(@Param("convId") String convId,
                         @Param("lastActiveAt") LocalDateTime lastActiveAt,
                         @Param("messageCount") int messageCount);

    int updateTitle(@Param("convId") String convId, @Param("title") String title);

    int softDelete(@Param("convId") String convId, @Param("userId") Long userId);

    /** 将匿名对话归属到真实用户 */
    int claimConversations(@Param("anonymousUserId") long anonymousUserId,
                           @Param("realUserId") long realUserId);

    /** 查询匿名对话 */
    List<AiConversation> selectAnonymous(@Param("limit") int limit, @Param("offset") int offset);
}
