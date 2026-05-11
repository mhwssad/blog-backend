package com.cybzacg.blogbackend.module.auth.account.convert;

import com.cybzacg.blogbackend.dto.domain.auth.SysUser;
import com.cybzacg.blogbackend.module.auth.account.model.user.PublicUserSearchVO;
import com.cybzacg.blogbackend.module.auth.account.model.user.UserProfileUpdateRequest;
import com.cybzacg.blogbackend.module.auth.account.model.user.UserProfileVO;
import org.mapstruct.*;

/**
 * 用户自服务对象转换器。
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface UserProfileModelConvert {

    UserProfileVO toUserProfileVO(SysUser user);

    PublicUserSearchVO toPublicUserSearchVO(SysUser user);

    @InheritConfiguration
    void updateProfile(UserProfileUpdateRequest request, @MappingTarget SysUser user);
}
