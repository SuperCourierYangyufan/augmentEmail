package org.my.augment.config;

import org.my.augment.entity.AuthKey;
import org.my.augment.repository.AuthKeyRepository;
import org.my.augment.repository.EmailLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据库连接测试控制器
 * 用于测试MySQL数据库连接和基本操作
 * 
 * @author 杨宇帆
 * @create 2025-07-25
 */
@RestController
@RequestMapping("/test")
public class DatabaseTestController {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseTestController.class);

    @Autowired
    private DataSource dataSource;

    @Autowired
    private AuthKeyRepository authKeyRepository;

    @Autowired
    private EmailLogRepository emailLogRepository;

    /**
     * 测试数据库连接
     * 
     * @return 连接信息
     */
    @GetMapping("/db-connection")
    public ResponseEntity<Map<String, Object>> testDatabaseConnection() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 测试数据源连接
            Connection connection = dataSource.getConnection();
            DatabaseMetaData metaData = connection.getMetaData();
            
            result.put("connected", true);
            result.put("databaseProductName", metaData.getDatabaseProductName());
            result.put("databaseProductVersion", metaData.getDatabaseProductVersion());
            result.put("driverName", metaData.getDriverName());
            result.put("driverVersion", metaData.getDriverVersion());
            result.put("url", metaData.getURL());
            result.put("userName", metaData.getUserName());
            
            connection.close();
            
            logger.info("数据库连接测试成功: {}", metaData.getURL());
            
        } catch (Exception e) {
            result.put("connected", false);
            result.put("error", e.getMessage());
            logger.error("数据库连接测试失败: {}", e.getMessage(), e);
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 测试认证密钥表操作
     * 
     * @return 操作结果
     */
    @GetMapping("/auth-keys")
    public ResponseEntity<Map<String, Object>> testAuthKeys() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 查询所有认证密钥
            List<AuthKey> authKeys = authKeyRepository.findAll();
            result.put("success", true);
            result.put("totalCount", authKeys.size());
            result.put("authKeys", authKeys);
            
            logger.info("认证密钥表查询成功，共 {} 条记录", authKeys.size());
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            logger.error("认证密钥表查询失败: {}", e.getMessage(), e);
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 测试邮件日志表操作
     * 
     * @return 操作结果
     */
    @GetMapping("/email-logs")
    public ResponseEntity<Map<String, Object>> testEmailLogs() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 查询邮件日志总数
            long totalLogs = emailLogRepository.count();
            result.put("success", true);
            result.put("totalCount", totalLogs);
            
            logger.info("邮件日志表查询成功，共 {} 条记录", totalLogs);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            logger.error("邮件日志表查询失败: {}", e.getMessage(), e);
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 综合数据库健康检查
     * 
     * @return 健康状态
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> result = new HashMap<>();
        boolean allHealthy = true;
        
        try {
            // 1. 测试数据库连接
            Connection connection = dataSource.getConnection();
            result.put("databaseConnection", "OK");
            connection.close();
            
        } catch (Exception e) {
            result.put("databaseConnection", "FAILED: " + e.getMessage());
            allHealthy = false;
        }
        
        try {
            // 2. 测试认证密钥表
            long authKeyCount = authKeyRepository.count();
            result.put("authKeysTable", "OK (" + authKeyCount + " records)");
            
        } catch (Exception e) {
            result.put("authKeysTable", "FAILED: " + e.getMessage());
            allHealthy = false;
        }
        
        try {
            // 3. 测试邮件日志表
            long emailLogCount = emailLogRepository.count();
            result.put("emailLogsTable", "OK (" + emailLogCount + " records)");
            
        } catch (Exception e) {
            result.put("emailLogsTable", "FAILED: " + e.getMessage());
            allHealthy = false;
        }
        
        result.put("overallStatus", allHealthy ? "HEALTHY" : "UNHEALTHY");
        result.put("timestamp", System.currentTimeMillis());
        
        if (allHealthy) {
            logger.info("数据库健康检查通过");
            return ResponseEntity.ok(result);
        } else {
            logger.warn("数据库健康检查失败");
            return ResponseEntity.status(500).body(result);
        }
    }
}
