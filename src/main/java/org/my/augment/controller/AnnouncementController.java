package org.my.augment.controller;

import org.my.augment.entity.Announcement;
import org.my.augment.service.AnnouncementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 公告管理控制器
 * 提供公告的REST API接口，包括增删改查等功能
 * 需要超级管理员权限才能访问
 *
 * @author 杨宇帆
 * @create 2025-08-07
 */
@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private static final Logger logger = LoggerFactory.getLogger(AnnouncementController.class);

    @Autowired
    private AnnouncementService announcementService;

    /**
     * 获取公告列表（管理员视图）
     * 
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @param title 标题搜索关键词（可选）
     * @param isVisible 是否可见（可选）
     * @param isPinned 是否置顶（可选）
     * @param type 公告类型（可选）
     * @return 分页的公告列表
     */
    @GetMapping("/admin")
    public ResponseEntity<Map<String, Object>> getAnnouncementsForAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Boolean isVisible,
            @RequestParam(required = false) Boolean isPinned,
            @RequestParam(required = false) String type) {
        
        logger.info("获取公告列表（管理员），页码: {}, 大小: {}", page, size);
        
        try {
            Page<Announcement> announcementPage;
            
            // 根据搜索条件决定使用哪个查询方法
            if (title != null || isVisible != null || isPinned != null || type != null) {
                Announcement.AnnouncementType announcementType = null;
                if (type != null) {
                    try {
                        announcementType = Announcement.AnnouncementType.valueOf(type.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        logger.warn("无效的公告类型: {}", type);
                    }
                }
                announcementPage = announcementService.searchAnnouncements(title, isVisible, isPinned, announcementType, page, size);
            } else {
                announcementPage = announcementService.getAllAnnouncements(page, size);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("content", announcementPage.getContent());
            response.put("totalElements", announcementPage.getTotalElements());
            response.put("totalPages", announcementPage.getTotalPages());
            response.put("currentPage", page);
            response.put("size", size);
            response.put("hasNext", announcementPage.hasNext());
            response.put("hasPrevious", announcementPage.hasPrevious());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取公告列表失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "获取公告列表失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 获取可见公告列表（用户视图）
     * 
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 分页的可见公告列表
     */
    @GetMapping("/public")
    public ResponseEntity<Map<String, Object>> getVisibleAnnouncements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        logger.info("获取可见公告列表，页码: {}, 大小: {}", page, size);
        
        try {
            Page<Announcement> announcementPage = announcementService.getVisibleAnnouncements(page, size);
            
            Map<String, Object> response = new HashMap<>();
            response.put("content", announcementPage.getContent());
            response.put("totalElements", announcementPage.getTotalElements());
            response.put("totalPages", announcementPage.getTotalPages());
            response.put("currentPage", page);
            response.put("size", size);
            response.put("hasNext", announcementPage.hasNext());
            response.put("hasPrevious", announcementPage.hasPrevious());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取可见公告列表失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "获取公告列表失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 获取可见公告列表（不分页，用于侧边栏）
     * 
     * @return 可见公告列表
     */
    @GetMapping("/sidebar")
    public ResponseEntity<Map<String, Object>> getAnnouncementsForSidebar() {
        logger.info("获取侧边栏公告列表");
        
        try {
            List<Announcement> announcements = announcementService.getVisibleAnnouncementsList();
            
            Map<String, Object> response = new HashMap<>();
            response.put("announcements", announcements);
            response.put("count", announcements.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取侧边栏公告列表失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "获取公告列表失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 根据ID获取公告详情
     * 
     * @param id 公告ID
     * @return 公告详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getAnnouncementById(@PathVariable Long id) {
        logger.info("获取公告详情，ID: {}", id);
        
        try {
            Announcement announcement = announcementService.getAnnouncementById(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("announcement", announcement);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            logger.warn("获取公告详情失败: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(404).body(errorResponse);
        } catch (Exception e) {
            logger.error("获取公告详情失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "获取公告详情失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 创建新公告
     * 
     * @param announcement 公告信息
     * @param request HTTP请求对象
     * @return 创建的公告
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createAnnouncement(
            @RequestBody Announcement announcement, 
            HttpServletRequest request) {
        
        logger.info("创建新公告，标题: {}", announcement.getTitle());
        
        try {
            // 获取创建者信息（从会话中获取）
            HttpSession session = request.getSession(false);
            String creator = "super_admin"; // 默认创建者标识
            if (session != null && session.getAttribute("authKey") != null) {
                creator = "super_admin_" + session.getId().substring(0, 8);
            }
            
            Announcement createdAnnouncement = announcementService.createAnnouncement(announcement, creator);
            
            Map<String, Object> response = new HashMap<>();
            response.put("announcement", createdAnnouncement);
            response.put("message", "公告创建成功");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("创建公告失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "创建公告失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 更新公告
     * 
     * @param id 公告ID
     * @param announcement 更新的公告信息
     * @return 更新后的公告
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateAnnouncement(
            @PathVariable Long id, 
            @RequestBody Announcement announcement) {
        
        logger.info("更新公告，ID: {}", id);
        
        try {
            Announcement updatedAnnouncement = announcementService.updateAnnouncement(id, announcement);
            
            Map<String, Object> response = new HashMap<>();
            response.put("announcement", updatedAnnouncement);
            response.put("message", "公告更新成功");
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            logger.warn("更新公告失败: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(404).body(errorResponse);
        } catch (Exception e) {
            logger.error("更新公告失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "更新公告失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 删除公告
     *
     * @param id 公告ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteAnnouncement(@PathVariable Long id) {
        logger.info("删除公告，ID: {}", id);

        try {
            announcementService.deleteAnnouncement(id);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "公告删除成功");

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            logger.warn("删除公告失败: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(404).body(errorResponse);
        } catch (Exception e) {
            logger.error("删除公告失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "删除公告失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 切换公告可见状态
     *
     * @param id 公告ID
     * @return 更新后的公告
     */
    @PatchMapping("/{id}/visibility")
    public ResponseEntity<Map<String, Object>> toggleVisibility(@PathVariable Long id) {
        logger.info("切换公告可见状态，ID: {}", id);

        try {
            Announcement updatedAnnouncement = announcementService.toggleVisibility(id);

            Map<String, Object> response = new HashMap<>();
            response.put("announcement", updatedAnnouncement);
            response.put("message", "公告状态更新成功");

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            logger.warn("切换公告可见状态失败: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(404).body(errorResponse);
        } catch (Exception e) {
            logger.error("切换公告可见状态失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "状态更新失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 切换公告置顶状态
     *
     * @param id 公告ID
     * @return 更新后的公告
     */
    @PatchMapping("/{id}/pinned")
    public ResponseEntity<Map<String, Object>> togglePinned(@PathVariable Long id) {
        logger.info("切换公告置顶状态，ID: {}", id);

        try {
            Announcement updatedAnnouncement = announcementService.togglePinned(id);

            Map<String, Object> response = new HashMap<>();
            response.put("announcement", updatedAnnouncement);
            response.put("message", "公告置顶状态更新成功");

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            logger.warn("切换公告置顶状态失败: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(404).body(errorResponse);
        } catch (Exception e) {
            logger.error("切换公告置顶状态失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "置顶状态更新失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }



    /**
     * 批量更新公告可见状态
     *
     * @param request 批量更新请求
     * @return 更新结果
     */
    @PatchMapping("/batch/visibility")
    public ResponseEntity<Map<String, Object>> batchUpdateVisibility(
            @RequestBody Map<String, Object> request) {

        logger.info("批量更新公告可见状态");

        try {
            @SuppressWarnings("unchecked")
            List<Long> ids = (List<Long>) request.get("ids");
            Boolean isVisible = (Boolean) request.get("isVisible");

            if (ids == null || ids.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "请选择要更新的公告");
                return ResponseEntity.status(400).body(errorResponse);
            }

            int updatedCount = announcementService.batchUpdateVisibility(ids, isVisible);

            Map<String, Object> response = new HashMap<>();
            response.put("updatedCount", updatedCount);
            response.put("message", "批量更新成功，共更新 " + updatedCount + " 条记录");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("批量更新公告可见状态失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "批量更新失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 获取公告统计信息
     *
     * @return 统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        logger.info("获取公告统计信息");

        try {
            Map<String, Object> statistics = announcementService.getAnnouncementStatistics();

            Map<String, Object> response = new HashMap<>();
            response.put("statistics", statistics);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("获取公告统计信息失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "获取统计信息失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
