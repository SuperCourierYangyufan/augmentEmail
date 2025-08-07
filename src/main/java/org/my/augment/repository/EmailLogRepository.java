package org.my.augment.repository;

import org.my.augment.entity.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 邮件日志数据访问层
 * 
 * @author 杨宇帆
 * @create 2025-07-25
 */
@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

    /**
     * 根据认证密钥查找日志
     * 
     * @param authKey 认证密钥
     * @return 日志列表
     */
    List<EmailLog> findByAuthKeyOrderByCreateTimeDesc(String authKey);

    /**
     * 根据认证密钥和时间范围查找日志
     * 
     * @param authKey 认证密钥
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 日志列表
     */
    List<EmailLog> findByAuthKeyAndCreateTimeBetweenOrderByCreateTimeDesc(
            String authKey, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计指定认证密钥的操作次数
     * 
     * @param authKey 认证密钥
     * @param operationType 操作类型
     * @return 操作次数
     */
    @Query("SELECT COUNT(e) FROM EmailLog e WHERE e.authKey = :authKey " +
           "AND e.operationType = :operationType")
    long countByAuthKeyAndOperationType(@Param("authKey") String authKey, 
                                       @Param("operationType") EmailLog.OperationType operationType);

    /**
     * 统计指定时间范围内的操作次数
     * 
     * @param authKey 认证密钥
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 操作次数
     */
    @Query("SELECT COUNT(e) FROM EmailLog e WHERE e.authKey = :authKey " +
           "AND e.createTime BETWEEN :startTime AND :endTime")
    long countByAuthKeyAndTimeRange(@Param("authKey") String authKey,
                                   @Param("startTime") LocalDateTime startTime,
                                   @Param("endTime") LocalDateTime endTime);

    /**
     * 查找最近的操作记录
     * 
     * @param authKey 认证密钥
     * @param limit 限制数量
     * @return 最近的操作记录
     */
    @Query("SELECT e FROM EmailLog e WHERE e.authKey = :authKey " +
           "ORDER BY e.createTime DESC")
    List<EmailLog> findRecentLogs(@Param("authKey") String authKey, 
                                 org.springframework.data.domain.Pageable pageable);
}
