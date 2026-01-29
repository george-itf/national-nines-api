package uk.co.nationalninesgolf.api.controller;

import com.stripe.exception.StripeException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.co.nationalninesgolf.api.model.Order;
import uk.co.nationalninesgolf.api.service.OrderService;
import uk.co.nationalninesgolf.api.service.StripeService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"https://nationalninesgolf.co.uk", "http://localhost:4321", "http://localhost:3000"})
public class OrderController {
    
    private final OrderService orderService;
    private final StripeService stripeService;
    
    /**
     * Create a new order and get checkout URL
     */
    @PostMapping
    public ResponseEntity<?> createOrder(@Valid @RequestBody Order order) {
        try {
            Order saved = orderService.createOrder(order);
            
            // Create Stripe checkout session
            String checkoutUrl = stripeService.createOrderCheckoutSession(saved);
            
            Map<String, Object> response = new HashMap<>();
            response.put("order", saved);
            response.put("checkoutUrl", checkoutUrl);
            
            return ResponseEntity.ok(response);
            
        } catch (StripeException e) {
            log.error("Stripe error creating order checkout", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Payment system error. Please try again."));
        }
    }
    
    /**
     * Get order by order number
     */
    @GetMapping("/{orderNumber}")
    public ResponseEntity<Order> getOrder(@PathVariable String orderNumber) {
        return orderService.findByOrderNumber(orderNumber)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get order status (for tracking)
     */
    @GetMapping("/{orderNumber}/status")
    public ResponseEntity<Map<String, String>> getOrderStatus(@PathVariable String orderNumber) {
        return orderService.findByOrderNumber(orderNumber)
            .map(order -> ResponseEntity.ok(Map.of(
                "orderNumber", order.getOrderNumber(),
                "status", order.getStatus().name()
            )))
            .orElse(ResponseEntity.notFound().build());
    }
}
