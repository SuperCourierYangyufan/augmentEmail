package org.my.augment.repository;

import org.my.augment.entity.GoogleInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Google邀请数据访问层
 *
 * @author 杨宇帆
 * @create 2025-08-20
 */
@Repository
public interface GoogleInviteRepository extends JpaRepository<GoogleInvite, Long> {

    /**
     * 根据邀请码查找记录
     *
     * @param inviteCode 邀请码
     * @return 邀请记录
     */
    Optional<GoogleInvite> findByInviteCode(String inviteCode);

    /**
     * 检查邀请码是否存在
     *
     * @param inviteCode 邀请码
     * @return 是否存在
     */
    boolean existsByInviteCode(String inviteCode);

    /**
     * 获取所有邀请记录，按状态和填充时间排序
     * 排序规则：待邀请(SUBMITTED)在前，待填写(PENDING)次之，已邀请/已取消在后
     *          同状态下按填充时间升序（先填写的在前），无填充时间的按创建时间降序
     *
     * @return 排序后的邀请列表
     */
    @Query("SELECT g FROM GoogleInvite g ORDER BY " +
           "CASE WHEN g.status = 'SUBMITTED' THEN 0 " +
           "     WHEN g.status = 'PENDING' THEN 1 " +
           "     WHEN g.status = 'INVITED' THEN 2 " +
           "     ELSE 3 END ASC, " +
           "CASE WHEN g.fillTime IS NULL THEN 1 ELSE 0 END ASC, " +
           "g.fillTime ASC, " +
           "g.createTime DESC")
    List<GoogleInvite> findAllOrderByStatusAndFillTime();

    /**
     * 统计指定状态的数量
     *
     * @param status 邀请状态
     * @return 数量
     */
    long countByStatus(GoogleInvite.InviteStatus status);

    /**
     * 检查邮箱是否已被使用
     *
     * @param emailAddress 邮箱地址
     * @return 是否存在
     */
    boolean existsByEmailAddress(String emailAddress);

    /**
     * 根据邮箱地址查找记录
     *
     * @param emailAddress 邮箱地址
     * @return 邀请记录
     */
    Optional<GoogleInvite> findByEmailAddress(String emailAddress);
}
