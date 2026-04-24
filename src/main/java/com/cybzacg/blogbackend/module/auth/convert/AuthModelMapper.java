package com.cybzacg.blogbackend.module.auth.convert;

import com.cybzacg.blogbackend.domain.SysMenu;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.module.auth.model.AuthMenuInfo;
import com.cybzacg.blogbackend.module.auth.model.AuthRegisterRequest;
import com.cybzacg.blogbackend.module.auth.model.AuthUserInfo;
import com.cybzacg.blogbackend.utils.StrUtils;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.ArrayList;
import java.util.List;

/** 认证模块对象转换器，处理用户注册、登录信息及菜单树结构映射。 */
@Mapper(componentModel = "spring", imports = StrUtils.class)
public interface AuthModelMapper {

    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "permissions", ignore = true)
    AuthUserInfo toAuthUserInfo(SysUser user);

    @Mapping(target = "children", ignore = true)
    AuthMenuInfo toAuthMenuInfo(SysMenu menu);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", expression = "java(StrUtils.trimToNull(request.getUsername()))")
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "nickname", expression = "java(StrUtils.hasText(request.getNickname()) ? StrUtils.trim(request.getNickname()) : StrUtils.trimToNull(request.getUsername()))")
    @Mapping(target = "email", expression = "java(StrUtils.trimToLowerCase(request.getEmail()))")
    @Mapping(target = "phone", expression = "java(StrUtils.trimToNull(request.getPhone()))")
    @Mapping(target = "avatar", ignore = true)
    @Mapping(target = "gender", ignore = true)
    @Mapping(target = "birthday", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "lastLoginTime", ignore = true)
    @Mapping(target = "lastLoginIp", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedFlag", ignore = true)
    @Mapping(target = "remark", ignore = true)
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
