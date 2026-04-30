package com.cybzacg.blogbackend.module.file.service.impl;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.file.FileBusinessInfo;
import com.cybzacg.blogbackend.domain.file.FileInfo;
import com.cybzacg.blogbackend.domain.file.FileUploadTask;
import com.cybzacg.blogbackend.module.file.convert.FileModelMapper;
import com.cybzacg.blogbackend.module.file.model.user.UserFilePageQuery;
import com.cybzacg.blogbackend.module.file.model.user.UserFileTaskPageQuery;
import com.cybzacg.blogbackend.module.file.model.user.UserFileTaskVO;
import com.cybzacg.blogbackend.module.file.model.user.UserFileVO;
import com.cybzacg.blogbackend.module.file.repository.FileBusinessInfoRepository;
import com.cybzacg.blogbackend.module.file.repository.FileInfoRepository;
import com.cybzacg.blogbackend.module.file.repository.FileUploadTaskRepository;
import com.cybzacg.blogbackend.module.file.service.UserFileQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户文件查询服务实现。
 * 负责用户文件列表查询、上传任务列表查询及相关业务逻辑。
 */
@Service
@RequiredArgsConstructor
public class UserFileQueryServiceImpl implements UserFileQueryService {
    private final FileInfoRepository fileInfoRepository;
    private final FileUploadTaskRepository fileUploadTaskRepository;
    private final FileBusinessInfoRepository fileBusinessInfoRepository;
    private final FileModelMapper fileModelMapper;

    @Override
    public PageResult<UserFileVO> pageMyFiles(Long userId, UserFilePageQuery query) {
        validateUserFileQuery(query);
        long current = query.getCurrent() == null ? 1L : query.getCurrent();
        long size = query.getSize() == null ? 10L : query.getSize();
        Set<Long> fileIds = null;
        if (org.springframework.util.StringUtils.hasText(query.getKeyword()) || query.getStatus() != null) {
            fileIds = fileInfoRepository.findIdsByStatusAndKeyword(query.getStatus(), query.getKeyword());
            if (fileIds.isEmpty()) {
                return PageResult.<UserFileVO>builder().total(0L).current(current).size(size).records(List.of()).build();
            }
        }
        query.setCurrent(current);
        query.setSize(size);
        var page = fileBusinessInfoRepository.pageByUserAndFilters(userId, query, fileIds);
        Map<Long, FileInfo> fileMap = loadFileInfoMap(page.getRecords().stream().map(FileBusinessInfo::getFileId).collect(Collectors.toSet()));
        List<UserFileVO> records = page.getRecords().stream()
                .map(ref -> toUserFileVO(ref, fileMap.get(ref.getFileId())))
                .filter(Objects::nonNull)
                .toList();
        return PageResult.of(page, records);
    }

    @Override
    public PageResult<UserFileTaskVO> pageMyUploadTasks(Long userId, UserFileTaskPageQuery query) {
        validateTaskPageQuery(query.getTaskStatus());
        query.setCurrent(query.getCurrent() == null ? 1L : query.getCurrent());
        query.setSize(query.getSize() == null ? 10L : query.getSize());
        var page = fileUploadTaskRepository.pageByUserAndStatus(userId, query);
        List<UserFileTaskVO> records = page.getRecords().stream().map(this::toUserTaskVO).toList();
        return PageResult.of(page, records);
    }

    private void validateUserFileQuery(UserFilePageQuery query) {
        com.cybzacg.blogbackend.utils.ExceptionThrowerCore.throwBusinessIf(
                org.springframework.util.StringUtils.hasText(query.getCategory()) && !com.cybzacg.blogbackend.enums.file.FileCategoryEnum.contains(query.getCategory()),
                com.cybzacg.blogbackend.enums.error.ResultErrorCode.ILLEGAL_ARGUMENT
        );
        com.cybzacg.blogbackend.utils.ExceptionThrowerCore.throwBusinessIf(
                org.springframework.util.StringUtils.hasText(query.getReferenceType()) && !com.cybzacg.blogbackend.enums.file.FileReferenceTypeEnum.contains(query.getReferenceType()),
                com.cybzacg.blogbackend.enums.error.ResultErrorCode.ILLEGAL_ARGUMENT
        );
        com.cybzacg.blogbackend.utils.ExceptionThrowerCore.throwBusinessIf(
                query.getStatus() != null && !com.cybzacg.blogbackend.enums.file.FileStatusEnum.contains(query.getStatus()),
                com.cybzacg.blogbackend.enums.file.FileResultCode.FILE_STATUS_INVALID
        );
    }

    private void validateTaskPageQuery(Integer taskStatus) {
        com.cybzacg.blogbackend.utils.ExceptionThrowerCore.throwBusinessIf(
                taskStatus != null && com.cybzacg.blogbackend.enums.storage.TaskStatusEnum.getByCode(taskStatus) == null,
                com.cybzacg.blogbackend.enums.file.FileResultCode.UPLOAD_TASK_STATUS_INVALID
        );
    }

    private Map<Long, FileInfo> loadFileInfoMap(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, FileInfo> map = new HashMap<>();
        for (FileInfo info : fileInfoRepository.listByIds(ids)) {
            map.put(info.getId(), info);
        }
        return map;
    }

    private UserFileVO toUserFileVO(FileBusinessInfo ref, FileInfo fileInfo) {
        if (ref == null || fileInfo == null) {
            return null;
        }
        return fileModelMapper.toUserFileVOFromBoth(ref, fileInfo);
    }

    private UserFileTaskVO toUserTaskVO(FileUploadTask task) {
        return fileModelMapper.toUserFileTaskVO(task);
    }
}
