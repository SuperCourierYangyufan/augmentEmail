package org.my.augment.controller;

import org.my.augment.service.GoogleOnlineInviteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 谷歌在线邀请控制器
 * 提供在线邀请相关的REST API接口
 *
 * @author AI Assistant
 * @create 2025-01-19
 */
@RestController
@RequestMapping("/api/online-invite")
public class GoogleOnlineInviteController {

    private static final Logger logger = LoggerFactory.getLogger(GoogleOnlineInviteController.class);

    @Autowired
    private GoogleOnlineInviteService onlineInviteService;

    /**
     * 用户提交在线邀请申请
     * 无需认证
     *
     * @param requestBody 请求体（包含orderNumber和verifyAddress）
     * @return 申请结果
     */
    @PostMapping("/apply")
    public ResponseEntity<Map<String, Object>> apply(@RequestBody Map<String, String> requestBody) {
        String orderNumber = requestBody.get("orderNumber");
        String verifyAddress = requestBody.get("verifyAddress");
        logger.info("在线邀请申请，订单号: {}, 验证地址: {}", orderNumber, verifyAddress);

        try {
            Map<String, Object> result = onlineInviteService.applyOnline(orderNumber, verifyAddress);

            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            logger.error("在线邀请申请失败: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "申请失败: " + e.getMessage());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 根据订单号查询申请状态
     * 无需认证
     *
     * @param orderNumber 订单号
     * @return 查询结果
     */
    @GetMapping("/query")
    public ResponseEntity<Map<String, Object>> query(@RequestParam String orderNumber) {
        logger.info("查询在线邀请，订单号: {}", orderNumber);

        try {
            Map<String, Object> result = onlineInviteService.queryByOrderNumber(orderNumber);

            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            logger.error("查询失败: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "查询失败: " + e.getMessage());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 获取在线邀请列表（管理员）
     * 需要超级管理员权限
     *
     * @return 邀请列表
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getList() {
        logger.info("获取在线邀请列表");

        try {
            List<Map<String, Object>> records = onlineInviteService.getAllRecords();
            Map<String, Object> statistics = onlineInviteService.getStatistics();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", records);
            response.put("statistics", statistics);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("获取列表失败: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取列表失败: " + e.getMessage());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 确认处理完成（管理员）
     * 需要超级管理员权限
     *
     * @param id 记录ID
     * @return 处理结果
     */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<Map<String, Object>> confirm(@PathVariable Long id) {
        logger.info("确认处理，ID: {}", id);

        try {
            Map<String, Object> result = onlineInviteService.confirmProcess(id);

            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            logger.error("确认处理失败: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "确认处理失败: " + e.getMessage());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 驳回申请（管理员）
     * 需要超级管理员权限
     *
     * @param id 记录ID
     * @param requestBody 请求体（包含reason）
     * @return 驳回结果
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<Map<String, Object>> reject(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> requestBody) {
        String reason = requestBody != null ? requestBody.get("reason") : null;
        logger.info("驳回申请，ID: {}, 原因: {}", id, reason);

        try {
            Map<String, Object> result = onlineInviteService.reject(id, reason);

            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            logger.error("驳回申请失败: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "驳回失败: " + e.getMessage());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 删除记录（管理员）
     * 需要超级管理员权限
     *
     * @param id 记录ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        logger.info("删除记录，ID: {}", id);

        try {
            Map<String, Object> result = onlineInviteService.delete(id);

            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            logger.error("删除记录失败: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "删除失败: " + e.getMessage());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
