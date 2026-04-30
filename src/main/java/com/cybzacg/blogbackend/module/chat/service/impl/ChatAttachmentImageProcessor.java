package com.cybzacg.blogbackend.module.chat.service.impl;

import com.cybzacg.blogbackend.common.storage.MediaAssetPathUtils;
import com.cybzacg.blogbackend.common.storage.StorageService;
import com.cybzacg.blogbackend.domain.file.FileInfo;
import com.cybzacg.blogbackend.module.chat.model.common.ChatFilePayloadVO;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Objects;

/**
 * 聊天图片附件处理器。<p>负责图片元数据补全（宽高）和缩略图生成上传。</p>
 */
@Slf4j
@Component
class ChatAttachmentImageProcessor {
    private static final int IMAGE_THUMBNAIL_MAX_EDGE = 480;

    boolean enrichImagePayload(ChatFilePayloadVO payload, StorageService storageService, FileInfo fileInfo) throws Exception {
        try (InputStream inputStream = storageService.download(fileInfo.getFilePath())) {
            byte[] sourceBytes = inputStream.readAllBytes();
            BufferedImage sourceImage = ImageIO.read(new ByteArrayInputStream(sourceBytes));
            if (sourceImage == null) {
                return false;
            }
            boolean changed = false;
            if (!Objects.equals(payload.getWidth(), sourceImage.getWidth())) {
                payload.setWidth(sourceImage.getWidth());
                changed = true;
            }
            if (!Objects.equals(payload.getHeight(), sourceImage.getHeight())) {
                payload.setHeight(sourceImage.getHeight());
                changed = true;
            }
            String thumbnailUrl = uploadImageThumbnail(storageService, fileInfo, sourceImage);
            if (StrUtils.hasText(thumbnailUrl) && !Objects.equals(payload.getThumbnailUrl(), thumbnailUrl)) {
                payload.setThumbnailUrl(thumbnailUrl);
                changed = true;
            }
            return changed;
        }
    }

    private String uploadImageThumbnail(StorageService storageService, FileInfo fileInfo, BufferedImage sourceImage) throws Exception {
        BufferedImage thumbnail = scaleImage(sourceImage);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(thumbnail, "jpg", outputStream);
            return storageService.upload(
                    new ByteArrayInputStream(outputStream.toByteArray()),
                    MediaAssetPathUtils.buildChatImageThumbnailPath(fileInfo.getFilePath()),
                    "image/jpeg");
        }
    }

    private BufferedImage scaleImage(BufferedImage sourceImage) {
        int sourceWidth = sourceImage.getWidth();
        int sourceHeight = sourceImage.getHeight();
        int maxEdge = Math.max(sourceWidth, sourceHeight);
        if (maxEdge <= IMAGE_THUMBNAIL_MAX_EDGE) {
            return toRgbImage(sourceImage, sourceWidth, sourceHeight);
        }
        double ratio = IMAGE_THUMBNAIL_MAX_EDGE / (double) maxEdge;
        int targetWidth = Math.max(1, (int) Math.round(sourceWidth * ratio));
        int targetHeight = Math.max(1, (int) Math.round(sourceHeight * ratio));
        BufferedImage targetImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = targetImage.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(sourceImage, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }
        return targetImage;
    }

    private BufferedImage toRgbImage(BufferedImage sourceImage, int width, int height) {
        BufferedImage rgbImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgbImage.createGraphics();
        try {
            graphics.drawImage(sourceImage, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return rgbImage;
    }
}
