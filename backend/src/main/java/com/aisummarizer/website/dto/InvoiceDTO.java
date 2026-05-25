package com.aisummarizer.website.dto;


import lombok.Data;

import java.math.BigDecimal;

@Data
public class InvoiceDTO {
    private String id;
    private Long total;
    private Long amountRemaining; // amount_remaining
    private String currency;
    private String status;
    private Long created;
    private String invoiceUrl;
}

