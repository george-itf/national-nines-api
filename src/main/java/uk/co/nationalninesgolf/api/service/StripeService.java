package uk.co.nationalninesgolf.api.service;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.co.nationalninesgolf.api.model.Entry;
import uk.co.nationalninesgolf.api.model.Order;
import uk.co.nationalninesgolf.api.model.OrderItem;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeService {
    
    @Value("${stripe.api.key:}")
    private String stripeApiKey;
    
    @Value("${stripe.webhook.secret:}")
    private String webhookSecret;
    
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;
    
    @Value("${app.frontend-url:https://nationalninesgolf.co.uk}")
    private String frontendUrl;
    
    private final EntryService entryService;
    private final OrderService orderService;
    
    @PostConstruct
    public void init() {
        if (stripeApiKey != null && !stripeApiKey.isEmpty()) {
            Stripe.apiKey = stripeApiKey;
            log.info("Stripe API initialized");
        } else {
            log.warn("Stripe API key not configured");
        }
    }
    
    /**
     * Create a Stripe Checkout session for a competition entry
     */
    public String createEntryCheckoutSession(Entry entry) throws StripeException {
        String eventName = entry.getEvent().contains("KENT") ? "Kent Nines 2026" : "Essex Nines 2026";
        long amountInPence = entry.getEntryFee().multiply(new BigDecimal("100")).longValue();
        
        SessionCreateParams params = SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.PAYMENT)
            .setSuccessUrl(frontendUrl + "/events/" + (entry.getEvent().contains("KENT") ? "kent-nines" : "essex-nines") + "?entered=true&session_id={CHECKOUT_SESSION_ID}")
            .setCancelUrl(frontendUrl + "/events/" + (entry.getEvent().contains("KENT") ? "kent-nines" : "essex-nines") + "#enter")
            .setCustomerEmail(entry.getPlayer1Email())
            .addLineItem(
                SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(
                        SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency("gbp")
                            .setUnitAmount(amountInPence)
                            .setProductData(
                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName(eventName + " Entry")
                                    .setDescription("Pair entry: " + entry.getPlayer1Name() + " & " + entry.getPlayer2Name() + " (" + entry.getClubName() + ")")
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .putMetadata("type", "entry")
            .putMetadata("entry_id", entry.getId().toString())
            .putMetadata("event", entry.getEvent())
            .build();
        
        Session session = Session.create(params);
        
        // Save session ID to entry
        entryService.updateStripeSession(entry.getId(), session.getId());
        
        log.info("Created Stripe checkout session {} for entry {}", session.getId(), entry.getId());
        
        return session.getUrl();
    }
    
    /**
     * Create a Stripe Checkout session for a shop order
     */
    public String createOrderCheckoutSession(Order order) throws StripeException {
        SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.PAYMENT)
            .setSuccessUrl(frontendUrl + "/cart?success=true&order=" + order.getOrderNumber())
            .setCancelUrl(frontendUrl + "/cart")
            .setCustomerEmail(order.getCustomerEmail());
        
        // Add line items
        for (OrderItem item : order.getItems()) {
            long amountInPence = item.getUnitPrice().multiply(new BigDecimal("100")).longValue();
            
            paramsBuilder.addLineItem(
                SessionCreateParams.LineItem.builder()
                    .setQuantity(item.getQuantity().longValue())
                    .setPriceData(
                        SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency("gbp")
                            .setUnitAmount(amountInPence)
                            .setProductData(
                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName(item.getProductName())
                                    .build()
                            )
                            .build()
                    )
                    .build()
            );
        }
        
        // Add shipping if applicable
        if (order.getShippingCost().compareTo(BigDecimal.ZERO) > 0) {
            long shippingInPence = order.getShippingCost().multiply(new BigDecimal("100")).longValue();
            
            paramsBuilder.addLineItem(
                SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(
                        SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency("gbp")
                            .setUnitAmount(shippingInPence)
                            .setProductData(
                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName("UK Shipping")
                                    .build()
                            )
                            .build()
                    )
                    .build()
            );
        }
        
        paramsBuilder.putMetadata("type", "order");
        paramsBuilder.putMetadata("order_id", order.getId().toString());
        paramsBuilder.putMetadata("order_number", order.getOrderNumber());
        
        Session session = Session.create(paramsBuilder.build());
        
        // Save session ID to order
        orderService.updateStripeSession(order.getId(), session.getId());
        
        log.info("Created Stripe checkout session {} for order {}", session.getId(), order.getOrderNumber());
        
        return session.getUrl();
    }
    
    /**
     * Handle Stripe webhook events
     */
    public void handleWebhook(String payload, String sigHeader) throws SignatureVerificationException {
        Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        
        log.info("Received Stripe webhook: {}", event.getType());
        
        switch (event.getType()) {
            case "checkout.session.completed" -> handleCheckoutCompleted(event);
            case "payment_intent.succeeded" -> handlePaymentSucceeded(event);
            case "payment_intent.payment_failed" -> handlePaymentFailed(event);
            default -> log.debug("Unhandled webhook event type: {}", event.getType());
        }
    }
    
    private void handleCheckoutCompleted(Event event) {
        Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
        if (session == null) return;
        
        Map<String, String> metadata = session.getMetadata();
        String type = metadata.get("type");
        
        if ("entry".equals(type)) {
            Long entryId = Long.parseLong(metadata.get("entry_id"));
            entryService.markAsPaid(entryId, session.getPaymentIntent());
            log.info("Entry {} payment completed", entryId);
        } else if ("order".equals(type)) {
            Long orderId = Long.parseLong(metadata.get("order_id"));
            orderService.markAsPaid(orderId, session.getPaymentIntent());
            log.info("Order {} payment completed", metadata.get("order_number"));
        }
    }
    
    private void handlePaymentSucceeded(Event event) {
        log.info("Payment succeeded");
    }
    
    private void handlePaymentFailed(Event event) {
        log.warn("Payment failed");
    }
}
