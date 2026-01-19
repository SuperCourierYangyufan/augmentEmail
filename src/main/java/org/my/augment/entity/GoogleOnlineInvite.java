package org.my.augment.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 谷歌在线邀请实体类
 * 用于管理谷歌在线邀请申请记录（订单号+验证地址）
 *
 * @author AI Assistant
 * @create 2025-01-19
 */
@Entity
@Table(name = "google_online_invites")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoogleOnlineInvite {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 订单号（用户填写，唯一标识）
     */
    @Column(name = "order_number", unique = true, nullable = false, length = 100)
    private String orderNumber;

    /**
     * 验证地址（URL格式）
     */
    @Column(name = "verify_address", length = 500)
    private String verifyAddress;

    /**
     * 邀请状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @lombok.Builder.Default
    private OnlineInviteStatus status = OnlineInviteStatus.PENDING;

    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    /**
     * 提交时间
     */
    @Column(name = "submit_time")
    private LocalDateTime submitTime;

    /**
     * 处理时间
     */
    @Column(name = "process_time")
    private LocalDateTime processTime;

    /**
     * 备注信息（管理员使用）
     */
    @Column(name = "remarks", length = 500)
    private String remarks;

    /**
     * 驳回原因（管理员填写）
     */
    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    /**
     * 在线邀请状态枚举
     */
    public enum OnlineInviteStatus {
        /**
         * 待提交（记录已创建，但用户尚未填写完整信息）
         */
        PENDING("待提交"),

        /**
         * 待处理（用户已提交，等待管理员处理）
         */
        SUBMITTED("待处理"),

        /**
         * 已处理（管理员已确认处理完成）
         */
        PROCESSED("已处理"),

        /**
         * 已驳回（管理员驳回该申请）
         */
        REJECTED("已驳回");

        private final String description;

        OnlineInviteStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 检查是否可修改
     * 只有待提交或已驳回状态才能修改
     *
     * @return 是否可修改
     */
    public boolean canModify() {
        return status == OnlineInviteStatus.PENDING || status == OnlineInviteStatus.REJECTED;
    }

    /**
     * 检查是否已填写验证地址
     *
     * @return 是否已填写
     */
    public boolean isVerifyAddressFilled() {
        return verifyAddress != null && !verifyAddress.trim().isEmpty();
    }

    /**
     * 提交申请
     */
    public void submit(String verifyAddress) {
        this.verifyAddress = verifyAddress;
        this.status = OnlineInviteStatus.SUBMITTED;
        this.submitTime = LocalDateTime.now();
        // 清除之前的驳回原因
        this.rejectReason = null;
    }

    /**
     * 确认处理完成
     */
    public void confirmProcess() {
        this.status = OnlineInviteStatus.PROCESSED;
        this.processTime = LocalDateTime.now();
    }

    /**
     * 驳回申请（状态变为已驳回，用户可重新提交）
     */
    public void reject(String reason) {
        this.status = OnlineInviteStatus.REJECTED;
        this.rejectReason = reason;
    }

    /**
     * 在持久化之前设置创建时间和默认值
     */
    @PrePersist
    protected void onCreate() {
        if (createTime == null) {
            createTime = LocalDateTime.now();
        }
        if (status == null) {
            status = OnlineInviteStatus.PENDING;
        }
    }
}
