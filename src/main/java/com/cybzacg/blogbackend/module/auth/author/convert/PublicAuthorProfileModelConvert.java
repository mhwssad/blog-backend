package com.cybzacg.blogbackend.module.auth.author.convert;

import com.cybzacg.blogbackend.dto.domain.auth.SysUser;
import com.cybzacg.blogbackend.module.auth.author.model.publics.PublicAuthorProfileVO;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * 公开作者主页对象转换器。
 */
@Mapper(componentModel = "spring")
public interface PublicAuthorProfileModelConvert {

    @Mapping(target = "userId", source = "id")
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "authorBadge", ignore = true)
    @Mapping(target = "publicArticleCount", ignore = true)
    @Mapping(target = "publicSeriesCount", ignore = true)
    @Mapping(target = "showcaseArticleIds", ignore = true)
    @Mapping(target = "representativeArticleIds", ignore = true)
    @Mapping(target = "featuredSeriesIds", ignore = true)
    @Mapping(target = "featuredColumnIds", ignore = true)
    PublicAuthorProfileVO toPublicAuthorProfileVO(SysUser user);

    @AfterMapping
    default void initReservedFields(@MappingTarget PublicAuthorProfileVO vo) {
        vo.setShowcaseArticleIds(List.of());
        vo.setRepresentativeArticleIds(List.of());
        vo.setFeaturedSeriesIds(List.of());
        vo.setFeaturedColumnIds(List.of());
    }
}
