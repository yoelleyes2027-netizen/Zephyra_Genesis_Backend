package com.zephyra.genesis.dto;

import java.time.LocalDateTime;

public record DocumentResponse(
        Long id,
        String name,
        Long size,
        String mimeType,
        LocalDateTime uploadedAt,
        String bucket,
        String s3Key
) {
}
