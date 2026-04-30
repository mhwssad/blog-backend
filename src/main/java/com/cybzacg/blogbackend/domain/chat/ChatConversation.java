package com.cybzacg.blogbackend.domain.chat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天会话表。
 */
@Data
@TableName("chat_conversation")
public class ChatConversation {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 会话类型（single-单聊，group-群聊，system-系统）
     */
    private String conversationType;
    /**
     * 会话名称（群聊时由创建者指定）
     */
    private String name;
    /**
     * 会话头像URL
     */
    private String avatar;
    /**
     * 创建者ID（群主）
     */
    private Long ownerId;
    /**
     * 单聊唯一标识（双方用户ID排序拼接的哈希）
     */
    private String singlePairKey;
    /**
     * 业务场景：single_chat/user_group/hall_channel/topic_channel/global_channel
     */
    private String sceneType;
    /**
     * 可见范围：public/member/private
     */
    private String visibilityScope;
    /**
     * 访客是否可见：0-否，1-是
     */
    private Integer allowGuestView;
    /**
     * 是否需要加入后发言：0-否，1-是
     */
    private Integer requireJoinToSpeak;
    /**
     * 加入规则：free/approval/invite_only
     */
    private String joinRule;
    /**
     * 发言最低等级限制
     */
    private Integer speakLevelLimit;
    /**
     * 成员上限：0-不限制
     */
    private Integer memberLimit;
    /**
     * 是否全站会话：0-否，1-是
     */
    private Integer isAllSite;
    /**
     * 全站会话唯一标识
     */
    private String allSiteKey;
    /**
     * 会话状态：0-禁用，1-正常，2-已解散
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;
    /**
     * 频道/群公告
     */
    private String announcement;
    /**
     * 慢速模式秒数：0-关闭
     */
    private Integer slowModeSeconds;
    /**
     * 展示排序
     */
    private Integer displaySort;
    /**
     * 频道分类编码/群分类编码
     */
    private String channelCategoryCode;
    /**
     * 最新消息ID
     */
    private Long lastMessageId;
    /**
     * 最新消息时间
     */
    private LocalDateTime lastMessageTime;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
