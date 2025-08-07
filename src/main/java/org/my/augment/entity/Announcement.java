package org.my.augment.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 公告实体类
 * 用于管理系统公告信息，支持富文本内容和附件上传
 * 
 * @author 杨宇帆
 * @create 2025-08-07
 */
@Entity
@Table(name = "announcements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Announcement {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 公告标题
     */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /**
     * 公告内容（富文本）
     */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 是否显示（true-显示，false-隐藏）
     */
    @Column(name = "is_visible", nullable = false)
    private Boolean isVisible = true;

    /**
     * 是否置顶（true-置顶，false-普通）
     */
    @Column(name = "is_pinned", nullable = false)
    private Boolean isPinned = false;

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
     * 创建者（超级管理员标识）
     */
    @Column(name = "creator", length = 100)
    private String creator;

    /**
     * 排序权重（数值越大越靠前，用于置顶公告的排序）
     */
    @Column(name = "sort_weight", nullable = false)
    private Integer sortWeight = 0;

    /**
     * 公告类型（预留字段）
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20)
    private AnnouncementType type = AnnouncementType.GENERAL;



    /**
     * 公告类型枚举
     */
    public enum AnnouncementType {
        /**
         * 普通公告
         */
        GENERAL("普通公告"),
        
        /**
         * 重要通知
         */
        IMPORTANT("重要通知"),
        
        /**
         * 系统维护
         */
        MAINTENANCE("系统维护"),
        
        /**
         * 功能更新
         */
        UPDATE("功能更新");

        private final String description;

        AnnouncementType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }





    /**
     * 检查公告是否可见
     * 
     * @return true如果可见，false如果隐藏
     */
    public boolean isVisible() {
        return isVisible != null && isVisible;
    }

    /**
     * 检查公告是否置顶
     * 
     * @return true如果置顶，false如果普通
     */
    public boolean isPinned() {
        return isPinned != null && isPinned;
    }

    /**
     * 设置为置顶状态
     */
    public void pin() {
        this.isPinned = true;
        // 置顶公告的权重设置为较高值
        if (this.sortWeight == null || this.sortWeight < 1000) {
            this.sortWeight = 1000;
        }
    }

    /**
     * 取消置顶状态
     */
    public void unpin() {
        this.isPinned = false;
        // 取消置顶时重置权重
        if (this.sortWeight != null && this.sortWeight >= 1000) {
            this.sortWeight = 0;
        }
    }

    /**
     * 显示公告
     */
    public void show() {
        this.isVisible = true;
    }

    /**
     * 隐藏公告
     */
    public void hide() {
        this.isVisible = false;
    }

    /**
     * 在持久化之前设置创建时间和默认值
     */
    @PrePersist
    protected void onCreate() {
        if (createTime == null) {
            createTime = LocalDateTime.now();
        }
        if (isVisible == null) {
            isVisible = true;
        }
        if (isPinned == null) {
            isPinned = false;
        }
        if (sortWeight == null) {
            sortWeight = 0;
        }
        if (type == null) {
            type = AnnouncementType.GENERAL;
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
