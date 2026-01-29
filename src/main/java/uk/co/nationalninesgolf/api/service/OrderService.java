package uk.co.nationalninesgolf.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nationalninesgolf.api.model.Order;
import uk.co.nationalninesgolf.api.model.OrderItem;
import uk.co.nationalninesgolf.api.repository.OrderRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    
    private final OrderRepository orderRepository;
    
    // Shipping costs
    private static final BigDecimal SHIPPING_SMALL = new BigDecimal("5.00");
    private static final BigDecimal SHIPPING_MEDIUM = new BigDecimal("10.00");
    private static final BigDecimal SHIPPING_LARGE = new BigDecimal("15.00");
    
    @Transactional
    public Order createOrder(Order order) {
        // Calculate totals
        BigDecimal subtotal = order.getItems().stream()
            .map(OrderItem::getLineTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        order.setSubtotal(subtotal);
        
        // Calculate shipping
        if (order.getDeliveryMethod() == Order.DeliveryMethod.COLLECTION) {
            order.setShippingCost(BigDecimal.ZERO);
        } else {
            order.setShippingCost(calculateShipping(subtotal));
        }
        
        order.setTotal(order.getSubtotal().add(order.getShippingCost()));
        order.setStatus(Order.OrderStatus.PENDING);
        
        Order saved = orderRepository.save(order);
        log.info("Created order {} for {} - total £{}", 
            saved.getOrderNumber(), saved.getCustomerEmail(), saved.getTotal());
        
        return saved;
    }
    
    private BigDecimal calculateShipping(BigDecimal subtotal) {
        // Simple shipping calculation
        if (subtotal.compareTo(new BigDecimal("30")) < 0) {
            return SHIPPING_SMALL;
        } else if (subtotal.compareTo(new BigDecimal("75")) < 0) {
            return SHIPPING_MEDIUM;
        } else {
            return SHIPPING_LARGE;
        }
    }
    
    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }
    
    public Optional<Order> findByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber);
    }
    
    public Optional<Order> findByStripeSessionId(String sessionId) {
        return orderRepository.findByStripeSessionId(sessionId);
    }
    
    public List<Order> findByStatus(Order.OrderStatus status) {
        return orderRepository.findByStatus(status);
    }
    
    public List<Order> findOrdersToFulfill() {
        return orderRepository.findOrdersToFulfill();
    }
    
    @Transactional
    public Order markAsPaid(Long orderId, String paymentIntentId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        
        order.setStatus(Order.OrderStatus.PAID);
        order.setStripePaymentIntentId(paymentIntentId);
        order.setPaidAt(LocalDateTime.now());
        
        Order updated = orderRepository.save(order);
        log.info("Order {} marked as PAID", order.getOrderNumber());
        
        return updated;
    }
    
    @Transactional
    public Order updateStatus(Long orderId, Order.OrderStatus status) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        
        order.setStatus(status);
        
        if (status == Order.OrderStatus.DELIVERED || status == Order.OrderStatus.COLLECTED) {
            order.setFulfilledAt(LocalDateTime.now());
        }
        
        Order updated = orderRepository.save(order);
        log.info("Order {} status updated to {}", order.getOrderNumber(), status);
        
        return updated;
    }
    
    @Transactional
    public Order updateStripeSession(Long orderId, String sessionId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        
        order.setStripeSessionId(sessionId);
        return orderRepository.save(order);
    }
    
    public List<Order> findAll() {
        return orderRepository.findAll();
    }
    
    public BigDecimal calculateTotalRevenue() {
        BigDecimal revenue = orderRepository.calculateTotalRevenue();
        return revenue != null ? revenue : BigDecimal.ZERO;
    }
    
    public long countByStatus(Order.OrderStatus status) {
        return orderRepository.countByStatus(status);
    }
}
