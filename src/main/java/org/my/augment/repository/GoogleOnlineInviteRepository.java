package org.my.augment.repository;

import org.my.augment.entity.GoogleOnlineInvite;
import org.my.augment.entity.GoogleOnlineInvite.OnlineInviteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 谷歌在线邀请数据访问层
 *
 * @author AI Assistant
 * @create 2025-01-19
 */
@Repository
public interface GoogleOnlineInviteRepository extends JpaRepository<GoogleOnlineInvite, Long> {

    /**
     * 根据订单号查询
     *
     * @param orderNumber 订单号
     * @return 邀请记录
     */
    Optional<GoogleOnlineInvite> findByOrderNumber(String orderNumber);

    /**
     * 根据验证地址查询
     *
     * @param verifyAddress 验证地址
     * @return 邀请记录
     */
    Optional<GoogleOnlineInvite> findByVerifyAddress(String verifyAddress);

    /**
     * 检查订单号是否存在
     *
     * @param orderNumber 订单号
     * @return 是否存在
     */
    boolean existsByOrderNumber(String orderNumber);

    /**
     * 根据状态统计数量
     *
     * @param status 状态
     * @return 数量
     */
    long countByStatus(OnlineInviteStatus status);

    /**
     * 获取所有记录，按状态和提交时间排序
     * 排序优先级：待处理 > 待提交 > 已驳回 > 已处理，同状态按提交时间倒序
     *
     * @return 邀请列表
     */
    @Query("SELECT g FROM GoogleOnlineInvite g ORDER BY " +
            "CASE g.status " +
            "WHEN 'SUBMITTED' THEN 1 " +
            "WHEN 'PENDING' THEN 2 " +
            "WHEN 'REJECTED' THEN 3 " +
            "WHEN 'PROCESSED' THEN 4 " +
            "END, " +
            "g.submitTime DESC NULLS LAST, g.createTime DESC")
    List<GoogleOnlineInvite> findAllOrderByStatusAndSubmitTime();

    /**
     * 获取所有记录，按创建时间倒序
     *
     * @return 邀请列表
     */
    List<GoogleOnlineInvite> findAllByOrderByCreateTimeDesc();
}
