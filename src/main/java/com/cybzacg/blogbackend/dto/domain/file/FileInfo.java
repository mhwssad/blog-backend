package com.cybzacg.blogbackend.dto.domain.file;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件物理信息表。
 */
@Data
@TableName("file_info")
public class FileInfo {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联上传任务ID
     */
    private Long uploadTaskId;
    /**
     * 存储文件名
     */
    private String fileName;
    /**
     * 原始文件名
     */
    private String originalName;
    /**
     * 文件存储路径
     */
    private String filePath;
    /**
     * 存储唯一键
     */
    private String storageKey;
    /**
     * 文件访问URL
     */
    private String fileUrl;
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    /**
     * 文件类型（image-图片，document-文档，video-视频，audio-音频，other-其他）
     */
    private String fileType;
    /**
     * MIME类型
     */
    private String mimeType;
    /**
     * 文件扩展名
     */
    private String fileExtension;
    /**
     * 文件MD5哈希值
     */
    private String md5;
    /**
     * 业务引用计数
     */
    private Integer referenceCount;
    /**
     * 是否公开：0-私密，1-公开
     */
    private Integer isPublic;
    /**
     * 文件分类
     */
    private String category;
    /**
     * 下载次数
     */
    private Integer downloadCount;
    /**
     * 上传用户ID
     */
    private Long uploadUserId;
    /**
     * 备注
     */
    private String remark;
    /**
     * 文件状态：0-已删除，1-正常，2-待物理删除，3-审核中，4-违规下架
     */
    private Integer status;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
