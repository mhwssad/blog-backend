package com.cybzacg.blogbackend.module.migration.service.impl;

import cn.idev.excel.ExcelWriter;
import cn.idev.excel.FastExcel;
import cn.idev.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.dto.domain.auth.SysUser;
import com.cybzacg.blogbackend.dto.domain.content.SysCategory;
import com.cybzacg.blogbackend.dto.domain.content.SysTag;
import com.cybzacg.blogbackend.dto.domain.migration.BlogMigrationAttachment;
import com.cybzacg.blogbackend.dto.domain.migration.BlogMigrationRecord;
import com.cybzacg.blogbackend.dto.domain.migration.BlogMigrationTask;
import com.cybzacg.blogbackend.dto.repository.auth.account.SysUserRepository;
import com.cybzacg.blogbackend.dto.repository.content.SysCategoryRepository;
import com.cybzacg.blogbackend.dto.repository.content.SysTagRepository;
import com.cybzacg.blogbackend.dto.repository.migration.BlogMigrationAttachmentRepository;
import com.cybzacg.blogbackend.dto.repository.migration.BlogMigrationRecordRepository;
import com.cybzacg.blogbackend.dto.repository.migration.BlogMigrationTaskRepository;
import com.cybzacg.blogbackend.enums.article.ArticleVisibilityScopeEnum;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.enums.migration.BlogMigrationAttachmentStatusEnum;
import com.cybzacg.blogbackend.enums.migration.BlogMigrationRecordStatusEnum;
import com.cybzacg.blogbackend.enums.migration.BlogMigrationTaskStatusEnum;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleDetailVO;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleSaveRequest;
import com.cybzacg.blogbackend.module.article.service.ArticleAdminService;
import com.cybzacg.blogbackend.module.auth.author.service.AuthorPermissionService;
import com.cybzacg.blogbackend.module.migration.convert.BlogMigrationModelConvert;
import com.cybzacg.blogbackend.module.migration.model.admin.*;
import com.cybzacg.blogbackend.module.migration.model.data.BlogMigrationAttachmentItem;
import com.cybzacg.blogbackend.module.migration.model.data.BlogMigrationImportFile;
import com.cybzacg.blogbackend.module.migration.model.data.BlogMigrationPostItem;
import com.cybzacg.blogbackend.module.migration.model.internal.BlogMigrationDownloadedAttachment;
import com.cybzacg.blogbackend.module.migration.service.BlogMigrationAdminService;
import com.cybzacg.blogbackend.module.migration.service.BlogMigrationAttachmentImportService;
import com.cybzacg.blogbackend.utils.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 外部博客迁移后台服务实现。
 *
 * <p>迁移服务负责任务状态、预检、幂等和统计收口；文章落库统一复用后台文章创建服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlogMigrationAdminServiceImpl implements BlogMigrationAdminService {
    private static final String ARTICLE_TARGET_TYPE = "article";

    private final BlogMigrationTaskRepository blogMigrationTaskRepository;
    private final BlogMigrationRecordRepository blogMigrationRecordRepository;
    private final BlogMigrationAttachmentRepository blogMigrationAttachmentRepository;
    private final SysUserRepository sysUserRepository;
    private final AuthorPermissionService authorPermissionService;
    private final SysCategoryRepository sysCategoryRepository;
    private final SysTagRepository sysTagRepository;
    private final ArticleAdminService articleAdminService;
    private final BlogMigrationAttachmentImportService blogMigrationAttachmentImportService;
    private final BlogMigrationModelConvert blogMigrationModelConvert;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BlogMigrationTaskVO createTask(BlogMigrationCreateRequest request, MultipartFile file, Long operatorId) {
        validateCreateRequest(request, file);
        String fileContentJson = readFileContent(file);
        BlogMigrationImportFile importFile = parseMigrationFile(fileContentJson);
        validateImportFile(importFile);

        BlogMigrationTask task = new BlogMigrationTask();
        task.setSourcePlatform(normalizeSourcePlatform(importFile.getSourcePlatform()));
        task.setOriginalFileName(file.getOriginalFilename());
        task.setFileMd5(resolveFileMd5(file));
        task.setFileSize(file.getSize());
        task.setFileContentJson(fileContentJson);
        task.setAuthorId(request.getAuthorId());
        task.setStatus(BlogMigrationTaskStatusEnum.CREATED.getValue());
        task.setTotalCount(safePostCount(importFile));
        task.setSuccessCount(0);
        task.setFailCount(0);
        task.setSkipCount(0);
        task.setCreatedBy(operatorId);
        task.setUpdatedBy(operatorId);
        task.setRemark(request.getRemark());
        blogMigrationTaskRepository.save(task);
        return blogMigrationModelConvert.toTaskVO(task);
    }

    /**
     * 执行迁移预检，失败明细写入记录表并随响应返回，不把普通预检失败转换为异常。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BlogMigrationPrecheckResultVO precheck(Long taskId, Long operatorId) {
        BlogMigrationTask task = getTaskOrThrow(taskId);
        validateTaskPrecheckState(task);
        BlogMigrationImportFile importFile = parseMigrationFile(task.getFileContentJson());
        validateImportFile(importFile);
        validateAuthor(task.getAuthorId());

        blogMigrationRecordRepository.removeByTaskId(task.getId());
        blogMigrationAttachmentRepository.removeByTaskId(task.getId());

        Map<String, Integer> duplicateMap = countIdempotentKeys(importFile);
        List<BlogMigrationRecordVO> errors = new ArrayList<>();
        for (BlogMigrationPostItem postItem : safePosts(importFile)) {
            String error = validatePost(importFile.getSourcePlatform(), postItem, duplicateMap);
            if (error != null) {
                BlogMigrationRecord record = createOrUpdateRecord(task, importFile.getSourcePlatform(), postItem,
                        BlogMigrationRecordStatusEnum.FAILED.getValue(), null, error);
                errors.add(blogMigrationModelConvert.toRecordVO(record));
            }
        }

        boolean passed = errors.isEmpty();
        task.setStatus(passed ? BlogMigrationTaskStatusEnum.PRECHECKED.getValue() : BlogMigrationTaskStatusEnum.CREATED.getValue());
        task.setPrecheckedAt(LocalDateTime.now());
        task.setUpdatedBy(operatorId);
        task.setErrorSummary(passed ? null : buildErrorSummary(errors));
        blogMigrationTaskRepository.updateById(task);

        BlogMigrationPrecheckResultVO result = new BlogMigrationPrecheckResultVO();
        result.setTaskId(task.getId());
        result.setTotalCount(safePostCount(importFile));
        result.setPassed(passed);
        result.setErrors(errors);
        return result;
    }

    /**
     * 同步执行导入任务。单篇文章失败会落失败记录并继续处理后续文章，最终回写任务统计。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BlogMigrationTaskVO execute(Long taskId, Long operatorId) {
        BlogMigrationTask task = getTaskOrThrow(taskId);
        validateExecuteState(task);
        BlogMigrationImportFile importFile = parseMigrationFile(task.getFileContentJson());
        validateImportFile(importFile);

        task.setStatus(BlogMigrationTaskStatusEnum.RUNNING.getValue());
        task.setStartedAt(LocalDateTime.now());
        task.setUpdatedBy(operatorId);
        blogMigrationTaskRepository.updateById(task);

        int success = 0;
        int fail = 0;
        int skip = 0;
        for (BlogMigrationPostItem postItem : safePosts(importFile)) {
            String sourcePlatform = importFile.getSourcePlatform();
            String idempotentKey = buildIdempotentKey(sourcePlatform, postItem.getExternalPostId());
            BlogMigrationRecord globalExisting = blogMigrationRecordRepository.findByIdempotentKey(idempotentKey);
            if (isImportedByOtherTask(task, globalExisting)) {
                createOrUpdateRecord(task, sourcePlatform, postItem,
                        BlogMigrationRecordStatusEnum.SKIPPED.getValue(),
                        globalExisting.getTargetArticleId(), "幂等键已导入，跳过");
                skip++;
                continue;
            }

            BlogMigrationRecord record = createOrUpdateRecord(task, sourcePlatform, postItem,
                    BlogMigrationRecordStatusEnum.PENDING.getValue(), null, null);
            try {
                Long articleId = importPost(task, postItem, record);
                record.setStatus(BlogMigrationRecordStatusEnum.SUCCESS.getValue());
                record.setTargetArticleId(articleId);
                record.setErrorMessage(null);
                blogMigrationRecordRepository.updateById(record);
                success++;
            } catch (BusinessException ex) {
                fail++;
                updateFailedRecord(task, sourcePlatform, postItem, ex.getMessage());
            } catch (Exception ex) {
                fail++;
                updateFailedRecord(task, sourcePlatform, postItem, ex.getMessage());
            }
        }

        task.setSuccessCount(success);
        task.setFailCount(fail);
        task.setSkipCount(skip);
        task.setCompletedAt(LocalDateTime.now());
        task.setUpdatedBy(operatorId);
        task.setStatus(fail == 0 ? BlogMigrationTaskStatusEnum.COMPLETED.getValue() : BlogMigrationTaskStatusEnum.FAILED.getValue());
        task.setErrorSummary(fail == 0 ? null : "存在 " + fail + " 篇文章导入失败");
        blogMigrationTaskRepository.updateById(task);
        return blogMigrationModelConvert.toTaskVO(task);
    }

    @Override
    public PageResult<BlogMigrationTaskVO> pageTasks(BlogMigrationTaskPageQuery query) {
        query.setCurrent(PaginationUtils.normalizeCurrent(query.getCurrent()));
        query.setSize(PaginationUtils.normalizeSize(query.getSize(), 20L, 100L));
        if (StrUtils.hasText(query.getSourcePlatform())) {
            query.setSourcePlatform(normalizeSourcePlatform(query.getSourcePlatform()));
        }
        Page<BlogMigrationTask> page = blogMigrationTaskRepository.pageByQuery(query);
        List<BlogMigrationTaskVO> records = page.getRecords().stream()
                .map(blogMigrationModelConvert::toTaskVO)
                .toList();
        return PageResult.of(page, records);
    }

    @Override
    public BlogMigrationTaskVO getTask(Long taskId) {
        return blogMigrationModelConvert.toTaskVO(getTaskOrThrow(taskId));
    }

    @Override
    public PageResult<BlogMigrationRecordVO> pageRecords(BlogMigrationRecordPageQuery query) {
        query.setCurrent(PaginationUtils.normalizeCurrent(query.getCurrent()));
        query.setSize(PaginationUtils.normalizeSize(query.getSize(), 20L, 100L));
        Page<BlogMigrationRecord> page = blogMigrationRecordRepository.pageByQuery(query);
        List<BlogMigrationRecordVO> records = page.getRecords().stream()
                .map(blogMigrationModelConvert::toRecordVO)
                .toList();
        return PageResult.of(page, records);
    }

    @Override
    public byte[] exportFailures(Long taskId) {
        getTaskOrThrow(taskId);
        List<BlogMigrationRecord> failures = blogMigrationRecordRepository.listFailuresByTaskId(taskId);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ExcelWriter excelWriter = FastExcel.write(outputStream).autoCloseStream(false).build()) {
            WriteSheet sheet = FastExcel.writerSheet(0, "失败记录")
                    .head(List.of(
                            List.of("外部文章ID"),
                            List.of("原始标题"),
                            List.of("错误信息")))
                    .build();
            List<List<Object>> rows = failures.stream()
                    .map(record -> List.<Object>of(
                            defaultString(record.getExternalPostId()),
                            defaultString(record.getOriginalTitle()),
                            defaultString(record.getErrorMessage())))
                    .toList();
            excelWriter.write(rows, sheet);
            excelWriter.finish();
            return outputStream.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("导出迁移失败记录失败", ex);
        }
    }

    private void validateCreateRequest(BlogMigrationCreateRequest request, MultipartFile file) {
        ExceptionThrowerCore.throwBusinessIfNull(request, ResultErrorCode.ILLEGAL_ARGUMENT, "请求不能为空");
        ExceptionThrowerCore.throwBusinessIfNull(file, ResultErrorCode.MIGRATION_FILE_INVALID, "迁移文件不能为空");
        ExceptionThrowerCore.throwBusinessIf(file.isEmpty(), ResultErrorCode.MIGRATION_FILE_INVALID, "迁移文件不能为空");
        validateAuthor(request.getAuthorId());
    }

    private void validateAuthor(Long authorId) {
        ExceptionThrowerCore.throwBusinessIfNull(authorId, ResultErrorCode.ILLEGAL_ARGUMENT, "作者不能为空");
        SysUser author = sysUserRepository.getById(authorId);
        ExceptionThrowerCore.throwBusinessIf(author == null || Integer.valueOf(1).equals(author.getDeletedFlag()),
                ResultErrorCode.USER_NOT_FOUND, "作者不存在");
        ExceptionThrowerCore.throwBusinessIfNot(authorPermissionService.hasAuthorRole(authorId),
                ResultErrorCode.ILLEGAL_ARGUMENT, "作者不具备文章创建资格");
    }

    private BlogMigrationImportFile parseMigrationFile(String json) {
        try {
            return JsonUtils.fromJson(json, BlogMigrationImportFile.class);
        } catch (Exception e) {
            ExceptionThrowerCore.throwBusiness(ResultErrorCode.MIGRATION_FILE_INVALID, "迁移文件格式错误");
            return null;
        }
    }

    private void validateImportFile(BlogMigrationImportFile importFile) {
        ExceptionThrowerCore.throwBusinessIfNull(importFile, ResultErrorCode.MIGRATION_FILE_INVALID, "迁移文件内容不能为空");
        normalizeSourcePlatform(importFile.getSourcePlatform());
        ExceptionThrowerCore.throwBusinessIf(CollectionUtils.isEmpty(importFile.getPosts()),
                ResultErrorCode.MIGRATION_FILE_INVALID, "迁移文件文章列表不能为空");
    }

    private String readFileContent(MultipartFile file) {
        try {
            return new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            ExceptionThrowerCore.throwBusiness(ResultErrorCode.MIGRATION_FILE_INVALID, "无法读取迁移文件");
            return null;
        }
    }

    private String resolveFileMd5(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                md5.update(buffer, 0, len);
            }
            return FileUtils.toHex(md5.digest());
        } catch (Exception e) {
            ExceptionThrowerCore.throwBusiness(ResultErrorCode.MIGRATION_FILE_INVALID, "无法计算迁移文件MD5");
            return null;
        }
    }

    private int safePostCount(BlogMigrationImportFile importFile) {
        return safePosts(importFile).size();
    }

    private List<BlogMigrationPostItem> safePosts(BlogMigrationImportFile importFile) {
        return importFile == null || importFile.getPosts() == null ? List.of() : importFile.getPosts();
    }

    private String normalizeSourcePlatform(String sourcePlatform) {
        ExceptionThrowerCore.throwBusinessIfBlank(sourcePlatform, ResultErrorCode.MIGRATION_FILE_INVALID, "来源平台不能为空");
        return sourcePlatform.trim().toLowerCase(Locale.ROOT);
    }

    private void validateTaskPrecheckState(BlogMigrationTask task) {
        ExceptionThrowerCore.throwBusinessIf(!BlogMigrationTaskStatusEnum.CREATED.getValue().equals(task.getStatus()),
                ResultErrorCode.MIGRATION_TASK_STATUS_INVALID, "任务状态不允许预检");
    }

    private void validateExecuteState(BlogMigrationTask task) {
        ExceptionThrowerCore.throwBusinessIf(!BlogMigrationTaskStatusEnum.PRECHECKED.getValue().equals(task.getStatus()),
                ResultErrorCode.MIGRATION_TASK_STATUS_INVALID, "任务状态不允许执行");
    }

    private Map<String, Integer> countIdempotentKeys(BlogMigrationImportFile importFile) {
        Map<String, Integer> duplicateMap = new HashMap<>();
        for (BlogMigrationPostItem postItem : safePosts(importFile)) {
            if (postItem == null || !StrUtils.hasText(postItem.getExternalPostId())) {
                continue;
            }
            duplicateMap.merge(buildIdempotentKey(importFile.getSourcePlatform(), postItem.getExternalPostId()), 1, Integer::sum);
        }
        return duplicateMap;
    }

    private BlogMigrationRecord createOrUpdateRecord(BlogMigrationTask task, String sourcePlatform,
                                                     BlogMigrationPostItem postItem, Integer status,
                                                     Long articleId, String errorMessage) {
        String externalPostId = postItem == null ? null : postItem.getExternalPostId();
        String idempotentKey = StrUtils.hasText(externalPostId)
                ? buildIdempotentKey(sourcePlatform, externalPostId)
                : normalizeSourcePlatform(sourcePlatform) + ":invalid:" + System.nanoTime();
        BlogMigrationRecord record = blogMigrationRecordRepository.findByTaskIdAndIdempotentKey(task.getId(), idempotentKey);
        if (record == null) {
            record = new BlogMigrationRecord();
            record.setTaskId(task.getId());
            record.setSourcePlatform(normalizeSourcePlatform(sourcePlatform));
            record.setExternalPostId(externalPostId);
            record.setIdempotentKey(idempotentKey);
            record.setOriginalTitle(postItem == null ? null : postItem.getTitle());
            record.setRawContentJson(JsonUtils.toJson(postItem));
        }
        record.setStatus(status);
        record.setTargetArticleId(articleId);
        record.setErrorMessage(errorMessage);
        if (record.getId() == null) {
            blogMigrationRecordRepository.save(record);
        } else {
            blogMigrationRecordRepository.updateById(record);
        }
        return record;
    }

    private String validatePost(String sourcePlatform, BlogMigrationPostItem postItem, Map<String, Integer> duplicateMap) {
        if (postItem == null) {
            return "文章数据为空";
        }
        if (!StrUtils.hasText(postItem.getExternalPostId())) {
            return "外部文章ID不能为空";
        }
        if (duplicateMap.getOrDefault(buildIdempotentKey(sourcePlatform, postItem.getExternalPostId()), 0) > 1) {
            return "外部文章ID重复";
        }
        if (!StrUtils.hasText(postItem.getTitle())) {
            return "文章标题不能为空";
        }
        String categoryError = validateCategories(postItem.getCategoryCodes());
        if (categoryError != null) {
            return categoryError;
        }
        String tagError = validateTags(postItem.getTagNames());
        if (tagError != null) {
            return tagError;
        }
        if (hasInvalidAttachmentUrl(postItem)) {
            return "附件URL无效";
        }
        return null;
    }

    private String validateCategories(List<String> categoryCodes) {
        List<String> normalized = normalizeTextList(categoryCodes);
        if (CollectionUtils.isEmpty(normalized)) {
            return null;
        }
        List<SysCategory> categories = sysCategoryRepository.listByTypeStatusAndCodes(ARTICLE_TARGET_TYPE, 1, normalized);
        Set<String> existingCodes = categories.stream()
                .map(SysCategory::getCode)
                .filter(StrUtils::hasText)
                .collect(Collectors.toSet());
        return existingCodes.containsAll(normalized) ? null : "分类不存在或未启用";
    }

    private String validateTags(List<String> tagNames) {
        List<String> normalized = normalizeTextList(tagNames);
        if (CollectionUtils.isEmpty(normalized)) {
            return null;
        }
        List<SysTag> tags = sysTagRepository.listByNames(normalized);
        Set<String> existingNames = tags.stream()
                .map(SysTag::getName)
                .filter(StrUtils::hasText)
                .collect(Collectors.toSet());
        return existingNames.containsAll(normalized) ? null : "标签不存在";
    }

    private boolean hasInvalidAttachmentUrl(BlogMigrationPostItem postItem) {
        if (StrUtils.hasText(postItem.getCoverImageUrl()) && !isHttpUrl(postItem.getCoverImageUrl())) {
            return true;
        }
        for (BlogMigrationAttachmentItem attachmentItem : safeAttachmentItems(postItem)) {
            if (attachmentItem != null && StrUtils.hasText(attachmentItem.getUrl()) && !isHttpUrl(attachmentItem.getUrl())) {
                return true;
            }
        }
        return false;
    }

    private boolean isHttpUrl(String url) {
        try {
            URI uri = URI.create(url);
            return uri.getScheme() != null
                    && (uri.getScheme().equalsIgnoreCase("http") || uri.getScheme().equalsIgnoreCase("https"));
        } catch (Exception e) {
            return false;
        }
    }

    private Long importPost(BlogMigrationTask task, BlogMigrationPostItem postItem, BlogMigrationRecord record) {
        Map<String, BlogMigrationDownloadedAttachment> downloaded = downloadAttachments(task, record, postItem);
        ArticleSaveRequest request = buildArticleRequest(task, postItem, downloaded);
        ArticleDetailVO article = articleAdminService.createArticle(request);
        return article.getId();
    }

    /**
     * 附件先下载入库并替换外部 URL；任一附件失败时，该文章不进入创建链路。
     */
    private Map<String, BlogMigrationDownloadedAttachment> downloadAttachments(BlogMigrationTask task,
                                                                               BlogMigrationRecord record,
                                                                               BlogMigrationPostItem postItem) {
        Map<String, BlogMigrationDownloadedAttachment> result = new LinkedHashMap<>();
        for (BlogMigrationAttachmentItem attachmentItem : collectAttachmentItems(postItem)) {
            String externalUrl = attachmentItem.getUrl();
            if (result.containsKey(externalUrl)) {
                saveAttachmentRecord(task, record, attachmentItem, result.get(externalUrl), null,
                        BlogMigrationAttachmentStatusEnum.SKIPPED.getValue());
                continue;
            }
            try {
                BlogMigrationDownloadedAttachment downloaded = blogMigrationAttachmentImportService.downloadAndSave(task, attachmentItem);
                result.put(externalUrl, downloaded);
                saveAttachmentRecord(task, record, attachmentItem, downloaded, null,
                        BlogMigrationAttachmentStatusEnum.SUCCESS.getValue());
            } catch (BusinessException ex) {
                saveAttachmentRecord(task, record, attachmentItem, null, ex.getMessage(),
                        BlogMigrationAttachmentStatusEnum.FAILED.getValue());
                throw ex;
            } catch (Exception ex) {
                saveAttachmentRecord(task, record, attachmentItem, null, ex.getMessage(),
                        BlogMigrationAttachmentStatusEnum.FAILED.getValue());
                throw new BusinessException(ResultErrorCode.MIGRATION_ATTACHMENT_DOWNLOAD_FAILED.getCode(), "附件下载失败");
            }
        }
        return result;
    }

    private void saveAttachmentRecord(BlogMigrationTask task, BlogMigrationRecord record,
                                      BlogMigrationAttachmentItem attachmentItem,
                                      BlogMigrationDownloadedAttachment downloaded,
                                      String errorMessage, Integer status) {
        BlogMigrationAttachment attachment = new BlogMigrationAttachment();
        attachment.setTaskId(task.getId());
        attachment.setRecordId(record.getId());
        attachment.setExternalUrl(attachmentItem.getUrl());
        attachment.setOriginalName(attachmentItem.getOriginalName());
        attachment.setStatus(status);
        if (downloaded != null) {
            attachment.setFileId(downloaded.getFileId());
            attachment.setFileUrl(downloaded.getFileUrl());
        }
        attachment.setErrorMessage(errorMessage);
        blogMigrationAttachmentRepository.save(attachment);
    }

    private ArticleSaveRequest buildArticleRequest(BlogMigrationTask task, BlogMigrationPostItem postItem,
                                                   Map<String, BlogMigrationDownloadedAttachment> downloaded) {
        ArticleSaveRequest request = new ArticleSaveRequest();
        request.setTitle(postItem.getTitle());
        request.setSummary(postItem.getSummary());
        request.setContent(replaceExternalUrls(postItem.getContent(), downloaded));
        request.setCoverImage(replaceExternalUrls(postItem.getCoverImageUrl(), downloaded));
        request.setAuthorId(task.getAuthorId());
        request.setIsOriginal(postItem.getIsOriginal());
        request.setSourceUrl(postItem.getSourceUrl());
        request.setStatus(postItem.getStatus());
        request.setPublishTime(postItem.getPublishTime());
        request.setVisibilityScope(ArticleVisibilityScopeEnum.PUBLIC.getValue());
        request.setAccessLevel(0);
        request.setCategoryIds(resolveCategoryIds(postItem.getCategoryCodes()));
        request.setTagIds(resolveTagIds(postItem.getTagNames()));
        request.setAccessList(List.of());
        return request;
    }

    private String replaceExternalUrls(String value, Map<String, BlogMigrationDownloadedAttachment> downloaded) {
        if (!StrUtils.hasText(value) || downloaded.isEmpty()) {
            return value;
        }
        String result = value;
        for (Map.Entry<String, BlogMigrationDownloadedAttachment> entry : downloaded.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue().getFileUrl());
        }
        return result;
    }

    private List<Long> resolveCategoryIds(List<String> categoryCodes) {
        List<String> normalized = normalizeTextList(categoryCodes);
        if (CollectionUtils.isEmpty(normalized)) {
            return List.of();
        }
        Map<String, Long> categoryMap = sysCategoryRepository.listByTypeStatusAndCodes(ARTICLE_TARGET_TYPE, 1, normalized).stream()
                .collect(Collectors.toMap(SysCategory::getCode, SysCategory::getId, (left, right) -> left));
        return normalized.stream()
                .map(categoryMap::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<Long> resolveTagIds(List<String> tagNames) {
        List<String> normalized = normalizeTextList(tagNames);
        if (CollectionUtils.isEmpty(normalized)) {
            return List.of();
        }
        Map<String, Long> tagMap = sysTagRepository.listByNames(normalized).stream()
                .collect(Collectors.toMap(SysTag::getName, SysTag::getId, (left, right) -> left));
        return normalized.stream()
                .map(tagMap::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<BlogMigrationAttachmentItem> collectAttachmentItems(BlogMigrationPostItem postItem) {
        Map<String, BlogMigrationAttachmentItem> items = new LinkedHashMap<>();
        for (BlogMigrationAttachmentItem item : safeAttachmentItems(postItem)) {
            if (item != null && StrUtils.hasText(item.getUrl())) {
                items.putIfAbsent(item.getUrl().trim(), normalizeAttachmentItem(item));
            }
        }
        if (StrUtils.hasText(postItem.getCoverImageUrl())) {
            BlogMigrationAttachmentItem cover = new BlogMigrationAttachmentItem();
            cover.setUrl(postItem.getCoverImageUrl().trim());
            cover.setOriginalName("cover");
            items.putIfAbsent(cover.getUrl(), cover);
        }
        return new ArrayList<>(items.values());
    }

    private List<BlogMigrationAttachmentItem> safeAttachmentItems(BlogMigrationPostItem postItem) {
        return postItem == null || postItem.getAttachments() == null ? List.of() : postItem.getAttachments();
    }

    private BlogMigrationAttachmentItem normalizeAttachmentItem(BlogMigrationAttachmentItem item) {
        BlogMigrationAttachmentItem normalized = new BlogMigrationAttachmentItem();
        normalized.setUrl(item.getUrl().trim());
        normalized.setOriginalName(item.getOriginalName());
        return normalized;
    }

    private List<String> normalizeTextList(Collection<String> values) {
        if (CollectionUtils.isEmpty(values)) {
            return List.of();
        }
        return values.stream()
                .filter(StrUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();
    }

    private boolean isImportedByOtherTask(BlogMigrationTask task, BlogMigrationRecord globalExisting) {
        return globalExisting != null
                && !Objects.equals(globalExisting.getTaskId(), task.getId())
                && BlogMigrationRecordStatusEnum.SUCCESS.getValue().equals(globalExisting.getStatus())
                && globalExisting.getTargetArticleId() != null;
    }

    private void updateFailedRecord(BlogMigrationTask task, String sourcePlatform, BlogMigrationPostItem postItem,
                                    String errorMessage) {
        createOrUpdateRecord(task, sourcePlatform, postItem,
                BlogMigrationRecordStatusEnum.FAILED.getValue(), null, errorMessage);
    }

    private BlogMigrationTask getTaskOrThrow(Long taskId) {
        return ExceptionThrowerCore.requireNonNull(blogMigrationTaskRepository.getById(taskId),
                ResultErrorCode.MIGRATION_TASK_NOT_FOUND);
    }

    private String buildIdempotentKey(String sourcePlatform, String externalPostId) {
        return normalizeSourcePlatform(sourcePlatform) + ":" + externalPostId.trim();
    }

    private String buildErrorSummary(List<BlogMigrationRecordVO> errors) {
        return errors.stream()
                .map(BlogMigrationRecordVO::getErrorMessage)
                .filter(StrUtils::hasText)
                .limit(3)
                .collect(Collectors.joining("; "));
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}
