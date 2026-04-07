package org.my.augment.controller.dto;

/**
 * WPS 文件信息 - 外层响应 DTO
 * 包含 code 状态码和 data 数据体
 *
 * @author 杨宇帆
 * @create 2026-04-07
 */
public class FileInfoResponse {

    /** 状态码，0 表示成功 */
    private Integer code;

    /** 文件信息数据 */
    private FileInfoData data;

    public FileInfoResponse() {
    }

    public FileInfoResponse(Integer code, FileInfoData data) {
        this.code = code;
        this.data = data;
    }

    /**
     * 快捷构建成功响应
     */
    public static FileInfoResponse success(FileInfoData data) {
        return new FileInfoResponse(0, data);
    }

    // --- Getters & Setters ---

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public FileInfoData getData() {
        return data;
    }

    public void setData(FileInfoData data) {
        this.data = data;
    }
}
