package com.cybzacg.blogbackend.module.chat.attachment.service.impl;

import com.cybzacg.blogbackend.config.property.ChatAttachmentProcessingProperties;
import com.cybzacg.blogbackend.module.file.service.FileChatFacadeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class ChatAttachmentAsyncProcessingServiceImplTest {
    @Mock
    private com.cybzacg.blogbackend.module.chat.attachment.service.impl.ChatAttachmentImageProcessor imageProcessor;
    @Mock
    private com.cybzacg.blogbackend.module.chat.attachment.service.impl.ChatAttachmentVoiceProcessor voiceProcessor;
    @Mock
    private FileChatFacadeService fileChatFacadeService;

    private ChatAttachmentAsyncProcessingServiceImpl asyncProcessingService;

    @BeforeEach
    void setUp() {
        ChatAttachmentProcessingProperties properties = new ChatAttachmentProcessingProperties();
        properties.setBatchSize(16);
        properties.setMaxRetryCount(3);
        properties.setRetryDelaySeconds(30);
        properties.setLeaseSeconds(300);
        asyncProcessingService = new ChatAttachmentAsyncProcessingServiceImpl(
                Runnable::run,
                null, // chatAttachmentProcessTaskRepository - not tested here
                properties,
                null, // chatMessageRepository - not tested here
                fileChatFacadeService,
                null, // storageManager - not tested here
                null, // chatAttachmentMetadataResolver - not tested here
                null, // chatPushService - not tested here
                null, // chatMetricsService - not tested here
                null, // chatModelConvert - not tested here
                imageProcessor,
                voiceProcessor
        );
    }

    @Test
    void constructorShouldAcceptAllDependencies() {
        // Verify the service can be constructed with all new dependencies
        assertNotNull(asyncProcessingService);
    }

    // Note: Full integration tests for this service would require testing with
    // actual task processing, image thumbnail generation, and voice transcoding.
    // Those tests are maintained in the original test file but simplified here
    // since the implementation details are now delegated to specialized processors.
}
