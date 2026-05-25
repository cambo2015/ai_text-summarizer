package com.aisummarizer.website.dto;


import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class CheckoutResponse {
    private String checkoutUrl = "";
    public CheckoutResponse(String checkoutUrl) {
        this.checkoutUrl = checkoutUrl;
    }
}
