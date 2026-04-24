package com.cybzacg.blogbackend.common.storage;

import com.cybzacg.blogbackend.utils.StrUtils;

/**
 * 媒体派生资源路径工具。
 *
 * <p>当前主要为聊天图片缩略图和语音转码预览生成稳定的 sidecar 路径，
 * 便于异步任务写入，也便于原文件回收时一起尽力清理。
 */
public final class MediaAssetPathUtils {
    private static final String CHAT_IMAGE_THUMBNAIL_SUFFIX = "__chat_thumb.jpg";
    private static final String CHAT_VOICE_PREVIEW_SUFFIX = "__chat_preview.wav";

    private MediaAssetPathUtils() {
    }

    /** 根据原图路径生成聊天缩略图的 sidecar 路径。 */
    public static String buildChatImageThumbnailPath(String originalPath) {
        return appendSuffixBeforeExtension(originalPath, CHAT_IMAGE_THUMBNAIL_SUFFIX);
    }

    /** 根据原始语音路径生成转码预览的 sidecar 路径。 */
    public static String buildChatVoicePreviewPath(String originalPath) {
        return appendSuffixBeforeExtension(originalPath, CHAT_VOICE_PREVIEW_SUFFIX);
    }

    private static String appendSuffixBeforeExtension(String originalPath, String suffix) {
        String normalizedPath = StrUtils.trimToNull(originalPath);
        if (normalizedPath == null) {
            return null;
        }
        int dotIndex = normalizedPath.lastIndexOf('.');
        int slashIndex = Math.max(normalizedPath.lastIndexOf('/'), normalizedPath.lastIndexOf('\\'));
        if (dotIndex <= slashIndex) {
            return normalizedPath + suffix;
        }
        return normalizedPath.substring(0, dotIndex) + suffix;
    }
}
