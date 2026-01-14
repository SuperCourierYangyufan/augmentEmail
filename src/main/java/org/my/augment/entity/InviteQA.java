package org.my.augment.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 邀请页面Q&A实体类
 * 用于管理邀请页面显示的常见问题
 *
 * @author System
 * @create 2026-01-14
 */
@Entity
@Table(name = "invite_qa")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InviteQA {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 问题标题
     */
    @Column(name = "question", nullable = false, length = 500)
    private String question;

    /**
     * 答案内容
     */
    @Column(name = "answer", nullable = false, columnDefinition = "TEXT")
    private String answer;

    /**
     * 排序权重（越大越靠前）
     */
    @Column(name = "sort_order", nullable = false)
    @lombok.Builder.Default
    private Integer sortOrder = 0;

    /**
     * 是否启用
     */
    @Column(name = "enabled", nullable = false)
    @lombok.Builder.Default
    private Boolean enabled = true;

    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    /**
     * 在持久化之前设置创建时间
     */
    @PrePersist
    protected void onCreate() {
        if (createTime == null) {
            createTime = LocalDateTime.now();
        }
        if (sortOrder == null) {
            sortOrder = 0;
        }
        if (enabled == null) {
            enabled = true;
        }
    }

    /**
     * 在更新之前设置更新时间
     */
    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
