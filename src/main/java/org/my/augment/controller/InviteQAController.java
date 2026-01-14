package org.my.augment.controller;

import org.my.augment.service.InviteQAService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 邀请页面Q&A控制器
 *
 * @author System
 * @create 2026-01-14
 */
@RestController
@RequestMapping("/api/invite/qa")
public class InviteQAController {

    private static final Logger logger = LoggerFactory.getLogger(InviteQAController.class);

    @Autowired
    private InviteQAService inviteQAService;

    /**
     * 获取启用的Q&A列表（前台公开接口）
     *
     * @return Q&A列表
     */
    @GetMapping("/public")
    public ResponseEntity<Map<String, Object>> getPublicQAList() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Map<String, Object>> qaList = inviteQAService.getEnabledQAList();
            response.put("success", true);
            response.put("data", qaList);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取Q&A列表失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "获取失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取所有Q&A列表（管理端）
     * 需要超级管理员权限
     *
     * @return Q&A列表
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getAllQAList() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Map<String, Object>> qaList = inviteQAService.getAllQAList();
            response.put("success", true);
            response.put("data", qaList);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取Q&A列表失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "获取失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 添加Q&A（管理端）
     * 需要超级管理员权限
     *
     * @param requestBody 请求体
     * @return 操作结果
     */
    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addQA(@RequestBody Map<String, String> requestBody) {
        String question = requestBody.get("question");
        String answer = requestBody.get("answer");

        logger.info("添加Q&A，问题: {}", question);

        try {
            Map<String, Object> result = inviteQAService.addQA(question, answer);
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            logger.error("添加Q&A失败: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "添加失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 更新Q&A（管理端）
     * 需要超级管理员权限
     *
     * @param id Q&A ID
     * @param requestBody 请求体
     * @return 操作结果
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateQA(
            @PathVariable Long id,
            @RequestBody Map<String, String> requestBody) {
        String question = requestBody.get("question");
        String answer = requestBody.get("answer");

        logger.info("更新Q&A，ID: {}", id);

        try {
            Map<String, Object> result = inviteQAService.updateQA(id, question, answer);
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            logger.error("更新Q&A失败: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "更新失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 删除Q&A（管理端）
     * 需要超级管理员权限
     *
     * @param id Q&A ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteQA(@PathVariable Long id) {
        logger.info("删除Q&A，ID: {}", id);

        try {
            Map<String, Object> result = inviteQAService.deleteQA(id);
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            logger.error("删除Q&A失败: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "删除失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 切换Q&A启用状态（管理端）
     * 需要超级管理员权限
     *
     * @param id Q&A ID
     * @return 操作结果
     */
    @PostMapping("/{id}/toggle")
    public ResponseEntity<Map<String, Object>> toggleEnabled(@PathVariable Long id) {
        logger.info("切换Q&A状态，ID: {}", id);

        try {
            Map<String, Object> result = inviteQAService.toggleEnabled(id);
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            logger.error("切换Q&A状态失败: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "操作失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 更新Q&A排序（管理端）
     * 需要超级管理员权限
     *
     * @param id Q&A ID
     * @param requestBody 请求体
     * @return 操作结果
     */
    @PostMapping("/{id}/sort")
    public ResponseEntity<Map<String, Object>> updateSortOrder(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> requestBody) {
        Integer sortOrder = requestBody.get("sortOrder");

        logger.info("更新Q&A排序，ID: {}, 排序: {}", id, sortOrder);

        try {
            Map<String, Object> result = inviteQAService.updateSortOrder(id, sortOrder);
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            logger.error("更新Q&A排序失败: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "操作失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
