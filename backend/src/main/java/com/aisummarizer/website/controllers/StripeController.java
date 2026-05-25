package com.aisummarizer.website.controllers;


import com.aisummarizer.website.aspects.RequiresSubscription;
import com.aisummarizer.website.dao.UserRepository;
import com.aisummarizer.website.dto.*;
import com.aisummarizer.website.entities.AppUser;
import com.aisummarizer.website.helpers.StorageQuota;
import com.aisummarizer.website.services.StripeService;
import com.aisummarizer.website.services.UserService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.apache.coyote.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;



@RestController
@RequestMapping("/api/stripe")
public class StripeController {
    private static final Logger logger = LoggerFactory.getLogger(StripeController.class);

    @Value("${website.clientUrl}")
    private String clientURL;

//    STARTER
    @Value("${stripe.price.starter.flat}")
    private String starterFlatPrice;
    @Value("${stripe.price.starter.usage}")
    private String starterUsagePrice;

//    PRO
    @Value("${stripe.price.pro.flat}")
    private String proFlatPrice;

    @Value("${stripe.price.pro.usage}")
    private String proUsagePrice;

//    BUSINESS
    @Value("${stripe.price.business.flat}")
    private String businessFlatPrice;

    @Value("${stripe.price.business.usage}")
    private String businessUsagePrice;

//    other values
    @Value("${stripe.webhook-secret}")
    private String stripeWebhookSecret;

    StripeService stripeService;

    UserRepository userRepository;

    UserService userService;

    @Enumerated(EnumType.STRING)
    private Tiers tier;

    public StripeController(StripeService stripeService, UserRepository userRepository,UserService userService) {
        this.stripeService = stripeService;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @PostMapping("/invoices")
    public ResponseEntity<InvoiceResponse> getInvoices(){

        String email = userService.getUserEmail();
        //get user
        Optional<AppUser> auOptional = userRepository.findByUsername(email);
        if(auOptional.isPresent()){
            AppUser appUser =  auOptional.get();
            try{
                List<Invoice> invoices = stripeService.getOpenInvoices(appUser.getStripeCustomerId());

                InvoiceResponse response = new InvoiceResponse();
                List<InvoiceDTO> dtos = invoices.stream().map(inv -> {
                    InvoiceDTO dto = new InvoiceDTO();
                    dto.setId(inv.getId());
                    // These are direct getters; no expand needed
                    dto.setAmountRemaining(inv.getAmountRemaining());
                    dto.setTotal(inv.getTotal());
                    dto.setCurrency(inv.getCurrency());
                    dto.setStatus(inv.getStatus());
                    dto.setCreated(inv.getCreated());
                    dto.setInvoiceUrl(inv.getHostedInvoiceUrl());
                    return dto;
                }).toList();
                response.setInvoices(dtos);
                response.setMessage("success");
                return  ResponseEntity.ok(response);
            }
            catch(Exception e){
                InvoiceResponse response = new InvoiceResponse();
                response.setInvoices(null);
                response.setMessage("Server had an error and could not give the results.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        }

        InvoiceResponse response = new InvoiceResponse();
        response.setInvoices(null);
        response.setMessage("Please login to see your invoices.");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }


    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkout(@RequestBody  Map<String,String> request){

        String plan =  request.get("plan");
        if(plan == null || plan.isBlank()){
            return ResponseEntity.badRequest().build();
        }
        try{
            AppUser user = userService.getCurrentUser();
            if(user == null){
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            //get the selected plan
            Tiers tier = stripeService.getTierFromString(plan);

            String flatPrice;
            String usagePrice;

            switch(tier) {
                case STARTER:
                    flatPrice = starterFlatPrice;
                    usagePrice = starterUsagePrice;
                    break;
                case PRO:
                    flatPrice = proFlatPrice;
                    usagePrice = proUsagePrice;
                    break;
                case BUSINESS:
                    flatPrice = businessFlatPrice;
                    usagePrice = businessUsagePrice;
                    break;
                default:
                    return ResponseEntity.badRequest().build();
            }

            SessionCreateParams.Builder builder = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setSuccessUrl(clientURL + "/success?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(clientURL + "/cancel")
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setPrice(flatPrice)
                                    .setQuantity(1L)
                                    .build()
                    )
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setPrice(usagePrice)
                                    .build()
                    );

            if(user.getStripeCustomerId() != null){
                builder.setCustomer(user.getStripeCustomerId());
            }
            Session session = Session.create(builder.build());
            return ResponseEntity.ok(
                    new CheckoutResponse(session.getUrl())
            );

        } catch (StripeException e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new CheckoutResponse(""));
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader
    ) throws SignatureVerificationException {
        Event event = Webhook.constructEvent(
                payload,
                sigHeader,
                stripeWebhookSecret
        );

        try{
            switch (event.getType()) {

                case "checkout.session.completed" -> {
                    Session session = (Session) event.getDataObjectDeserializer()
                            .getObject().orElseThrow();

                    String customerId = session.getCustomer();
                    String subscriptionId = session.getSubscription();

                    //retrieve the full subscription from Stripe
                    Subscription subscription = Subscription.retrieve(subscriptionId);

//                    Get first subscription item (flat price)
                    String priceId = subscription.getItems()
                            .getData()
                            .get(0)
                            .getPrice()
                            .getId();

                    AppUser user = userRepository.findByStripeCustomerId(customerId)
                            .orElseThrow();

                    user.setSubscribed(true);
                    user.setStripeSubscriptionId(subscriptionId);

                    if(priceId.equals(starterFlatPrice)){
                        Tiers tier = Tiers.STARTER;
                        user.setTiers(tier);
                        user.setQuotaFileSizeBytes(StorageQuota.getQuotaFromTier(tier));
                    }else if(priceId.equals(proFlatPrice)){
                        Tiers tier = Tiers.PRO;
                        user.setTiers(tier);
                        user.setQuotaFileSizeBytes(StorageQuota.getQuotaFromTier(tier));
                    }else if(priceId.equals(businessFlatPrice)){
                        Tiers tier = Tiers.BUSINESS;
                        user.setTiers(tier);
                        user.setQuotaFileSizeBytes(StorageQuota.getQuotaFromTier(tier));
                    }

                    userRepository.save(user);
                    System.out.println("customer subscription was completed!");
                }

                case "customer.subscription.deleted" -> {
                    Subscription sub = (Subscription) event.getDataObjectDeserializer()
                            .getObject().orElseThrow();

                    AppUser user = userRepository.findByStripeSubscriptionId(sub.getId())
                            .orElseThrow();

                    user.setSubscribed(false);
                    user.setStripeSubscriptionId(null);
                    user.setQuotaFileSizeBytes(StorageQuota.getQuotaFromTier(Tiers.STARTER));
                    user.setTiers(null);

                    userRepository.save(user);
                    System.out.println("Subscription was deleted");
                }
            }

            return ResponseEntity.ok("ok");
        } catch (StripeException e) {
            ResponseEntity.internalServerError().body(e.getMessage());
        }

        return ResponseEntity.ok("Successfully saved");
    }

    @GetMapping("/subscription/status")
    public ResponseEntity<Map<String, Object>> subscriptionStatus() {
        AppUser user = userService.getCurrentUser();

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(Map.of(
                "subscribed", user.getSubscribed(),
                "subscriptionId", user.getStripeSubscriptionId()
        ));
    }

    @PostMapping("/subscription/portal")
    public ResponseEntity<Map<String, String>> openCustomerPortal() throws StripeException {

        AppUser user = userService.getCurrentUser();
        if (user == null || user.getStripeCustomerId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        com.stripe.param.billingportal.SessionCreateParams params =
                com.stripe.param.billingportal.SessionCreateParams.builder()
                        .setCustomer(user.getStripeCustomerId())
                        .setReturnUrl(clientURL + "/account")
                        .build();

        com.stripe.model.billingportal.Session portalSession =
                com.stripe.model.billingportal.Session.create(params);

        return ResponseEntity.ok(Map.of(
                "url", portalSession.getUrl()
        ));
    }



}


//    @PostMapping("/create-checkout-session")
//    public ResponseEntity<CheckoutResponse> createCheckoutSession(@RequestBody CheckoutItemDTO checkoutItemDTO, UserService  userService) {
//
//        try {
////            get userEmail
//            String userEmail = userService.getUserEmail();
//
//            List<SessionCreateParams.LineItem> lineItems = stripeService.getLineItems(checkoutItemDTO);
//
//            System.out.println("host url:"+ clientURL);
//            SessionCreateParams.Builder sessionBuilder = SessionCreateParams.builder()
//                    .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
//                    .addPaymentMethodType(SessionCreateParams.PaymentMethodType.AMAZON_PAY) // Or other types
////                    .addPaymentMethodType(SessionCreateParams.PaymentMethodType.PAYPAL)
//                    .addPaymentMethodType(SessionCreateParams.PaymentMethodType.KLARNA)
//                    .setMode(SessionCreateParams.Mode.PAYMENT)
//                    .setSuccessUrl(clientURL+"/success?session_id={CHECKOUT_SESSION_ID}")
//                    .setCancelUrl(clientURL+"/cancel")
//                    .addAllLineItem(lineItems);
//
//            //if email found build the session
//            if(userEmail != null) {
//
//                SessionCreateParams params = sessionBuilder
//                        .setCustomerEmail(userEmail)
//                        .build();
//
//                Session session = Session.create(params);
//                CheckoutResponse checkoutResponse = new CheckoutResponse();
//                checkoutResponse.setCheckoutUrl(session.getUrl());
//                return new ResponseEntity<CheckoutResponse>(checkoutResponse, HttpStatus.OK);
//            }
////          if no email found build the session without email
//            SessionCreateParams params = sessionBuilder.build();
////          create session
//            Session session = Session.create(params);
////            send sessionUrl
//            CheckoutResponse checkoutResponse = new CheckoutResponse();
//            checkoutResponse.setCheckoutUrl(session.getUrl());
//            return new ResponseEntity<CheckoutResponse>(checkoutResponse, HttpStatus.OK);
//
//        } catch (Exception e) {
//            System.out.print("error in StripeController.createCheckoutSession:"+e.getMessage());
//            CheckoutResponse checkoutResponse = new CheckoutResponse();
//            checkoutResponse.setCheckoutUrl("");
//            return new ResponseEntity<CheckoutResponse>(checkoutResponse, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
