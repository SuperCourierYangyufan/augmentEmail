package org.my.augment.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 邀请页面控制器
 * 处理邀请页面的路由
 *
 * @author 杨宇帆
 * @create 2025-08-20
 */
@Controller
public class InvitePageController {

    /**
     * 邀请收集页面（需要邀请码）
     * 无需认证
     *
     * @param code 邀请码
     * @return 转发到邀请页面
     */
    @GetMapping("/invite/{code}")
    public String invitePage(@PathVariable String code) {
        // 返回邀请页面，code 通过 JavaScript 从 URL 获取
        return "forward:/invite.html";
    }

    /**
     * 直接申请页面（无需邀请码）
     * 无需认证
     *
     * @return 转发到申请页面
     */
    @GetMapping("/invite-apply")
    public String inviteApplyPage() {
        return "forward:/invite-apply.html";
    }
}
