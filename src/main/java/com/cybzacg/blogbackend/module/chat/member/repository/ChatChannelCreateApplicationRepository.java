package com.cybzacg.blogbackend.module.chat.member.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.chat.ChatChannelCreateApplication;
import com.cybzacg.blogbackend.module.chat.member.model.admin.ChatChannelApplicationAdminPageQuery;

/**
 * 频道创建申请 Repository。
 */
public interface ChatChannelCreateApplicationRepository extends IService<ChatChannelCreateApplication> {

    /**
     * 查询用户最近一次频道创建申请。
     */
    ChatChannelCreateApplication findLatestByApplicantUserId(Long applicantUserId);

    /**
     * 分页查询用户自己的频道创建申请。
     */
    Page<ChatChannelCreateApplication> pageByApplicantUserId(Long applicantUserId, long current, long size);

    /**
     * 后台按条件分页查询频道创建申请。
     */
    Page<ChatChannelCreateApplication> pageByAdminConditions(ChatChannelApplicationAdminPageQuery query);
}
