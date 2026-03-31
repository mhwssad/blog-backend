package com.cybzacg.blogbackend.module.chat;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.domain.ChatConversation;
import com.cybzacg.blogbackend.domain.ChatConversationMember;
import com.cybzacg.blogbackend.domain.ChatMessage;
import com.cybzacg.blogbackend.domain.ChatMessageRecipient;
import com.cybzacg.blogbackend.domain.FileBusinessInfo;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.mapper.ChatConversationMapper;
import com.cybzacg.blogbackend.mapper.ChatMessageMapper;
import com.cybzacg.blogbackend.module.auth.service.SysUserService;
import com.cybzacg.blogbackend.module.chat.constant.ChatConstants;
import com.cybzacg.blogbackend.module.chat.convert.ChatModelMapper;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminConversationPageQuery;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminConversationVO;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminMemberMuteUpdateRequest;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminMemberRoleUpdateRequest;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminMemberStatusUpdateRequest;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminMessageDetailVO;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminMessagePageQuery;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminMessageReceiptPageQuery;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminMessageReceiptVO;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminMessageVO;
import com.cybzacg.blogbackend.module.chat.model.common.ChatMessagePayloadVO;
import com.cybzacg.blogbackend.module.chat.model.data.ChatAdminConversationListItem;
import com.cybzacg.blogbackend.module.chat.model.data.ChatAdminMessageItem;
import com.cybzacg.blogbackend.module.chat.model.user.ChatConversationLastMessageVO;
import com.cybzacg.blogbackend.module.chat.model.user.ChatMemberVO;
import com.cybzacg.blogbackend.module.chat.service.ChatConversationMemberService;
import com.cybzacg.blogbackend.module.chat.service.ChatConversationService;
import com.cybzacg.blogbackend.module.chat.service.ChatPushService;
import com.cybzacg.blogbackend.module.chat.service.ChatMessageRecipientService;
import com.cybzacg.blogbackend.module.chat.service.ChatMessageService;
import com.cybzacg.blogbackend.module.chat.service.impl.ChatAdminServiceImpl;
import com.cybzacg.blogbackend.module.file.service.FileBusinessInfoService;
import com.cybzacg.blogbackend.module.file.service.FileLifecycleService;
import com.cybzacg.blogbackend.utils.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatAdminServiceImplTest {
    @Mock
    private ChatConversationMapper chatConversationMapper;
    @Mock
    private ChatMessageMapper chatMessageMapper;
    @Mock
    private ChatConversationService chatConversationService;
    @Mock
    private ChatConversationMemberService chatConversationMemberService;
    @Mock
    private ChatMessageService chatMessageService;
    @Mock
    private ChatMessageRecipientService chatMessageRecipientService;
    @Mock
    private SysUserService sysUserService;
    @Mock
    private ChatModelMapper chatModelMapper;
    @Mock
    private ChatPushService chatPushService;
    @Mock
    private FileBusinessInfoService fileBusinessInfoService;
    @Mock
    private FileLifecycleService fileLifecycleService;

    @Mock
    private LambdaQueryChainWrapper<ChatConversationMember> memberListQuery;

    private ChatAdminServiceImpl chatAdminService;

    @BeforeEach
    void setUp() {
        chatAdminService = new ChatAdminServiceImpl(
                chatConversationMapper,
                chatMessageMapper,
                chatConversationService,
                chatConversationMemberService,
                chatMessageService,
                chatMessageRecipientService,
                sysUserService,
                chatModelMapper,
                chatPushService,
                fileBusinessInfoService,
                fileLifecycleService
        );
    }

    @Test
    void pageConversationsShouldBuildFallbackSingleConversationName() {
        ChatAdminConversationPageQuery query = new ChatAdminConversationPageQuery();

        ChatAdminConversationListItem item = new ChatAdminConversationListItem();
        item.setId(1001L);
        item.setConversationType(ChatConstants.CONVERSATION_TYPE_SINGLE);
        item.setMemberCount(2L);

        ChatConversationMember selfMember = new ChatConversationMember();
        selfMember.setConversationId(1001L);
        selfMember.setUserId(1L);

        ChatConversationMember targetMember = new ChatConversationMember();
        targetMember.setConversationId(1001L);
        targetMember.setUserId(2L);

        SysUser selfUser = new SysUser();
        selfUser.setId(1L);
        selfUser.setNickname("管理员");
        selfUser.setUsername("admin");

        SysUser targetUser = new SysUser();
        targetUser.setId(2L);
        targetUser.setNickname("张三");
        targetUser.setUsername("zhangsan");

        ChatAdminConversationVO vo = new ChatAdminConversationVO();
        vo.setId(1001L);
        vo.setConversationType(ChatConstants.CONVERSATION_TYPE_SINGLE);

        when(chatConversationMapper.countAdminConversationPage(query)).thenReturn(1L);
        when(chatConversationMapper.selectAdminConversationPage(query, 0L, 10L)).thenReturn(List.of(item));
        when(chatConversationMemberService.lambdaQuery()).thenReturn(memberListQuery);
        when(memberListQuery.in(anySFunction(), anyCollection())).thenReturn(memberListQuery);
        when(memberListQuery.list()).thenReturn(List.of(selfMember, targetMember));
        when(sysUserService.listByIds(any())).thenReturn(List.of(selfUser, targetUser));
        when(chatModelMapper.toAdminConversationVO(item)).thenReturn(vo);

        var result = chatAdminService.pageConversations(query);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals("管理员 / 张三", result.getRecords().get(0).getName());
        assertEquals(2L, result.getRecords().get(0).getMemberCount());
    }

    @Test
    void pageMessagesShouldFillSenderInfo() {
        ChatConversation conversation = new ChatConversation();
        conversation.setId(2001L);

        ChatAdminMessagePageQuery query = new ChatAdminMessagePageQuery();

        ChatAdminMessageItem item = new ChatAdminMessageItem();
        item.setId(9001L);
        item.setConversationId(2001L);
        item.setSenderId(2L);
        item.setMessageType(ChatConstants.MESSAGE_TYPE_TEXT);
        item.setContent("hello");
        item.setTotalRecipientCount(2L);
        item.setDeliveredRecipientCount(1L);
        item.setReadRecipientCount(1L);

        SysUser sender = new SysUser();
        sender.setId(2L);
        sender.setUsername("zhangsan");
        sender.setNickname("张三");
        sender.setAvatar("https://example.com/u2.png");

        ChatAdminMessageVO vo = new ChatAdminMessageVO();
        vo.setId(9001L);
        vo.setConversationId(2001L);

        when(chatConversationService.getById(2001L)).thenReturn(conversation);
        when(chatMessageMapper.countAdminMessagePage(2001L, query)).thenReturn(1L);
        when(chatMessageMapper.selectAdminMessagePage(2001L, query, 0L, 20L)).thenReturn(List.of(item));
        when(sysUserService.listByIds(any())).thenReturn(List.of(sender));
        when(chatModelMapper.toAdminMessageVO(item)).thenReturn(vo);

        var result = chatAdminService.pageMessages(2001L, query);

        assertEquals(1L, result.getTotal());
        assertEquals("zhangsan", result.getRecords().get(0).getSenderUsername());
        assertEquals("张三", result.getRecords().get(0).getSenderNickname());
        assertEquals("https://example.com/u2.png", result.getRecords().get(0).getSenderAvatar());
    }

    @Test
    void pageMessagesShouldAllowDissolvedConversationForAudit() {
        Long conversationId = 2004L;

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_GROUP);
        conversation.setStatus(ChatConstants.CONVERSATION_STATUS_DISSOLVED);

        ChatAdminMessagePageQuery query = new ChatAdminMessagePageQuery();

        ChatAdminMessageItem item = new ChatAdminMessageItem();
        item.setId(9004L);
        item.setConversationId(conversationId);
        item.setSenderId(4L);
        item.setMessageType(ChatConstants.MESSAGE_TYPE_TEXT);
        item.setContent("archived");

        SysUser sender = new SysUser();
        sender.setId(4L);
        sender.setUsername("wangwu");
        sender.setNickname("王五");

        ChatAdminMessageVO vo = new ChatAdminMessageVO();
        vo.setId(9004L);
        vo.setConversationId(conversationId);

        when(chatConversationService.getById(conversationId)).thenReturn(conversation);
        when(chatMessageMapper.countAdminMessagePage(conversationId, query)).thenReturn(1L);
        when(chatMessageMapper.selectAdminMessagePage(conversationId, query, 0L, 20L)).thenReturn(List.of(item));
        when(sysUserService.listByIds(any())).thenReturn(List.of(sender));
        when(chatModelMapper.toAdminMessageVO(item)).thenReturn(vo);

        var result = chatAdminService.pageMessages(conversationId, query);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals("wangwu", result.getRecords().get(0).getSenderUsername());
        assertEquals("王五", result.getRecords().get(0).getSenderNickname());
    }

    @Test
    void pageMessagesShouldBuildReplySnapshotForLegacyPayload() {
        Long conversationId = 2003L;
        Long replyMessageId = 8801L;

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);

        ChatAdminMessagePageQuery query = new ChatAdminMessagePageQuery();

        ChatAdminMessageItem item = new ChatAdminMessageItem();
        item.setId(9003L);
        item.setConversationId(conversationId);
        item.setSenderId(2L);
        item.setMessageType(ChatConstants.MESSAGE_TYPE_TEXT);
        item.setContent("reply");
        item.setReplyMessageId(replyMessageId);

        ChatMessagePayloadVO replyPayload = new ChatMessagePayloadVO();
        var replyFile = new com.cybzacg.blogbackend.module.chat.model.common.ChatFilePayloadVO();
        replyFile.setFileUrl("https://example.com/demo.png");
        replyPayload.setFile(replyFile);

        ChatAdminMessageItem replyItem = new ChatAdminMessageItem();
        replyItem.setId(replyMessageId);
        replyItem.setConversationId(conversationId);
        replyItem.setSenderId(3L);
        replyItem.setMessageType(ChatConstants.MESSAGE_TYPE_IMAGE);
        replyItem.setReplyMessageId(7701L);
        replyItem.setContent("[图片] demo.png");
        replyItem.setPayloadJson(JsonUtils.toJson(replyPayload));

        SysUser sender = new SysUser();
        sender.setId(2L);
        sender.setUsername("zhangsan");
        sender.setNickname("张三");

        SysUser replySender = new SysUser();
        replySender.setId(3L);
        replySender.setUsername("lisi");
        replySender.setNickname("李四");

        ChatAdminMessageVO vo = new ChatAdminMessageVO();
        vo.setId(9003L);
        vo.setConversationId(conversationId);

        when(chatConversationService.getById(conversationId)).thenReturn(conversation);
        when(chatMessageMapper.countAdminMessagePage(conversationId, query)).thenReturn(1L);
        when(chatMessageMapper.selectAdminMessagePage(conversationId, query, 0L, 20L)).thenReturn(List.of(item));
        when(chatMessageMapper.selectAdminMessagesByIds(conversationId, List.of(replyMessageId))).thenReturn(List.of(replyItem));
        when(sysUserService.listByIds(any())).thenReturn(List.of(sender, replySender));
        when(chatModelMapper.toAdminMessageVO(item)).thenReturn(vo);

        var result = chatAdminService.pageMessages(conversationId, query);

        assertNotNull(result.getRecords().get(0).getReply());
        assertEquals(replyMessageId, result.getRecords().get(0).getReply().getId());
        assertEquals("李四", result.getRecords().get(0).getReply().getSenderNickname());
        assertEquals(ChatConstants.MESSAGE_TYPE_IMAGE, result.getRecords().get(0).getReply().getMessageType());
        assertEquals("https://example.com/demo.png", result.getRecords().get(0).getReply().getFile().getFileUrl());
        assertEquals(7701L, result.getRecords().get(0).getReply().getReplyToMessageId());
        assertEquals(ChatConstants.REPLY_STATE_NORMAL, result.getRecords().get(0).getReply().getState());
    }

    @Test
    void pageMessagesShouldPreferLiveReplySnapshotOverPayloadSnapshot() {
        Long conversationId = 2004L;
        Long replyMessageId = 8802L;

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);

        ChatAdminMessagePageQuery query = new ChatAdminMessagePageQuery();

        com.cybzacg.blogbackend.module.chat.model.common.ChatReplyMessageVO payloadReply =
                new com.cybzacg.blogbackend.module.chat.model.common.ChatReplyMessageVO();
        payloadReply.setId(replyMessageId);
        payloadReply.setContent("old snapshot");
        payloadReply.setState(ChatConstants.REPLY_STATE_NORMAL);
        ChatMessagePayloadVO payload = new ChatMessagePayloadVO();
        payload.setReply(payloadReply);

        ChatAdminMessageItem item = new ChatAdminMessageItem();
        item.setId(9004L);
        item.setConversationId(conversationId);
        item.setSenderId(2L);
        item.setMessageType(ChatConstants.MESSAGE_TYPE_TEXT);
        item.setContent("reply");
        item.setReplyMessageId(replyMessageId);
        item.setPayloadJson(JsonUtils.toJson(payload));

        ChatAdminMessageItem replyItem = new ChatAdminMessageItem();
        replyItem.setId(replyMessageId);
        replyItem.setConversationId(conversationId);
        replyItem.setSenderId(3L);
        replyItem.setMessageType(ChatConstants.MESSAGE_TYPE_TEXT);
        replyItem.setContent("new live content");
        replyItem.setRevokeStatus(ChatConstants.REVOKE_STATUS_NORMAL);

        SysUser sender = new SysUser();
        sender.setId(2L);
        sender.setUsername("zhangsan");
        sender.setNickname("张三");

        SysUser replySender = new SysUser();
        replySender.setId(3L);
        replySender.setUsername("lisi");
        replySender.setNickname("李四");

        ChatAdminMessageVO vo = new ChatAdminMessageVO();
        vo.setId(9004L);
        vo.setConversationId(conversationId);

        when(chatConversationService.getById(conversationId)).thenReturn(conversation);
        when(chatMessageMapper.countAdminMessagePage(conversationId, query)).thenReturn(1L);
        when(chatMessageMapper.selectAdminMessagePage(conversationId, query, 0L, 20L)).thenReturn(List.of(item));
        when(chatMessageMapper.selectAdminMessagesByIds(conversationId, List.of(replyMessageId))).thenReturn(List.of(replyItem));
        when(sysUserService.listByIds(any())).thenReturn(List.of(sender, replySender));
        when(chatModelMapper.toAdminMessageVO(item)).thenReturn(vo);

        var result = chatAdminService.pageMessages(conversationId, query);

        assertNotNull(result.getRecords().get(0).getReply());
        assertEquals("new live content", result.getRecords().get(0).getReply().getContent());
        assertEquals(ChatConstants.REPLY_STATE_NORMAL, result.getRecords().get(0).getReply().getState());
    }

    @Test
    void pageMessagesShouldFallbackToUsernameWhenSenderNicknameIsBlank() {
        Long conversationId = 2002L;

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);

        ChatAdminMessagePageQuery query = new ChatAdminMessagePageQuery();

        ChatAdminMessageItem item = new ChatAdminMessageItem();
        item.setId(9002L);
        item.setConversationId(conversationId);
        item.setSenderId(3L);
        item.setMessageType(ChatConstants.MESSAGE_TYPE_TEXT);
        item.setContent("hello");

        SysUser sender = new SysUser();
        sender.setId(3L);
        sender.setUsername("lisi");
        sender.setNickname("   ");
        sender.setAvatar(null);

        ChatAdminMessageVO vo = new ChatAdminMessageVO();
        vo.setId(9002L);
        vo.setConversationId(conversationId);

        when(chatConversationService.getById(conversationId)).thenReturn(conversation);
        when(chatMessageMapper.countAdminMessagePage(conversationId, query)).thenReturn(1L);
        when(chatMessageMapper.selectAdminMessagePage(conversationId, query, 0L, 20L)).thenReturn(List.of(item));
        when(sysUserService.listByIds(any())).thenReturn(List.of(sender));
        when(chatModelMapper.toAdminMessageVO(item)).thenReturn(vo);

        var result = chatAdminService.pageMessages(conversationId, query);

        assertEquals("lisi", result.getRecords().get(0).getSenderNickname());
        assertEquals("lisi", result.getRecords().get(0).getSenderUsername());
        assertEquals(null, result.getRecords().get(0).getSenderAvatar());
    }

    @Test
    void updateConversationStatusShouldRejectUnsupportedStatus() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> chatAdminService.updateConversationStatus(3001L, ChatConstants.CONVERSATION_STATUS_DISSOLVED));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("后台只支持将会话状态更新为禁用或正常", exception.getMessage());
        verify(chatConversationService, never()).getById(any());
    }

    @Test
    void updateConversationStatusShouldPersistChangedStatus() {
        ChatConversation conversation = new ChatConversation();
        conversation.setId(3001L);
        conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);

        LambdaQueryChainWrapper<ChatConversationMember> activeMembersQuery = org.mockito.Mockito.mock(LambdaQueryChainWrapper.class);

        when(chatConversationService.getById(3001L)).thenReturn(conversation);
        when(chatConversationService.updateById(conversation)).thenReturn(true);
        when(chatConversationMemberService.lambdaQuery()).thenReturn(activeMembersQuery);
        when(activeMembersQuery.eq(anySFunction(), any())).thenReturn(activeMembersQuery);
        when(activeMembersQuery.list()).thenReturn(List.of());

        chatAdminService.updateConversationStatus(3001L, ChatConstants.CONVERSATION_STATUS_DISABLED);

        assertEquals(ChatConstants.CONVERSATION_STATUS_DISABLED, conversation.getStatus());
        verify(chatConversationService).updateById(conversation);
    }

    @Test
    void getConversationShouldFillOwnerAndLastMessageSummary() {
        Long conversationId = 4001L;

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);

        ChatAdminConversationListItem item = new ChatAdminConversationListItem();
        item.setId(conversationId);
        item.setConversationType(ChatConstants.CONVERSATION_TYPE_GROUP);
        item.setName("学习群");
        item.setOwnerId(1L);
        item.setMemberCount(2L);
        item.setLastMessageId(9001L);
        item.setLastMessageSenderId(2L);
        item.setLastMessageType(ChatConstants.MESSAGE_TYPE_TEXT);
        item.setLastMessageContent("hello");

        ChatConversationMember ownerMember = new ChatConversationMember();
        ownerMember.setConversationId(conversationId);
        ownerMember.setUserId(1L);

        ChatConversationMember senderMember = new ChatConversationMember();
        senderMember.setConversationId(conversationId);
        senderMember.setUserId(2L);

        SysUser owner = new SysUser();
        owner.setId(1L);
        owner.setUsername("admin");
        owner.setNickname("管理员");

        SysUser sender = new SysUser();
        sender.setId(2L);
        sender.setUsername("zhangsan");
        sender.setNickname("张三");

        ChatAdminConversationVO vo = new ChatAdminConversationVO();
        vo.setId(conversationId);
        vo.setConversationType(ChatConstants.CONVERSATION_TYPE_GROUP);
        vo.setName("学习群");

        ChatConversationLastMessageVO lastMessageVO = new ChatConversationLastMessageVO();
        lastMessageVO.setId(9001L);
        lastMessageVO.setSenderId(2L);
        lastMessageVO.setMessageType(ChatConstants.MESSAGE_TYPE_TEXT);
        lastMessageVO.setContent("hello");

        LambdaQueryChainWrapper<ChatConversationMember> detailMemberQuery = org.mockito.Mockito.mock(LambdaQueryChainWrapper.class);

        when(chatConversationService.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMapper.selectAdminConversationDetail(conversationId)).thenReturn(item);
        when(chatConversationMemberService.lambdaQuery()).thenReturn(detailMemberQuery);
        when(detailMemberQuery.in(anySFunction(), anyCollection())).thenReturn(detailMemberQuery);
        when(detailMemberQuery.list()).thenReturn(List.of(ownerMember, senderMember));
        when(sysUserService.listByIds(any())).thenReturn(List.of(owner, sender));
        when(chatModelMapper.toAdminConversationVO(item)).thenReturn(vo);
        when(chatModelMapper.toConversationLastMessageVO(item)).thenReturn(lastMessageVO);

        ChatAdminConversationVO result = chatAdminService.getConversation(conversationId);

        assertEquals(conversationId, result.getId());
        assertEquals("admin", result.getOwnerUsername());
        assertEquals("管理员", result.getOwnerNickname());
        assertEquals(2L, result.getMemberCount());
        assertEquals("张三", result.getLastMessage().getSenderNickname());
        assertEquals("hello", result.getLastMessage().getContent());
    }

    @Test
    void getConversationShouldFallbackWhenOwnerAndSenderUsersMissing() {
        Long conversationId = 4002L;

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);

        ChatAdminConversationListItem item = new ChatAdminConversationListItem();
        item.setId(conversationId);
        item.setConversationType(ChatConstants.CONVERSATION_TYPE_GROUP);
        item.setOwnerId(8L);
        item.setLastMessageId(9002L);
        item.setLastMessageSenderId(9L);
        item.setLastMessageType(ChatConstants.MESSAGE_TYPE_TEXT);
        item.setLastMessageContent("notice");

        ChatAdminConversationVO vo = new ChatAdminConversationVO();
        vo.setId(conversationId);
        vo.setConversationType(ChatConstants.CONVERSATION_TYPE_GROUP);

        ChatConversationLastMessageVO lastMessageVO = new ChatConversationLastMessageVO();
        lastMessageVO.setId(9002L);
        lastMessageVO.setSenderId(9L);
        lastMessageVO.setContent("notice");

        LambdaQueryChainWrapper<ChatConversationMember> detailMemberQuery = org.mockito.Mockito.mock(LambdaQueryChainWrapper.class);

        when(chatConversationService.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMapper.selectAdminConversationDetail(conversationId)).thenReturn(item);
        when(chatConversationMemberService.lambdaQuery()).thenReturn(detailMemberQuery);
        when(detailMemberQuery.in(anySFunction(), anyCollection())).thenReturn(detailMemberQuery);
        when(detailMemberQuery.list()).thenReturn(List.of());
        when(sysUserService.listByIds(any())).thenReturn(List.of());
        when(chatModelMapper.toAdminConversationVO(item)).thenReturn(vo);
        when(chatModelMapper.toConversationLastMessageVO(item)).thenReturn(lastMessageVO);

        ChatAdminConversationVO result = chatAdminService.getConversation(conversationId);

        assertEquals(conversationId, result.getId());
        assertEquals("用户9", result.getLastMessage().getSenderNickname());
        assertEquals("notice", result.getLastMessage().getContent());
        assertEquals(0L, result.getMemberCount());
    }

    @Test
    void listMembersShouldSortByRoleStatusAndJoinTime() {
        Long conversationId = 5001L;

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);

        ChatConversationMember ownerMember = new ChatConversationMember();
        ownerMember.setConversationId(conversationId);
        ownerMember.setUserId(1L);
        ownerMember.setMemberRole(ChatConstants.MEMBER_ROLE_OWNER);
        ownerMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        ownerMember.setJoinedAt(new java.util.Date(1_000L));

        ChatConversationMember adminMember = new ChatConversationMember();
        adminMember.setConversationId(conversationId);
        adminMember.setUserId(2L);
        adminMember.setMemberRole(ChatConstants.MEMBER_ROLE_ADMIN);
        adminMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        adminMember.setJoinedAt(new java.util.Date(2_000L));

        ChatConversationMember normalMember = new ChatConversationMember();
        normalMember.setConversationId(conversationId);
        normalMember.setUserId(3L);
        normalMember.setMemberRole(ChatConstants.MEMBER_ROLE_MEMBER);
        normalMember.setStatus(ChatConstants.MEMBER_STATUS_REMOVED);
        normalMember.setJoinedAt(new java.util.Date(3_000L));

        SysUser owner = new SysUser();
        owner.setId(1L);
        owner.setUsername("owner");
        owner.setNickname("群主");

        SysUser admin = new SysUser();
        admin.setId(2L);
        admin.setUsername("admin");
        admin.setNickname("管理员");

        SysUser member = new SysUser();
        member.setId(3L);
        member.setUsername("member");
        member.setNickname("成员");

        ChatMemberVO ownerVO = new ChatMemberVO();
        ChatMemberVO adminVO = new ChatMemberVO();
        ChatMemberVO memberVO = new ChatMemberVO();

        LambdaQueryChainWrapper<ChatConversationMember> memberQuery = org.mockito.Mockito.mock(LambdaQueryChainWrapper.class);

        when(chatConversationService.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberService.lambdaQuery()).thenReturn(memberQuery);
        when(memberQuery.eq(anySFunction(), any())).thenReturn(memberQuery);
        when(memberQuery.list()).thenReturn(List.of(normalMember, adminMember, ownerMember));
        when(sysUserService.listByIds(any())).thenReturn(List.of(owner, admin, member));
        when(chatModelMapper.toMemberVO(ownerMember)).thenReturn(ownerVO);
        when(chatModelMapper.toMemberVO(adminMember)).thenReturn(adminVO);
        when(chatModelMapper.toMemberVO(normalMember)).thenReturn(memberVO);

        List<ChatMemberVO> result = chatAdminService.listMembers(conversationId);

        assertEquals(List.of(1L, 2L, 3L), result.stream().map(ChatMemberVO::getUserId).toList());
        assertEquals(List.of("群主", "管理员", "成员"), result.stream().map(ChatMemberVO::getNickname).toList());
    }

    @Test
    void listMembersShouldApplyStableOrderForSameRoleStatusAndNullJoinTime() {
        Long conversationId = 5002L;

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);

        ChatConversationMember ownerMember = new ChatConversationMember();
        ownerMember.setConversationId(conversationId);
        ownerMember.setUserId(1L);
        ownerMember.setMemberRole(ChatConstants.MEMBER_ROLE_OWNER);
        ownerMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        ownerMember.setJoinedAt(new Date(5_000L));

        ChatConversationMember adminEarly = new ChatConversationMember();
        adminEarly.setConversationId(conversationId);
        adminEarly.setUserId(2L);
        adminEarly.setMemberRole(ChatConstants.MEMBER_ROLE_ADMIN);
        adminEarly.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        adminEarly.setJoinedAt(new Date(1_000L));

        ChatConversationMember adminLate = new ChatConversationMember();
        adminLate.setConversationId(conversationId);
        adminLate.setUserId(3L);
        adminLate.setMemberRole(ChatConstants.MEMBER_ROLE_ADMIN);
        adminLate.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        adminLate.setJoinedAt(new Date(2_000L));

        ChatConversationMember adminWithoutJoinTime = new ChatConversationMember();
        adminWithoutJoinTime.setConversationId(conversationId);
        adminWithoutJoinTime.setUserId(4L);
        adminWithoutJoinTime.setMemberRole(ChatConstants.MEMBER_ROLE_ADMIN);
        adminWithoutJoinTime.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);

        ChatConversationMember memberLeft = new ChatConversationMember();
        memberLeft.setConversationId(conversationId);
        memberLeft.setUserId(5L);
        memberLeft.setMemberRole(ChatConstants.MEMBER_ROLE_MEMBER);
        memberLeft.setStatus(ChatConstants.MEMBER_STATUS_LEFT);
        memberLeft.setJoinedAt(new Date(3_000L));

        ChatConversationMember memberNormalHigherUserId = new ChatConversationMember();
        memberNormalHigherUserId.setConversationId(conversationId);
        memberNormalHigherUserId.setUserId(7L);
        memberNormalHigherUserId.setMemberRole(ChatConstants.MEMBER_ROLE_MEMBER);
        memberNormalHigherUserId.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        memberNormalHigherUserId.setJoinedAt(new Date(4_000L));

        ChatConversationMember memberNormalLowerUserId = new ChatConversationMember();
        memberNormalLowerUserId.setConversationId(conversationId);
        memberNormalLowerUserId.setUserId(6L);
        memberNormalLowerUserId.setMemberRole(ChatConstants.MEMBER_ROLE_MEMBER);
        memberNormalLowerUserId.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        memberNormalLowerUserId.setJoinedAt(new Date(4_000L));

        ChatConversationMember memberDisabled = new ChatConversationMember();
        memberDisabled.setConversationId(conversationId);
        memberDisabled.setUserId(8L);
        memberDisabled.setMemberRole(ChatConstants.MEMBER_ROLE_MEMBER);
        memberDisabled.setStatus(ChatConstants.MEMBER_STATUS_DISABLED);
        memberDisabled.setJoinedAt(new Date(500L));

        LambdaQueryChainWrapper<ChatConversationMember> memberQuery = org.mockito.Mockito.mock(LambdaQueryChainWrapper.class);

        when(chatConversationService.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberService.lambdaQuery()).thenReturn(memberQuery);
        when(memberQuery.eq(anySFunction(), any())).thenReturn(memberQuery);
        when(memberQuery.list()).thenReturn(List.of(
                memberDisabled,
                adminWithoutJoinTime,
                memberNormalHigherUserId,
                ownerMember,
                memberLeft,
                adminLate,
                memberNormalLowerUserId,
                adminEarly
        ));
        when(sysUserService.listByIds(any())).thenReturn(List.of(
                buildUser(1L, "owner", "群主"),
                buildUser(2L, "admin-2", "管理员-早"),
                buildUser(3L, "admin-3", "管理员-晚"),
                buildUser(4L, "admin-4", "管理员-无加入时间"),
                buildUser(5L, "member-5", "已退群成员"),
                buildUser(6L, "member-6", "普通成员-小ID"),
                buildUser(7L, "member-7", "普通成员-大ID"),
                buildUser(8L, "member-8", "禁用成员")
        ));
        when(chatModelMapper.toMemberVO(any(ChatConversationMember.class))).thenAnswer(invocation -> new ChatMemberVO());

        List<ChatMemberVO> result = chatAdminService.listMembers(conversationId);

        assertEquals(List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L),
                result.stream().map(ChatMemberVO::getUserId).toList());
        assertEquals(List.of(
                        "群主",
                        "管理员-早",
                        "管理员-晚",
                        "管理员-无加入时间",
                        "已退群成员",
                        "普通成员-小ID",
                        "普通成员-大ID",
                        "禁用成员"
                ),
                result.stream().map(ChatMemberVO::getNickname).toList());
    }

    @Test
    void pageConversationsShouldReturnEmptyPageWhenNoRecords() {
        ChatAdminConversationPageQuery query = new ChatAdminConversationPageQuery();
        query.setCurrent(3L);
        query.setSize(15L);

        when(chatConversationMapper.countAdminConversationPage(query)).thenReturn(0L);

        var result = chatAdminService.pageConversations(query);

        assertEquals(0L, result.getTotal());
        assertEquals(3L, result.getCurrent());
        assertEquals(15L, result.getSize());
        assertTrue(result.getRecords().isEmpty());
    }

    @Test
    void pageMessagesShouldReturnEmptyPageWhenNoRecords() {
        Long conversationId = 6001L;

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);

        ChatAdminMessagePageQuery query = new ChatAdminMessagePageQuery();
        query.setCurrent(2L);
        query.setSize(5L);

        when(chatConversationService.getById(conversationId)).thenReturn(conversation);
        when(chatMessageMapper.countAdminMessagePage(conversationId, query)).thenReturn(0L);

        var result = chatAdminService.pageMessages(conversationId, query);

        assertEquals(0L, result.getTotal());
        assertEquals(2L, result.getCurrent());
        assertEquals(5L, result.getSize());
        assertTrue(result.getRecords().isEmpty());
    }

    @Test
    void pageConversationsShouldNormalizeCurrentAndSizeWhenNoRecords() {
        ChatAdminConversationPageQuery query = new ChatAdminConversationPageQuery();
        query.setCurrent(0L);
        query.setSize(999L);

        when(chatConversationMapper.countAdminConversationPage(query)).thenReturn(0L);

        var result = chatAdminService.pageConversations(query);

        assertEquals(1L, result.getCurrent());
        assertEquals(100L, result.getSize());
        assertTrue(result.getRecords().isEmpty());
    }

    @Test
    void pageConversationsShouldPassNormalizedOffsetToMapper() {
        ChatAdminConversationPageQuery query = new ChatAdminConversationPageQuery();
        query.setCurrent(3L);
        query.setSize(999L);

        ChatAdminConversationListItem item = new ChatAdminConversationListItem();
        item.setId(6101L);
        item.setConversationType(ChatConstants.CONVERSATION_TYPE_GROUP);
        item.setName("运营群");
        item.setOwnerId(1L);

        ChatAdminConversationVO vo = new ChatAdminConversationVO();
        vo.setId(6101L);
        vo.setConversationType(ChatConstants.CONVERSATION_TYPE_GROUP);

        when(chatConversationMapper.countAdminConversationPage(query)).thenReturn(1L);
        when(chatConversationMapper.selectAdminConversationPage(query, 200L, 100L)).thenReturn(List.of(item));
        when(chatConversationMemberService.lambdaQuery()).thenReturn(memberListQuery);
        when(memberListQuery.in(anySFunction(), anyCollection())).thenReturn(memberListQuery);
        when(memberListQuery.list()).thenReturn(List.of());
        when(sysUserService.listByIds(any())).thenReturn(List.of());
        when(chatModelMapper.toAdminConversationVO(item)).thenReturn(vo);

        var result = chatAdminService.pageConversations(query);

        assertEquals(3L, result.getCurrent());
        assertEquals(100L, result.getSize());
        assertEquals(1, result.getRecords().size());
        verify(chatConversationMapper).selectAdminConversationPage(query, 200L, 100L);
    }

    @Test
    void pageConversationsShouldPassFilterQueryToMapper() {
        ChatAdminConversationPageQuery query = new ChatAdminConversationPageQuery();
        query.setCurrent(2L);
        query.setSize(10L);
        query.setKeyword("运营");
        query.setConversationType(ChatConstants.CONVERSATION_TYPE_SINGLE);
        query.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);
        query.setOwnerId(1L);
        query.setMemberUserId(2L);
        query.setIsAllSite(0);

        ChatAdminConversationListItem item = new ChatAdminConversationListItem();
        item.setId(6102L);
        item.setConversationType(ChatConstants.CONVERSATION_TYPE_SINGLE);
        item.setOwnerId(1L);
        item.setMemberCount(2L);

        ChatConversationMember selfMember = new ChatConversationMember();
        selfMember.setConversationId(6102L);
        selfMember.setUserId(1L);

        ChatConversationMember targetMember = new ChatConversationMember();
        targetMember.setConversationId(6102L);
        targetMember.setUserId(2L);

        SysUser owner = new SysUser();
        owner.setId(1L);
        owner.setUsername("owner");
        owner.setNickname("群主");

        SysUser target = new SysUser();
        target.setId(2L);
        target.setUsername("zhangsan");
        target.setNickname("张三");

        ChatAdminConversationVO vo = new ChatAdminConversationVO();
        vo.setId(6102L);
        vo.setConversationType(ChatConstants.CONVERSATION_TYPE_SINGLE);

        when(chatConversationMapper.countAdminConversationPage(query)).thenReturn(1L);
        when(chatConversationMapper.selectAdminConversationPage(query, 10L, 10L)).thenReturn(List.of(item));
        when(chatConversationMemberService.lambdaQuery()).thenReturn(memberListQuery);
        when(memberListQuery.in(anySFunction(), anyCollection())).thenReturn(memberListQuery);
        when(memberListQuery.list()).thenReturn(List.of(selfMember, targetMember));
        when(sysUserService.listByIds(any())).thenReturn(List.of(owner, target));
        when(chatModelMapper.toAdminConversationVO(item)).thenReturn(vo);

        var result = chatAdminService.pageConversations(query);

        assertEquals(1L, result.getTotal());
        assertEquals("群主 / 张三", result.getRecords().get(0).getName());
        verify(chatConversationMapper).countAdminConversationPage(query);
        verify(chatConversationMapper).selectAdminConversationPage(query, 10L, 10L);
    }

    @Test
    void pageConversationsShouldNormalizeExtremePaginationWhenMemberFilterPresent() {
        ChatAdminConversationPageQuery query = new ChatAdminConversationPageQuery();
        query.setCurrent(-5L);
        query.setSize(0L);
        query.setMemberUserId(2L);

        ChatAdminConversationListItem item = new ChatAdminConversationListItem();
        item.setId(6103L);
        item.setConversationType(ChatConstants.CONVERSATION_TYPE_GROUP);
        item.setName("测试群");
        item.setOwnerId(1L);
        item.setMemberCount(1L);

        ChatAdminConversationVO vo = new ChatAdminConversationVO();
        vo.setId(6103L);
        vo.setConversationType(ChatConstants.CONVERSATION_TYPE_GROUP);
        vo.setName("测试群");

        when(chatConversationMapper.countAdminConversationPage(query)).thenReturn(1L);
        when(chatConversationMapper.selectAdminConversationPage(query, 0L, 10L)).thenReturn(List.of(item));
        when(chatConversationMemberService.lambdaQuery()).thenReturn(memberListQuery);
        when(memberListQuery.in(anySFunction(), anyCollection())).thenReturn(memberListQuery);
        when(memberListQuery.list()).thenReturn(List.of());
        when(chatModelMapper.toAdminConversationVO(item)).thenReturn(vo);

        var result = chatAdminService.pageConversations(query);

        assertEquals(1L, result.getCurrent());
        assertEquals(10L, result.getSize());
        assertEquals(1L, result.getTotal());
        verify(chatConversationMapper).selectAdminConversationPage(query, 0L, 10L);
    }

    @Test
    void pageMessagesShouldNormalizeCurrentAndSizeWhenNoRecords() {
        Long conversationId = 6051L;
        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);

        ChatAdminMessagePageQuery query = new ChatAdminMessagePageQuery();
        query.setCurrent(0L);
        query.setSize(999L);

        when(chatConversationService.getById(conversationId)).thenReturn(conversation);
        when(chatMessageMapper.countAdminMessagePage(conversationId, query)).thenReturn(0L);

        var result = chatAdminService.pageMessages(conversationId, query);

        assertEquals(1L, result.getCurrent());
        assertEquals(100L, result.getSize());
        assertTrue(result.getRecords().isEmpty());
    }

    @Test
    void pageMessagesShouldPassNormalizedOffsetAndFallbackSenderName() {
        Long conversationId = 6052L;

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);

        ChatAdminMessagePageQuery query = new ChatAdminMessagePageQuery();
        query.setCurrent(0L);
        query.setSize(999L);

        ChatAdminMessageItem item = new ChatAdminMessageItem();
        item.setId(9101L);
        item.setConversationId(conversationId);
        item.setSenderId(9L);
        item.setContent("系统通知");
        item.setMessageType(ChatConstants.MESSAGE_TYPE_TEXT);

        ChatAdminMessageVO vo = new ChatAdminMessageVO();
        vo.setId(9101L);

        when(chatConversationService.getById(conversationId)).thenReturn(conversation);
        when(chatMessageMapper.countAdminMessagePage(conversationId, query)).thenReturn(1L);
        when(chatMessageMapper.selectAdminMessagePage(conversationId, query, 0L, 100L)).thenReturn(List.of(item));
        when(sysUserService.listByIds(any())).thenReturn(List.of());
        when(chatModelMapper.toAdminMessageVO(item)).thenReturn(vo);

        var result = chatAdminService.pageMessages(conversationId, query);

        assertEquals(1L, result.getCurrent());
        assertEquals(100L, result.getSize());
        assertEquals("用户9", result.getRecords().get(0).getSenderNickname());
        verify(chatMessageMapper).selectAdminMessagePage(conversationId, query, 0L, 100L);
    }

    @Test
    void pageMessagesShouldNormalizeExtremePaginationWhenRecordsExist() {
        Long conversationId = 6054L;

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);

        ChatAdminMessagePageQuery query = new ChatAdminMessagePageQuery();
        query.setCurrent(-2L);
        query.setSize(0L);

        ChatAdminMessageItem item = new ChatAdminMessageItem();
        item.setId(9103L);
        item.setConversationId(conversationId);
        item.setSenderId(8L);
        item.setMessageType(ChatConstants.MESSAGE_TYPE_TEXT);
        item.setContent("hello again");

        ChatAdminMessageVO vo = new ChatAdminMessageVO();
        vo.setId(9103L);

        when(chatConversationService.getById(conversationId)).thenReturn(conversation);
        when(chatMessageMapper.countAdminMessagePage(conversationId, query)).thenReturn(1L);
        when(chatMessageMapper.selectAdminMessagePage(conversationId, query, 0L, 20L)).thenReturn(List.of(item));
        when(sysUserService.listByIds(any())).thenReturn(List.of());
        when(chatModelMapper.toAdminMessageVO(item)).thenReturn(vo);

        var result = chatAdminService.pageMessages(conversationId, query);

        assertEquals(1L, result.getCurrent());
        assertEquals(20L, result.getSize());
        assertEquals(1L, result.getTotal());
        verify(chatMessageMapper).selectAdminMessagePage(conversationId, query, 0L, 20L);
    }

    @Test
    void pageMessagesShouldPassFilterQueryToMapper() {
        Long conversationId = 6053L;

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);

        ChatAdminMessagePageQuery query = new ChatAdminMessagePageQuery();
        query.setCurrent(2L);
        query.setSize(5L);
        query.setBeforeMessageId(99L);
        query.setSenderId(8L);
        query.setMessageType(ChatConstants.MESSAGE_TYPE_TEXT);
        query.setKeyword("hello");

        ChatAdminMessageItem item = new ChatAdminMessageItem();
        item.setId(9102L);
        item.setConversationId(conversationId);
        item.setSenderId(8L);
        item.setMessageType(ChatConstants.MESSAGE_TYPE_TEXT);
        item.setContent("hello world");

        SysUser sender = new SysUser();
        sender.setId(8L);
        sender.setUsername("sender8");
        sender.setNickname("发送者");

        ChatAdminMessageVO vo = new ChatAdminMessageVO();
        vo.setId(9102L);

        when(chatConversationService.getById(conversationId)).thenReturn(conversation);
        when(chatMessageMapper.countAdminMessagePage(conversationId, query)).thenReturn(1L);
        when(chatMessageMapper.selectAdminMessagePage(conversationId, query, 5L, 5L)).thenReturn(List.of(item));
        when(sysUserService.listByIds(any())).thenReturn(List.of(sender));
        when(chatModelMapper.toAdminMessageVO(item)).thenReturn(vo);

        var result = chatAdminService.pageMessages(conversationId, query);

        assertEquals(1L, result.getTotal());
        assertEquals("发送者", result.getRecords().get(0).getSenderNickname());
        verify(chatMessageMapper).countAdminMessagePage(conversationId, query);
        verify(chatMessageMapper).selectAdminMessagePage(conversationId, query, 5L, 5L);
    }

    @Test
    void getConversationShouldRejectMissingConversation() {
        when(chatConversationService.getById(7001L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> chatAdminService.getConversation(7001L));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("会话不存在", exception.getMessage());
    }

    @Test
    void getConversationShouldRejectNullConversationId() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> chatAdminService.getConversation(null));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("会话ID不能为空", exception.getMessage());
        verify(chatConversationService, never()).getById(any());
    }

    @Test
    void updateConversationStatusShouldSkipWhenStatusUnchanged() {
        ChatConversation conversation = new ChatConversation();
        conversation.setId(7101L);
        conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);

        when(chatConversationService.getById(7101L)).thenReturn(conversation);

        chatAdminService.updateConversationStatus(7101L, ChatConstants.CONVERSATION_STATUS_NORMAL);

        verify(chatConversationService, never()).updateById(any(ChatConversation.class));
    }

    @Test
    void getMessageDetailShouldAssembleFilePayloadAndReceiptCounts() {
        Long conversationId = 8001L;
        Long messageId = 9101L;
        Date createdAt = new Date(1_000L);
        Date updatedAt = new Date(2_000L);

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);

        ChatMessagePayloadVO payload = new ChatMessagePayloadVO();
        var filePayload = new com.cybzacg.blogbackend.module.chat.model.common.ChatFilePayloadVO();
        filePayload.setBusinessId(501L);
        filePayload.setFileId(801L);
        filePayload.setFileName("demo.pdf");
        payload.setFile(filePayload);
        var replySnapshot = new com.cybzacg.blogbackend.module.chat.model.common.ChatReplyMessageVO();
        replySnapshot.setId(9000L);
        replySnapshot.setContent("引用摘要");
        replySnapshot.setDeleted(false);
        payload.setReply(replySnapshot);

        ChatMessage message = new ChatMessage();
        message.setId(messageId);
        message.setConversationId(conversationId);
        message.setSenderId(2L);
        message.setMessageType(ChatConstants.MESSAGE_TYPE_FILE);
        message.setContent("[文件] demo.pdf");
        message.setPayloadJson(JsonUtils.toJson(payload));
        message.setReplyMessageId(9000L);
        message.setSendStatus(ChatConstants.SEND_STATUS_SENT);
        message.setRevokeStatus(ChatConstants.REVOKE_STATUS_NORMAL);
        message.setCreatedAt(createdAt);
        message.setUpdatedAt(updatedAt);

        ChatMessageRecipient deliveredRecipient = new ChatMessageRecipient();
        deliveredRecipient.setMessageId(messageId);
        deliveredRecipient.setRecipientUserId(2L);
        deliveredRecipient.setDeliveryStatus(ChatConstants.DELIVERY_STATUS_DELIVERED);

        ChatMessageRecipient readRecipient = new ChatMessageRecipient();
        readRecipient.setMessageId(messageId);
        readRecipient.setRecipientUserId(3L);
        readRecipient.setDeliveryStatus(ChatConstants.DELIVERY_STATUS_READ);

        SysUser sender = new SysUser();
        sender.setId(2L);
        sender.setUsername("zhangsan");
        sender.setNickname("张三");

        LambdaQueryChainWrapper<ChatMessageRecipient> recipientQuery = org.mockito.Mockito.mock(LambdaQueryChainWrapper.class);

        when(chatConversationService.getById(conversationId)).thenReturn(conversation);
        when(chatMessageService.getById(messageId)).thenReturn(message);
        when(chatMessageRecipientService.lambdaQuery()).thenReturn(recipientQuery);
        when(recipientQuery.eq(anySFunction(), any())).thenReturn(recipientQuery);
        when(recipientQuery.list()).thenReturn(List.of(deliveredRecipient, readRecipient));
        when(sysUserService.listByIds(any())).thenReturn(List.of(sender));

        ChatAdminMessageDetailVO result = chatAdminService.getMessageDetail(conversationId, messageId);

        assertEquals(messageId, result.getId());
        assertEquals("张三", result.getSenderNickname());
        assertEquals(2L, result.getTotalRecipientCount());
        assertEquals(2L, result.getDeliveredRecipientCount());
        assertEquals(1L, result.getReadRecipientCount());
        assertEquals(801L, result.getFile().getFileId());
        assertNotNull(result.getReply());
        assertEquals(9000L, result.getReply().getId());
        assertEquals("引用摘要", result.getReply().getContent());
        assertEquals(Boolean.FALSE, result.getEdited());
    }

    @Test
    void pageMessageReceiptsShouldFillRecipientInfo() {
        Long conversationId = 8002L;
        Long messageId = 9102L;

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);

        ChatMessage message = new ChatMessage();
        message.setId(messageId);
        message.setConversationId(conversationId);

        ChatMessageRecipient recipient = new ChatMessageRecipient();
        recipient.setId(6001L);
        recipient.setMessageId(messageId);
        recipient.setConversationId(conversationId);
        recipient.setRecipientUserId(3L);
        recipient.setReceiveType("normal");
        recipient.setDeliveryStatus(ChatConstants.DELIVERY_STATUS_READ);
        recipient.setVisibleStatus(ChatConstants.VISIBLE_STATUS_VISIBLE);

        SysUser user = new SysUser();
        user.setId(3L);
        user.setUsername("lisi");
        user.setNickname("李四");

        ChatAdminMessageReceiptPageQuery query = new ChatAdminMessageReceiptPageQuery();
        query.setRecipientUserId(3L);
        query.setDeliveryStatus(ChatConstants.DELIVERY_STATUS_READ);
        query.setVisibleStatus(ChatConstants.VISIBLE_STATUS_VISIBLE);

        Page<ChatMessageRecipient> receiptPage = new Page<>(1L, 20L);
        receiptPage.setTotal(1L);
        receiptPage.setRecords(List.of(recipient));

        when(chatConversationService.getById(conversationId)).thenReturn(conversation);
        when(chatMessageService.getById(messageId)).thenReturn(message);
        when(chatMessageRecipientService.page(any(Page.class), any())).thenReturn(receiptPage);
        when(sysUserService.listByIds(any())).thenReturn(List.of(user));

        var result = chatAdminService.pageMessageReceipts(conversationId, messageId, query);

        assertEquals(1L, result.getTotal());
        assertEquals("lisi", result.getRecords().get(0).getRecipientUsername());
        assertEquals("李四", result.getRecords().get(0).getRecipientNickname());
        verify(chatMessageRecipientService).page(any(Page.class), any());
    }

    @Test
    void revokeMessageShouldClearPayloadAndReleaseFileReference() {
        Long conversationId = 8003L;
        Long messageId = 9103L;
        Long fileId = 7001L;

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);

        ChatMessage message = new ChatMessage();
        message.setId(messageId);
        message.setConversationId(conversationId);
        message.setSenderId(2L);
        message.setMessageType(ChatConstants.MESSAGE_TYPE_FILE);
        message.setPayloadJson("{\"fileId\":7001}");
        message.setRevokeStatus(ChatConstants.REVOKE_STATUS_NORMAL);

        FileBusinessInfo reference = new FileBusinessInfo();
        reference.setId(501L);
        reference.setFileId(fileId);
        reference.setReferenceType(ChatConstants.FILE_MESSAGE_REFERENCE_TYPE);
        reference.setReferenceId(messageId);

        LambdaQueryChainWrapper<FileBusinessInfo> fileReferenceQuery = org.mockito.Mockito.mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> activeMembersQuery = org.mockito.Mockito.mock(LambdaQueryChainWrapper.class);

        when(chatConversationService.getById(conversationId)).thenReturn(conversation);
        when(chatMessageService.getById(messageId)).thenReturn(message);
        when(chatMessageService.updateById(message)).thenReturn(true);
        when(fileBusinessInfoService.lambdaQuery()).thenReturn(fileReferenceQuery);
        when(fileReferenceQuery.eq(anySFunction(), any())).thenReturn(fileReferenceQuery);
        when(fileReferenceQuery.list()).thenReturn(List.of(reference));
        when(fileBusinessInfoService.removeByIds(List.of(501L))).thenReturn(true);
        when(chatConversationMemberService.lambdaQuery()).thenReturn(activeMembersQuery);
        when(activeMembersQuery.eq(anySFunction(), any())).thenReturn(activeMembersQuery);
        when(activeMembersQuery.list()).thenReturn(List.of());
        when(sysUserService.listByIds(any())).thenReturn(List.of());

        chatAdminService.revokeMessage(conversationId, messageId);

        assertEquals(ChatConstants.REVOKE_STATUS_REVOKED, message.getRevokeStatus());
        assertEquals(ChatConstants.MESSAGE_REVOKED_PLACEHOLDER, message.getContent());
        assertEquals(null, message.getPayloadJson());
        verify(fileLifecycleService).syncFileAfterReferenceRemoval(fileId);
    }

    @Test
    void updateMemberRoleShouldTransferOwner() {
        Long conversationId = 8004L;

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_GROUP);
        conversation.setOwnerId(1L);

        ChatConversationMember oldOwner = new ChatConversationMember();
        oldOwner.setId(11L);
        oldOwner.setConversationId(conversationId);
        oldOwner.setUserId(1L);
        oldOwner.setMemberRole(ChatConstants.MEMBER_ROLE_OWNER);

        ChatConversationMember targetMember = new ChatConversationMember();
        targetMember.setId(12L);
        targetMember.setConversationId(conversationId);
        targetMember.setUserId(2L);
        targetMember.setMemberRole(ChatConstants.MEMBER_ROLE_MEMBER);

        ChatMemberVO memberVO = new ChatMemberVO();

        LambdaQueryChainWrapper<ChatConversationMember> targetQuery = org.mockito.Mockito.mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> ownerQuery = org.mockito.Mockito.mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> listQuery = org.mockito.Mockito.mock(LambdaQueryChainWrapper.class);

        when(chatConversationService.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberService.lambdaQuery()).thenReturn(targetQuery, ownerQuery, listQuery);
        mockMemberFindQuery(targetQuery, targetMember);
        mockMemberRoleQuery(ownerQuery, oldOwner);
        when(listQuery.eq(anySFunction(), any())).thenReturn(listQuery);
        when(listQuery.list()).thenReturn(List.of(oldOwner, targetMember));
        when(chatConversationService.updateById(conversation)).thenReturn(true);
        when(chatConversationMemberService.updateById(oldOwner)).thenReturn(true);
        when(chatConversationMemberService.updateById(targetMember)).thenReturn(true);
        when(sysUserService.listByIds(any())).thenReturn(List.of());
        when(chatModelMapper.toMemberVO(any(ChatConversationMember.class))).thenReturn(memberVO);

        ChatAdminMemberRoleUpdateRequest request = new ChatAdminMemberRoleUpdateRequest();
        request.setRole(ChatConstants.MEMBER_ROLE_OWNER);

        chatAdminService.updateMemberRole(conversationId, 2L, request);

        assertEquals(2L, conversation.getOwnerId());
        assertEquals(ChatConstants.MEMBER_ROLE_ADMIN, oldOwner.getMemberRole());
        assertEquals(ChatConstants.MEMBER_ROLE_OWNER, targetMember.getMemberRole());
    }

    @Test
    void updateMemberRoleShouldRejectSingleConversation() {
        Long conversationId = 8005L;

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_SINGLE);
        conversation.setIsAllSite(0);

        when(chatConversationService.getById(conversationId)).thenReturn(conversation);

        ChatAdminMemberRoleUpdateRequest request = new ChatAdminMemberRoleUpdateRequest();
        request.setRole(ChatConstants.MEMBER_ROLE_ADMIN);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> chatAdminService.updateMemberRole(conversationId, 2L, request));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("后台成员治理仅支持普通群聊会话", exception.getMessage());
    }

    @Test
    void updateMemberStatusShouldUpdateTargetStatusAndPushMembersUpdated() {
        Long conversationId = 8006L;
        Long memberUserId = 2L;

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_GROUP);
        conversation.setIsAllSite(0);

        ChatConversationMember targetMember = new ChatConversationMember();
        targetMember.setId(21L);
        targetMember.setConversationId(conversationId);
        targetMember.setUserId(memberUserId);
        targetMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);

        ChatConversationMember ownerMember = new ChatConversationMember();
        ownerMember.setId(22L);
        ownerMember.setConversationId(conversationId);
        ownerMember.setUserId(1L);
        ownerMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        ownerMember.setMemberRole(ChatConstants.MEMBER_ROLE_OWNER);

        LambdaQueryChainWrapper<ChatConversationMember> targetQuery = org.mockito.Mockito.mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> listBeforeQuery = org.mockito.Mockito.mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> listAfterQuery = org.mockito.Mockito.mock(LambdaQueryChainWrapper.class);

        when(chatConversationService.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberService.lambdaQuery()).thenReturn(targetQuery, listBeforeQuery, listAfterQuery);
        mockMemberFindQuery(targetQuery, targetMember);
        when(listBeforeQuery.eq(anySFunction(), any())).thenReturn(listBeforeQuery);
        when(listBeforeQuery.list()).thenReturn(List.of(ownerMember, targetMember));
        when(listAfterQuery.eq(anySFunction(), any())).thenReturn(listAfterQuery);
        when(listAfterQuery.list()).thenReturn(List.of(ownerMember));
        when(chatConversationMemberService.updateById(targetMember)).thenReturn(true);
        when(sysUserService.listByIds(any())).thenReturn(List.of());
        when(chatModelMapper.toMemberVO(any(ChatConversationMember.class))).thenAnswer(invocation -> {
            ChatConversationMember source = invocation.getArgument(0);
            ChatMemberVO vo = new ChatMemberVO();
            vo.setRole(source.getMemberRole());
            vo.setStatus(source.getStatus());
            return vo;
        });

        ChatAdminMemberStatusUpdateRequest request = new ChatAdminMemberStatusUpdateRequest();
        request.setStatus(ChatConstants.MEMBER_STATUS_DISABLED);

        List<ChatMemberVO> result = chatAdminService.updateMemberStatus(conversationId, memberUserId, request);

        assertEquals(ChatConstants.MEMBER_STATUS_DISABLED, targetMember.getStatus());
        assertEquals(1, result.size());
        verify(chatPushService).pushMembersUpdated(argThat(payload ->
                        payload != null
                                && "admin_member_status_updated".equals(payload.getAction())
                                && memberUserId.equals(payload.getAffectedUserId())
                                && payload.getMembers() != null
                                && payload.getMembers().size() == 1),
                eq(List.of(1L, memberUserId)));
    }

    @Test
    void updateMemberStatusShouldRejectAllSiteConversation() {
        Long conversationId = 8006L;

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_GROUP);
        conversation.setIsAllSite(1);

        when(chatConversationService.getById(conversationId)).thenReturn(conversation);

        var request = new com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminMemberStatusUpdateRequest();
        request.setStatus(ChatConstants.MEMBER_STATUS_DISABLED);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> chatAdminService.updateMemberStatus(conversationId, 2L, request));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("后台成员治理仅支持普通群聊会话", exception.getMessage());
    }

    @Test
    void updateMemberMuteShouldUpdateMuteUntilAndPushMembersUpdated() {
        Long conversationId = 8007L;
        Long memberUserId = 2L;
        Date muteUntil = new Date(System.currentTimeMillis() + 60_000L);

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_GROUP);
        conversation.setIsAllSite(0);

        ChatConversationMember targetMember = new ChatConversationMember();
        targetMember.setId(31L);
        targetMember.setConversationId(conversationId);
        targetMember.setUserId(memberUserId);
        targetMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        targetMember.setMemberRole(ChatConstants.MEMBER_ROLE_MEMBER);

        ChatConversationMember ownerMember = new ChatConversationMember();
        ownerMember.setId(32L);
        ownerMember.setConversationId(conversationId);
        ownerMember.setUserId(1L);
        ownerMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        ownerMember.setMemberRole(ChatConstants.MEMBER_ROLE_OWNER);

        LambdaQueryChainWrapper<ChatConversationMember> targetQuery = org.mockito.Mockito.mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> listQuery = org.mockito.Mockito.mock(LambdaQueryChainWrapper.class);

        when(chatConversationService.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberService.lambdaQuery()).thenReturn(targetQuery, listQuery);
        mockMemberFindQuery(targetQuery, targetMember);
        when(listQuery.eq(anySFunction(), any())).thenReturn(listQuery);
        when(listQuery.list()).thenReturn(List.of(ownerMember, targetMember));
        when(chatConversationMemberService.updateById(targetMember)).thenReturn(true);
        when(sysUserService.listByIds(any())).thenReturn(List.of());
        when(chatModelMapper.toMemberVO(any(ChatConversationMember.class))).thenAnswer(invocation -> {
            ChatConversationMember source = invocation.getArgument(0);
            ChatMemberVO vo = new ChatMemberVO();
            vo.setRole(source.getMemberRole());
            vo.setStatus(source.getStatus());
            vo.setMuteUntil(source.getMuteUntil());
            return vo;
        });

        ChatAdminMemberMuteUpdateRequest request = new ChatAdminMemberMuteUpdateRequest();
        request.setMuteUntil(muteUntil);

        List<ChatMemberVO> result = chatAdminService.updateMemberMute(conversationId, memberUserId, request);

        assertEquals(muteUntil, targetMember.getMuteUntil());
        assertEquals(2, result.size());
        verify(chatPushService).pushMembersUpdated(argThat(payload ->
                        payload != null
                                && "admin_member_mute_updated".equals(payload.getAction())
                                && memberUserId.equals(payload.getAffectedUserId())),
                eq(List.of(1L, memberUserId)));
    }

    @Test
    void updateMemberMuteShouldRejectSingleConversation() {
        Long conversationId = 8007L;

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_SINGLE);
        conversation.setIsAllSite(0);

        when(chatConversationService.getById(conversationId)).thenReturn(conversation);

        var request = new ChatAdminMemberMuteUpdateRequest();
        request.setMuteUntil(new Date(System.currentTimeMillis() + 60_000L));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> chatAdminService.updateMemberMute(conversationId, 2L, request));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("后台成员治理仅支持普通群聊会话", exception.getMessage());
    }

    @SuppressWarnings("unchecked")
    private static <T> SFunction<T, ?> anySFunction() {
        return (SFunction<T, ?>) any(SFunction.class);
    }

    private static void mockMemberFindQuery(LambdaQueryChainWrapper<ChatConversationMember> query,
                                            ChatConversationMember result) {
        when(query.eq(anySFunction(), any())).thenReturn(query);
        when(query.orderByDesc(anySFunction())).thenReturn(query);
        when(query.last(any())).thenReturn(query);
        when(query.one()).thenReturn(result);
    }

    private static void mockMemberRoleQuery(LambdaQueryChainWrapper<ChatConversationMember> query,
                                            ChatConversationMember result) {
        when(query.eq(anySFunction(), any())).thenReturn(query);
        when(query.orderByDesc(anySFunction())).thenReturn(query);
        when(query.last(any())).thenReturn(query);
        when(query.one()).thenReturn(result);
    }

    private static SysUser buildUser(Long userId, String username, String nickname) {
        SysUser user = new SysUser();
        user.setId(userId);
        user.setUsername(username);
        user.setNickname(nickname);
        return user;
    }
}
