package org.my.augment.config;

import org.my.augment.interceptor.AuthInterceptor;
import org.my.augment.interceptor.SuperAdminInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类
 * 配置拦截器和其他Web相关设置
 * 
 * @author 杨宇帆
 * @create 2025-08-07
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WebConfig.class);

    @Autowired
    private AuthInterceptor authInterceptor;

    @Autowired
    private SuperAdminInterceptor superAdminInterceptor;

    /**
     * 配置拦截器
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        logger.info("开始配置拦截器...");

        // 配置基础认证拦截器
        registry.addInterceptor(authInterceptor)
                // 拦截所有请求
                .addPathPatterns("/**")
                // 排除不需要拦截的路径
                .excludePathPatterns(
                        // 登录相关页面和接口
                        "/login.html",
                        "/login",
                        "/api/login",
                        "/api/logout",
                        "/api/login-status",

                        // 用户指定的放行接口
                        "/generate-auth-key",
                        "/admin/generate-auth-key",
                        "/temp-email",
                        "/verification-code",

                        // 公告公共接口（允许普通用户访问）
                        "/api/announcements/public",
                        "/api/announcements/sidebar",

                        // 静态资源
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/favicon.ico",
                        "/robots.txt",

                        // 健康检查和测试接口
                        "/test/**",
                        "/actuator/**",

                        // 错误页面
                        "/error",
                        "/error/**"
                );

        // 配置超级管理员权限拦截器（优先级更高）
        registry.addInterceptor(superAdminInterceptor)
                .addPathPatterns(
                        // 公告管理相关接口
                        "/api/announcements/**",

                        // 超级管理员页面
                        "/announcement-admin.html",
                        "/announcement-admin",
                        "/announcement-edit.html",
                        "/announcement-edit"
                )
                .order(1); // 设置更高的优先级

        logger.info("拦截器配置完成");
    }
}
