package org.my.augment.service;

import org.my.augment.entity.AuthKey;
import org.my.augment.entity.EmailLog;
import org.my.augment.repository.AuthKeyRepository;
import org.my.augment.repository.EmailLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 认证服务类
 * 负责API认证密钥的验证和使用记录
 * 
 * @author 杨宇帆
 * @create 2025-07-25
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private AuthKeyRepository authKeyRepository;

    @Autowired
    private EmailLogRepository emailLogRepository;

    /**
     * 超级授权key配置
     */
    @Value("${app.super-auth-key:yangyufan}")
    private String superAuthKey;

    /**
     * 验证认证密钥的有效性（用于API调用，检查使用次数）
     *
     * @param authKey 认证密钥
     * @return 验证结果
     */
    public AuthValidationResult validateAuthKey(String authKey) {
        if (authKey == null || authKey.trim().isEmpty()) {
            return AuthValidationResult.failure("认证密钥不能为空");
        }

        try {
            // 检查是否为超级授权key
            if (isSuperAuthKey(authKey)) {
                logger.debug("超级授权密钥验证成功: {}", authKey);
                return AuthValidationResult.superSuccess(authKey);
            }

            Optional<AuthKey> authKeyOpt = authKeyRepository.findValidAuthKey(authKey, LocalDateTime.now());

            if (!authKeyOpt.isPresent()) {
                logger.warn("无效的认证密钥: {}", authKey);
                return AuthValidationResult.failure("认证密钥无效或已过期");
            }

            AuthKey authKeyEntity = authKeyOpt.get();

            // 检查使用次数
            if (authKeyEntity.getUsedCount() >= authKeyEntity.getMaxCount()) {
                logger.warn("认证密钥使用次数已达上限: {}, 已使用: {}, 最大: {}",
                           authKey, authKeyEntity.getUsedCount(), authKeyEntity.getMaxCount());
                return AuthValidationResult.failure("认证密钥使用次数已达上限");
            }

            logger.debug("认证密钥验证成功: {}, 剩余次数: {}",
                        authKey, authKeyEntity.getRemainingCount());

            return AuthValidationResult.success(authKeyEntity);

        } catch (Exception e) {
            logger.error("验证认证密钥时发生异常: {}", e.getMessage(), e);
            return AuthValidationResult.failure("认证验证失败");
        }
    }

    /**
     * 验证认证密钥的有效性（用于登录，不检查使用次数限制）
     *
     * @param authKey 认证密钥
     * @return 验证结果
     */
    public AuthValidationResult validateAuthKeyForLogin(String authKey) {
        if (authKey == null || authKey.trim().isEmpty()) {
            return AuthValidationResult.failure("认证密钥不能为空");
        }

        try {
            // 检查是否为超级授权key
            if (isSuperAuthKey(authKey)) {
                logger.debug("超级授权密钥登录验证成功: {}", authKey);
                return AuthValidationResult.superSuccess(authKey);
            }

            // 查找认证密钥（不检查使用次数限制）
            Optional<AuthKey> authKeyOpt = authKeyRepository.findByAuthKey(authKey);

            if (!authKeyOpt.isPresent()) {
                logger.warn("认证密钥不存在: {}", authKey);
                return AuthValidationResult.failure("认证密钥不存在");
            }

            AuthKey authKeyEntity = authKeyOpt.get();

            // 检查状态
            if (authKeyEntity.getStatus() != 1) {
                logger.warn("认证密钥已被禁用: {}", authKey);
                return AuthValidationResult.failure("认证密钥已被禁用");
            }

            // 检查是否过期
            if (authKeyEntity.getExpireTime() != null && LocalDateTime.now().isAfter(authKeyEntity.getExpireTime())) {
                logger.warn("认证密钥已过期: {}", authKey);
                return AuthValidationResult.failure("认证密钥已过期");
            }

            logger.debug("认证密钥登录验证成功: {}, 剩余次数: {}",
                        authKey, authKeyEntity.getRemainingCount());

            return AuthValidationResult.success(authKeyEntity);

        } catch (Exception e) {
            logger.error("验证认证密钥时发生异常: {}", e.getMessage(), e);
            return AuthValidationResult.failure("认证验证失败");
        }
    }

    /**
     * 检查是否为超级授权key
     *
     * @param authKey 认证密钥
     * @return 是否为超级授权key
     */
    public boolean isSuperAuthKey(String authKey) {
        return superAuthKey != null && superAuthKey.equals(authKey);
    }

    /**
     * 使用认证密钥（增加使用次数）
     * 只有在使用次数未达到上限时才会成功
     *
     * @param authKey 认证密钥
     * @param request HTTP请求对象
     * @return 是否成功
     */
    @Transactional
    public boolean useAuthKey(String authKey, HttpServletRequest request) {
        try {
            // 检查是否为超级授权key，超级key无使用次数限制
            if (isSuperAuthKey(authKey)) {
                logger.debug("超级授权密钥无使用次数限制: {}", authKey);
                return true;
            }

            String clientIp = getClientIpAddress(request);
            int updatedRows = authKeyRepository.incrementUsedCount(authKey, clientIp, LocalDateTime.now());

            if (updatedRows > 0) {
                logger.debug("认证密钥使用次数更新成功: {}, IP: {}", authKey, clientIp);
                return true;
            } else {
                logger.warn("认证密钥使用次数更新失败，可能已达到使用上限或密钥无效: {}", authKey);
                return false;
            }

        } catch (Exception e) {
            logger.error("更新认证密钥使用次数时发生异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 记录邮件操作日志
     * 
     * @param authKey 认证密钥
     * @param operationType 操作类型
     * @param emailAddress 邮箱地址
     * @param result 操作结果
     * @param errorMessage 错误信息
     * @param request HTTP请求对象
     * @param responseTime 响应时间
     */
    public void logEmailOperation(String authKey, 
                                 EmailLog.OperationType operationType,
                                 String emailAddress,
                                 EmailLog.OperationResult result,
                                 String errorMessage,
                                 HttpServletRequest request,
                                 Long responseTime) {
        try {
            EmailLog emailLog = EmailLog.builder()
                    .authKey(authKey)
                    .operationType(operationType)
                    .emailAddress(emailAddress)
                    .result(result)
                    .errorMessage(errorMessage)
                    .requestIp(getClientIpAddress(request))
                    .userAgent(request.getHeader("User-Agent"))
                    .responseTime(responseTime)
                    .createTime(LocalDateTime.now())
                    .build();

            emailLogRepository.save(emailLog);
            
            logger.debug("邮件操作日志记录成功: authKey={}, operation={}, result={}", 
                        authKey, operationType, result);
            
        } catch (Exception e) {
            logger.error("记录邮件操作日志时发生异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取客户端真实IP地址
     * 
     * @param request HTTP请求对象
     * @return 客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * 认证验证结果类
     */
    public static class AuthValidationResult {
        private final boolean success;
        private final String message;
        private final AuthKey authKey;
        private final boolean isSuperAuth;
        private final String superAuthKey;

        private AuthValidationResult(boolean success, String message, AuthKey authKey, boolean isSuperAuth, String superAuthKey) {
            this.success = success;
            this.message = message;
            this.authKey = authKey;
            this.isSuperAuth = isSuperAuth;
            this.superAuthKey = superAuthKey;
        }

        public static AuthValidationResult success(AuthKey authKey) {
            return new AuthValidationResult(true, "验证成功", authKey, false, null);
        }

        public static AuthValidationResult superSuccess(String superAuthKey) {
            return new AuthValidationResult(true, "超级授权验证成功", null, true, superAuthKey);
        }

        public static AuthValidationResult failure(String message) {
            return new AuthValidationResult(false, message, null, false, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public AuthKey getAuthKey() {
            return authKey;
        }

        public boolean isSuperAuth() {
            return isSuperAuth;
        }

        public String getSuperAuthKey() {
            return superAuthKey;
        }

        /**
         * 获取实际的授权key字符串
         *
         * @return 授权key字符串
         */
        public String getActualAuthKey() {
            if (isSuperAuth) {
                return superAuthKey;
            } else if (authKey != null) {
                return authKey.getAuthKey();
            }
            return null;
        }
    }
}
