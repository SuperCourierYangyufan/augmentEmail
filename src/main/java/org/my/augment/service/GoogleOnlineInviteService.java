package org.my.augment.service;

import org.my.augment.entity.GoogleOnlineInvite;
import org.my.augment.entity.GoogleOnlineInvite.OnlineInviteStatus;
import org.my.augment.repository.GoogleOnlineInviteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 谷歌在线邀请业务逻辑层
 *
 * @author AI Assistant
 * @create 2025-01-19
 */
@Service
public class GoogleOnlineInviteService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleOnlineInviteService.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // URL 验证正则表达式
    private static final Pattern URL_PATTERN = Pattern.compile(
            "^(https?://)" +                              // 协议
            "([a-zA-Z0-9]([a-zA-Z0-9\\-]*[a-zA-Z0-9])?\\.)+" + // 子域名
            "[a-zA-Z]{2,}" +                              // 顶级域名
            "(:\\d{1,5})?" +                              // 端口（可选）
            "(/[^\\s]*)?$",                               // 路径（可选）
            Pattern.CASE_INSENSITIVE
    );

    @Autowired
    private GoogleOnlineInviteRepository repository;

    /**
     * 用户提交在线邀请申请
     *
     * @param orderNumber 订单号
     * @param verifyAddress 验证地址（URL格式）
     * @return 申请结果
     */
    public Map<String, Object> applyOnline(String orderNumber, String verifyAddress) {
        Map<String, Object> result = new HashMap<>();

        // 校验订单号
        if (orderNumber == null || orderNumber.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "订单号不能为空");
            return result;
        }

        // 校验验证地址
        if (verifyAddress == null || verifyAddress.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "验证地址不能为空");
            return result;
        }

        // 校验 URL 格式
        if (!isValidUrl(verifyAddress.trim())) {
            result.put("success", false);
            result.put("message", "验证地址格式不正确，请输入有效的 URL（以 http:// 或 https:// 开头）");
            return result;
        }

        String trimmedOrderNumber = orderNumber.trim();
        String trimmedVerifyAddress = verifyAddress.trim();

        // 检查订单号是否已被使用
        Optional<GoogleOnlineInvite> existingByOrder = repository.findByOrderNumber(trimmedOrderNumber);
        if (existingByOrder.isPresent()) {
            GoogleOnlineInvite existing = existingByOrder.get();
            // 如果是已驳回状态，允许重新提交
            if (existing.getStatus() == OnlineInviteStatus.REJECTED) {
                existing.submit(trimmedVerifyAddress);
                repository.save(existing);
                logger.info("重新提交申请成功，订单号: {}, 验证地址: {}", trimmedOrderNumber, trimmedVerifyAddress);
                result.put("success", true);
                result.put("message", "申请重新提交成功，请等待处理");
                return result;
            }
            result.put("success", false);
            result.put("message", "该订单号已提交过申请");
            return result;
        }

        // 创建新的邀请记录并提交
        GoogleOnlineInvite invite = GoogleOnlineInvite.builder()
                .orderNumber(trimmedOrderNumber)
                .verifyAddress(trimmedVerifyAddress)
                .status(OnlineInviteStatus.SUBMITTED)
                .createTime(LocalDateTime.now())
                .submitTime(LocalDateTime.now())
                .build();

        repository.save(invite);

        logger.info("在线邀请申请成功，订单号: {}, 验证地址: {}", trimmedOrderNumber, trimmedVerifyAddress);

        result.put("success", true);
        result.put("message", "申请提交成功，请等待处理");

        return result;
    }

    /**
     * 根据订单号查询申请状态
     *
     * @param orderNumber 订单号
     * @return 查询结果
     */
    public Map<String, Object> queryByOrderNumber(String orderNumber) {
        Map<String, Object> result = new HashMap<>();

        if (orderNumber == null || orderNumber.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "订单号不能为空");
            return result;
        }

        Optional<GoogleOnlineInvite> inviteOpt = repository.findByOrderNumber(orderNumber.trim());
        if (!inviteOpt.isPresent()) {
            result.put("success", false);
            result.put("message", "未找到该订单号对应的申请记录");
            return result;
        }

        GoogleOnlineInvite invite = inviteOpt.get();
        logger.info("订单号查询成功，订单号: {}", orderNumber.trim());

        result.put("success", true);
        result.put("message", "查询成功");
        result.put("data", convertToMap(invite));

        return result;
    }

    /**
     * 获取所有记录（管理员）
     *
     * @return 记录列表
     */
    public List<Map<String, Object>> getAllRecords() {
        List<GoogleOnlineInvite> invites = repository.findAllOrderByStatusAndSubmitTime();
        return invites.stream().map(this::convertToMap).collect(Collectors.toList());
    }

    /**
     * 获取统计信息
     *
     * @return 统计数据
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("pending", repository.countByStatus(OnlineInviteStatus.PENDING));
        stats.put("submitted", repository.countByStatus(OnlineInviteStatus.SUBMITTED));
        stats.put("processed", repository.countByStatus(OnlineInviteStatus.PROCESSED));
        stats.put("rejected", repository.countByStatus(OnlineInviteStatus.REJECTED));
        stats.put("total", repository.count());
        return stats;
    }

    /**
     * 确认处理完成（管理员）
     *
     * @param id 记录ID
     * @return 处理结果
     */
    public Map<String, Object> confirmProcess(Long id) {
        Map<String, Object> result = new HashMap<>();

        Optional<GoogleOnlineInvite> inviteOpt = repository.findById(id);
        if (!inviteOpt.isPresent()) {
            result.put("success", false);
            result.put("message", "记录不存在");
            return result;
        }

        GoogleOnlineInvite invite = inviteOpt.get();

        if (invite.getStatus() != OnlineInviteStatus.SUBMITTED) {
            result.put("success", false);
            result.put("message", "只有待处理状态的记录才能确认处理");
            return result;
        }

        invite.confirmProcess();
        repository.save(invite);

        logger.info("处理确认成功，ID: {}, 订单号: {}", id, invite.getOrderNumber());

        result.put("success", true);
        result.put("message", "处理确认成功");
        return result;
    }

    /**
     * 驳回申请（管理员）
     *
     * @param id 记录ID
     * @param reason 驳回原因
     * @return 驳回结果
     */
    public Map<String, Object> reject(Long id, String reason) {
        Map<String, Object> result = new HashMap<>();

        Optional<GoogleOnlineInvite> inviteOpt = repository.findById(id);
        if (!inviteOpt.isPresent()) {
            result.put("success", false);
            result.put("message", "记录不存在");
            return result;
        }

        GoogleOnlineInvite invite = inviteOpt.get();
        invite.reject(reason);
        repository.save(invite);

        logger.info("申请驳回成功，ID: {}, 原因: {}", id, reason);

        result.put("success", true);
        result.put("message", "申请已驳回");
        return result;
    }

    /**
     * 删除记录（管理员）
     *
     * @param id 记录ID
     * @return 删除结果
     */
    public Map<String, Object> delete(Long id) {
        Map<String, Object> result = new HashMap<>();

        Optional<GoogleOnlineInvite> inviteOpt = repository.findById(id);
        if (!inviteOpt.isPresent()) {
            result.put("success", false);
            result.put("message", "记录不存在");
            return result;
        }

        repository.deleteById(id);

        logger.info("记录删除成功，ID: {}", id);

        result.put("success", true);
        result.put("message", "记录已删除");
        return result;
    }

    /**
     * URL 格式验证
     *
     * @param url URL 地址
     * @return 是否有效
     */
    public boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        return URL_PATTERN.matcher(url.trim()).matches();
    }

    /**
     * 将实体转换为 Map
     *
     * @param invite 邀请实体
     * @return Map
     */
    private Map<String, Object> convertToMap(GoogleOnlineInvite invite) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", invite.getId());
        map.put("orderNumber", invite.getOrderNumber());
        map.put("verifyAddress", invite.getVerifyAddress());
        map.put("status", invite.getStatus().name());
        map.put("statusDescription", invite.getStatus().getDescription());
        map.put("createTime", invite.getCreateTime() != null ? invite.getCreateTime().format(DATE_TIME_FORMATTER) : null);
        map.put("submitTime", invite.getSubmitTime() != null ? invite.getSubmitTime().format(DATE_TIME_FORMATTER) : null);
        map.put("processTime", invite.getProcessTime() != null ? invite.getProcessTime().format(DATE_TIME_FORMATTER) : null);
        map.put("remarks", invite.getRemarks());
        map.put("rejectReason", invite.getRejectReason());
        map.put("canModify", invite.canModify());
        return map;
    }
}
