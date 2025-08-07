package org.my.augment.service;


import org.my.augment.entity.Announcement;
import org.my.augment.repository.AnnouncementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 公告业务逻辑服务类
 * 提供公告的完整业务逻辑，包括CRUD操作、权限验证等
 *
 * @author 杨宇帆
 * @create 2025-08-07
 */
@Service
@Transactional
public class AnnouncementService {

    private static final Logger logger = LoggerFactory.getLogger(AnnouncementService.class);

    @Autowired
    private AnnouncementRepository announcementRepository;





    /**
     * 创建新公告
     * 
     * @param announcement 公告对象
     * @param creator 创建者标识
     * @return 创建的公告
     */
    public Announcement createAnnouncement(Announcement announcement, String creator) {
        logger.info("创建新公告，标题: {}, 创建者: {}", announcement.getTitle(), creator);
        
        // 设置创建者
        announcement.setCreator(creator);
        
        // 如果是置顶公告，设置合适的权重
        if (announcement.isPinned()) {
            Integer maxWeight = announcementRepository.getMaxSortWeight();
            announcement.setSortWeight(maxWeight != null ? maxWeight + 1 : 1000);
        }
        
        Announcement savedAnnouncement = announcementRepository.save(announcement);
        logger.info("公告创建成功，ID: {}", savedAnnouncement.getId());
        
        return savedAnnouncement;
    }

    /**
     * 更新公告
     * 
     * @param id 公告ID
     * @param announcement 更新的公告信息
     * @return 更新后的公告
     */
    public Announcement updateAnnouncement(Long id, Announcement announcement) {
        logger.info("更新公告，ID: {}", id);
        
        Optional<Announcement> existingOpt = announcementRepository.findById(id);
        if (!existingOpt.isPresent()) {
            throw new RuntimeException("公告不存在，ID: " + id);
        }
        
        Announcement existing = existingOpt.get();
        
        // 更新基本信息
        existing.setTitle(announcement.getTitle());
        existing.setContent(announcement.getContent());
        existing.setType(announcement.getType());
        existing.setIsVisible(announcement.getIsVisible());
        
        // 处理置顶状态变更
        if (announcement.isPinned() != existing.isPinned()) {
            if (announcement.isPinned()) {
                // 设置为置顶
                Integer maxWeight = announcementRepository.getMaxSortWeight();
                existing.setSortWeight(maxWeight != null ? maxWeight + 1 : 1000);
                existing.setIsPinned(true);
            } else {
                // 取消置顶
                existing.setSortWeight(0);
                existing.setIsPinned(false);
            }
        }
        

        
        Announcement updatedAnnouncement = announcementRepository.save(existing);
        logger.info("公告更新成功，ID: {}", updatedAnnouncement.getId());
        
        return updatedAnnouncement;
    }

    /**
     * 删除公告
     * 
     * @param id 公告ID
     */
    public void deleteAnnouncement(Long id) {
        logger.info("删除公告，ID: {}", id);
        
        Optional<Announcement> announcementOpt = announcementRepository.findById(id);
        if (!announcementOpt.isPresent()) {
            throw new RuntimeException("公告不存在，ID: " + id);
        }
        
        Announcement announcement = announcementOpt.get();
        

        
        // 删除数据库记录
        announcementRepository.deleteById(id);
        
        logger.info("公告删除成功，ID: {}", id);
    }

    /**
     * 根据ID获取公告详情
     * 
     * @param id 公告ID
     * @return 公告详情
     */
    public Announcement getAnnouncementById(Long id) {
        Optional<Announcement> announcementOpt = announcementRepository.findById(id);
        if (!announcementOpt.isPresent()) {
            throw new RuntimeException("公告不存在，ID: " + id);
        }
        return announcementOpt.get();
    }

    /**
     * 获取所有公告（管理员视图）
     * 
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 分页的公告列表
     */
    public Page<Announcement> getAllAnnouncements(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return announcementRepository.findAllOrderByPinnedAndCreateTime(pageable);
    }

    /**
     * 获取可见的公告列表（用户视图）
     * 
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 分页的可见公告列表
     */
    public Page<Announcement> getVisibleAnnouncements(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return announcementRepository.findVisibleAnnouncementsOrderByPinnedAndWeight(pageable);
    }

    /**
     * 获取可见的公告列表（不分页，用于侧边栏展示）
     * 
     * @return 可见公告列表
     */
    public List<Announcement> getVisibleAnnouncementsList() {
        return announcementRepository.findVisibleAnnouncementsOrderByPinnedAndWeight();
    }

    /**
     * 根据条件搜索公告
     * 
     * @param title 标题关键词
     * @param isVisible 是否可见
     * @param isPinned 是否置顶
     * @param type 公告类型
     * @param page 页码
     * @param size 每页大小
     * @return 分页的搜索结果
     */
    public Page<Announcement> searchAnnouncements(String title, Boolean isVisible, Boolean isPinned, 
                                                 Announcement.AnnouncementType type, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return announcementRepository.findByConditions(title, isVisible, isPinned, type, pageable);
    }

    /**
     * 切换公告的可见状态
     * 
     * @param id 公告ID
     * @return 更新后的公告
     */
    public Announcement toggleVisibility(Long id) {
        logger.info("切换公告可见状态，ID: {}", id);
        
        Announcement announcement = getAnnouncementById(id);
        announcement.setIsVisible(!announcement.getIsVisible());
        
        return announcementRepository.save(announcement);
    }

    /**
     * 切换公告的置顶状态
     * 
     * @param id 公告ID
     * @return 更新后的公告
     */
    public Announcement togglePinned(Long id) {
        logger.info("切换公告置顶状态，ID: {}", id);
        
        Announcement announcement = getAnnouncementById(id);
        
        if (announcement.isPinned()) {
            // 取消置顶
            announcement.unpin();
        } else {
            // 设置置顶
            announcement.pin();
            // 设置权重为当前最大值+1
            Integer maxWeight = announcementRepository.getMaxSortWeight();
            announcement.setSortWeight(maxWeight != null ? maxWeight + 1 : 1000);
        }
        
        return announcementRepository.save(announcement);
    }

    /**
     * 批量更新公告状态
     * 
     * @param ids 公告ID列表
     * @param isVisible 是否可见
     * @return 更新的记录数
     */
    public int batchUpdateVisibility(List<Long> ids, Boolean isVisible) {
        logger.info("批量更新公告可见状态，数量: {}, 状态: {}", ids.size(), isVisible);
        return announcementRepository.updateVisibilityByIds(ids, isVisible, LocalDateTime.now());
    }

    /**
     * 获取公告统计信息
     *
     * @return 统计信息Map
     */
    public Map<String, Object> getAnnouncementStatistics() {
        Map<String, Object> statistics = new HashMap<>();

        long totalCount = announcementRepository.count();
        long visibleCount = announcementRepository.countVisibleAnnouncements();
        long pinnedCount = announcementRepository.countPinnedAnnouncements();

        statistics.put("totalCount", totalCount);
        statistics.put("visibleCount", visibleCount);
        statistics.put("hiddenCount", totalCount - visibleCount);
        statistics.put("pinnedCount", pinnedCount);

        return statistics;
    }








}
