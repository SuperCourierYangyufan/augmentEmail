package org.my.augment.service;

import org.my.augment.entity.InviteQA;
import org.my.augment.repository.InviteQARepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 邀请页面Q&A服务层
 *
 * @author System
 * @create 2026-01-14
 */
@Service
public class InviteQAService {

    private static final Logger logger = LoggerFactory.getLogger(InviteQAService.class);

    @Autowired
    private InviteQARepository inviteQARepository;

    /**
     * 获取所有启用的Q&A（前台展示用）
     *
     * @return Q&A列表
     */
    public List<Map<String, Object>> getEnabledQAList() {
        List<InviteQA> qaList = inviteQARepository.findAllEnabledOrderBySortOrder();
        return qaList.stream().map(this::convertToMap).collect(Collectors.toList());
    }

    /**
     * 根据类型获取所有启用的Q&A（前台展示用）
     *
     * @param qaType Q&A类型（INVITE/ONLINE）
     * @return Q&A列表
     */
    public List<Map<String, Object>> getEnabledQAListByType(String qaType) {
        List<InviteQA> qaList = inviteQARepository.findAllEnabledByTypeOrderBySortOrder(qaType);
        return qaList.stream().map(this::convertToMap).collect(Collectors.toList());
    }

    /**
     * 获取所有Q&A（管理端使用）
     *
     * @return Q&A列表
     */
    public List<Map<String, Object>> getAllQAList() {
        List<InviteQA> qaList = inviteQARepository.findAllOrderBySortOrder();
        return qaList.stream().map(this::convertToMapWithDetails).collect(Collectors.toList());
    }

    /**
     * 根据类型获取所有Q&A（管理端使用）
     *
     * @param qaType Q&A类型（INVITE/ONLINE）
     * @return Q&A列表
     */
    public List<Map<String, Object>> getAllQAListByType(String qaType) {
        List<InviteQA> qaList = inviteQARepository.findAllByTypeOrderBySortOrder(qaType);
        return qaList.stream().map(this::convertToMapWithDetails).collect(Collectors.toList());
    }

    /**
     * 添加Q&A
     *
     * @param question 问题
     * @param answer 答案
     * @return 操作结果
     */
    @Transactional
    public Map<String, Object> addQA(String question, String answer) {
        return addQAWithType(question, answer, "INVITE");
    }

    /**
     * 添加指定类型的Q&A
     *
     * @param question 问题
     * @param answer 答案
     * @param qaType Q&A类型
     * @return 操作结果
     */
    @Transactional
    public Map<String, Object> addQAWithType(String question, String answer, String qaType) {
        Map<String, Object> result = new HashMap<>();

        if (question == null || question.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "问题不能为空");
            return result;
        }

        if (answer == null || answer.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "答案不能为空");
            return result;
        }

        // 获取当前类型的最大排序值
        Integer maxSortOrder = inviteQARepository.getMaxSortOrderByType(qaType);

        InviteQA qa = InviteQA.builder()
                .question(question.trim())
                .answer(answer.trim())
                .sortOrder(maxSortOrder + 1)
                .enabled(true)
                .qaType(qaType)
                .build();

        inviteQARepository.save(qa);
        logger.info("添加Q&A成功，ID: {}, 类型: {}", qa.getId(), qaType);

        result.put("success", true);
        result.put("message", "添加成功");
        result.put("data", convertToMapWithDetails(qa));
        return result;
    }

    /**
     * 更新Q&A
     *
     * @param id Q&A ID
     * @param question 问题
     * @param answer 答案
     * @return 操作结果
     */
    @Transactional
    public Map<String, Object> updateQA(Long id, String question, String answer) {
        Map<String, Object> result = new HashMap<>();

        Optional<InviteQA> qaOpt = inviteQARepository.findById(id);
        if (!qaOpt.isPresent()) {
            result.put("success", false);
            result.put("message", "Q&A不存在");
            return result;
        }

        if (question == null || question.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "问题不能为空");
            return result;
        }

        if (answer == null || answer.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "答案不能为空");
            return result;
        }

        InviteQA qa = qaOpt.get();
        qa.setQuestion(question.trim());
        qa.setAnswer(answer.trim());
        inviteQARepository.save(qa);

        logger.info("更新Q&A成功，ID: {}", id);

        result.put("success", true);
        result.put("message", "更新成功");
        return result;
    }

    /**
     * 删除Q&A
     *
     * @param id Q&A ID
     * @return 操作结果
     */
    @Transactional
    public Map<String, Object> deleteQA(Long id) {
        Map<String, Object> result = new HashMap<>();

        if (!inviteQARepository.existsById(id)) {
            result.put("success", false);
            result.put("message", "Q&A不存在");
            return result;
        }

        inviteQARepository.deleteById(id);
        logger.info("删除Q&A成功，ID: {}", id);

        result.put("success", true);
        result.put("message", "删除成功");
        return result;
    }

    /**
     * 切换Q&A启用状态
     *
     * @param id Q&A ID
     * @return 操作结果
     */
    @Transactional
    public Map<String, Object> toggleEnabled(Long id) {
        Map<String, Object> result = new HashMap<>();

        Optional<InviteQA> qaOpt = inviteQARepository.findById(id);
        if (!qaOpt.isPresent()) {
            result.put("success", false);
            result.put("message", "Q&A不存在");
            return result;
        }

        InviteQA qa = qaOpt.get();
        qa.setEnabled(!qa.getEnabled());
        inviteQARepository.save(qa);

        logger.info("切换Q&A状态成功，ID: {}, 新状态: {}", id, qa.getEnabled());

        result.put("success", true);
        result.put("message", qa.getEnabled() ? "已启用" : "已禁用");
        result.put("enabled", qa.getEnabled());
        return result;
    }

    /**
     * 更新Q&A排序
     *
     * @param id Q&A ID
     * @param sortOrder 新排序值
     * @return 操作结果
     */
    @Transactional
    public Map<String, Object> updateSortOrder(Long id, Integer sortOrder) {
        Map<String, Object> result = new HashMap<>();

        Optional<InviteQA> qaOpt = inviteQARepository.findById(id);
        if (!qaOpt.isPresent()) {
            result.put("success", false);
            result.put("message", "Q&A不存在");
            return result;
        }

        InviteQA qa = qaOpt.get();
        qa.setSortOrder(sortOrder);
        inviteQARepository.save(qa);

        logger.info("更新Q&A排序成功，ID: {}, 新排序: {}", id, sortOrder);

        result.put("success", true);
        result.put("message", "排序更新成功");
        return result;
    }

    /**
     * 上移Q&A（与上一个交换排序值）
     *
     * @param id Q&A ID
     * @return 操作结果
     */
    @Transactional
    public Map<String, Object> moveUp(Long id) {
        Map<String, Object> result = new HashMap<>();

        Optional<InviteQA> qaOpt = inviteQARepository.findById(id);
        if (!qaOpt.isPresent()) {
            result.put("success", false);
            result.put("message", "Q&A不存在");
            return result;
        }

        InviteQA current = qaOpt.get();
        List<InviteQA> allQaList = inviteQARepository.findAllOrderBySortOrder();

        // 找到当前项在列表中的位置
        int currentIndex = -1;
        for (int i = 0; i < allQaList.size(); i++) {
            if (allQaList.get(i).getId().equals(id)) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex <= 0) {
            result.put("success", false);
            result.put("message", "已经是第一个了");
            return result;
        }

        // 交换排序值
        InviteQA prev = allQaList.get(currentIndex - 1);
        Integer tempSort = current.getSortOrder();
        current.setSortOrder(prev.getSortOrder());
        prev.setSortOrder(tempSort);

        inviteQARepository.save(current);
        inviteQARepository.save(prev);

        logger.info("上移Q&A成功，ID: {}", id);

        result.put("success", true);
        result.put("message", "上移成功");
        return result;
    }

    /**
     * 下移Q&A（与下一个交换排序值）
     *
     * @param id Q&A ID
     * @return 操作结果
     */
    @Transactional
    public Map<String, Object> moveDown(Long id) {
        Map<String, Object> result = new HashMap<>();

        Optional<InviteQA> qaOpt = inviteQARepository.findById(id);
        if (!qaOpt.isPresent()) {
            result.put("success", false);
            result.put("message", "Q&A不存在");
            return result;
        }

        InviteQA current = qaOpt.get();
        List<InviteQA> allQaList = inviteQARepository.findAllOrderBySortOrder();

        // 找到当前项在列表中的位置
        int currentIndex = -1;
        for (int i = 0; i < allQaList.size(); i++) {
            if (allQaList.get(i).getId().equals(id)) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex < 0 || currentIndex >= allQaList.size() - 1) {
            result.put("success", false);
            result.put("message", "已经是最后一个了");
            return result;
        }

        // 交换排序值
        InviteQA next = allQaList.get(currentIndex + 1);
        Integer tempSort = current.getSortOrder();
        current.setSortOrder(next.getSortOrder());
        next.setSortOrder(tempSort);

        inviteQARepository.save(current);
        inviteQARepository.save(next);

        logger.info("下移Q&A成功，ID: {}", id);

        result.put("success", true);
        result.put("message", "下移成功");
        return result;
    }

    /**
     * 转换为前端展示用的Map（简化版）
     */
    private Map<String, Object> convertToMap(InviteQA qa) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", qa.getId());
        map.put("question", qa.getQuestion());
        map.put("answer", qa.getAnswer());
        return map;
    }

    /**
     * 转换为管理端使用的Map（完整版）
     */
    private Map<String, Object> convertToMapWithDetails(InviteQA qa) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", qa.getId());
        map.put("question", qa.getQuestion());
        map.put("answer", qa.getAnswer());
        map.put("sortOrder", qa.getSortOrder());
        map.put("enabled", qa.getEnabled());
        map.put("createTime", qa.getCreateTime() != null ? qa.getCreateTime().toString() : null);
        map.put("updateTime", qa.getUpdateTime() != null ? qa.getUpdateTime().toString() : null);
        return map;
    }
}
