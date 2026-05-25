package com.aisummarizer.website.dto;


import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
public class CheckoutItemDTO {
   private List<CheckoutItem> checkoutItems;

}
