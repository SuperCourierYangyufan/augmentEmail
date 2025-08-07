package org.my.augment.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;

/**
 * 页面控制器
 * 提供静态页面访问路由
 *
 * @author 杨宇帆
 * @create 2025-08-02
 */
@Controller
public class PageController {

    /**
     * 登录页面
     *
     * @param redirect 登录成功后的重定向地址
     * @return 登录页面
     */
    @GetMapping("/login")
    public String login(@RequestParam(required = false) String redirect) {
        return "redirect:/login.html" + (redirect != null ? "?redirect=" + redirect : "");
    }

    /**
     * 临时邮箱管理页面
     * 需要登录后才能访问，由拦截器控制
     *
     * @return 重定向到静态页面
     */
    @GetMapping("/temp-email-manager")
    public String tempEmailManager() {
        return "redirect:/temp-email-manager.html";
    }

    /**
     * 首页重定向逻辑
     * 如果用户已登录，重定向到临时邮箱管理页面
     * 如果用户未登录，由拦截器重定向到登录页面
     *
     * @param request HTTP请求对象
     * @return 重定向地址
     */
    @GetMapping("/")
    public String index(HttpServletRequest request) {
        // 检查用户是否已登录
        if (LoginController.isUserLoggedIn(request)) {
            return "redirect:/temp-email-manager";
        } else {
            // 未登录用户会被拦截器重定向到登录页面
            // 这里提供一个备用重定向，以防拦截器未生效
            return "redirect:/login";
        }
    }
}
