package com.osys.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.osys.audit.entity.RiskEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 风险事件Mapper
 */
@Mapper
public interface RiskEventMapper extends BaseMapper<RiskEvent> {

    /**
     * 根据处理状态查询
     */
    @Select("SELECT * FROM risk_events WHERE handle_status = #{handleStatus} AND created_at BETWEEN #{start} AND #{end} ORDER BY created_at DESC")
    List<RiskEvent> selectByHandleStatusAndTime(@Param("handleStatus") Integer handleStatus, 
                                                @Param("start") LocalDateTime start, 
                                                @Param("end") LocalDateTime end);

    /**
     * 查询高等级未处理风险
     */
    @Select("SELECT * FROM risk_events WHERE risk_level >= #{riskLevel} AND handle_status = #{handleStatus} ORDER BY created_at DESC")
    List<RiskEvent> selectByRiskLevelAndHandleStatus(@Param("riskLevel") Integer riskLevel, @Param("handleStatus") Integer handleStatus);

    /**
     * 统计待处理风险数
     */
    @Select("SELECT COUNT(*) FROM risk_events WHERE handle_status = #{handleStatus}")
    Long countByHandleStatus(Integer handleStatus);

    /**
     * 统计风险数
     */
    @Select("SELECT COUNT(*) FROM risk_events WHERE risk_level >= #{riskLevel} AND created_at BETWEEN #{start} AND #{end}")
    Long countRiskByTime(@Param("riskLevel") Integer riskLevel, 
                         @Param("start") LocalDateTime start, 
                         @Param("end") LocalDateTime end);
}
