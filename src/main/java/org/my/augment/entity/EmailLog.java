package org.my.augment.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 邮件操作日志实体类
 * 记录所有邮件相关操作的日志信息
 * 
 * @author 杨宇帆
 * @create 2025-07-25
 */
@Entity
@Table(name = "email_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailLog {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 使用的认证密钥
     */
    @Column(name = "auth_key", nullable = false, length = 64)
    private String authKey;

    /**
     * 操作类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 20)
    private OperationType operationType;

    /**
     * 邮箱地址
     */
    @Column(name = "email_address")
    private String emailAddress;

    /**
     * 操作结果
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 20)
    private OperationResult result;

    /**
     * 错误信息
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 请求IP
     */
    @Column(name = "request_ip", length = 45)
    private String requestIp;

    /**
     * 用户代理
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * 创建时间
     */
    @Column(name = "create_time", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createTime;

    /**
     * 响应时间(毫秒)
     */
    @Column(name = "response_time")
    private Long responseTime;

    /**
     * 操作类型枚举
     */
    public enum OperationType {
        GENERATE_EMAIL("生成邮箱"),
        GET_VERIFICATION("获取验证码"),
        LOGIN("登录"),
        LOGOUT("退出"),
        ADD_EMAIL("新增邮箱"),
        DELETE_ALL_EMAILS("删除所有邮件"),
        BAN_EMAIL("封禁邮箱");

        private final String description;

        OperationType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 操作结果枚举
     */
    public enum OperationResult {
        SUCCESS("成功"),
        FAILED("失败");

        private final String description;

        OperationResult(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 设置创建时间为当前时间
     */
    @PrePersist
    public void prePersist() {
        if (createTime == null) {
            createTime = LocalDateTime.now();
        }
    }
}
