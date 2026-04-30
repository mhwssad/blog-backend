package com.cybzacg.blogbackend.module.auth.experience.convert;

import com.cybzacg.blogbackend.domain.auth.UserExperienceLog;
import com.cybzacg.blogbackend.module.auth.experience.model.admin.ExperienceLogVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 经验体系 MapStruct 映射器。
 */
@Mapper(componentModel = "spring")
public interface ExperienceModelMapper {

    @Mapping(target = "id", ignore = true)
    ExperienceLogVO toLogVO(UserExperienceLog log);
}
