package org.my.augment.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 认证密钥实体类
 * 用于管理API访问的认证密钥信息
 * 
 * @author 杨宇帆
 * @create 2025-07-25
 */
@Entity
@Table(name = "auth_keys")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthKey {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 认证密钥
     */
    @Column(name = "auth_key", nullable = false, unique = true, length = 64)
    private String authKey;

    /**
     * 最大使用次数
     */
    @Column(name = "max_count", nullable = false)
    private Integer maxCount = 100;

    /**
     * 已使用次数
     */
    @Column(name = "used_count", nullable = false)
    private Integer usedCount = 0;

    /**
     * 状态：1-有效，0-无效
     */
    @Column(name = "status", nullable = false)
    private Integer status = 1;

    /**
     * 密钥描述
     */
    @Column(name = "description")
    private String description;

    /**
     * 创建时间
     */
    @Column(name = "create_time", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createTime;

    /**
     * 过期时间
     */
    @Column(name = "expire_time", columnDefinition = "TIMESTAMP NULL")
    private LocalDateTime expireTime;

    /**
     * 最后使用时间
     */
    @Column(name = "last_used_time", columnDefinition = "TIMESTAMP NULL")
    private LocalDateTime lastUsedTime;

    /**
     * 最后使用IP
     */
    @Column(name = "last_used_ip", length = 45)
    private String lastUsedIp;

    /**
     * 检查密钥是否有效
     * 
     * @return 是否有效
     */
    public boolean isValid() {
        // 检查状态
        if (status == null || status != 1) {
            return false;
        }
        
        // 检查是否过期
        if (expireTime != null && LocalDateTime.now().isAfter(expireTime)) {
            return false;
        }
        
        // 检查使用次数
        if (usedCount != null && maxCount != null && usedCount >= maxCount) {
            return false;
        }
        
        return true;
    }

    /**
     * 检查是否还有剩余使用次数
     * 
     * @return 剩余次数
     */
    public int getRemainingCount() {
        if (maxCount == null || usedCount == null) {
            return 0;
        }
        return Math.max(0, maxCount - usedCount);
    }

    /**
     * 增加使用次数
     */
    public void incrementUsedCount() {
        if (usedCount == null) {
            usedCount = 0;
        }
        usedCount++;
        lastUsedTime = LocalDateTime.now();
    }
}
