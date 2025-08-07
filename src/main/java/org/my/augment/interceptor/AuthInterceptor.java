package org.my.augment.interceptor;

import org.my.augment.controller.LoginController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 认证拦截器
 * 拦截未登录用户的请求，对指定接口放行
 * 
 * @author 杨宇帆
 * @create 2025-08-07
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AuthInterceptor.class);

    /**
     * 不需要登录验证的接口路径
     * 包括登录相关接口和用户指定的放行接口
     */
    private static final Set<String> EXCLUDED_PATHS = new HashSet<>(Arrays.asList(
            // 登录相关接口
            "/api/login",
            "/api/logout", 
            "/api/login-status",
            "/login.html",
            "/login",
            
            // 用户指定的放行接口
            "/generate-auth-key",
            "/temp-email", 
            "/verification-code",
            
            // 静态资源
            "/css/",
            "/js/",
            "/images/",
            "/favicon.ico",
            
            // 健康检查和测试接口
            "/test/",
            "/actuator/"
    ));

    /**
     * 不需要登录验证的接口前缀
     */
    private static final Set<String> EXCLUDED_PREFIXES = new HashSet<>(Arrays.asList(
            "/test/",
            "/actuator/",
            "/css/",
            "/js/",
            "/images/"
    ));

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) 
            throws Exception {
        
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        
        logger.debug("拦截器处理请求: {} {}", method, requestURI);

        // 检查是否为放行路径
        if (isExcludedPath(requestURI)) {
            logger.debug("请求路径在放行列表中，允许访问: {}", requestURI);
            return true;
        }

        // 检查用户是否已登录
        if (LoginController.isUserLoggedIn(request)) {
            logger.debug("用户已登录，允许访问: {}", requestURI);
            return true;
        }

        // 用户未登录，根据请求类型进行处理
        logger.info("用户未登录，拒绝访问: {} {}", method, requestURI);
        
        if (isAjaxRequest(request)) {
            // AJAX请求返回JSON响应
            handleAjaxUnauthorized(response);
        } else {
            // 普通请求重定向到登录页面
            handlePageUnauthorized(request, response);
        }
        
        return false;
    }

    /**
     * 检查请求路径是否在放行列表中
     *
     * @param requestURI 请求URI
     * @return 是否放行
     */
    private boolean isExcludedPath(String requestURI) {
        // 精确匹配
        if (EXCLUDED_PATHS.contains(requestURI)) {
            return true;
        }
        
        // 前缀匹配
        for (String prefix : EXCLUDED_PREFIXES) {
            if (requestURI.startsWith(prefix)) {
                return true;
            }
        }
        
        // 特殊处理：管理员接口中的生成认证密钥接口
        if (requestURI.equals("/admin/generate-auth-key")) {
            return true;
        }
        
        return false;
    }

    /**
     * 判断是否为AJAX请求
     *
     * @param request HTTP请求
     * @return 是否为AJAX请求
     */
    private boolean isAjaxRequest(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        String contentType = request.getHeader("Content-Type");
        String accept = request.getHeader("Accept");
        
        return "XMLHttpRequest".equals(requestedWith) ||
               (contentType != null && contentType.contains("application/json")) ||
               (accept != null && accept.contains("application/json"));
    }

    /**
     * 处理AJAX请求的未授权访问
     *
     * @param response HTTP响应
     * @throws IOException IO异常
     */
    private void handleAjaxUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        
        String jsonResponse = "{"
                + "\"success\": false,"
                + "\"message\": \"用户未登录或会话已过期\","
                + "\"code\": 401,"
                + "\"redirectUrl\": \"/login.html\""
                + "}";
        
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }

    /**
     * 处理页面请求的未授权访问
     *
     * @param request HTTP请求
     * @param response HTTP响应
     * @throws IOException IO异常
     */
    private void handlePageUnauthorized(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        String requestURI = request.getRequestURI();
        String queryString = request.getQueryString();
        
        // 构建重定向URL，包含原始请求路径作为参数
        StringBuilder redirectUrl = new StringBuilder("/login.html");
        
        if (requestURI != null && !requestURI.equals("/")) {
            redirectUrl.append("?redirect=").append(java.net.URLEncoder.encode(requestURI, "UTF-8"));
            
            if (queryString != null && !queryString.isEmpty()) {
                String fullUrl = requestURI + "?" + queryString;
                redirectUrl = new StringBuilder("/login.html?redirect=")
                        .append(java.net.URLEncoder.encode(fullUrl, "UTF-8"));
            }
        }
        
        logger.debug("重定向到登录页面: {}", redirectUrl.toString());
        response.sendRedirect(redirectUrl.toString());
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                               Object handler, Exception ex) throws Exception {
        // 请求完成后的清理工作（如果需要）
        if (ex != null) {
            logger.error("请求处理过程中发生异常: {} {}", request.getMethod(), request.getRequestURI(), ex);
        }
    }
}
