package org.my.augment.controller;

import org.my.augment.controller.dto.FileInfoData;
import org.my.augment.controller.dto.FileInfoResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

/**
 * WPS 第三方文件接口
 *
 * @author 杨宇帆
 * @create 2026-04-07
 */
@RestController
@RequestMapping("/v3/3rd")
public class WpsController {

    /**
     * 预定义的随机文件名列表，覆盖常见办公文档类型
     */
    private static final String[] RANDOM_FILE_NAMES = {
            "统计月报.xlsx",
            "项目计划书.docx",
            "季度总结.pptx",
            "产品需求文档.docx",
            "财务报表.xlsx",
            "会议纪要.docx",
            "技术方案设计.docx",
            "用户反馈汇总.xlsx",
            "年度工作报告.pptx",
            "合同模板.docx",
            "数据分析报告.xlsx",
            "培训资料.pptx",
            "接口文档.docx",
            "测试报告.docx",
            "运营周报.xlsx"
    };

    /**
     * 获取文件元信息
     * <p>
     * 根据传入的 file_id 返回随机生成的文件元信息，
     * 其中 id 字段始终与传入的 file_id 保持一致。
     *
     * @param fileId 文档 ID，最大长度 47
     * @return 包含 code 和 data 的标准响应
     */
    @GetMapping("/files/{file_id}")
    public FileInfoResponse getFileInfo(@PathVariable("file_id") String fileId) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        // 随机文件名
        String name = RANDOM_FILE_NAMES[random.nextInt(RANDOM_FILE_NAMES.length)];

        // 版本号：1 ~ 500
        int version = random.nextInt(1, 501);

        // 文件大小：1KB ~ 100MB（单位 byte）
        int size = random.nextInt(1024, 100 * 1024 * 1024);

        // 创建时间：最近一年内的随机时间戳（纪元秒）
        long now = Instant.now().getEpochSecond();
        long oneYearAgo = now - 365L * 24 * 60 * 60;
        long createTime = random.nextLong(oneYearAgo, now);

        // 修改时间：在创建时间之后、当前时间之前
        long modifyTime = random.nextLong(createTime, now + 1);

        // 创建者 ID 和修改者 ID：100 ~ 999 的随机数字字符串
        String creatorId = String.valueOf(random.nextInt(100, 1000));
        String modifierId = String.valueOf(random.nextInt(100, 1000));

        FileInfoData data = new FileInfoData(
                fileId, name, version, size,
                createTime, modifyTime, creatorId, modifierId
        );

        return FileInfoResponse.success(data);
    }
}
