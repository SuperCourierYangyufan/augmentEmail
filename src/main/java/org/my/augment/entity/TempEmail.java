package org.my.augment.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 临时邮箱管理实体类
 * 用于管理临时邮箱的生命周期和状态
 * 过期判断通过SQL查询实现，无需定时任务
 *
 * @author 杨宇帆
 * @create 2025-08-02
 */
@Entity
@Table(name = "temp_emails")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TempEmail {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 邮箱地址
     */
    @Column(name = "email_address", nullable = false, unique = true, length = 255)
    private String emailAddress;

    /**
     * 邮箱状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EmailStatus status = EmailStatus.ACTIVE;

    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;





    /**
     * 备注信息
     */
    @Column(name = "remarks", length = 500)
    private String remarks;

    /**
     * 授权密钥（用于数据隔离）
     */
    @Column(name = "auth_key", nullable = false, length = 64)
    private String authKey;

    /**
     * 邮箱状态枚举
     */
    public enum EmailStatus {
        /**
         * 正常状态
         */
        ACTIVE("正常"),

        /**
         * 已封禁
         */
        BANNED("已封禁");

        private final String description;

        EmailStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 检查邮箱是否已过期（已删除过期时间概念，始终返回false）
     *
     * @return 始终返回false，因为不再有过期时间概念
     */
    public boolean isExpired() {
        return false;
    }

    /**
     * 检查邮箱是否可用（未过期且未封禁）
     * 
     * @return true如果可用，false如果不可用
     */
    public boolean isAvailable() {
        return status == EmailStatus.ACTIVE && !isExpired();
    }

    /**
     * 获取已生成小时数
     *
     * @return 从创建时间到现在的小时数
     */
    public long getGeneratedHours() {
        return java.time.Duration.between(createTime, LocalDateTime.now()).toHours();
    }

    /**
     * 获取已生成天数
     *
     * @return 从创建时间到现在的天数
     */
    public long getGeneratedDays() {
        return java.time.Duration.between(createTime, LocalDateTime.now()).toDays();
    }



    /**
     * 设置为封禁状态
     */
    public void ban() {
        this.status = EmailStatus.BANNED;
    }

    /**
     * 激活邮箱
     */
    public void activate() {
        this.status = EmailStatus.ACTIVE;
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
            status = EmailStatus.ACTIVE;
        }
    }
}
