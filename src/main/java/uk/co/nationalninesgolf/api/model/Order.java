package uk.co.nationalninesgolf.api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Shop order for golf equipment
 */
@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String orderNumber;
    
    // Customer
    @NotBlank
    private String customerName;
    
    @Email
    @NotBlank
    private String customerEmail;
    
    @NotBlank
    private String customerPhone;
    
    // Delivery
    @Enumerated(EnumType.STRING)
    @NotNull
    private DeliveryMethod deliveryMethod;
    
    private String shippingAddress;
    private String shippingCity;
    private String shippingPostcode;
    
    private String notes;
    
    // Items
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();
    
    // Totals
    @NotNull
    private BigDecimal subtotal;
    
    @NotNull
    private BigDecimal shippingCost;
    
    @NotNull
    private BigDecimal total;
    
    // Status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;
    
    // Payment
    private String stripePaymentIntentId;
    private String stripeSessionId;
    
    // Timestamps
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime paidAt;
    private LocalDateTime fulfilledAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (orderNumber == null) {
            orderNumber = "NN-" + System.currentTimeMillis();
        }
    }
    
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }
    
    public enum DeliveryMethod {
        COLLECTION,
        SHIPPING
    }
    
    public enum OrderStatus {
        PENDING,
        PAID,
        PROCESSING,
        SHIPPED,
        DELIVERED,
        COLLECTED,
        CANCELLED,
        REFUNDED
    }
}
