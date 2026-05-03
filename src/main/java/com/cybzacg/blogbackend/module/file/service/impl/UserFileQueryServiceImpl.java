package com.cybzacg.blogbackend.module.file.service.impl;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.file.FileBusinessInfo;
import com.cybzacg.blogbackend.domain.file.FileInfo;
import com.cybzacg.blogbackend.domain.file.FileUploadTask;
import com.cybzacg.blogbackend.module.file.convert.FileModelConvert;
import com.cybzacg.blogbackend.module.file.model.user.UserFilePageQuery;
import com.cybzacg.blogbackend.module.file.model.user.UserFileTaskPageQuery;
import com.cybzacg.blogbackend.module.file.model.user.UserFileTaskVO;
import com.cybzacg.blogbackend.module.file.model.user.UserFileVO;
import com.cybzacg.blogbackend.module.file.repository.FileBusinessInfoRepository;
import com.cybzacg.blogbackend.module.file.repository.FileInfoRepository;
import com.cybzacg.blogbackend.module.file.repository.FileUploadTaskRepository;
import com.cybzacg.blogbackend.module.file.service.UserFileQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户文件查询服务实现。
 * 负责用户文件列表查询、上传任务列表查询及相关业务逻辑。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserFileQueryServiceImpl implements UserFileQueryService {
    private final FileInfoRepository fileInfoRepository;
    private final FileUploadTaskRepository fileUploadTaskRepository;
    private final FileBusinessInfoRepository fileBusinessInfoRepository;
    private final FileModelConvert fileModelConvert;

    @Override
    public PageResult<UserFileVO> pageMyFiles(Long userId, UserFilePageQuery query) {
        // 设置分页默认值
        long current = query.getCurrent() == null ? 1L : query.getCurrent();
        long size = query.getSize() == null ? 10L : query.getSize();

        // 根据关键字或状态筛选文件ID集合（若无需筛选条件则不查）
        Set<Long> fileIds = null;
        if (org.springframework.util.StringUtils.hasText(query.getKeyword()) || query.getStatus() != null) {
            fileIds = fileInfoRepository.findIdsByStatusAndKeyword(query.getStatus(), query.getKeyword());
            // 筛选结果为空，直接返回空分页结果
            if (fileIds.isEmpty()) {
                log.debug("[文件查询] 用户 {} 无符合筛选条件的文件，keyword: {}，status: {}",
                        userId, query.getKeyword(), query.getStatus());
                return PageResult.empty(current, size);
            }
        }

        // 修正分页参数后执行分页查询
        query.setCurrent(current);
        query.setSize(size);
        var page = fileBusinessInfoRepository.pageByUserAndFilters(userId, query, fileIds);

        // 批量加载文件实体，构建 ID -> FileInfo 映射
        Map<Long, FileInfo> fileMap = loadFileInfoMap(page.getRecords().stream()
                .map(FileBusinessInfo::getFileId)
                .collect(Collectors.toSet()));

        // 将业务引用与文件实体组合转换为 VO，过滤无效记录
        List<UserFileVO> records = page.getRecords().stream()
                .map(ref -> toUserFileVO(ref, fileMap.get(ref.getFileId())))
                .filter(Objects::nonNull)
                .toList();

        log.debug("[文件查询] 用户 {} 文件列表查询完成，总数: {}，本页: {}", userId, page.getTotal(), records.size());
        return PageResult.of(page, records);
    }

    @Override
    public PageResult<UserFileTaskVO> pageMyUploadTasks(Long userId, UserFileTaskPageQuery query) {
        // 设置分页默认值
        query.setCurrent(query.getCurrent() == null ? 1L : query.getCurrent());
        query.setSize(query.getSize() == null ? 10L : query.getSize());

        // 分页查询用户的上传任务列表
        var page = fileUploadTaskRepository.pageByUserAndStatus(userId, query);

        // 转换任务实体为 VO
        List<UserFileTaskVO> records = page.getRecords().stream()
                .map(this::toUserTaskVO)
                .toList();

        log.debug("[任务查询] 用户 {} 上传任务列表查询完成，总数: {}，本页: {}",
                userId, page.getTotal(), records.size());
        return PageResult.of(page, records);
    }

    /**
     * 批量加载文件实体，构建 ID -> FileInfo 映射。
     * 用于在分页查询业务引用后，一次性加载关联的文件实体，避免 N+1 查询。
     *
     * @param ids 文件 ID 集合
     * @return 文件 ID 与文件实体的映射表
     */
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

    /**
     * 将文件业务引用与文件实体组合转换为用户文件 VO。
     * 若引用或文件实体为 null，则返回 null（由外层 filter 过滤）。
     *
     * @param ref     文件业务引用
     * @param fileInfo 文件实体
     * @return 用户文件 VO
     */
    private UserFileVO toUserFileVO(FileBusinessInfo ref, FileInfo fileInfo) {
        if (ref == null || fileInfo == null) {
            return null;
        }
        return fileModelConvert.toUserFileVOFromBoth(ref, fileInfo);
    }

    /**
     * 将上传任务实体转换为用户任务 VO。
     *
     * @param task 上传任务实体
     * @return 用户任务 VO
     */
    private UserFileTaskVO toUserTaskVO(FileUploadTask task) {
        return fileModelConvert.toUserFileTaskVO(task);
    }
}
