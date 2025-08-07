package org.my.augment.repository;

import org.my.augment.entity.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 公告数据访问接口
 * 提供公告的基本CRUD操作和自定义查询方法
 * 
 * @author 杨宇帆
 * @create 2025-08-07
 */
@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    /**
     * 查询所有可见的公告，按置顶状态和排序权重排序
     * 置顶公告优先显示，同级别按权重和创建时间排序
     * 
     * @param pageable 分页参数
     * @return 分页的公告列表
     */
    @Query("SELECT a FROM Announcement a WHERE a.isVisible = true " +
           "ORDER BY a.isPinned DESC, a.sortWeight DESC, a.createTime DESC")
    Page<Announcement> findVisibleAnnouncementsOrderByPinnedAndWeight(Pageable pageable);

    /**
     * 查询所有可见的公告列表（不分页）
     * 用于前端侧边栏展示
     * 
     * @return 公告列表
     */
    @Query("SELECT a FROM Announcement a WHERE a.isVisible = true " +
           "ORDER BY a.isPinned DESC, a.sortWeight DESC, a.createTime DESC")
    List<Announcement> findVisibleAnnouncementsOrderByPinnedAndWeight();

    /**
     * 查询所有公告（管理员视图），按置顶状态和创建时间排序
     * 
     * @param pageable 分页参数
     * @return 分页的公告列表
     */
    @Query("SELECT a FROM Announcement a " +
           "ORDER BY a.isPinned DESC, a.sortWeight DESC, a.createTime DESC")
    Page<Announcement> findAllOrderByPinnedAndCreateTime(Pageable pageable);

    /**
     * 根据标题模糊查询公告（管理员搜索功能）
     * 
     * @param title 标题关键词
     * @param pageable 分页参数
     * @return 分页的公告列表
     */
    @Query("SELECT a FROM Announcement a WHERE a.title LIKE %:title% " +
           "ORDER BY a.isPinned DESC, a.sortWeight DESC, a.createTime DESC")
    Page<Announcement> findByTitleContainingOrderByPinnedAndCreateTime(@Param("title") String title, Pageable pageable);

    /**
     * 根据可见状态查询公告
     * 
     * @param isVisible 是否可见
     * @param pageable 分页参数
     * @return 分页的公告列表
     */
    @Query("SELECT a FROM Announcement a WHERE a.isVisible = :isVisible " +
           "ORDER BY a.isPinned DESC, a.sortWeight DESC, a.createTime DESC")
    Page<Announcement> findByIsVisibleOrderByPinnedAndCreateTime(@Param("isVisible") Boolean isVisible, Pageable pageable);

    /**
     * 根据置顶状态查询公告
     * 
     * @param isPinned 是否置顶
     * @param pageable 分页参数
     * @return 分页的公告列表
     */
    @Query("SELECT a FROM Announcement a WHERE a.isPinned = :isPinned " +
           "ORDER BY a.sortWeight DESC, a.createTime DESC")
    Page<Announcement> findByIsPinnedOrderByWeightAndCreateTime(@Param("isPinned") Boolean isPinned, Pageable pageable);

    /**
     * 根据公告类型查询公告
     * 
     * @param type 公告类型
     * @param pageable 分页参数
     * @return 分页的公告列表
     */
    @Query("SELECT a FROM Announcement a WHERE a.type = :type " +
           "ORDER BY a.isPinned DESC, a.sortWeight DESC, a.createTime DESC")
    Page<Announcement> findByTypeOrderByPinnedAndCreateTime(@Param("type") Announcement.AnnouncementType type, Pageable pageable);

    /**
     * 复合条件查询公告（管理员高级搜索）
     * 
     * @param title 标题关键词（可为null）
     * @param isVisible 是否可见（可为null）
     * @param isPinned 是否置顶（可为null）
     * @param type 公告类型（可为null）
     * @param pageable 分页参数
     * @return 分页的公告列表
     */
    @Query("SELECT a FROM Announcement a WHERE " +
           "(:title IS NULL OR a.title LIKE %:title%) AND " +
           "(:isVisible IS NULL OR a.isVisible = :isVisible) AND " +
           "(:isPinned IS NULL OR a.isPinned = :isPinned) AND " +
           "(:type IS NULL OR a.type = :type) " +
           "ORDER BY a.isPinned DESC, a.sortWeight DESC, a.createTime DESC")
    Page<Announcement> findByConditions(@Param("title") String title,
                                       @Param("isVisible") Boolean isVisible,
                                       @Param("isPinned") Boolean isPinned,
                                       @Param("type") Announcement.AnnouncementType type,
                                       Pageable pageable);

    /**
     * 查询指定时间范围内的公告
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 分页的公告列表
     */
    @Query("SELECT a FROM Announcement a WHERE a.createTime BETWEEN :startTime AND :endTime " +
           "ORDER BY a.isPinned DESC, a.sortWeight DESC, a.createTime DESC")
    Page<Announcement> findByCreateTimeBetween(@Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime,
                                              Pageable pageable);

    /**
     * 统计可见公告数量
     * 
     * @return 可见公告数量
     */
    @Query("SELECT COUNT(a) FROM Announcement a WHERE a.isVisible = true")
    long countVisibleAnnouncements();

    /**
     * 统计置顶公告数量
     * 
     * @return 置顶公告数量
     */
    @Query("SELECT COUNT(a) FROM Announcement a WHERE a.isPinned = true")
    long countPinnedAnnouncements();

    /**
     * 批量更新公告的可见状态
     * 
     * @param ids 公告ID列表
     * @param isVisible 是否可见
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE Announcement a SET a.isVisible = :isVisible, a.updateTime = :updateTime WHERE a.id IN :ids")
    int updateVisibilityByIds(@Param("ids") List<Long> ids, 
                             @Param("isVisible") Boolean isVisible,
                             @Param("updateTime") LocalDateTime updateTime);

    /**
     * 批量更新公告的置顶状态
     * 
     * @param ids 公告ID列表
     * @param isPinned 是否置顶
     * @param sortWeight 排序权重
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE Announcement a SET a.isPinned = :isPinned, a.sortWeight = :sortWeight, a.updateTime = :updateTime WHERE a.id IN :ids")
    int updatePinnedStatusByIds(@Param("ids") List<Long> ids,
                               @Param("isPinned") Boolean isPinned,
                               @Param("sortWeight") Integer sortWeight,
                               @Param("updateTime") LocalDateTime updateTime);

    /**
     * 获取最大排序权重值（用于新置顶公告的权重设置）
     * 
     * @return 最大排序权重值
     */
    @Query("SELECT COALESCE(MAX(a.sortWeight), 0) FROM Announcement a WHERE a.isPinned = true")
    Integer getMaxSortWeight();
}
