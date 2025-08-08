package org.my.augment.service;

import org.my.augment.entity.TempEmail;
import org.my.augment.repository.TempEmailRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 临时邮箱业务逻辑层
 * 提供邮箱管理的核心业务功能
 * 通过SQL查询实现过期状态判断，无需定时任务
 * 
 * @author 杨宇帆
 * @create 2025-08-02
 */
@Service
public class TempEmailService {

    private static final Logger logger = LoggerFactory.getLogger(TempEmailService.class);

    @Autowired
    private TempEmailRepository tempEmailRepository;

    /**
     * 超级授权key配置
     */
    @Value("${app.super-auth-key:yangyufan}")
    private String superAuthKey;

    /**
     * 检查是否为超级授权key
     *
     * @param authKey 授权密钥
     * @return 是否为超级授权key
     */
    private boolean isSuperAuthKey(String authKey) {
        return superAuthKey != null && superAuthKey.equals(authKey);
    }

    /**
     * 创建新的临时邮箱记录
     *
     * @param emailAddress 邮箱地址
     * @param remarks 备注信息
     * @param authKey 授权密钥
     * @return 创建的邮箱记录
     */
    public TempEmail createTempEmail(String emailAddress, String remarks, String authKey) {
        logger.info("开始创建临时邮箱记录: {}, 授权key: {}", emailAddress, authKey);

        // 检查邮箱是否已存在
        Optional<TempEmail> existingEmail = tempEmailRepository.findByEmailAddress(emailAddress);
        if (existingEmail.isPresent()) {
            logger.warn("邮箱地址已存在: {}", emailAddress);
            throw new IllegalArgumentException("邮箱地址已存在: " + emailAddress);
        }

        // 创建新的邮箱记录
        TempEmail tempEmail = TempEmail.builder()
                .emailAddress(emailAddress)
                .status(TempEmail.EmailStatus.ACTIVE)
                .createTime(LocalDateTime.now())
                .remarks(remarks)
                .authKey(authKey)
                .build();

        TempEmail savedEmail = tempEmailRepository.save(tempEmail);
        logger.info("成功创建临时邮箱记录: {}, ID: {}, 授权key: {}", emailAddress, savedEmail.getId(), authKey);

        return savedEmail;
    }

    /**
     * 创建新的临时邮箱记录（兼容旧版本）
     *
     * @param emailAddress 邮箱地址
     * @param remarks 备注信息
     * @return 创建的邮箱记录
     */
    @Deprecated
    public TempEmail createTempEmail(String emailAddress, String remarks) {
        return createTempEmail(emailAddress, remarks, superAuthKey);
    }

    /**
     * 获取所有邮箱列表（包括封禁的）
     * 按创建时间降序排序（新的在前面）
     *
     * @param authKey 授权密钥
     * @return 邮箱列表
     */
    public List<TempEmail> getAllEmailsOrderByCreateTime(String authKey) {
        List<TempEmail> emails;

        if (isSuperAuthKey(authKey)) {
            // 超级授权key获取所有邮箱
            emails = tempEmailRepository.findAllEmailsOrderByCreateTimeDesc();
            logger.info("超级授权key查询到 {} 个邮箱", emails.size());
        } else {
            // 普通授权key只获取自己的邮箱
            emails = tempEmailRepository.findAllEmailsByAuthKeyOrderByCreateTimeDesc(authKey);
            logger.info("授权key {} 查询到 {} 个邮箱", authKey, emails.size());
        }

        return emails;
    }

    /**
     * 获取所有邮箱列表（兼容旧版本）
     * 按创建时间降序排序
     *
     * @return 邮箱列表
     */
    @Deprecated
    public List<TempEmail> getActiveEmailsOrderByRemainingTime() {
        return getAllEmailsOrderByCreateTime(superAuthKey);
    }

    /**
     * 分页获取邮箱列表
     *
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @param authKey 授权密钥
     * @return 分页数据
     */
    public Map<String, Object> getActiveEmailsPage(int page, int size, String authKey) {
        // 获取所有邮箱（包括封禁的）
        List<TempEmail> allEmails;
        if (isSuperAuthKey(authKey)) {
            // 超级授权key获取所有邮箱
            allEmails = tempEmailRepository.findAllEmailsOrderByCreateTimeDesc();
        } else {
            // 普通授权key只获取自己的邮箱
            allEmails = tempEmailRepository.findAllEmailsByAuthKeyOrderByCreateTimeDesc(authKey);
        }

        // 手动实现分页
        int totalElements = allEmails.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, totalElements);

        List<TempEmail> pageContent = allEmails.subList(startIndex, endIndex);

        // 转换为前端需要的格式
        List<Map<String, Object>> emailList = pageContent.stream()
                .map(this::convertToEmailMap)
                .collect(Collectors.toList());

        // 构建分页结果
        Map<String, Object> pageData = new HashMap<>();
        pageData.put("content", emailList);
        pageData.put("totalElements", totalElements);
        pageData.put("totalPages", totalPages);
        pageData.put("number", page);
        pageData.put("size", size);
        pageData.put("first", page == 0);
        pageData.put("last", page >= totalPages - 1);

        logger.info("分页查询有效邮箱，授权key: {}, 页码: {}, 每页大小: {}, 总数: {}", authKey, page, size, totalElements);
        return pageData;
    }

    /**
     * 分页获取有效邮箱列表（兼容旧版本）
     *
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 分页数据
     */
    @Deprecated
    public Map<String, Object> getActiveEmailsPage(int page, int size) {
        return getActiveEmailsPage(page, size, superAuthKey);
    }

    /**
     * 将TempEmail转换为前端需要的Map格式
     */
    private Map<String, Object> convertToEmailMap(TempEmail email) {
        Map<String, Object> emailMap = new HashMap<>();
        emailMap.put("id", email.getId());

        // 对封禁邮箱进行模糊处理
        String emailAddress = email.getEmailAddress();
        if (email.getStatus() == TempEmail.EmailStatus.BANNED) {
            emailAddress = maskEmailAddress(emailAddress);
        }
        emailMap.put("emailAddress", emailAddress);

        emailMap.put("status", email.getStatus().name());
        emailMap.put("statusDescription", email.getStatus().getDescription());
        emailMap.put("createTime", email.getCreateTime());
        emailMap.put("remarks", email.getRemarks());
        emailMap.put("isExpired", email.isExpired());

        // 计算已生成时间
        long generatedHours = email.getGeneratedHours();
        long generatedDays = email.getGeneratedDays();
        emailMap.put("generatedHours", generatedHours);
        emailMap.put("generatedDays", generatedDays);

        return emailMap;
    }

    /**
     * 对邮箱地址进行模糊处理
     * 例如：test@example.com -> t***@e*****.com
     */
    private String maskEmailAddress(String emailAddress) {
        if (emailAddress == null || emailAddress.isEmpty()) {
            return emailAddress;
        }

        int atIndex = emailAddress.indexOf('@');
        if (atIndex <= 0) {
            return emailAddress;
        }

        String localPart = emailAddress.substring(0, atIndex);
        String domainPart = emailAddress.substring(atIndex + 1);

        // 处理本地部分：保留第一个字符，其余用*替换
        String maskedLocal = localPart.length() > 1
            ? localPart.charAt(0) + "***"
            : localPart;

        // 处理域名部分：保留第一个字符和最后的.xxx，中间用*替换
        String maskedDomain;
        int lastDotIndex = domainPart.lastIndexOf('.');
        if (lastDotIndex > 0) {
            String domainName = domainPart.substring(0, lastDotIndex);
            String topLevelDomain = domainPart.substring(lastDotIndex);
            maskedDomain = domainName.charAt(0) + "*****" + topLevelDomain;
        } else {
            maskedDomain = domainPart.charAt(0) + "*****";
        }

        return maskedLocal + "@" + maskedDomain;
    }

    /**
     * 根据邮箱地址查找邮箱记录
     *
     * @param emailAddress 邮箱地址
     * @param authKey 授权密钥
     * @return 邮箱记录，如果不存在返回空
     */
    public Optional<TempEmail> findByEmailAddress(String emailAddress, String authKey) {
        if (isSuperAuthKey(authKey)) {
            // 超级授权key可以查找任何邮箱
            return tempEmailRepository.findByEmailAddress(emailAddress);
        } else {
            // 普通授权key只能查找自己的邮箱
            return tempEmailRepository.findByEmailAddressAndAuthKey(emailAddress, authKey);
        }
    }

    /**
     * 根据邮箱地址查找邮箱记录（兼容旧版本）
     *
     * @param emailAddress 邮箱地址
     * @return 邮箱记录，如果不存在返回空
     */
    @Deprecated
    public Optional<TempEmail> findByEmailAddress(String emailAddress) {
        return tempEmailRepository.findByEmailAddress(emailAddress);
    }

    /**
     * 根据邮箱ID查找邮箱记录
     *
     * @param emailId 邮箱ID
     * @return 邮箱记录，如果不存在返回空
     */
    public Optional<TempEmail> findById(Long emailId) {
        return tempEmailRepository.findById(emailId);
    }

    /**
     * 封禁指定的邮箱
     * 
     * @param emailId 邮箱ID
     * @return 是否封禁成功
     */
    public boolean banEmail(Long emailId) {
        logger.info("开始封禁邮箱, ID: {}", emailId);

        Optional<TempEmail> emailOpt = tempEmailRepository.findById(emailId);
        if (!emailOpt.isPresent()) {
            logger.warn("邮箱不存在, ID: {}", emailId);
            return false;
        }

        TempEmail email = emailOpt.get();
        if (email.getStatus() == TempEmail.EmailStatus.BANNED) {
            logger.warn("邮箱已经是封禁状态, ID: {}", emailId);
            return false;
        }

        email.ban();
        tempEmailRepository.save(email);
        
        logger.info("成功封禁邮箱: {}, ID: {}", email.getEmailAddress(), emailId);
        return true;
    }

    /**
     * 激活指定的邮箱
     * 
     * @param emailId 邮箱ID
     * @return 是否激活成功
     */
    public boolean activateEmail(Long emailId) {
        logger.info("开始激活邮箱, ID: {}", emailId);

        Optional<TempEmail> emailOpt = tempEmailRepository.findById(emailId);
        if (!emailOpt.isPresent()) {
            logger.warn("邮箱不存在, ID: {}", emailId);
            return false;
        }

        TempEmail email = emailOpt.get();
        if (email.isExpired()) {
            logger.warn("邮箱已过期，无法激活, ID: {}", emailId);
            return false;
        }

        email.activate();
        tempEmailRepository.save(email);
        
        logger.info("成功激活邮箱: {}, ID: {}", email.getEmailAddress(), emailId);
        return true;
    }



    /**
     * 检查邮箱是否有效（存在且未封禁）
     *
     * @param emailAddress 邮箱地址
     * @param authKey 授权密钥
     * @return 是否有效
     */
    public boolean isEmailValid(String emailAddress, String authKey) {
        if (isSuperAuthKey(authKey)) {
            // 超级授权key可以检查任何邮箱
            return tempEmailRepository.existsActiveEmail(emailAddress);
        } else {
            // 普通授权key只能检查自己的邮箱
            return tempEmailRepository.existsActiveEmailByAuthKey(emailAddress, authKey);
        }
    }

    /**
     * 检查邮箱是否有效（兼容旧版本）
     *
     * @param emailAddress 邮箱地址
     * @return 是否有效
     */
    @Deprecated
    public boolean isEmailValid(String emailAddress) {
        return tempEmailRepository.existsActiveEmail(emailAddress);
    }

    /**
     * 获取邮箱统计信息
     *
     * @param authKey 授权密钥
     * @return 统计信息
     */
    public EmailStatistics getEmailStatistics(String authKey) {
        long totalEmails, activeEmails, expiredEmails, bannedEmails;

        if (isSuperAuthKey(authKey)) {
            // 超级授权key获取所有统计信息
            totalEmails = tempEmailRepository.count();
            activeEmails = tempEmailRepository.countActiveEmails();
            expiredEmails = 0; // 不再有过期概念
            bannedEmails = tempEmailRepository.countByStatus(TempEmail.EmailStatus.BANNED);
        } else {
            // 普通授权key只获取自己的统计信息
            totalEmails = tempEmailRepository.countByAuthKeyAndStatus(authKey, TempEmail.EmailStatus.ACTIVE) +
                         tempEmailRepository.countByAuthKeyAndStatus(authKey, TempEmail.EmailStatus.BANNED);
            activeEmails = tempEmailRepository.countActiveEmailsByAuthKey(authKey);
            expiredEmails = 0; // 不再有过期概念
            bannedEmails = tempEmailRepository.countByAuthKeyAndStatus(authKey, TempEmail.EmailStatus.BANNED);
        }

        return EmailStatistics.builder()
                .totalEmails(totalEmails)
                .activeEmails(activeEmails)
                .expiredEmails(expiredEmails)
                .bannedEmails(bannedEmails)
                .build();
    }

    /**
     * 获取邮箱统计信息（兼容旧版本）
     *
     * @return 统计信息
     */
    @Deprecated
    public EmailStatistics getEmailStatistics() {
        return getEmailStatistics(superAuthKey);
    }



    /**
     * 删除邮箱记录
     * 
     * @param emailId 邮箱ID
     * @return 是否删除成功
     */
    public boolean deleteEmail(Long emailId) {
        logger.info("开始删除邮箱记录, ID: {}", emailId);

        if (!tempEmailRepository.existsById(emailId)) {
            logger.warn("邮箱不存在, ID: {}", emailId);
            return false;
        }

        tempEmailRepository.deleteById(emailId);
        logger.info("成功删除邮箱记录, ID: {}", emailId);
        return true;
    }

    /**
     * 邮箱统计信息内部类
     */
    public static class EmailStatistics {
        private long totalEmails;
        private long activeEmails;
        private long expiredEmails;
        private long bannedEmails;

        public static EmailStatisticsBuilder builder() {
            return new EmailStatisticsBuilder();
        }

        // Getters
        public long getTotalEmails() { return totalEmails; }
        public long getActiveEmails() { return activeEmails; }
        public long getExpiredEmails() { return expiredEmails; }
        public long getBannedEmails() { return bannedEmails; }

        // Builder pattern
        public static class EmailStatisticsBuilder {
            private long totalEmails;
            private long activeEmails;
            private long expiredEmails;
            private long bannedEmails;

            public EmailStatisticsBuilder totalEmails(long totalEmails) {
                this.totalEmails = totalEmails;
                return this;
            }

            public EmailStatisticsBuilder activeEmails(long activeEmails) {
                this.activeEmails = activeEmails;
                return this;
            }

            public EmailStatisticsBuilder expiredEmails(long expiredEmails) {
                this.expiredEmails = expiredEmails;
                return this;
            }

            public EmailStatisticsBuilder bannedEmails(long bannedEmails) {
                this.bannedEmails = bannedEmails;
                return this;
            }

            public EmailStatistics build() {
                EmailStatistics statistics = new EmailStatistics();
                statistics.totalEmails = this.totalEmails;
                statistics.activeEmails = this.activeEmails;
                statistics.expiredEmails = this.expiredEmails;
                statistics.bannedEmails = this.bannedEmails;
                return statistics;
            }
        }
    }
}
