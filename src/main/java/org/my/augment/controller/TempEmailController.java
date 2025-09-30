package org.my.augment.controller;

import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import javax.servlet.http.HttpServletRequest;
import org.my.augment.entity.AuthKey;
import org.my.augment.entity.EmailLog;
import org.my.augment.entity.TempEmail;
import org.my.augment.service.AuthService;
import org.my.augment.service.EmailService;
import org.my.augment.service.TempEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 临时邮箱管理控制器
 * 提供邮箱管理相关的REST API接口
 * 
 * @author 杨宇帆
 * @create 2025-08-02
 */
@RestController
@RequestMapping("/api/temp-email")
public class TempEmailController {

    private static final Logger logger = LoggerFactory.getLogger(TempEmailController.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String EMAIL_DOMAIN = "@supercourier.top";
    private static final String[] FIRST_NAMES = {
        "Emma", "Olivia", "Liam", "Noah", "Ava", "Sophia",
        "Mason", "Isabella", "Ethan", "Mia", "Lucas", "Amelia"
    };
    private static final String[] LAST_NAMES = {
        "Smith", "Johnson", "Brown", "Taylor", "Anderson", "Thomas",
        "Jackson", "White", "Harris", "Martin", "Thompson", "Garcia"
    };
    private static final int MIN_AGE = 25;
    private static final int MAX_AGE = 60;
    private static final DateTimeFormatter BIRTHDAY_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    @Autowired
    private TempEmailService tempEmailService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private AuthService authService;

    /**
     * 获取有效邮箱列表（支持分页）
     * 按剩余时间从多到少排序，不显示过期和封禁的邮箱
     *
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @param request HTTP请求对象
     * @return 有效邮箱列表
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getActiveEmailList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        try {
            // 获取当前用户的授权key
            String authKey = LoginController.getCurrentAuthKey(request);
            if (authKey == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "用户未登录或会话已过期");
                return ResponseEntity.status(401).body(errorResponse);
            }

            logger.info("获取有效邮箱列表请求，授权key: {}, 页码: {}, 每页大小: {}", authKey, page, size);

            // 转换为Spring Data JPA的页码（从0开始）
            int pageIndex = Math.max(0, page - 1);

            Map<String, Object> pageData = tempEmailService.getActiveEmailsPage(pageIndex, size, authKey);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", pageData);
            response.put("message", "获取邮箱列表成功");

            logger.info("成功获取邮箱列表，授权key: {}, 页码: {}, 每页大小: {}, 总数: {}",
                       authKey, page, size, pageData.get("totalElements"));
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("获取邮箱列表失败: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取邮箱列表失败: " + e.getMessage());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 生成新邮箱并自动加入邮箱库
     * 调用AugmentController的temp-email接口生成邮箱，然后自动保存到数据库
     *
     * @param request HTTP请求对象
     * @return 生成结果
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateNewEmail(HttpServletRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            // 获取当前用户的授权key
            String authKey = LoginController.getCurrentAuthKey(request);
            if (authKey == null) {
                // 记录新增邮箱失败日志
                authService.logEmailOperation("", EmailLog.OperationType.ADD_EMAIL,
                                            null, EmailLog.OperationResult.FAILED,
                                            "用户未登录或会话已过期", request, System.currentTimeMillis() - startTime);

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "用户未登录或会话已过期");
                return ResponseEntity.status(401).body(errorResponse);
            }

            logger.info("生成新邮箱请求，授权key: {}", authKey);

            // 先获取当前用户信息，用于计算更新后的剩余次数
            AuthService.AuthValidationResult currentValidationResult = authService.validateAuthKeyForLogin(authKey);
            if (!currentValidationResult.isSuccess()) {
                // 记录新增邮箱失败日志
                authService.logEmailOperation(authKey, EmailLog.OperationType.ADD_EMAIL,
                                            null, EmailLog.OperationResult.FAILED,
                                            "认证密钥验证失败: " + currentValidationResult.getMessage(), request, System.currentTimeMillis() - startTime);

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "认证密钥验证失败: " + currentValidationResult.getMessage());
                return ResponseEntity.status(401).body(errorResponse);
            }

            // 调用AugmentController生成临时邮箱
            String generatedEmail = generateTempEmailFromAugmentController(authKey, request);

            if (generatedEmail == null) {
                // 记录新增邮箱失败日志
                authService.logEmailOperation(authKey, EmailLog.OperationType.ADD_EMAIL,
                                            null, EmailLog.OperationResult.FAILED,
                                            "生成邮箱失败", request, System.currentTimeMillis() - startTime);

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "生成邮箱失败");
                return ResponseEntity.status(500).body(errorResponse);
            }

            // 自动将生成的邮箱加入邮箱库
            TempEmail tempEmail = tempEmailService.createTempEmail(generatedEmail, null, authKey);

            // 记录新增邮箱成功日志
            authService.logEmailOperation(authKey, EmailLog.OperationType.ADD_EMAIL,
                                        generatedEmail, EmailLog.OperationResult.SUCCESS,
                                        null, request, System.currentTimeMillis() - startTime);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("emailAddress", generatedEmail);
            response.put("data", convertToEmailMap(tempEmail));
            response.put("message", "邮箱生成成功并已自动加入邮箱库");

            // 计算更新后的剩余次数（因为已经使用了一次）
            if (currentValidationResult.isSuperAuth()) {
                response.put("remainingCount", -1);
                response.put("maxCount", -1);
            } else {
                AuthKey authKeyEntity = currentValidationResult.getAuthKey();
                int newRemainingCount = Math.max(0, authKeyEntity.getRemainingCount() - 1);
                response.put("remainingCount", newRemainingCount);
                response.put("maxCount", authKeyEntity.getMaxCount());
            }

            logger.info("成功生成邮箱并加入邮箱库: {}, 授权key: {}", generatedEmail, authKey);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("生成邮箱失败: {}", e.getMessage());

            // 记录新增邮箱失败日志
            String authKey = LoginController.getCurrentAuthKey(request);
            authService.logEmailOperation(authKey != null ? authKey : "", EmailLog.OperationType.ADD_EMAIL,
                                        null, EmailLog.OperationResult.FAILED,
                                        e.getMessage(), request, System.currentTimeMillis() - startTime);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            logger.error("生成邮箱失败: {}", e.getMessage(), e);

            // 记录新增邮箱异常日志
            String authKey = LoginController.getCurrentAuthKey(request);
            authService.logEmailOperation(authKey != null ? authKey : "", EmailLog.OperationType.ADD_EMAIL,
                                        null, EmailLog.OperationResult.FAILED,
                                        "生成邮箱失败: " + e.getMessage(), request, System.currentTimeMillis() - startTime);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "生成邮箱失败: " + e.getMessage());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 将邮箱加入邮箱库
     *
     * @param emailAddress 邮箱地址
     * @param remarks 备注信息（可选）
     * @param request HTTP请求对象
     * @return 添加结果
     */
    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addEmailToLibrary(@RequestParam String emailAddress,
                                                               @RequestParam(required = false) String remarks,
                                                               HttpServletRequest request) {
        try {
            // 获取当前用户的授权key
            String authKey = LoginController.getCurrentAuthKey(request);
            if (authKey == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "用户未登录或会话已过期");
                return ResponseEntity.status(401).body(errorResponse);
            }

            logger.info("加入邮箱库请求，授权key: {}, 邮箱: {}, 备注: {}", authKey, emailAddress, remarks);

            // 保存到数据库
            TempEmail tempEmail = tempEmailService.createTempEmail(emailAddress, remarks, authKey);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", convertToEmailMap(tempEmail));
            response.put("message", "邮箱已加入邮箱库");

            logger.info("成功加入邮箱库: {}, 授权key: {}", emailAddress, authKey);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("加入邮箱库失败: {}", e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            logger.error("加入邮箱库失败: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "加入邮箱库失败: " + e.getMessage());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }



    /**
     * 封禁邮箱
     *
     * @param emailId 邮箱ID
     * @param request HTTP请求对象
     * @return 封禁结果
     */
    @PostMapping("/ban/{emailId}")
    public ResponseEntity<Map<String, Object>> banEmail(@PathVariable Long emailId, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            // 获取当前用户的授权key
            String authKey = LoginController.getCurrentAuthKey(request);
            if (authKey == null) {
                // 记录封禁邮箱失败日志
                authService.logEmailOperation("", EmailLog.OperationType.BAN_EMAIL,
                                            null, EmailLog.OperationResult.FAILED,
                                            "用户未登录或会话已过期", request, System.currentTimeMillis() - startTime);

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "用户未登录或会话已过期");
                return ResponseEntity.status(401).body(errorResponse);
            }

            logger.info("封禁邮箱请求，ID: {}, 授权key: {}", emailId, authKey);

            // 先获取邮箱信息用于日志记录
            Optional<TempEmail> tempEmailOpt = tempEmailService.findById(emailId);
            String emailAddress = tempEmailOpt.map(TempEmail::getEmailAddress).orElse(null);

            boolean success = tempEmailService.banEmail(emailId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", success);

            if (success) {
                // 记录封禁邮箱成功日志
                authService.logEmailOperation(authKey, EmailLog.OperationType.BAN_EMAIL,
                                            emailAddress, EmailLog.OperationResult.SUCCESS,
                                            null, request, System.currentTimeMillis() - startTime);

                response.put("message", "邮箱封禁成功");
                logger.info("成功封禁邮箱，ID: {}, 邮箱: {}", emailId, emailAddress);
                return ResponseEntity.ok(response);
            } else {
                // 记录封禁邮箱失败日志
                authService.logEmailOperation(authKey, EmailLog.OperationType.BAN_EMAIL,
                                            emailAddress, EmailLog.OperationResult.FAILED,
                                            "邮箱不存在或已是封禁状态", request, System.currentTimeMillis() - startTime);

                response.put("message", "邮箱封禁失败，邮箱不存在或已是封禁状态");
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            logger.error("封禁邮箱失败: {}", e.getMessage(), e);

            // 记录封禁邮箱异常日志
            String authKey = LoginController.getCurrentAuthKey(request);
            authService.logEmailOperation(authKey != null ? authKey : "", EmailLog.OperationType.BAN_EMAIL,
                                        null, EmailLog.OperationResult.FAILED,
                                        "封禁邮箱失败: " + e.getMessage(), request, System.currentTimeMillis() - startTime);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "封禁邮箱失败: " + e.getMessage());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 获取邮箱验证码
     * 调用EmailService获取验证码，支持重试机制
     *
     * @param emailAddress 邮箱地址
     * @param request HTTP请求对象
     * @return 验证码内容
     */
    @GetMapping("/verification-code")
    public ResponseEntity<Map<String, Object>> getVerificationCode(@RequestParam String emailAddress,
                                                                  HttpServletRequest request) {
        try {
            // 获取当前用户的授权key
            String authKey = LoginController.getCurrentAuthKey(request);
            if (authKey == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "用户未登录或会话已过期");
                return ResponseEntity.status(401).body(errorResponse);
            }

            logger.info("获取验证码请求，授权key: {}, 邮箱: {}", authKey, emailAddress);

            // 对于临时邮箱库中的邮箱，检查是否有效
            Optional<TempEmail> tempEmailOpt = tempEmailService.findByEmailAddress(emailAddress, authKey);
            if (tempEmailOpt.isPresent() && !tempEmailService.isEmailValid(emailAddress, authKey)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "邮箱已过期或已封禁");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 调用EmailService获取验证码
            String verificationCode = emailService.getVerificationCode(emailAddress);

            Map<String, Object> response = new HashMap<>();

            if (verificationCode != null && !verificationCode.trim().isEmpty()) {
                response.put("success", true);
                response.put("verificationCode", verificationCode);
                response.put("message", "获取验证码成功");

                logger.info("成功获取验证码: {}", verificationCode);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "未找到验证码邮件");

                logger.warn("未找到验证码邮件，邮箱: {}", emailAddress);
                return ResponseEntity.ok(response); // 返回200状态码，让前端处理重试
            }

        } catch (Exception e) {
            logger.error("获取验证码失败: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取验证码失败: " + e.getMessage());

            return ResponseEntity.ok(errorResponse); // 返回200状态码，让前端处理重试
        }
    }

    /**
     * 获取邮箱校验地址
     * 调用EmailService获取校验地址，支持重试机制
     *
     * @param emailAddress 邮箱地址
     * @param request HTTP请求对象
     * @return 校验地址内容
     */
    @GetMapping("/verification-url")
    public ResponseEntity<Map<String, Object>> getVerificationUrl(@RequestParam String emailAddress,
                                                                 HttpServletRequest request) {
        try {
            // 获取当前用户的授权key
            String authKey = LoginController.getCurrentAuthKey(request);
            if (authKey == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "用户未登录或会话已过期");
                return ResponseEntity.status(401).body(errorResponse);
            }

            logger.info("获取校验地址请求，授权key: {}, 邮箱: {}", authKey, emailAddress);

            // 对于临时邮箱库中的邮箱，检查是否有效
            Optional<TempEmail> tempEmailOpt = tempEmailService.findByEmailAddress(emailAddress, authKey);
            if (tempEmailOpt.isPresent() && !tempEmailService.isEmailValid(emailAddress, authKey)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "邮箱已过期或已封禁");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 调用EmailService获取校验地址
            String verificationUrl = emailService.getVerificationUrl(emailAddress);

            Map<String, Object> response = new HashMap<>();

            if (verificationUrl != null && !verificationUrl.trim().isEmpty()) {
                response.put("success", true);
                response.put("verificationUrl", verificationUrl);
                response.put("message", "获取校验地址成功");

                logger.info("成功获取校验地址: {}", verificationUrl);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "未找到校验地址邮件");

                logger.warn("未找到校验地址邮件，邮箱: {}", emailAddress);
                return ResponseEntity.ok(response); // 返回200状态码，让前端处理重试
            }

        } catch (Exception e) {
            logger.error("获取校验地址失败: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取校验地址失败: " + e.getMessage());

            return ResponseEntity.ok(errorResponse); // 返回200状态码，让前端处理重试
        }
    }

    /**
     * 删除指定邮箱的所有邮件
     * 调用EmailService删除邮箱中的所有邮件
     *
     * @param emailAddress 邮箱地址
     * @param request HTTP请求对象
     * @return 删除结果
     */
    @PostMapping("/delete-all-emails")
    public ResponseEntity<Map<String, Object>> deleteAllEmails(@RequestParam String emailAddress,
                                                              HttpServletRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            // 获取当前用户的授权key
            String authKey = LoginController.getCurrentAuthKey(request);
            if (authKey == null) {
                // 记录删除所有邮件失败日志
                authService.logEmailOperation("", EmailLog.OperationType.DELETE_ALL_EMAILS,
                                            emailAddress, EmailLog.OperationResult.FAILED,
                                            "用户未登录或会话已过期", request, System.currentTimeMillis() - startTime);

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "用户未登录或会话已过期");
                return ResponseEntity.status(401).body(errorResponse);
            }

            logger.info("删除所有邮件请求，授权key: {}, 邮箱: {}", authKey, emailAddress);

            // 对于临时邮箱库中的邮箱，检查是否有效
            Optional<TempEmail> tempEmailOpt = tempEmailService.findByEmailAddress(emailAddress, authKey);
            if (tempEmailOpt.isPresent() && !tempEmailService.isEmailValid(emailAddress, authKey)) {
                // 记录删除所有邮件失败日志
                authService.logEmailOperation(authKey, EmailLog.OperationType.DELETE_ALL_EMAILS,
                                            emailAddress, EmailLog.OperationResult.FAILED,
                                            "邮箱已过期或已封禁", request, System.currentTimeMillis() - startTime);

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "邮箱已过期或已封禁");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 调用EmailService删除所有邮件
            int deletedCount = emailService.deleteAllEmailsForAddress(emailAddress);

            Map<String, Object> response = new HashMap<>();

            if (deletedCount >= 0) {
                // 记录删除所有邮件成功日志
                authService.logEmailOperation(authKey, EmailLog.OperationType.DELETE_ALL_EMAILS,
                                            emailAddress, EmailLog.OperationResult.SUCCESS,
                                            String.format("成功删除 %d 封邮件", deletedCount), request, System.currentTimeMillis() - startTime);

                response.put("success", true);
                response.put("deletedCount", deletedCount);
                response.put("message", deletedCount > 0
                    ? String.format("成功删除 %d 封邮件", deletedCount)
                    : "该邮箱暂无邮件需要删除");

                logger.info("成功删除邮箱 {} 的 {} 封邮件", emailAddress, deletedCount);
                return ResponseEntity.ok(response);
            } else {
                // 记录删除所有邮件失败日志
                authService.logEmailOperation(authKey, EmailLog.OperationType.DELETE_ALL_EMAILS,
                                            emailAddress, EmailLog.OperationResult.FAILED,
                                            "删除邮件失败，请稍后重试", request, System.currentTimeMillis() - startTime);

                response.put("success", false);
                response.put("message", "删除邮件失败，请稍后重试");

                logger.warn("删除邮箱 {} 的邮件失败", emailAddress);
                return ResponseEntity.ok(response); // 返回200状态码，让前端处理
            }

        } catch (Exception e) {
            logger.error("删除邮件失败: {}", e.getMessage(), e);

            // 记录删除所有邮件异常日志
            String authKey = LoginController.getCurrentAuthKey(request);
            authService.logEmailOperation(authKey != null ? authKey : "", EmailLog.OperationType.DELETE_ALL_EMAILS,
                                        emailAddress, EmailLog.OperationResult.FAILED,
                                        "删除邮件失败: " + e.getMessage(), request, System.currentTimeMillis() - startTime);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "删除邮件失败: " + e.getMessage());

            return ResponseEntity.ok(errorResponse); // 返回200状态码，让前端处理
        }
    }

    /**
     * 获取邮箱统计信息
     *
     * @param request HTTP请求对象
     * @return 统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getEmailStatistics(HttpServletRequest request) {
        try {
            // 获取当前用户的授权key
            String authKey = LoginController.getCurrentAuthKey(request);
            if (authKey == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "用户未登录或会话已过期");
                return ResponseEntity.status(401).body(errorResponse);
            }

            TempEmailService.EmailStatistics statistics = tempEmailService.getEmailStatistics(authKey);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", statistics);
            response.put("message", "获取统计信息成功");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("获取统计信息失败: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取统计信息失败: " + e.getMessage());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 将TempEmail实体转换为前端需要的Map格式
     *
     * @param email 邮箱实体
     * @return Map格式的邮箱信息
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
        emailMap.put("createTime", email.getCreateTime().format(DATE_TIME_FORMATTER));
        emailMap.put("generatedHours", email.getGeneratedHours());
        emailMap.put("generatedDays", email.getGeneratedDays());
        emailMap.put("isExpired", email.isExpired());
        emailMap.put("isAvailable", email.isAvailable());

        emailMap.put("remarks", email.getRemarks());

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
     * 使用随机姓名与生日构造可信的临时邮箱地址。
     *
     * @return 包含域名的完整邮箱地址
     */
    private String buildCredibleTempEmail() {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
        String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];

        LocalDate now = LocalDate.now();
        int age = random.nextInt(MIN_AGE, MAX_AGE + 1);
        int birthYear = now.getYear() - age;
        int dayOfYear = random.nextInt(1, Year.of(birthYear).length() + 1);
        LocalDate birthday = LocalDate.ofYearDay(birthYear, dayOfYear);

        String localPart = (firstName  + lastName + birthday.format(BIRTHDAY_FORMATTER)).toLowerCase(Locale.ROOT);
        int numericSuffix = random.nextInt(10, 100);

        return localPart + numericSuffix + EMAIL_DOMAIN;
    }

    /**
     * 生成可信的临时邮箱地址，复用认证逻辑并扣减密钥额度。
     *
     * @param authKey 当前用户的认证密钥
     * @param request HTTP 请求对象
     * @return 生成的临时邮箱地址
     */
    private String generateTempEmailFromAugmentController(String authKey, HttpServletRequest request) {
        try {
            // 验证认证密钥
            AuthService.AuthValidationResult validationResult =
                    authService.validateAuthKey(authKey);
            if (!validationResult.isSuccess()) {
                logger.warn("认证失败: {}", validationResult.getMessage());
                return null;
            }

            // 先尝试更新认证密钥使用次数，确保还有剩余次数
            boolean useResult = authService.useAuthKey(authKey, request);
            if (!useResult) {
                logger.warn("认证密钥使用次数已达上限或更新失败: {}", authKey);
                return null;
            }

            String generatedEmail = buildCredibleTempEmail();
            logger.info("成功生成临时邮箱: {}, 授权密钥使用次数已更新", generatedEmail);

            return generatedEmail;

        } catch (Exception e) {
            logger.error("生成临时邮箱失败: {}", e.getMessage(), e);
            return null;
        }
    }
}
