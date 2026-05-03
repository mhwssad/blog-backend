package com.cybzacg.blogbackend.module.auth.account.convert;

import com.cybzacg.blogbackend.domain.auth.SysMenu;
import com.cybzacg.blogbackend.domain.auth.SysUser;
import com.cybzacg.blogbackend.module.auth.account.model.AuthMenuInfo;
import com.cybzacg.blogbackend.module.auth.account.model.AuthRegisterRequest;
import com.cybzacg.blogbackend.module.auth.account.model.AuthUserInfo;
import com.cybzacg.blogbackend.utils.StrUtils;
import org.mapstruct.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 认证模块对象转换器，处理用户注册、登录信息及菜单树结构映射。
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        imports = StrUtils.class
)
public interface AuthModelConvert {

    AuthUserInfo toAuthUserInfo(SysUser user);

    AuthMenuInfo toAuthMenuInfo(SysMenu menu);

    @Mapping(target = "username", expression = "java(StrUtils.trimToNull(request.getUsername()))")
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "nickname", expression = "java(StrUtils.hasText(request.getNickname()) ? StrUtils.trim(request.getNickname()) : StrUtils.trimToNull(request.getUsername()))")
    @Mapping(target = "email", expression = "java(StrUtils.trimToLowerCase(request.getEmail()))")
    @Mapping(target = "phone", expression = "java(StrUtils.trimToNull(request.getPhone()))")
    SysUser toRegisterUser(AuthRegisterRequest request);

    default AuthUserInfo toAuthUserInfo(SysUser user, List<String> roles, List<String> permissions) {
        AuthUserInfo userInfo = toAuthUserInfo(user);
        userInfo.setRoles(roles);
        userInfo.setPermissions(permissions);
        return userInfo;
    }

    @AfterMapping
    default void initChildren(@MappingTarget AuthMenuInfo menuInfo) {
        menuInfo.setChildren(new ArrayList<>());
    }
}
