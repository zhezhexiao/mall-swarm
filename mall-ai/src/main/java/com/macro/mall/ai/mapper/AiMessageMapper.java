package com.macro.mall.ai.mapper;

import com.macro.mall.ai.model.AiMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiMessageMapper {

    int insertBatch(@Param("messages") List<AiMessage> messages);

    List<AiMessage> selectByConvId(@Param("convId") String convId);

    int countByConvId(@Param("convId") String convId);
}
