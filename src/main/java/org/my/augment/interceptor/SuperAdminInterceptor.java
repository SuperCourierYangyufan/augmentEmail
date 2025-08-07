package org.my.augment.interceptor;

import org.my.augment.service.AuthService;
import org.my.augment.service.AuthService.AuthValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 超级管理员权限验证拦截器
 * 专门用于保护公告管理相关接口，只允许超级管理员访问
 * 
 * @author 杨宇帆
 * @create 2025-08-07
 */
@Component
public class SuperAdminInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(SuperAdminInterceptor.class);

    @Autowired
    private AuthService authService;

    /**
     * 需要超级管理员权限的接口路径前缀
     */
    private static final Set<String> SUPER_ADMIN_PATHS = new HashSet<>(Arrays.asList(
            "/api/announcements",
            "/announcement-admin.html",
            "/announcement-admin",
            "/announcement-edit.html",
            "/announcement-edit"
    ));

    /**
     * 不需要超级管理员权限的公共接口（允许普通用户访问）
     */
    private static final Set<String> PUBLIC_PATHS = new HashSet<>(Arrays.asList(
            "/api/announcements/public",
            "/api/announcements/sidebar"
    ));

    /**
     * 公共接口的路径模式（支持路径参数）
     */
    private static final Set<String> PUBLIC_PATH_PATTERNS = new HashSet<>(Arrays.asList(
            "/api/announcements/\\d+"  // 匹配 /api/announcements/{id} 格式
    ));

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        
        logger.debug("超级管理员权限检查，URI: {}, Method: {}", requestURI, method);

        // 检查是否为需要超级管理员权限的路径
        if (!needsSuperAdminPermission(requestURI)) {
            logger.debug("路径不需要超级管理员权限，放行: {}", requestURI);
            return true;
        }

        // 检查是否为公共接口（只读操作）
        if (isPublicPath(requestURI) && "GET".equalsIgnoreCase(method)) {
            logger.debug("公共只读接口，放行: {}", requestURI);
            return true;
        }

        // 验证超级管理员权限
        if (!hasSuperAdminPermission(request)) {
            logger.warn("访问被拒绝，缺少超级管理员权限，URI: {}, IP: {}", 
                       requestURI, getClientIP(request));
            handleUnauthorized(response, "需要超级管理员权限才能访问此资源");
            return false;
        }

        logger.debug("超级管理员权限验证通过，URI: {}", requestURI);
        return true;
    }

    /**
     * 检查路径是否需要超级管理员权限
     * 
     * @param requestURI 请求URI
     * @return 是否需要超级管理员权限
     */
    private boolean needsSuperAdminPermission(String requestURI) {
        return SUPER_ADMIN_PATHS.stream().anyMatch(requestURI::startsWith);
    }

    /**
     * 检查是否为公共接口
     *
     * @param requestURI 请求URI
     * @return 是否为公共接口
     */
    private boolean isPublicPath(String requestURI) {
        // 检查精确匹配的公共路径
        if (PUBLIC_PATHS.stream().anyMatch(requestURI::startsWith)) {
            return true;
        }

        // 检查路径模式匹配
        return PUBLIC_PATH_PATTERNS.stream().anyMatch(pattern -> requestURI.matches(pattern));
    }

    /**
     * 验证是否具有超级管理员权限
     * 
     * @param request HTTP请求对象
     * @return 是否具有超级管理员权限
     */
    private boolean hasSuperAdminPermission(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        
        if (session == null) {
            logger.debug("会话不存在，无超级管理员权限");
            return false;
        }

        // 检查会话中是否有认证密钥
        String authKey = (String) session.getAttribute("authKey");
        if (authKey == null || authKey.trim().isEmpty()) {
            logger.debug("会话中无认证密钥，无超级管理员权限");
            return false;
        }

        // 验证认证密钥是否为超级授权key
        try {
            AuthValidationResult validationResult = authService.validateAuthKeyForLogin(authKey);
            
            if (validationResult.isSuccess() && validationResult.isSuperAuth()) {
                logger.debug("超级管理员权限验证成功，会话ID: {}", session.getId());
                return true;
            } else {
                logger.debug("认证密钥不是超级授权key或已失效，authKey: {}", authKey);
                // 清除无效会话
                session.invalidate();
                return false;
            }
        } catch (Exception e) {
            logger.error("验证超级管理员权限时发生异常", e);
            return false;
        }
    }

    /**
     * 处理未授权访问
     * 
     * @param response HTTP响应对象
     * @param message 错误消息
     * @throws IOException IO异常
     */
    private void handleUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        
        String jsonResponse = String.format(
            "{\"error\": \"%s\", \"code\": 401, \"timestamp\": \"%s\"}", 
            message, 
            java.time.LocalDateTime.now().toString()
        );
        
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }

    /**
     * 获取客户端真实IP地址
     * 
     * @param request HTTP请求对象
     * @return 客户端IP地址
     */
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty() && !"unknown".equalsIgnoreCase(xRealIP)) {
            return xRealIP;
        }
        
        return request.getRemoteAddr();
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 记录访问日志（如果需要）
        if (ex != null) {
            logger.error("处理超级管理员请求时发生异常，URI: {}", request.getRequestURI(), ex);
        }
    }
}
