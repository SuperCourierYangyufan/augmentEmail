package org.my.augment.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 邮件内容DTO（仅用于前后端数据传输，不做持久化）
 * 包含：发送人、发送时间、完整正文内容（优先HTML）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailContentDto {
    /**
     * 发送人显示名或邮箱地址
     */
    private String sender;

    /**
     * 发送时间，格式：yyyy-MM-dd HH:mm:ss
     */
    private String sentTime;

    /**
     * 完整正文内容（优先HTML，无法解析时回退为纯文本）
     */
    private String content;
}

