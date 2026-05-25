package com.aisummarizer.website.dto;

import java.util.UUID;

public record AudioFileDto(
        UUID id,
        String name,
        long size,
        String summaryFileName,
        String transcriptionFileName,
        String originalFileName
) {}

