package org.my.augment.config;

import org.my.augment.interceptor.AuthInterceptor;
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

    /**
     * 配置拦截器
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        logger.info("开始配置认证拦截器...");
        
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
        
        logger.info("认证拦截器配置完成");
    }
}
