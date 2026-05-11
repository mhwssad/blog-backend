package com.cybzacg.blogbackend.module.ai.model.data;

import com.cybzacg.blogbackend.dto.domain.file.FileInfo;
import lombok.Data;

import java.util.List;

/**
 * 多模态附件解析结果。
 */
@Data
public class AiAttachmentResolution {
    /** 校验通过的文件列表 */
    private List<FileInfo> validAttachments;
    /** 其中为图片类型的文件列表（用于构造 UserMessage） */
    private List<FileInfo> imageAttachments;
}
