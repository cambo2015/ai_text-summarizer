package com.aisummarizer.website.dto;


import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class GetSummaryRequest {
    private UUID jobId;
}
