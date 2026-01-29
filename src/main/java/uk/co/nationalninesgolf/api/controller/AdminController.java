package uk.co.nationalninesgolf.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.co.nationalninesgolf.api.model.Entry;
import uk.co.nationalninesgolf.api.model.Order;
import uk.co.nationalninesgolf.api.service.EntryService;
import uk.co.nationalninesgolf.api.service.OrderService;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin endpoints for managing entries and orders
 * NOTE: In production, secure these with proper authentication!
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {
    
    private final EntryService entryService;
    private final OrderService orderService;
    
    // ========== ENTRIES ==========
    
    /**
     * Get all entries (admin view)
     */
    @GetMapping("/entries")
    public ResponseEntity<List<Entry>> getAllEntries() {
        return ResponseEntity.ok(entryService.findAll());
    }
    
    /**
     * Get entries for a specific event
     */
    @GetMapping("/entries/event/{event}")
    public ResponseEntity<List<Entry>> getEntriesByEvent(@PathVariable String event) {
        return ResponseEntity.ok(entryService.findByEvent(event));
    }
    
    /**
     * Manually mark entry as paid (for bank transfers)
     */
    @PostMapping("/entries/{id}/mark-paid")
    public ResponseEntity<Entry> markEntryAsPaid(@PathVariable Long id) {
        Entry updated = entryService.markAsPaid(id, "MANUAL_PAYMENT");
        return ResponseEntity.ok(updated);
    }
    
    // ========== ORDERS ==========
    
    /**
     * Get all orders
     */
    @GetMapping("/orders")
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.findAll());
    }
    
    /**
     * Get orders by status
     */
    @GetMapping("/orders/status/{status}")
    public ResponseEntity<List<Order>> getOrdersByStatus(@PathVariable Order.OrderStatus status) {
        return ResponseEntity.ok(orderService.findByStatus(status));
    }
    
    /**
     * Get orders that need fulfilment
     */
    @GetMapping("/orders/to-fulfill")
    public ResponseEntity<List<Order>> getOrdersToFulfill() {
        return ResponseEntity.ok(orderService.findOrdersToFulfill());
    }
    
    /**
     * Update order status
     */
    @PostMapping("/orders/{id}/status")
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam Order.OrderStatus status) {
        Order updated = orderService.updateStatus(id, status);
        return ResponseEntity.ok(updated);
    }
    
    /**
     * Manually mark order as paid (for bank transfers)
     */
    @PostMapping("/orders/{id}/mark-paid")
    public ResponseEntity<Order> markOrderAsPaid(@PathVariable Long id) {
        Order updated = orderService.markAsPaid(id, "MANUAL_PAYMENT");
        return ResponseEntity.ok(updated);
    }
    
    // ========== DASHBOARD ==========
    
    /**
     * Dashboard statistics
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        Map<String, Object> stats = new HashMap<>();
        
        // Entry stats
        stats.put("kentNinesEntries", entryService.countPaidEntries("KENT_NINES_2026"));
        stats.put("essexNinesEntries", entryService.countPaidEntries("ESSEX_NINES_2026"));
        
        // Order stats
        stats.put("pendingOrders", orderService.countByStatus(Order.OrderStatus.PENDING));
        stats.put("paidOrders", orderService.countByStatus(Order.OrderStatus.PAID));
        stats.put("processingOrders", orderService.countByStatus(Order.OrderStatus.PROCESSING));
        stats.put("fulfilledOrders", 
            orderService.countByStatus(Order.OrderStatus.DELIVERED) + 
            orderService.countByStatus(Order.OrderStatus.COLLECTED));
        
        // Revenue
        stats.put("totalRevenue", orderService.calculateTotalRevenue());
        
        // Entry fees
        BigDecimal kentFees = entryService.getEntryFee("KENT")
            .multiply(BigDecimal.valueOf(entryService.countPaidEntries("KENT_NINES_2026")));
        BigDecimal essexFees = entryService.getEntryFee("ESSEX")
            .multiply(BigDecimal.valueOf(entryService.countPaidEntries("ESSEX_NINES_2026")));
        stats.put("totalEntryFees", kentFees.add(essexFees));
        
        return ResponseEntity.ok(stats);
    }
}
