package com.cybzacg.blogbackend.module.auth.author.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.auth.author.model.user.UserAuthorApplicationPageQuery;
import com.cybzacg.blogbackend.module.auth.author.model.user.UserAuthorApplicationSubmitRequest;
import com.cybzacg.blogbackend.module.auth.author.model.user.UserAuthorApplicationVO;

/**
 * 用户作者申请服务。
 */
public interface UserAuthorApplicationService {
    UserAuthorApplicationVO submitApplication(UserAuthorApplicationSubmitRequest request);

    UserAuthorApplicationVO getLatestApplication();

    PageResult<UserAuthorApplicationVO> pageMyApplications(UserAuthorApplicationPageQuery query);
}
