package com.osys.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.osys.audit.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审计日志Mapper
 */
@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {

    /**
     * 根据用户ID查询
     */
    @Select("SELECT * FROM audit_logs WHERE user_id = #{userId} AND created_at BETWEEN #{start} AND #{end} ORDER BY created_at DESC")
    List<AuditLog> selectByUserIdAndTime(@Param("userId") Long userId, 
                                         @Param("start") LocalDateTime start, 
                                         @Param("end") LocalDateTime end);

    /**
     * 根据用户ID分页查询
     */
    IPage<AuditLog> selectPageByUserId(Page<AuditLog> page, 
                                       @Param("userId") Long userId, 
                                       @Param("start") LocalDateTime start, 
                                       @Param("end") LocalDateTime end);

    /**
     * 根据对象查询
     */
    @Select("SELECT * FROM audit_logs WHERE target_type = #{targetType} AND target_id = #{targetId} AND created_at BETWEEN #{start} AND #{end} ORDER BY created_at DESC")
    List<AuditLog> selectByTarget(@Param("targetType") String targetType, 
                                  @Param("targetId") String targetId, 
                                  @Param("start") LocalDateTime start, 
                                  @Param("end") LocalDateTime end);

    /**
     * 查询风险日志
     */
    @Select("SELECT * FROM audit_logs WHERE risk_level > 0 AND created_at BETWEEN #{start} AND #{end} ORDER BY created_at DESC")
    List<AuditLog> selectRiskLogs(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 统计总数
     */
    @Select("SELECT COUNT(*) FROM audit_logs WHERE created_at BETWEEN #{start} AND #{end}")
    Long countByTime(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 统计风险数
     */
    @Select("SELECT COUNT(*) FROM audit_logs WHERE risk_level > 0 AND created_at BETWEEN #{start} AND #{end}")
    Long countRiskByTime(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 查询最近的分区数据
     */
    @Select("SELECT * FROM audit_logs WHERE partition_key = #{partitionKey} ORDER BY id DESC LIMIT #{limit}")
    List<AuditLog> selectRecentByPartition(@Param("partitionKey") Integer partitionKey, @Param("limit") Integer limit);
}
