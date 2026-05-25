package com.aisummarizer.website.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommonWordRequest {

    private String transcriptionFileName;
    private Integer top;
}
