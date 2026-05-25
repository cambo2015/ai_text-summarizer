package com.aisummarizer.website.services;

import com.aisummarizer.website.dto.CheckoutItemDTO;
import com.aisummarizer.website.dto.Tiers;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Invoice;
import com.stripe.model.billing.MeterEvent;
import com.stripe.model.checkout.Session;
import com.stripe.param.billing.MeterEventCreateParams;
import com.stripe.param.checkout.SessionCreateParams;

import java.util.List;

public interface StripeService {
    public Session getCheckoutKey(String price,long quantity) throws StripeException;
    public void setApiKey();
    public Customer createCustomer(String name,String email) throws StripeException;
    public List<Invoice> getOpenInvoices(String userId) throws StripeException;

    public List<SessionCreateParams.LineItem> getLineItems(CheckoutItemDTO request) throws StripeException;

    public Tiers getTierFromString(String s);

    public void reportUsage(String stripeCustomerId, double hours) throws StripeException;
}
