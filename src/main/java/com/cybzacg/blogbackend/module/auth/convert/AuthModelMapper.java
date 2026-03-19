package com.cybzacg.blogbackend.module.auth.convert;

import com.cybzacg.blogbackend.domain.SysMenu;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.module.auth.model.AuthMenuInfo;
import com.cybzacg.blogbackend.module.auth.model.AuthUserInfo;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public interface AuthModelMapper {

    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "permissions", ignore = true)
    AuthUserInfo toAuthUserInfo(SysUser user);

    @Mapping(target = "children", ignore = true)
    AuthMenuInfo toAuthMenuInfo(SysMenu menu);

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
