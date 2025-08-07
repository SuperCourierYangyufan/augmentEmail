package org.my.augment.controller;

import java.util.Random;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.my.augment.service.EmailService;
import org.my.augment.service.AuthService;
import org.my.augment.entity.EmailLog;

/**
 * Augment控制器
 * 提供各种增强功能的API接口
 *
 * @author 杨宇帆
 * @create 2025-07-25
 */
@RestController
public class AugmentController {

    private static final Logger logger = LoggerFactory.getLogger(AugmentController.class);

    private static final String EMAIL_DOMAIN = "@supercourier.top";
    private static final String LETTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String ALPHANUMERIC = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private final Random random = new Random();

    @Autowired
    private EmailService emailService;

    @Autowired
    private AuthService authService;

    /**
     * 生成临时邮箱地址
     * 邮箱格式：随机字母开头 + 当前时间戳 + 随机字符或数字10位 + @supercourier.top
     * 需要提供有效的authKey进行认证
     *
     * @param authKey 认证密钥
     * @param request HTTP请求对象
     * @return 生成的临时邮箱地址
     */
    @GetMapping("/temp-email")
    public ResponseEntity<String> generateTempEmail(@RequestParam String authKey,
                                                   HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String generatedEmail = null;

        try {
            logger.info("收到生成临时邮箱请求，authKey: {}", authKey);

            // 1. 验证认证密钥
            AuthService.AuthValidationResult validationResult = authService.validateAuthKey(authKey);
            if (!validationResult.isSuccess()) {
                logger.warn("认证失败: {}", validationResult.getMessage());

                // 记录失败日志
                authService.logEmailOperation(authKey, EmailLog.OperationType.GENERATE_EMAIL,
                                            null, EmailLog.OperationResult.FAILED,
                                            validationResult.getMessage(), request,
                                            System.currentTimeMillis() - startTime);

                return ResponseEntity.badRequest().body("认证失败: " + validationResult.getMessage());
            }

            // 2. 生成临时邮箱
            StringBuilder emailBuilder = new StringBuilder();

            // 随机字母开头
            char randomLetter = LETTERS.charAt(random.nextInt(LETTERS.length()));
            emailBuilder.append(randomLetter);

            // 当前时间戳
            long timestamp = System.currentTimeMillis();
            emailBuilder.append(timestamp);

            // 随机字符或数字10位
            for (int i = 0; i < 10; i++) {
                char randomChar = ALPHANUMERIC.charAt(random.nextInt(ALPHANUMERIC.length()));
                emailBuilder.append(randomChar);
            }

            // 添加域名
            emailBuilder.append(EMAIL_DOMAIN);

            generatedEmail = emailBuilder.toString();

            // 3. 更新认证密钥使用次数
            authService.useAuthKey(authKey, request);

            // 4. 记录成功日志
            authService.logEmailOperation(authKey, EmailLog.OperationType.GENERATE_EMAIL,
                                        generatedEmail, EmailLog.OperationResult.SUCCESS,
                                        null, request, System.currentTimeMillis() - startTime);

            logger.info("临时邮箱生成成功: {}", generatedEmail);
            return ResponseEntity.ok(generatedEmail);

        } catch (Exception e) {
            logger.error("生成临时邮箱时发生异常: {}", e.getMessage(), e);

            // 记录异常日志
            authService.logEmailOperation(authKey, EmailLog.OperationType.GENERATE_EMAIL,
                                        generatedEmail, EmailLog.OperationResult.FAILED,
                                        e.getMessage(), request, System.currentTimeMillis() - startTime);

            return ResponseEntity.status(500).body("生成临时邮箱失败: " + e.getMessage());
        }
    }

    /**
     * 获取指定邮箱地址的最新验证码邮件
     * 使用EmailService获取验证码，复用邮箱连接
     * 需要提供有效的authKey进行认证
     *
     * @param authKey 认证密钥
     * @param emailAddress 目标邮箱地址
     * @param request HTTP请求对象
     * @return 验证码内容，如果没有找到则返回错误信息
     */
    @GetMapping("/verification-code")
    public ResponseEntity<String> getVerificationCode(@RequestParam String authKey,
                                                     @RequestParam String emailAddress,
                                                     HttpServletRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            logger.info("收到获取验证码请求，authKey: {}, 目标邮箱: {}", authKey, emailAddress);

            // 1. 验证认证密钥
            AuthService.AuthValidationResult validationResult = authService.validateAuthKey(authKey);
            if (!validationResult.isSuccess()) {
                logger.warn("认证失败: {}", validationResult.getMessage());

                // 记录失败日志
                authService.logEmailOperation(authKey, EmailLog.OperationType.GET_VERIFICATION,
                                            emailAddress, EmailLog.OperationResult.FAILED,
                                            validationResult.getMessage(), request,
                                            System.currentTimeMillis() - startTime);

                return ResponseEntity.badRequest().body("认证失败: " + validationResult.getMessage());
            }

            // 2. 使用EmailService获取验证码
            String verificationCode = emailService.getVerificationCode(emailAddress);

            if (verificationCode != null) {
                // 3. 更新认证密钥使用次数
                authService.useAuthKey(authKey, request);

                // 4. 记录成功日志
                authService.logEmailOperation(authKey, EmailLog.OperationType.GET_VERIFICATION,
                                            emailAddress, EmailLog.OperationResult.SUCCESS,
                                            null, request, System.currentTimeMillis() - startTime);

                logger.info("成功获取验证码: {}", verificationCode);
                return ResponseEntity.ok(verificationCode);
            } else {
                logger.warn("未能获取到验证码");

                // 记录失败日志
                authService.logEmailOperation(authKey, EmailLog.OperationType.GET_VERIFICATION,
                                            emailAddress, EmailLog.OperationResult.FAILED,
                                            "未找到验证码", request, System.currentTimeMillis() - startTime);

                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            logger.error("获取验证码时发生异常: {}", e.getMessage(), e);

            // 记录异常日志
            authService.logEmailOperation(authKey, EmailLog.OperationType.GET_VERIFICATION,
                                        emailAddress, EmailLog.OperationResult.FAILED,
                                        e.getMessage(), request, System.currentTimeMillis() - startTime);

            return ResponseEntity.status(500).body("获取验证码失败: " + e.getMessage());
        }
    }

}
