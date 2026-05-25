package com.aisummarizer.website.dto;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckoutItem {
    private String priceId;
    private long quantity;
}
