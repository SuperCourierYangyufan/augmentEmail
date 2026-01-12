package org.my.augment.service;

import org.my.augment.entity.GoogleInvite;
import org.my.augment.repository.GoogleInviteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Google邀请业务逻辑层
 *
 * @author 杨宇帆
 * @create 2025-08-20
 */
@Service
public class GoogleInviteService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleInviteService.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private GoogleInviteRepository googleInviteRepository;

    /**
     * 域名配置
     */
    @Value("${app.invite.domain:http://localhost:8081}")
    private String inviteDomain;

    /**
     * 生成邀请链接
     *
     * @return 完整的邀请链接
     */
    public String generateInviteLink() {
        // 生成16位短UUID
        String inviteCode = generateShortUUID();

        // 确保唯一性
        while (googleInviteRepository.existsByInviteCode(inviteCode)) {
            inviteCode = generateShortUUID();
        }

        // 创建邀请记录
        GoogleInvite invite = GoogleInvite.builder()
                .inviteCode(inviteCode)
                .status(GoogleInvite.InviteStatus.PENDING)
                .createTime(LocalDateTime.now())
                .build();

        googleInviteRepository.save(invite);
        logger.info("生成邀请链接成功，邀请码: {}", inviteCode);

        return inviteDomain + "/invite/" + inviteCode;
    }

    /**
     * 生成16位短UUID
     *
     * @return 16位短UUID
     */
    private String generateShortUUID() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return uuid.substring(0, 16);
    }

    /**
     * 校验邀请码状态
     *
     * @param inviteCode 邀请码
     * @return 校验结果
     */
    public Map<String, Object> checkInviteCode(String inviteCode) {
        Map<String, Object> result = new HashMap<>();

        Optional<GoogleInvite> inviteOpt = googleInviteRepository.findByInviteCode(inviteCode);

        if (!inviteOpt.isPresent()) {
            result.put("valid", false);
            result.put("message", "邀请码不存在");
            return result;
        }

        GoogleInvite invite = inviteOpt.get();
        result.put("valid", true);
        result.put("status", invite.getStatus().name());
        result.put("statusDescription", invite.getStatus().getDescription());
        result.put("emailAddress", invite.getEmailAddress());
        result.put("canModify", invite.canModify());
        result.put("isEmailFilled", invite.isEmailFilled());
        result.put("rejectReason", invite.getRejectReason());

        return result;
    }

    /**
     * 提交邮箱信息
     *
     * @param inviteCode 邀请码
     * @param emailAddress 邮箱地址
     * @return 提交结果
     */
    public Map<String, Object> submitEmail(String inviteCode, String emailAddress) {
        Map<String, Object> result = new HashMap<>();

        // 校验邀请码
        Optional<GoogleInvite> inviteOpt = googleInviteRepository.findByInviteCode(inviteCode);
        if (!inviteOpt.isPresent()) {
            result.put("success", false);
            result.put("message", "邀请码不存在");
            return result;
        }

        GoogleInvite invite = inviteOpt.get();

        // 检查是否可修改
        if (!invite.canModify()) {
            result.put("success", false);
            result.put("message", "该邀请已处理，无法修改");
            return result;
        }

        // 校验邮箱格式
        if (!isValidEmail(emailAddress)) {
            result.put("success", false);
            result.put("message", "邮箱格式不正确");
            return result;
        }

        // 检查邮箱是否已被其他邀请使用（排除当前记录）
        Optional<GoogleInvite> existingInvite = googleInviteRepository.findByEmailAddress(emailAddress);
        if (existingInvite.isPresent() && !existingInvite.get().getId().equals(invite.getId())) {
            result.put("success", false);
            result.put("message", "该邮箱已被其他邀请使用");
            return result;
        }

        // 提交邮箱
        invite.submitEmail(emailAddress);
        googleInviteRepository.save(invite);

        logger.info("邮箱提交成功，邀请码: {}, 邮箱: {}", inviteCode, emailAddress);

        result.put("success", true);
        result.put("message", "邮箱提交成功，请等待邀请");
        result.put("status", invite.getStatus().name());

        return result;
    }

    /**
     * 更新邮箱信息
     *
     * @param inviteCode 邀请码
     * @param emailAddress 邮箱地址
     * @return 更新结果
     */
    public Map<String, Object> updateEmail(String inviteCode, String emailAddress) {
        // 逻辑与 submitEmail 相同，复用即可
        return submitEmail(inviteCode, emailAddress);
    }

    /**
     * 获取所有邀请列表（管理员）
     *
     * @return 邀请列表
     */
    public List<Map<String, Object>> getAllInvites() {
        List<GoogleInvite> invites = googleInviteRepository.findAllOrderByStatusAndFillTime();
        return invites.stream().map(this::convertToMap).collect(Collectors.toList());
    }

    /**
     * 确认邀请成功（管理员）
     *
     * @param id 邀请记录ID
     * @return 确认结果
     */
    public Map<String, Object> confirmInvite(Long id) {
        Map<String, Object> result = new HashMap<>();

        Optional<GoogleInvite> inviteOpt = googleInviteRepository.findById(id);
        if (!inviteOpt.isPresent()) {
            result.put("success", false);
            result.put("message", "邀请记录不存在");
            return result;
        }

        GoogleInvite invite = inviteOpt.get();

        if (invite.getStatus() != GoogleInvite.InviteStatus.SUBMITTED) {
            result.put("success", false);
            result.put("message", "只有待邀请状态的记录才能确认");
            return result;
        }

        invite.confirmInvite();
        googleInviteRepository.save(invite);

        logger.info("邀请确认成功，ID: {}, 邮箱: {}", id, invite.getEmailAddress());

        result.put("success", true);
        result.put("message", "邀请确认成功");
        return result;
    }

    /**
     * 驳回申请（管理员）
     * 驳回后用户可以看到原因并重新填写
     *
     * @param id 邀请记录ID
     * @param reason 驳回原因
     * @return 驳回结果
     */
    public Map<String, Object> rejectInvite(Long id, String reason) {
        Map<String, Object> result = new HashMap<>();

        Optional<GoogleInvite> inviteOpt = googleInviteRepository.findById(id);
        if (!inviteOpt.isPresent()) {
            result.put("success", false);
            result.put("message", "邀请记录不存在");
            return result;
        }

        GoogleInvite invite = inviteOpt.get();
        invite.reject(reason);
        googleInviteRepository.save(invite);

        logger.info("邀请驳回成功，ID: {}, 原因: {}", id, reason);

        result.put("success", true);
        result.put("message", "申请已驳回，用户可重新填写");
        return result;
    }

    /**
     * 删除邀请记录（管理员）
     *
     * @param id 邀请记录ID
     * @return 删除结果
     */
    public Map<String, Object> deleteInvite(Long id) {
        Map<String, Object> result = new HashMap<>();

        Optional<GoogleInvite> inviteOpt = googleInviteRepository.findById(id);
        if (!inviteOpt.isPresent()) {
            result.put("success", false);
            result.put("message", "邀请记录不存在");
            return result;
        }

        googleInviteRepository.deleteById(id);

        logger.info("邀请记录删除成功，ID: {}", id);

        result.put("success", true);
        result.put("message", "记录已删除");
        return result;
    }

    /**
     * 获取统计信息
     *
     * @return 统计数据
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("pending", googleInviteRepository.countByStatus(GoogleInvite.InviteStatus.PENDING));
        stats.put("submitted", googleInviteRepository.countByStatus(GoogleInvite.InviteStatus.SUBMITTED));
        stats.put("invited", googleInviteRepository.countByStatus(GoogleInvite.InviteStatus.INVITED));
        stats.put("cancelled", googleInviteRepository.countByStatus(GoogleInvite.InviteStatus.CANCELLED));
        stats.put("total", googleInviteRepository.count());
        return stats;
    }

    /**
     * 邮箱格式校验
     *
     * @param email 邮箱地址
     * @return 是否有效
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email.matches(emailRegex);
    }

    /**
     * 将实体转换为Map
     *
     * @param invite 邀请实体
     * @return Map
     */
    private Map<String, Object> convertToMap(GoogleInvite invite) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", invite.getId());
        map.put("inviteCode", invite.getInviteCode());
        map.put("emailAddress", invite.getEmailAddress());
        map.put("status", invite.getStatus().name());
        map.put("statusDescription", invite.getStatus().getDescription());
        map.put("createTime", invite.getCreateTime() != null ? invite.getCreateTime().format(DATE_TIME_FORMATTER) : null);
        map.put("fillTime", invite.getFillTime() != null ? invite.getFillTime().format(DATE_TIME_FORMATTER) : null);
        map.put("confirmTime", invite.getConfirmTime() != null ? invite.getConfirmTime().format(DATE_TIME_FORMATTER) : null);
        map.put("remarks", invite.getRemarks());
        map.put("rejectReason", invite.getRejectReason());
        map.put("canModify", invite.canModify());
        return map;
    }
}
