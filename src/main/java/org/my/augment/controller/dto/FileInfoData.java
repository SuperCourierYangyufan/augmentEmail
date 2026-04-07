package org.my.augment.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * WPS 文件信息 - 数据层 DTO
 * 对应 GET /v3/3rd/files/:file_id 接口的 data 字段
 *
 * @author 杨宇帆
 * @create 2026-04-07
 */
public class FileInfoData {

    /** 文档 ID，与传入的 file_id 一致，最大长度 47 */
    private String id;

    /** 文档名称，最大长度 240 */
    private String name;

    /** 文档版本号，无符号 int32，从 1 开始 */
    private Integer version;

    /** 文档大小，单位 byte */
    private Integer size;

    /** 文档创建时间戳，单位纪元秒 */
    @JsonProperty("create_time")
    private Long createTime;

    /** 文档最后修改时间戳，单位纪元秒 */
    @JsonProperty("modify_time")
    private Long modifyTime;

    /** 文档创建者 Id */
    @JsonProperty("creator_id")
    private String creatorId;

    /** 文档最后修改者 Id */
    @JsonProperty("modifier_id")
    private String modifierId;

    public FileInfoData() {
    }

    public FileInfoData(String id, String name, Integer version, Integer size,
                        Long createTime, Long modifyTime, String creatorId, String modifierId) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.size = size;
        this.createTime = createTime;
        this.modifyTime = modifyTime;
        this.creatorId = creatorId;
        this.modifierId = modifierId;
    }

    // --- Getters & Setters ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    public Long getModifyTime() {
        return modifyTime;
    }

    public void setModifyTime(Long modifyTime) {
        this.modifyTime = modifyTime;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public String getModifierId() {
        return modifierId;
    }

    public void setModifierId(String modifierId) {
        this.modifierId = modifierId;
    }
}
