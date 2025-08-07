package org.my.augment.controller;

import org.my.augment.entity.AuthKey;
import org.my.augment.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * 登录控制器
 * 处理用户登录认证和会话管理
 * 
 * @author 杨宇帆
 * @create 2025-08-07
 */
@RestController
@RequestMapping("/api")
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);
    
    /**
     * Session中存储登录状态的键名
     */
    public static final String SESSION_USER_KEY = "loginUser";
    
    /**
     * Session中存储认证密钥的键名
     */
    public static final String SESSION_AUTH_KEY = "authKey";

    @Autowired
    private AuthService authService;

    /**
     * 用户登录接口
     * 验证认证密钥并创建用户会话
     *
     * @param loginRequest 登录请求参数
     * @param request HTTP请求对象
     * @return 登录结果
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest loginRequest, 
                                                    HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String authKey = loginRequest.getAuthKey();
            
            if (authKey == null || authKey.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "认证密钥不能为空");
                return ResponseEntity.badRequest().body(response);
            }

            logger.info("用户尝试登录，认证密钥: {}", authKey.substring(0, Math.min(8, authKey.length())) + "...");

            // 验证认证密钥（登录时不检查使用次数限制）
            AuthService.AuthValidationResult validationResult = authService.validateAuthKeyForLogin(authKey);
            
            if (!validationResult.isSuccess()) {
                logger.warn("登录失败，认证密钥验证失败: {}", validationResult.getMessage());
                response.put("success", false);
                response.put("message", validationResult.getMessage());
                return ResponseEntity.badRequest().body(response);
            }

            // 创建用户会话
            HttpSession session = request.getSession(true);
            session.setAttribute(SESSION_USER_KEY, true);
            session.setAttribute(SESSION_AUTH_KEY, authKey);

            // 设置会话超时时间（30分钟）
            session.setMaxInactiveInterval(30 * 60);

            response.put("success", true);
            response.put("message", "登录成功");
            response.put("sessionId", session.getId());

            if (validationResult.isSuperAuth()) {
                // 超级授权key的处理
                logger.info("超级用户登录成功，会话ID: {}, 授权key: {}",
                           session.getId(), authKey);

                response.put("remainingCount", -1); // -1 表示无限制
                response.put("maxCount", -1);
                response.put("description", "超级管理员账户");
            } else {
                // 普通授权key的处理
                AuthKey authKeyEntity = validationResult.getAuthKey();

                logger.info("用户登录成功，会话ID: {}, 认证密钥剩余次数: {}",
                           session.getId(), authKeyEntity.getRemainingCount());

                response.put("remainingCount", authKeyEntity.getRemainingCount());
                response.put("maxCount", authKeyEntity.getMaxCount());
                response.put("description", authKeyEntity.getDescription());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("登录过程中发生异常: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "登录失败，系统异常");
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 用户退出登录接口
     * 清除用户会话
     *
     * @param request HTTP请求对象
     * @return 退出结果
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            HttpSession session = request.getSession(false);
            
            if (session != null) {
                String sessionId = session.getId();
                session.invalidate();
                logger.info("用户退出登录成功，会话ID: {}", sessionId);
            }

            response.put("success", true);
            response.put("message", "退出登录成功");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("退出登录过程中发生异常: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "退出登录失败");
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 检查登录状态接口
     * 验证当前用户是否已登录
     *
     * @param request HTTP请求对象
     * @return 登录状态
     */
    @GetMapping("/login-status")
    public ResponseEntity<Map<String, Object>> checkLoginStatus(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            HttpSession session = request.getSession(false);
            
            if (session != null && Boolean.TRUE.equals(session.getAttribute(SESSION_USER_KEY))) {
                String authKey = (String) session.getAttribute(SESSION_AUTH_KEY);
                
                // 验证认证密钥是否仍然有效（检查登录状态时不检查使用次数限制）
                if (authKey != null) {
                    AuthService.AuthValidationResult validationResult = authService.validateAuthKeyForLogin(authKey);
                    
                    if (validationResult.isSuccess()) {
                        response.put("loggedIn", true);
                        response.put("sessionId", session.getId());

                        if (validationResult.isSuperAuth()) {
                            // 超级授权key的处理
                            response.put("remainingCount", -1); // -1 表示无限制
                            response.put("maxCount", -1);
                            response.put("description", "超级管理员账户");
                        } else {
                            // 普通授权key的处理
                            AuthKey authKeyEntity = validationResult.getAuthKey();
                            response.put("remainingCount", authKeyEntity.getRemainingCount());
                            response.put("maxCount", authKeyEntity.getMaxCount());
                            response.put("description", authKeyEntity.getDescription());
                        }

                        return ResponseEntity.ok(response);
                    } else {
                        // 认证密钥已失效，清除会话
                        session.invalidate();
                        logger.warn("会话中的认证密钥已失效，清除会话: {}", session.getId());
                    }
                }
            }

            response.put("loggedIn", false);
            response.put("message", "未登录或会话已过期");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("检查登录状态时发生异常: {}", e.getMessage(), e);
            response.put("loggedIn", false);
            response.put("message", "检查登录状态失败");
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 检查用户是否已登录的工具方法
     * 供其他控制器或拦截器使用
     *
     * @param request HTTP请求对象
     * @return 是否已登录
     */
    public static boolean isUserLoggedIn(HttpServletRequest request) {
        try {
            HttpSession session = request.getSession(false);
            return session != null && Boolean.TRUE.equals(session.getAttribute(SESSION_USER_KEY));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取当前登录用户的认证密钥
     * 供其他控制器使用
     *
     * @param request HTTP请求对象
     * @return 认证密钥，如果未登录则返回null
     */
    public static String getCurrentAuthKey(HttpServletRequest request) {
        try {
            HttpSession session = request.getSession(false);
            if (session != null && Boolean.TRUE.equals(session.getAttribute(SESSION_USER_KEY))) {
                return (String) session.getAttribute(SESSION_AUTH_KEY);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 登录请求参数类
     */
    public static class LoginRequest {
        private String authKey;

        public String getAuthKey() {
            return authKey;
        }

        public void setAuthKey(String authKey) {
            this.authKey = authKey;
        }
    }
}
