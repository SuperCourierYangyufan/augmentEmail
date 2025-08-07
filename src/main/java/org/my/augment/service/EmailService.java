package org.my.augment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.mail.*;
import javax.mail.search.RecipientStringTerm;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 邮箱服务类
 * 负责邮箱连接管理和邮件操作
 * 在应用启动时初始化连接，提供公共的邮件操作方法
 * 
 * @author 杨宇帆
 * @create 2025-07-25
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    // 139邮箱IMAP配置
    private static final String IMAP_HOST = "imap.139.com";
    private static final String IMAP_PORT = "993";
    private static final String EMAIL_USERNAME = "18827421758@139.com";
    private static final String EMAIL_AUTH_CODE = "2b5f751d3842ccddf700";
    
    // 验证码匹配正则表达式（通用）
    private static final Pattern VERIFICATION_CODE_PATTERN = Pattern.compile("\\b\\d{4,8}\\b");

    // Cursor邮件专用验证码匹配正则表达式
    // 匹配格式：Your one-time code is: 后面跟着的数字（可能有空格分隔）
    private static final Pattern CURSOR_VERIFICATION_CODE_PATTERN = Pattern.compile("Your one-time code is:\\s*([\\d\\s]+?)(?:\\s*---|\\.)");

    // 验证邮件关键字（不区分大小写）
    private static final String VERIFICATION_KEYWORD = "verification";

    // 验证第二个邮箱关键字
    private static final String VERIFICATION_KEYWORD_SECOND = "cursor";
    
    // 邮箱连接相关对象
    private Session session;
    private Store store;
    private Properties properties;
    
    /**
     * Spring Boot应用启动时初始化邮箱连接
     * 使用@PostConstruct注解确保在Bean创建后立即执行
     */
    @PostConstruct
    public void initializeEmailConnection() {
        try {
            logger.info("开始初始化139邮箱IMAP连接...");
            
            // 1. 配置IMAP连接属性
            properties = new Properties();
            properties.put("mail.store.protocol", "imaps");
            properties.put("mail.imaps.host", IMAP_HOST);
            properties.put("mail.imaps.port", IMAP_PORT);
            properties.put("mail.imaps.ssl.enable", "true");
            properties.put("mail.imaps.ssl.trust", "*");
            properties.put("mail.imaps.ssl.checkserveridentity", "false");
            properties.put("mail.imaps.ssl.protocols", "TLSv1.2");
            // 添加认证相关配置
            properties.put("mail.imaps.auth", "true");
            properties.put("mail.imaps.auth.login.disable", "false");
            properties.put("mail.imaps.auth.plain.disable", "false");
            // 设置连接超时
            properties.put("mail.imaps.connectiontimeout", "10000");
            properties.put("mail.imaps.timeout", "10000");
            
            // 2. 创建会话
            session = Session.getInstance(properties);
            
            // 3. 建立初始连接测试
            connectToStore();
            
            logger.info("139邮箱IMAP连接初始化成功");
            
        } catch (Exception e) {
            logger.error("初始化邮箱连接失败: {}", e.getMessage(), e);
            // 不抛出异常，避免影响应用启动，后续使用时再重试
        }
    }
    
    /**
     * 连接到邮箱存储
     * 包含重连机制
     */
    private void connectToStore() throws MessagingException {
        if (store != null && store.isConnected()) {
            return; // 已连接，无需重复连接
        }
        
        try {
            store = session.getStore("imaps");
            store.connect(IMAP_HOST, EMAIL_USERNAME, EMAIL_AUTH_CODE);
            logger.debug("成功连接到139邮箱服务器");
        } catch (MessagingException e) {
            logger.error("连接139邮箱失败，错误信息: {}", e.getMessage());
            logger.error("请检查：1.邮箱IMAP服务是否开启 2.授权码是否正确 3.网络连接是否正常");
            throw e;
        }
    }
    
    /**
     * 检查连接状态，如果断开则重新连接
     */
    private void ensureConnection() throws MessagingException {
        if (store == null || !store.isConnected()) {
            logger.info("邮箱连接已断开，正在重新连接...");
            connectToStore();
        }
    }
    
    /**
     * 获取指定邮箱地址的最新验证码邮件
     * 支持两种邮件格式：
     * 1. 包含"verification"关键字的邮件 - 使用通用正则表达式提取4-8位数字验证码
     * 2. 包含"sign up for cursor"关键字的邮件 - 使用专用正则表达式提取"Your one-time code is:"后的验证码
     * 按时间倒序遍历，返回最新的符合条件的验证码
     *
     * @param emailAddress 目标邮箱地址
     * @return 验证码内容，如果没有找到包含相关关键字的验证码邮件则返回null
     */
    public String getVerificationCode(String emailAddress) {
        Folder folder = null;
        
        try {
            logger.info("开始获取邮箱 {} 的验证码", emailAddress);
            
            // 1. 确保连接有效
            ensureConnection();
            
            // 2. 打开收件箱文件夹
            folder = store.getFolder("INBOX");
            folder.open(Folder.READ_ONLY);
            
            logger.info("成功打开收件箱，邮件总数: {}", folder.getMessageCount());
            
            // 3. 搜索发送给指定邮箱地址的邮件
            RecipientStringTerm recipientTerm = new RecipientStringTerm(Message.RecipientType.TO, emailAddress);
            Message[] messages = folder.search(recipientTerm);
            
            if (messages.length == 0) {
                logger.warn("未找到发送给邮箱 {} 的邮件", emailAddress);
                return null;
            }
            
            // 4. 按时间倒序遍历邮件，查找包含verification关键字的最新邮件
            logger.info("找到 {} 封相关邮件，开始按时间倒序查找包含verification关键字的邮件", messages.length);

            // 从最新邮件开始遍历
            for (int i = messages.length - 1; i >= 0; i--) {
                Message message = messages[i];
                logger.debug("正在处理第 {} 封邮件，发送时间: {}", (messages.length - i), message.getSentDate());

                try {
                    // 5. 提取邮件内容
                    String content = getTextContent(message);
                    if (content == null) {
                        logger.debug("无法获取第 {} 封邮件内容，跳过", (messages.length - i));
                        continue;
                    }

                    // 6. 检查邮件内容是否包含verification关键字（不区分大小写）
                    String lowerContent = content.toLowerCase();
                    boolean containsVerification = lowerContent.contains(VERIFICATION_KEYWORD);
                    boolean containsCursor = lowerContent.contains(VERIFICATION_KEYWORD_SECOND);

                    if (!containsVerification && !containsCursor) {
                        logger.debug("第 {} 封邮件不包含verification关键字，跳过", (messages.length - i));
                        continue;
                    }

                    logger.info("第 {} 封邮件包含verification关键字，开始提取验证码", (messages.length - i));

                    // 7. 根据邮件类型使用不同的正则表达式提取验证码
                    String verificationCode = null;

                    if (containsVerification) {
                        // 使用通用的验证码匹配模式
                        Matcher matcher = VERIFICATION_CODE_PATTERN.matcher(content);
                        if (matcher.find()) {
                            verificationCode = matcher.group();
                            logger.info("成功从包含verification关键字的邮件中提取验证码: {}", verificationCode);
                        }
                    } else if (containsCursor) {
                        // 使用Cursor专用的验证码匹配模式
                        Matcher matcher = CURSOR_VERIFICATION_CODE_PATTERN.matcher(content);
                        if (matcher.find()) {
                            verificationCode = matcher.group(1).replaceAll("\\s", ""); // 获取第一个捕获组并去掉所有空格
                            logger.info("成功从Cursor邮件中提取验证码: {}", verificationCode);
                        }
                    }

                    if (verificationCode != null) {
                        return verificationCode;
                    } else {
                        logger.debug("第 {} 封邮件包含关键字但未找到验证码，继续查找", (messages.length - i));
                    }

                } catch (Exception e) {
                    logger.warn("处理第 {} 封邮件时发生异常: {}，继续处理下一封", (messages.length - i), e.getMessage());
                }
            }

            // 如果遍历完所有邮件都没有找到符合条件的验证码
            logger.warn("在所有 {} 封邮件中都未找到包含verification关键字且包含验证码的邮件", messages.length);
            return null;
            
        } catch (Exception e) {
            logger.error("获取验证码时发生异常: {}", e.getMessage(), e);
            return null;
        } finally {
            // 6. 关闭文件夹（但保持Store连接用于复用）
            if (folder != null && folder.isOpen()) {
                try {
                    folder.close(false);
                } catch (MessagingException e) {
                    logger.error("关闭邮箱文件夹时发生异常: {}", e.getMessage());
                }
            }
        }
    }
    
    /**
     * 提取邮件的文本内容
     * 支持纯文本和HTML格式的邮件
     * 
     * @param message 邮件消息对象
     * @return 邮件的文本内容
     * @throws Exception 处理邮件时可能发生的异常
     */
    private String getTextContent(Message message) throws Exception {
        if (message.isMimeType("text/plain")) {
            // 纯文本邮件
            return (String) message.getContent();
        } else if (message.isMimeType("text/html")) {
            // HTML邮件，提取文本内容
            String htmlContent = (String) message.getContent();
            // 简单的HTML标签移除（实际项目中建议使用专门的HTML解析库）
            return htmlContent.replaceAll("<[^>]+>", "");
        } else if (message.isMimeType("multipart/*")) {
            // 多部分邮件
            Multipart multipart = (Multipart) message.getContent();
            return getTextFromMultipart(multipart);
        }
        return null;
    }
    
    /**
     * 从多部分邮件中提取文本内容
     * 
     * @param multipart 多部分邮件对象
     * @return 提取的文本内容
     * @throws Exception 处理邮件时可能发生的异常
     */
    private String getTextFromMultipart(Multipart multipart) throws Exception {
        StringBuilder result = new StringBuilder();
        int count = multipart.getCount();
        
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            
            if (bodyPart.isMimeType("text/plain")) {
                result.append(bodyPart.getContent().toString());
            } else if (bodyPart.isMimeType("text/html")) {
                String htmlContent = bodyPart.getContent().toString();
                result.append(htmlContent.replaceAll("<[^>]+>", ""));
            } else if (bodyPart.isMimeType("multipart/*")) {
                // 递归处理嵌套的多部分内容
                result.append(getTextFromMultipart((Multipart) bodyPart.getContent()));
            }
        }
        
        return result.toString();
    }
    
    /**
     * 删除指定邮箱地址的所有邮件
     * 搜索发送给指定邮箱地址的所有邮件并标记为删除
     *
     * @param emailAddress 目标邮箱地址
     * @return 删除的邮件数量，如果操作失败返回-1
     */
    public int deleteAllEmailsForAddress(String emailAddress) {
        Folder folder = null;
        int deletedCount = 0;

        try {
            logger.info("开始删除邮箱 {} 的所有邮件", emailAddress);

            // 1. 确保连接有效
            ensureConnection();

            // 2. 打开收件箱文件夹（需要读写权限）
            folder = store.getFolder("INBOX");
            folder.open(Folder.READ_WRITE);

            logger.info("成功打开收件箱，邮件总数: {}", folder.getMessageCount());

            // 3. 搜索发送给指定邮箱地址的邮件
            RecipientStringTerm recipientTerm = new RecipientStringTerm(Message.RecipientType.TO, emailAddress);
            Message[] messages = folder.search(recipientTerm);

            if (messages.length == 0) {
                logger.info("未找到发送给邮箱 {} 的邮件，无需删除", emailAddress);
                return 0;
            }

            logger.info("找到 {} 封发送给邮箱 {} 的邮件，开始删除", messages.length, emailAddress);

            // 4. 标记所有邮件为删除
            for (int i = 0; i < messages.length; i++) {
                try {
                    Message message = messages[i];
                    message.setFlag(Flags.Flag.DELETED, true);
                    deletedCount++;
                    logger.debug("已标记删除第 {} 封邮件，发送时间: {}", (i + 1), message.getSentDate());
                } catch (Exception e) {
                    logger.warn("标记删除第 {} 封邮件时发生异常: {}，继续处理下一封", (i + 1), e.getMessage());
                }
            }

            // 5. 执行删除操作（expunge）
            if (deletedCount > 0) {
                folder.expunge();
                logger.info("成功删除邮箱 {} 的 {} 封邮件", emailAddress, deletedCount);
            }

            return deletedCount;

        } catch (Exception e) {
            logger.error("删除邮箱 {} 的邮件时发生异常: {}", emailAddress, e.getMessage(), e);
            return -1;
        } finally {
            // 6. 关闭文件夹（但保持Store连接用于复用）
            if (folder != null && folder.isOpen()) {
                try {
                    folder.close(true); // true表示执行expunge操作
                } catch (MessagingException e) {
                    logger.error("关闭邮箱文件夹时发生异常: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * 获取连接状态
     *
     * @return 连接是否有效
     */
    public boolean isConnected() {
        return store != null && store.isConnected();
    }
    
    /**
     * 应用关闭时清理资源
     * 使用@PreDestroy注解确保在Bean销毁前执行
     */
    @PreDestroy
    public void cleanup() {
        try {
            if (store != null && store.isConnected()) {
                store.close();
                logger.info("邮箱连接已关闭");
            }
        } catch (MessagingException e) {
            logger.error("关闭邮箱连接时发生异常: {}", e.getMessage());
        }
    }
}
