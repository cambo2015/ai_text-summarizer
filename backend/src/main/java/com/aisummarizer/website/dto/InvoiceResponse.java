package com.aisummarizer.website.dto;


import com.stripe.model.Invoice;
import lombok.Data;

import java.util.List;

@Data
public class InvoiceResponse {
    private List<InvoiceDTO> invoices;
    private String message = "";


}
