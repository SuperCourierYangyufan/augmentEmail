package org.my.augment.controller;

import org.my.augment.entity.AuthKey;
import org.my.augment.entity.EmailLog;
import org.my.augment.repository.AuthKeyRepository;
import org.my.augment.repository.EmailLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.security.SecureRandom;

/**
 * 管理控制器
 * 提供认证密钥和日志管理功能
 * 
 * @author 杨宇帆
 * @create 2025-07-25
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private AuthKeyRepository authKeyRepository;

    @Autowired
    private EmailLogRepository emailLogRepository;

    /**
     * 随机字符串生成器
     */
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 随机字符串字符集（大小写字母和数字）
     */
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    /**
     * 查看认证密钥信息
     * 
     * @param authKey 认证密钥
     * @return 密钥信息
     */
    @GetMapping("/auth-key/{authKey}")
    public ResponseEntity<Map<String, Object>> getAuthKeyInfo(@PathVariable String authKey) {
        try {
            Optional<AuthKey> authKeyOpt = authKeyRepository.findByAuthKey(authKey);
            
            if (!authKeyOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            AuthKey authKeyEntity = authKeyOpt.get();
            Map<String, Object> result = new HashMap<>();
            result.put("authKey", authKeyEntity.getAuthKey());
            result.put("maxCount", authKeyEntity.getMaxCount());
            result.put("usedCount", authKeyEntity.getUsedCount());
            result.put("remainingCount", authKeyEntity.getRemainingCount());
            result.put("status", authKeyEntity.getStatus());
            result.put("description", authKeyEntity.getDescription());
            result.put("createTime", authKeyEntity.getCreateTime());
            result.put("expireTime", authKeyEntity.getExpireTime());
            result.put("lastUsedTime", authKeyEntity.getLastUsedTime());
            result.put("lastUsedIp", authKeyEntity.getLastUsedIp());
            result.put("isValid", authKeyEntity.isValid());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("查询认证密钥信息时发生异常: {}", e.getMessage(), e);
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", "查询失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorMap);
        }
    }

    /**
     * 查看认证密钥的使用日志
     * 
     * @param authKey 认证密钥
     * @param limit 限制数量，默认50
     * @return 使用日志列表
     */
    @GetMapping("/auth-key/{authKey}/logs")
    public ResponseEntity<List<EmailLog>> getAuthKeyLogs(@PathVariable String authKey,
                                                        @RequestParam(defaultValue = "50") int limit) {
        try {
            List<EmailLog> logs = emailLogRepository.findRecentLogs(authKey, PageRequest.of(0, limit));
            return ResponseEntity.ok(logs);
            
        } catch (Exception e) {
            logger.error("查询认证密钥日志时发生异常: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 获取所有认证密钥列表
     * 
     * @return 认证密钥列表
     */
    @GetMapping("/auth-keys")
    public ResponseEntity<List<AuthKey>> getAllAuthKeys() {
        try {
            List<AuthKey> authKeys = authKeyRepository.findAll();
            return ResponseEntity.ok(authKeys);
            
        } catch (Exception e) {
            logger.error("查询所有认证密钥时发生异常: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 创建新的认证密钥
     * 
     * @param authKey 认证密钥
     * @param maxCount 最大使用次数
     * @param description 描述
     * @param expireTime 过期时间
     * @return 创建结果
     */
    @PostMapping("/auth-key")
    public ResponseEntity<Map<String, Object>> createAuthKey(@RequestParam String authKey,
                                                            @RequestParam(defaultValue = "100") Integer maxCount,
                                                            @RequestParam(required = false) String description,
                                                            @RequestParam(required = false) String expireTime) {
        try {
            // 检查密钥是否已存在
            if (authKeyRepository.findByAuthKey(authKey).isPresent()) {
                Map<String, Object> errorMap = new HashMap<>();
                errorMap.put("error", "认证密钥已存在");
                return ResponseEntity.badRequest().body(errorMap);
            }
            
            AuthKey newAuthKey = new AuthKey();
            newAuthKey.setAuthKey(authKey);
            newAuthKey.setMaxCount(maxCount);
            newAuthKey.setUsedCount(0);
            newAuthKey.setStatus(1);
            newAuthKey.setDescription(description);
            newAuthKey.setCreateTime(LocalDateTime.now());
            
            if (expireTime != null && !expireTime.trim().isEmpty()) {
                newAuthKey.setExpireTime(LocalDateTime.parse(expireTime));
            }
            
            AuthKey savedAuthKey = authKeyRepository.save(newAuthKey);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("authKey", savedAuthKey);
            result.put("message", "认证密钥创建成功");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("创建认证密钥时发生异常: {}", e.getMessage(), e);
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", "创建失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorMap);
        }
    }

    /**
     * 更新认证密钥状态
     * 
     * @param authKey 认证密钥
     * @param status 新状态
     * @return 更新结果
     */
    @PutMapping("/auth-key/{authKey}/status")
    public ResponseEntity<Map<String, Object>> updateAuthKeyStatus(@PathVariable String authKey,
                                                                  @RequestParam Integer status) {
        try {
            Optional<AuthKey> authKeyOpt = authKeyRepository.findByAuthKey(authKey);
            
            if (!authKeyOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            AuthKey authKeyEntity = authKeyOpt.get();
            authKeyEntity.setStatus(status);
            authKeyRepository.save(authKeyEntity);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "状态更新成功");
            result.put("authKey", authKey);
            result.put("newStatus", status);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("更新认证密钥状态时发生异常: {}", e.getMessage(), e);
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", "更新失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorMap);
        }
    }

    /**
     * 获取系统统计信息
     * 
     * @return 统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getSystemStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 认证密钥统计
            long totalAuthKeys = authKeyRepository.count();
            long activeAuthKeys = authKeyRepository.existsValidAuthKey("", LocalDateTime.now()) ? 
                                 authKeyRepository.findAll().stream()
                                     .mapToLong(key -> key.isValid() ? 1 : 0)
                                     .sum() : 0;
            
            // 邮件操作统计
            long totalEmailLogs = emailLogRepository.count();
            
            stats.put("totalAuthKeys", totalAuthKeys);
            stats.put("activeAuthKeys", activeAuthKeys);
            stats.put("totalEmailLogs", totalEmailLogs);
            stats.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("获取系统统计信息时发生异常: {}", e.getMessage(), e);
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", "获取统计信息失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorMap);
        }
    }

    /**
     * 生成随机认证密钥
     * 自动生成20位随机字符串作为认证密钥，包含大小写字母和数字
     *
     * @param maxCount 最大使用次数，默认100
     * @param description 密钥描述，可选
     * @return 生成结果
     */
    @GetMapping("/generate-auth-key")
    public ResponseEntity<String> generateAuthKey(@RequestParam(defaultValue = "100") Integer maxCount,
                                                              @RequestParam(required = false) String description) {
        try {
            // 生成唯一的随机密钥
            String randomAuthKey = generateUniqueRandomKey();

            // 创建新的认证密钥实体
            AuthKey newAuthKey = new AuthKey();
            newAuthKey.setAuthKey(randomAuthKey);
            newAuthKey.setMaxCount(maxCount);
            newAuthKey.setUsedCount(0);
            newAuthKey.setStatus(1);
            newAuthKey.setDescription(description != null ? description : "系统自动生成的认证密钥");
            newAuthKey.setCreateTime(LocalDateTime.now());

            // 设置过期时间为一年后
            newAuthKey.setExpireTime(LocalDateTime.now().plusYears(1));

            // 保存到数据库
            AuthKey savedAuthKey = authKeyRepository.save(newAuthKey);

            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("authKey", savedAuthKey.getAuthKey());
            result.put("maxCount", savedAuthKey.getMaxCount());
            result.put("description", savedAuthKey.getDescription());
            result.put("createTime", savedAuthKey.getCreateTime());
            result.put("expireTime", savedAuthKey.getExpireTime());
            result.put("message", "随机认证密钥生成成功");

            logger.info("成功生成随机认证密钥: {}, 最大使用次数: {}, 过期时间: {}",
                       randomAuthKey, maxCount, savedAuthKey.getExpireTime());

            return ResponseEntity.ok(savedAuthKey.getAuthKey());

        } catch (Exception e) {
            return ResponseEntity.status(500).body("生成失败: " + e.getMessage());
        }
    }

    /**
     * 生成20位随机字符串
     * 包含大小写字母和数字的组合
     *
     * @return 20位随机字符串
     */
    private String generateRandomKey() {
        StringBuilder sb = new StringBuilder(20);
        for (int i = 0; i < 20; i++) {
            int index = RANDOM.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }
        return sb.toString();
    }

    /**
     * 生成唯一的随机密钥
     * 确保生成的密钥在数据库中不存在
     *
     * @return 唯一的随机密钥
     */
    private String generateUniqueRandomKey() {
        String randomKey;
        int attempts = 0;
        int maxAttempts = 10; // 最大尝试次数，防止无限循环

        do {
            randomKey = generateRandomKey();
            attempts++;

            if (attempts > maxAttempts) {
                logger.warn("生成唯一随机密钥超过最大尝试次数: {}", maxAttempts);
                // 如果超过最大尝试次数，在随机字符串后添加时间戳确保唯一性
                randomKey = generateRandomKey().substring(0, 15) + System.currentTimeMillis() % 100000;
                break;
            }

        } while (authKeyRepository.findByAuthKey(randomKey).isPresent());

        logger.debug("生成唯一随机密钥成功: {}, 尝试次数: {}", randomKey, attempts);
        return randomKey;
    }
}
