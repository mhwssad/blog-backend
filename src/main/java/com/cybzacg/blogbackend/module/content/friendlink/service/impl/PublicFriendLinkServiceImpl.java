package com.cybzacg.blogbackend.module.content.friendlink.service.impl;

import com.cybzacg.blogbackend.dto.repository.content.BlogFriendLinkRepository;
import com.cybzacg.blogbackend.module.content.friendlink.convert.FriendLinkModelConvert;
import com.cybzacg.blogbackend.module.content.friendlink.model.publics.PublicFriendLinkVO;
import com.cybzacg.blogbackend.module.content.friendlink.service.PublicFriendLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PublicFriendLinkServiceImpl implements PublicFriendLinkService {
    private final BlogFriendLinkRepository friendLinkRepository;
    private final FriendLinkModelConvert friendLinkModelConvert;

    @Override
    public List<PublicFriendLinkVO> listEnabled() {
        return friendLinkModelConvert.toPublicVOList(friendLinkRepository.listEnabled());
    }
}
