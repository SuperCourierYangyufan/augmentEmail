package org.my.augment.repository;

import org.my.augment.entity.TempEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 临时邮箱数据访问层
 * 提供邮箱管理相关的数据库操作方法
 * 通过SQL查询实现过期状态判断，无需定时任务
 * 
 * @author 杨宇帆
 * @create 2025-08-02
 */
@Repository
public interface TempEmailRepository extends JpaRepository<TempEmail, Long> {

    /**
     * 根据邮箱地址查找邮箱记录
     *
     * @param emailAddress 邮箱地址
     * @return 邮箱记录
     */
    Optional<TempEmail> findByEmailAddress(String emailAddress);

    /**
     * 根据邮箱地址和授权key查找邮箱记录
     *
     * @param emailAddress 邮箱地址
     * @param authKey 授权密钥
     * @return 邮箱记录
     */
    Optional<TempEmail> findByEmailAddressAndAuthKey(String emailAddress, String authKey);

    /**
     * 查询所有邮箱（包括封禁的）
     * 按创建时间降序排序（新的在前面）
     *
     * @return 邮箱列表，按创建时间降序排列
     */
    @Query("SELECT t FROM TempEmail t ORDER BY t.createTime DESC")
    List<TempEmail> findAllEmailsOrderByCreateTimeDesc();

    /**
     * 查询指定授权key的所有邮箱（包括封禁的）
     * 按创建时间降序排序（新的在前面）
     *
     * @param authKey 授权密钥
     * @return 邮箱列表，按创建时间降序排列
     */
    @Query("SELECT t FROM TempEmail t WHERE t.authKey = :authKey ORDER BY t.createTime DESC")
    List<TempEmail> findAllEmailsByAuthKeyOrderByCreateTimeDesc(@Param("authKey") String authKey);

    /**
     * 查询指定状态的邮箱
     *
     * @param status 邮箱状态
     * @return 指定状态的邮箱列表
     */
    List<TempEmail> findByStatusOrderByCreateTimeDesc(TempEmail.EmailStatus status);

    /**
     * 查询指定授权key和状态的邮箱
     *
     * @param authKey 授权密钥
     * @param status 邮箱状态
     * @return 指定状态的邮箱列表
     */
    List<TempEmail> findByAuthKeyAndStatusOrderByCreateTimeDesc(String authKey, TempEmail.EmailStatus status);

    /**
     * 统计有效邮箱数量（未封禁）
     *
     * @return 有效邮箱数量
     */
    @Query("SELECT COUNT(t) FROM TempEmail t WHERE t.status = 'ACTIVE'")
    long countActiveEmails();

    /**
     * 统计指定授权key的有效邮箱数量（未封禁）
     *
     * @param authKey 授权密钥
     * @return 有效邮箱数量
     */
    @Query("SELECT COUNT(t) FROM TempEmail t WHERE t.authKey = :authKey AND t.status = 'ACTIVE'")
    long countActiveEmailsByAuthKey(@Param("authKey") String authKey);

    /**
     * 统计已封禁邮箱数量
     *
     * @return 已封禁邮箱数量
     */
    long countByStatus(TempEmail.EmailStatus status);

    /**
     * 统计指定授权key的已封禁邮箱数量
     *
     * @param authKey 授权密钥
     * @param status 邮箱状态
     * @return 已封禁邮箱数量
     */
    long countByAuthKeyAndStatus(String authKey, TempEmail.EmailStatus status);

    /**
     * 更新邮箱状态
     * 
     * @param id 邮箱ID
     * @param status 新状态
     * @return 更新的记录数
     */
    @Modifying
    @Transactional
    @Query("UPDATE TempEmail t SET t.status = :status WHERE t.id = :id")
    int updateEmailStatus(@Param("id") Long id, @Param("status") TempEmail.EmailStatus status);





    /**
     * 根据创建时间范围查询邮箱
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 指定时间范围内创建的邮箱列表
     */
    @Query("SELECT t FROM TempEmail t WHERE t.createTime BETWEEN :startTime AND :endTime " +
           "ORDER BY t.createTime DESC")
    List<TempEmail> findByCreateTimeBetween(@Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);

    /**
     * 根据授权key和创建时间范围查询邮箱
     *
     * @param authKey 授权密钥
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 指定时间范围内创建的邮箱列表
     */
    @Query("SELECT t FROM TempEmail t WHERE t.authKey = :authKey AND t.createTime BETWEEN :startTime AND :endTime " +
           "ORDER BY t.createTime DESC")
    List<TempEmail> findByAuthKeyAndCreateTimeBetween(@Param("authKey") String authKey,
                                                     @Param("startTime") LocalDateTime startTime,
                                                     @Param("endTime") LocalDateTime endTime);

    /**
     * 检查邮箱地址是否存在且有效
     *
     * @param emailAddress 邮箱地址
     * @return true如果邮箱存在且有效，false否则
     */
    @Query("SELECT COUNT(t) > 0 FROM TempEmail t WHERE t.emailAddress = :emailAddress " +
           "AND t.status = 'ACTIVE'")
    boolean existsActiveEmail(@Param("emailAddress") String emailAddress);

    /**
     * 检查指定授权key的邮箱地址是否存在且有效
     *
     * @param emailAddress 邮箱地址
     * @param authKey 授权密钥
     * @return true如果邮箱存在且有效，false否则
     */
    @Query("SELECT COUNT(t) > 0 FROM TempEmail t WHERE t.emailAddress = :emailAddress AND t.authKey = :authKey " +
           "AND t.status = 'ACTIVE'")
    boolean existsActiveEmailByAuthKey(@Param("emailAddress") String emailAddress,
                                      @Param("authKey") String authKey);
}
