package org.my.augment.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Google自助邀请实体类
 * 用于管理Google邀请申请记录
 *
 * @author 杨宇帆
 * @create 2025-08-20
 */
@Entity
@Table(name = "google_invites")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoogleInvite {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 邀请码（16位短UUID）
     */
    @Column(name = "invite_code", nullable = false, unique = true, length = 32)
    private String inviteCode;

    /**
     * 申请人邮箱地址（可为空，表示尚未填写）
     */
    @Column(name = "email_address", length = 255)
    private String emailAddress;

    /**
     * 邀请状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @lombok.Builder.Default
    private InviteStatus status = InviteStatus.PENDING;

    /**
     * 创建时间（邀请码生成时间）
     */
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    /**
     * 邮箱填充时间
     */
    @Column(name = "fill_time")
    private LocalDateTime fillTime;

    /**
     * 邀请确认时间
     */
    @Column(name = "confirm_time")
    private LocalDateTime confirmTime;

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
     * 邀请状态枚举
     */
    public enum InviteStatus {
        /**
         * 待填写（邀请码已生成，但用户尚未填写邮箱）
         */
        PENDING("待填写"),

        /**
         * 待邀请（用户已填写邮箱，等待管理员确认邀请）
         */
        SUBMITTED("待邀请"),

        /**
         * 已邀请（管理员已确认邀请成功）
         */
        INVITED("已邀请"),

        /**
         * 已取消（管理员取消该申请）
         */
        CANCELLED("已取消");

        private final String description;

        InviteStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 检查是否可修改邮箱
     * 只有待填写或待邀请状态才能修改
     *
     * @return 是否可修改
     */
    public boolean canModify() {
        return status == InviteStatus.PENDING || status == InviteStatus.SUBMITTED;
    }

    /**
     * 检查是否已填写邮箱
     *
     * @return 是否已填写
     */
    public boolean isEmailFilled() {
        return emailAddress != null && !emailAddress.trim().isEmpty();
    }

    /**
     * 提交邮箱
     */
    public void submitEmail(String email) {
        this.emailAddress = email;
        this.status = InviteStatus.SUBMITTED;
        this.fillTime = LocalDateTime.now();
        // 清除之前的驳回原因
        this.rejectReason = null;
    }

    /**
     * 确认邀请成功
     */
    public void confirmInvite() {
        this.status = InviteStatus.INVITED;
        this.confirmTime = LocalDateTime.now();
    }

    /**
     * 驳回申请（状态回到待填写，用户可重新提交）
     */
    public void reject(String reason) {
        this.status = InviteStatus.PENDING;
        this.rejectReason = reason;
        // 清空之前填写的邮箱，让用户重新填写
        this.emailAddress = null;
        this.fillTime = null;
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
            status = InviteStatus.PENDING;
        }
    }
}
