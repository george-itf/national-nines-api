package uk.co.nationalninesgolf.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.co.nationalninesgolf.api.model.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    Optional<Order> findByOrderNumber(String orderNumber);
    
    Optional<Order> findByStripeSessionId(String stripeSessionId);
    
    Optional<Order> findByStripePaymentIntentId(String paymentIntentId);
    
    List<Order> findByStatus(Order.OrderStatus status);
    
    List<Order> findByCustomerEmail(String email);
    
    List<Order> findByDeliveryMethod(Order.DeliveryMethod deliveryMethod);
    
    @Query("SELECT o FROM Order o WHERE o.createdAt >= ?1 ORDER BY o.createdAt DESC")
    List<Order> findRecentOrders(LocalDateTime since);
    
    @Query("SELECT o FROM Order o WHERE o.status IN ('PAID', 'PROCESSING') ORDER BY o.createdAt ASC")
    List<Order> findOrdersToFulfill();
    
    @Query("SELECT SUM(o.total) FROM Order o WHERE o.status = 'PAID' OR o.status = 'DELIVERED' OR o.status = 'COLLECTED'")
    BigDecimal calculateTotalRevenue();
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = ?1")
    long countByStatus(Order.OrderStatus status);
}
