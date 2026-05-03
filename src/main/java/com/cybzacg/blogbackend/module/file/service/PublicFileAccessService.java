package com.cybzacg.blogbackend.module.file.service;

import com.cybzacg.blogbackend.module.file.model.data.FileContentVO;

/**
 * 公开文件访问服务。
 * 封装文件访问权限校验与内容获取逻辑。
 */
public interface PublicFileAccessService {
    /**
     * 获取文件内容（已校验访问权限）。
     *
     * @param fileId 文件ID
     * @return 文件内容、文件名和 MIME 类型
     */
    FileContentVO getFileContent(Long fileId);
}
