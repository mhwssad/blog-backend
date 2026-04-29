package com.cybzacg.blogbackend.module.file.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.file.model.user.UserFileVO;
import com.cybzacg.blogbackend.module.file.model.user.UserFilePageQuery;
import com.cybzacg.blogbackend.module.file.model.user.UserFileTaskPageQuery;
import com.cybzacg.blogbackend.module.file.model.user.UserFileTaskVO;

/**
 * 用户文件查询服务接口。
 */
public interface UserFileQueryService {

    PageResult<UserFileVO> pageMyFiles(Long userId, UserFilePageQuery query);

    PageResult<UserFileTaskVO> pageMyUploadTasks(Long userId, UserFileTaskPageQuery query);
}