package com.cybzacg.blogbackend.module.auth.author.convert;

import com.cybzacg.blogbackend.domain.auth.SysAuthorApplication;
import com.cybzacg.blogbackend.domain.auth.SysUser;
import com.cybzacg.blogbackend.enums.auth.AuthorApplicationStatusEnum;
import com.cybzacg.blogbackend.module.auth.author.model.admin.SysAuthorApplicationAdminVO;
import com.cybzacg.blogbackend.module.auth.author.model.user.UserAuthorApplicationSubmitRequest;
import com.cybzacg.blogbackend.module.auth.author.model.user.UserAuthorApplicationVO;
import com.cybzacg.blogbackend.utils.JsonUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import org.mapstruct.*;

import java.util.List;

/**
 * 作者申请模型转换器。
 */
@Mapper(componentModel = "spring", imports = StrUtils.class, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AuthorApplicationModelConvert {

    @Mapping(target = "applyReason", expression = "java(StrUtils.normalize(request.getApplyReason()))")
    @Mapping(target = "contentDirection", expression = "java(StrUtils.normalize(request.getContentDirection()))")
    @Mapping(target = "introduction", expression = "java(StrUtils.trimToNull(request.getIntroduction()))")
    @Mapping(target = "sampleLinksJson", expression = "java(writeSampleLinks(request.getSampleLinks()))")
    SysAuthorApplication toApplication(UserAuthorApplicationSubmitRequest request);

    @InheritConfiguration(name = "toApplication")
    void updateApplication(UserAuthorApplicationSubmitRequest request, @MappingTarget SysAuthorApplication application);

    @Mapping(target = "applyStatusLabel", expression = "java(resolveStatusLabel(application.getApplyStatus()))")
    @Mapping(target = "sampleLinks", expression = "java(readSampleLinks(application.getSampleLinksJson()))")
    UserAuthorApplicationVO toUserVO(SysAuthorApplication application);

    @Mapping(target = "applyStatusLabel", expression = "java(resolveStatusLabel(application.getApplyStatus()))")
    @Mapping(target = "sampleLinks", expression = "java(readSampleLinks(application.getSampleLinksJson()))")
    SysAuthorApplicationAdminVO toAdminVO(SysAuthorApplication application);

    default SysAuthorApplicationAdminVO toAdminVO(SysAuthorApplication application,
                                                  SysUser applicant,
                                                  SysUser reviewer) {
        SysAuthorApplicationAdminVO vo = toAdminVO(application);
        if (applicant != null) {
            vo.setUsername(applicant.getUsername());
            vo.setNickname(applicant.getNickname());
        }
        if (reviewer != null) {
            vo.setReviewerUsername(reviewer.getUsername());
            vo.setReviewerNickname(reviewer.getNickname());
        }
        return vo;
    }

    default String writeSampleLinks(List<String> sampleLinks) {
        List<String> normalized = normalizeSampleLinks(sampleLinks);
        return normalized.isEmpty() ? null : JsonUtils.toJson(normalized);
    }

    default List<String> readSampleLinks(String sampleLinksJson) {
        List<String> sampleLinks = JsonUtils.fromJsonToList(sampleLinksJson, String.class);
        return sampleLinks == null ? List.of() : sampleLinks;
    }

    default String resolveStatusLabel(Integer status) {
        return AuthorApplicationStatusEnum.resolveLabel(status);
    }

    private List<String> normalizeSampleLinks(List<String> sampleLinks) {
        if (sampleLinks == null || sampleLinks.isEmpty()) {
            return List.of();
        }
        return sampleLinks.stream()
                .map(StrUtils::trimToNull)
                .filter(StrUtils::hasText)
                .distinct()
                .toList();
    }
}
