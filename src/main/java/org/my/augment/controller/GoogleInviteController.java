package org.my.augment.controller;

import org.my.augment.service.GoogleInviteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Google邀请控制器
 * 提供邀请相关的REST API接口
 *
 * @author 杨宇帆
 * @create 2025-08-20
 */
@RestController
@RequestMapping("/api/invite")
public class GoogleInviteController {

    private static final Logger logger = LoggerFactory.getLogger(GoogleInviteController.class);

    @Autowired
    private GoogleInviteService googleInviteService;

    /**
     * 生成邀请链接
     * 无需认证
     *
     * @return 完整的邀请链接
     */
    @RequestMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateInviteLink() {
        logger.info("生成邀请链接请求");

        try {
            String inviteLink = googleInviteService.generateInviteLink();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("inviteLink", inviteLink);
            response.put("message", "邀请链接生成成功");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("生成邀请链接失败: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "生成邀请链接失败: " + e.getMessage());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 校验邀请码状态
     * 无需认证
     *
     * @param code 邀请码
     * @return 校验结果
     */
    @GetMapping("/check/{code}")
    public ResponseEntity<Map<String, Object>> checkInviteCode(@PathVariable String code) {
        logger.info("校验邀请码: {}", code);

        try {
            Map<String, Object> result = googleInviteService.checkInviteCode(code);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", result);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("校验邀请码失败: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "校验失败: " + e.getMessage());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 提交邮箱信息
     * 无需认证
     *
     * @param code 邀请码
     * @param requestBody 请求体（包含emailAddress）
     * @return 提交结果
     */
    @PostMapping("/submit/{code}")
    public ResponseEntity<Map<String, Object>> submitEmail(
            @PathVariable String code,
            @RequestBody Map<String, String> requestBody) {

        String emailAddress = requestBody.get("emailAddress");
        logger.info("提交邮箱，邀请码: {}, 邮箱: {}", code, emailAddress);

        try {
            Map<String, Object> result = googleInviteService.submitEmail(code, emailAddress);

            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            logger.error("提交邮箱失败: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "提交失败: " + e.getMessage());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 更新邮箱信息
     * 无需认证
     *
     * @param code 邀请码
     * @param requestBody 请求体（包含emailAddress）
     * @return 更新结果
     */
    @PutMapping("/update/{code}")
    public ResponseEntity<Map<String, Object>> updateEmail(
            @PathVariable String code,
            @RequestBody Map<String, String> requestBody) {

        String emailAddress = requestBody.get("emailAddress");
        logger.info("更新邮箱，邀请码: {}, 邮箱: {}", code, emailAddress);

        try {
            Map<String, Object> result = googleInviteService.updateEmail(code, emailAddress);

            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            logger.error("更新邮箱失败: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "更新失败: " + e.getMessage());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 提交订单号和邮箱信息（新接口）
     * 无需认证
     *
     * @param code 邀请码
     * @param requestBody 请求体（包含orderNumber和emailAddress）
     * @return 提交结果
     */
    @PostMapping("/submitOrder/{code}")
    public ResponseEntity<Map<String, Object>> submitOrderAndEmail(
            @PathVariable String code,
            @RequestBody Map<String, String> requestBody) {

        String orderNumber = requestBody.get("orderNumber");
        String emailAddress = requestBody.get("emailAddress");
        logger.info("提交订单，邀请码: {}, 订单号: {}, 邮箱: {}", code, orderNumber, emailAddress);

        try {
            Map<String, Object> result = googleInviteService.submitOrderAndEmail(code, orderNumber, emailAddress);

            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            logger.error("提交订单失败: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "提交失败: " + e.getMessage());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 直接申请邀请（无需邀请码）
     * 无需认证
     *
     * @param requestBody 请求体（包含orderNumber和emailAddress）
     * @return 申请结果
     */
    @PostMapping("/apply")
    public ResponseEntity<Map<String, Object>> applyDirect(
            @RequestBody Map<String, String> requestBody) {

        String orderNumber = requestBody.get("orderNumber");
        String emailAddress = requestBody.get("emailAddress");
        logger.info("直接申请邀请，订单号: {}, 邮箱: {}", orderNumber, emailAddress);

        try {
            Map<String, Object> result = googleInviteService.applyDirect(orderNumber, emailAddress);

            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            logger.error("直接申请失败: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "申请失败: " + e.getMessage());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 根据邮箱查询订单信息
     * 无需认证
     *
     * @param email 邮箱地址
     * @return 查询结果
     */
    @GetMapping("/queryByEmail")
    public ResponseEntity<Map<String, Object>> queryByEmail(@RequestParam String email) {
        logger.info("根据邮箱查询订单，邮箱: {}", email);

        try {
            Map<String, Object> result = googleInviteService.queryByEmail(email);

            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            logger.error("邮箱查询失败: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "查询失败: " + e.getMessage());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 获取邀请列表（管理员）
     * 需要超级管理员权限
     *
     * @return 邀请列表
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getInviteList() {
        logger.info("获取邀请列表");

        try {
            List<Map<String, Object>> invites = googleInviteService.getAllInvites();
            Map<String, Object> statistics = googleInviteService.getStatistics();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", invites);
            response.put("statistics", statistics);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("获取邀请列表失败: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取列表失败: " + e.getMessage());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 确认邀请成功（管理员）
     * 需要超级管理员权限
     *
     * @param id 邀请记录ID
     * @return 确认结果
     */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<Map<String, Object>> confirmInvite(@PathVariable Long id) {
        logger.info("确认邀请，ID: {}", id);

        try {
            Map<String, Object> result = googleInviteService.confirmInvite(id);

            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            logger.error("确认邀请失败: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "确认失败: " + e.getMessage());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 驳回申请（管理员）
     * 驳回后用户可以看到原因并重新填写
     * 需要超级管理员权限
     *
     * @param id 邀请记录ID
     * @param requestBody 请求体（包含reason）
     * @return 驳回结果
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<Map<String, Object>> rejectInvite(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> requestBody) {
        String reason = requestBody != null ? requestBody.get("reason") : null;
        logger.info("驳回邀请，ID: {}, 原因: {}", id, reason);

        try {
            Map<String, Object> result = googleInviteService.rejectInvite(id, reason);

            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            logger.error("驳回邀请失败: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "驳回失败: " + e.getMessage());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 删除邀请记录（管理员）
     * 需要超级管理员权限
     *
     * @param id 邀请记录ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteInvite(@PathVariable Long id) {
        logger.info("删除邀请记录，ID: {}", id);

        try {
            Map<String, Object> result = googleInviteService.deleteInvite(id);

            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            logger.error("删除邀请记录失败: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "删除失败: " + e.getMessage());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
