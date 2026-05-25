package com.aisummarizer.website.services;
import com.aisummarizer.website.dto.CheckoutItem;
import com.aisummarizer.website.dto.CheckoutItemDTO;
import com.aisummarizer.website.dto.Tiers;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Invoice;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.InvoiceListParams;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.stripe.model.billing.MeterEvent;
import com.stripe.param.billing.MeterEventCreateParams;

import java.util.ArrayList;
import java.util.List;


@Service
public class StripeServiceImpl  implements   StripeService {

    @Value("${stripeApiKey}")
    private String stripeApiKey;

    @Value("${website.clientUrl}")
    private String domain;

    @PostConstruct
    public void init() {
        setApiKey();
    }

    @Override
    public Session getCheckoutKey(String priceId,long quantity) throws StripeException {
        SessionCreateParams params =
                SessionCreateParams.builder()
                        .setSuccessUrl(domain+"/success")
                        .setCancelUrl(domain+"/cancel")
                        .addLineItem(
                                SessionCreateParams.LineItem.builder()
                                        .setPrice(priceId)
                                        .setQuantity(quantity)
                                        .build()
                        )
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .build();

        return Session.create(params);
    }

    @Override
    public void setApiKey() {
        Stripe.apiKey = stripeApiKey;
    }

    public Customer createCustomer(String name,String email) throws StripeException {
        CustomerCreateParams params =
                CustomerCreateParams.builder()
                        .setName(name)
                        .setEmail(email)
                        .build();

        return Customer.create(params);
    }

    public Customer createCustomer(String email) throws StripeException {
        CustomerCreateParams params =
                CustomerCreateParams.builder()
                        .setEmail(email)
                        .build();

        return Customer.create(params);
    }

    public List<Invoice> getOpenInvoices(String stripeUserId) throws StripeException{

        InvoiceListParams params = InvoiceListParams.builder()
                .setCustomer(stripeUserId)
                .setStatus(InvoiceListParams.Status.OPEN)
                .setLimit(10L)
                .build();

        return  Invoice.list(params).getData();
    }
    public List<SessionCreateParams.LineItem> getLineItems(CheckoutItemDTO request) throws StripeException{

        List<SessionCreateParams.LineItem> lineItems = new ArrayList<>();
        for (CheckoutItem item : request.getCheckoutItems()) {
            // It is crucial to calculate prices on the backend to prevent fraud.
            // In a real application, you would look up the product price from your database
            // using the item's product ID, not trust the amount from the frontend.

            SessionCreateParams.LineItem lineItem = SessionCreateParams.LineItem.builder()
                    .setPrice(item.getPriceId())
                    .setQuantity(item.getQuantity())
                    .build();
            lineItems.add(lineItem);
        }
        return lineItems;
    }

    public Tiers getTierFromString(String s){
        return switch (s) {
            case "pro" -> Tiers.PRO;
            case "business" -> Tiers.BUSINESS;
            default -> Tiers.STARTER;
        };
    }

    //this sends updates to the stripe meter on the object
    public void reportUsage(String stripeCustomerId, double hours) throws StripeException {

        if (stripeCustomerId == null || hours <= 0) {
            return;
        }

        double minutes = Math.ceil(hours * 60.0);
        double billableHours = minutes/ 60.0;
        double formatted =  Math.round(billableHours * 1_000_000d) / 1_000_000d;
        System.out.println("reportUsage() 1. billableHours = " + billableHours);
        MeterEventCreateParams params =
                MeterEventCreateParams.builder()
                        .setEventName("hours_used")
                        .putPayload("stripe_customer_id", stripeCustomerId)
                        .putPayload("value", String.valueOf(formatted))
                        .build();

        MeterEvent.create(params);
    }
}
