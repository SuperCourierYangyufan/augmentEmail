package org.my.augment.repository;

import org.my.augment.entity.AuthKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 认证密钥数据访问层
 * 
 * @author 杨宇帆
 * @create 2025-07-25
 */
@Repository
public interface AuthKeyRepository extends JpaRepository<AuthKey, Long> {

    /**
     * 根据认证密钥查找
     * 
     * @param authKey 认证密钥
     * @return 认证密钥实体
     */
    Optional<AuthKey> findByAuthKey(String authKey);

    /**
     * 检查认证密钥是否存在且有效
     * 
     * @param authKey 认证密钥
     * @return 是否存在且有效
     */
    @Query("SELECT COUNT(a) > 0 FROM AuthKey a WHERE a.authKey = :authKey AND a.status = 1 " +
           "AND (a.expireTime IS NULL OR a.expireTime > :now) " +
           "AND a.usedCount < a.maxCount")
    boolean existsValidAuthKey(@Param("authKey") String authKey, @Param("now") LocalDateTime now);

    /**
     * 更新使用次数和最后使用信息
     * 只有在使用次数未达到上限时才会更新
     *
     * @param authKey 认证密钥
     * @param ip 使用IP
     * @param now 当前时间
     * @return 更新的记录数（0表示未更新，可能是因为达到使用限制）
     */
    @Modifying
    @Query("UPDATE AuthKey a SET a.usedCount = a.usedCount + 1, " +
           "a.lastUsedTime = :now, a.lastUsedIp = :ip " +
           "WHERE a.authKey = :authKey AND a.status = 1 " +
           "AND (a.expireTime IS NULL OR a.expireTime > :now) " +
           "AND a.usedCount < a.maxCount")
    int incrementUsedCount(@Param("authKey") String authKey,
                          @Param("ip") String ip,
                          @Param("now") LocalDateTime now);

    /**
     * 查找有效的认证密钥
     * 
     * @param authKey 认证密钥
     * @param now 当前时间
     * @return 认证密钥实体
     */
    @Query("SELECT a FROM AuthKey a WHERE a.authKey = :authKey AND a.status = 1 " +
           "AND (a.expireTime IS NULL OR a.expireTime > :now) " +
           "AND a.usedCount < a.maxCount")
    Optional<AuthKey> findValidAuthKey(@Param("authKey") String authKey, @Param("now") LocalDateTime now);
}
