package org.my.augment.repository;

import org.my.augment.entity.InviteQA;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 邀请页面Q&A数据访问层
 *
 * @author System
 * @create 2026-01-14
 */
@Repository
public interface InviteQARepository extends JpaRepository<InviteQA, Long> {

    /**
     * 获取所有启用的Q&A，按排序权重升序（从早到晚）
     *
     * @return Q&A列表
     */
    @Query("SELECT q FROM InviteQA q WHERE q.enabled = true ORDER BY q.sortOrder ASC, q.createTime ASC")
    List<InviteQA> findAllEnabledOrderBySortOrder();

    /**
     * 获取所有Q&A，按排序权重升序（管理端使用，从早到晚）
     *
     * @return Q&A列表
     */
    @Query("SELECT q FROM InviteQA q ORDER BY q.sortOrder ASC, q.createTime ASC")
    List<InviteQA> findAllOrderBySortOrder();

    /**
     * 获取最大排序权重
     *
     * @return 最大排序权重
     */
    @Query("SELECT COALESCE(MAX(q.sortOrder), 0) FROM InviteQA q")
    Integer getMaxSortOrder();
}
