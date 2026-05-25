package com.aisummarizer.website.dto;

import lombok.Data;

@Data
public class CheckoutRequest {

    private String priceId;
    private long quantity;
}
