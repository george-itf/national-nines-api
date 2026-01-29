package uk.co.nationalninesgolf.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.co.nationalninesgolf.api.model.Entry;
import uk.co.nationalninesgolf.api.model.Order;

import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    
    private final JavaMailSender mailSender;
    
    @Value("${spring.mail.username:noreply@nationalninesgolf.co.uk}")
    private String fromEmail;
    
    @Value("${app.admin-email:info@nationalninesgolf.co.uk}")
    private String adminEmail;
    
    /**
     * Send entry confirmation to customer
     */
    @Async
    public void sendEntryConfirmation(Entry entry) {
        try {
            String eventName = entry.getEvent().contains("KENT") ? "Kent Nines 2026" : "Essex Nines 2026";
            
            String subject = "Entry Confirmed - " + eventName;
            String body = String.format("""
                Dear %s and %s,
                
                Thank you for entering %s!
                
                Your entry details:
                - Club: %s
                - Players: %s (HI: %s) & %s (HI: %s)
                - Entry Fee: £%s
                
                %s
                
                If you have any questions, please reply to this email or contact us at info@nationalninesgolf.co.uk.
                
                Good luck!
                
                The National Nines Team
                https://nationalninesgolf.co.uk
                """,
                entry.getPlayer1Name(),
                entry.getPlayer2Name(),
                eventName,
                entry.getClubName(),
                entry.getPlayer1Name(),
                entry.getPlayer1Handicap(),
                entry.getPlayer2Name(),
                entry.getPlayer2Handicap(),
                entry.getEntryFee(),
                entry.getPaymentStatus() == Entry.PaymentStatus.PAID 
                    ? "Your payment has been received. You're all set!"
                    : "We're awaiting your payment confirmation."
            );
            
            sendEmail(entry.getPlayer1Email(), subject, body);
            
            // Also send to player 2 if different email
            if (!entry.getPlayer1Email().equalsIgnoreCase(entry.getPlayer2Email())) {
                sendEmail(entry.getPlayer2Email(), subject, body);
            }
            
            log.info("Entry confirmation sent to {} and {}", 
                entry.getPlayer1Email(), entry.getPlayer2Email());
                
        } catch (Exception e) {
            log.error("Failed to send entry confirmation", e);
        }
    }
    
    /**
     * Send order confirmation to customer
     */
    @Async
    public void sendOrderConfirmation(Order order) {
        try {
            StringBuilder itemsList = new StringBuilder();
            order.getItems().forEach(item -> {
                itemsList.append(String.format("- %s x%d: £%.2f%n",
                    item.getProductName(),
                    item.getQuantity(),
                    item.getLineTotal()));
            });
            
            String deliveryInfo = order.getDeliveryMethod() == Order.DeliveryMethod.COLLECTION
                ? "Collection (we'll contact you to arrange pickup)"
                : String.format("Shipping to:%n  %s%n  %s, %s",
                    order.getShippingAddress(),
                    order.getShippingCity(),
                    order.getShippingPostcode());
            
            String subject = "Order Confirmed - " + order.getOrderNumber();
            String body = String.format("""
                Dear %s,
                
                Thank you for your order!
                
                Order: %s
                
                Items:
                %s
                Subtotal: £%.2f
                Shipping: £%.2f
                Total: £%.2f
                
                Delivery: %s
                
                %s
                
                If you have any questions, please reply to this email or contact us at info@nationalninesgolf.co.uk.
                
                Thank you for supporting National Nines Golf!
                
                The National Nines Team
                https://nationalninesgolf.co.uk
                """,
                order.getCustomerName(),
                order.getOrderNumber(),
                itemsList,
                order.getSubtotal(),
                order.getShippingCost(),
                order.getTotal(),
                deliveryInfo,
                order.getStatus() == Order.OrderStatus.PAID
                    ? "Your payment has been received. We'll process your order shortly!"
                    : "We're awaiting your payment confirmation."
            );
            
            sendEmail(order.getCustomerEmail(), subject, body);
            
            log.info("Order confirmation sent to {}", order.getCustomerEmail());
            
        } catch (Exception e) {
            log.error("Failed to send order confirmation", e);
        }
    }
    
    /**
     * Notify admin of new entry
     */
    @Async
    public void notifyAdminNewEntry(Entry entry) {
        try {
            String eventName = entry.getEvent().contains("KENT") ? "Kent Nines 2026" : "Essex Nines 2026";
            
            String subject = "New Entry - " + eventName + " - " + entry.getClubName();
            String body = String.format("""
                New entry received for %s
                
                Club: %s
                Player 1: %s (%s) - HI: %s
                Player 2: %s (%s) - HI: %s
                Phone: %s
                Entry Fee: £%s
                Payment: %s
                Marketing Opt-in: %s
                
                View all entries: https://api.nationalninesgolf.co.uk/api/admin/entries
                """,
                eventName,
                entry.getClubName(),
                entry.getPlayer1Name(),
                entry.getPlayer1Email(),
                entry.getPlayer1Handicap(),
                entry.getPlayer2Name(),
                entry.getPlayer2Email(),
                entry.getPlayer2Handicap(),
                entry.getContactPhone(),
                entry.getEntryFee(),
                entry.getPaymentStatus(),
                entry.isMarketingOptIn() ? "Yes" : "No"
            );
            
            sendEmail(adminEmail, subject, body);
            
        } catch (Exception e) {
            log.error("Failed to notify admin of new entry", e);
        }
    }
    
    /**
     * Notify admin of new order
     */
    @Async
    public void notifyAdminNewOrder(Order order) {
        try {
            StringBuilder itemsList = new StringBuilder();
            order.getItems().forEach(item -> {
                itemsList.append(String.format("- %s x%d: £%.2f%n",
                    item.getProductName(),
                    item.getQuantity(),
                    item.getLineTotal()));
            });
            
            String subject = "New Order - " + order.getOrderNumber();
            String body = String.format("""
                New order received!
                
                Order: %s
                Customer: %s (%s)
                Phone: %s
                
                Items:
                %s
                Total: £%.2f
                
                Delivery: %s
                Address: %s, %s %s
                Notes: %s
                
                View orders to fulfill: https://api.nationalninesgolf.co.uk/api/admin/orders/to-fulfill
                """,
                order.getOrderNumber(),
                order.getCustomerName(),
                order.getCustomerEmail(),
                order.getCustomerPhone(),
                itemsList,
                order.getTotal(),
                order.getDeliveryMethod(),
                order.getShippingAddress() != null ? order.getShippingAddress() : "-",
                order.getShippingCity() != null ? order.getShippingCity() : "",
                order.getShippingPostcode() != null ? order.getShippingPostcode() : "",
                order.getNotes() != null ? order.getNotes() : "-"
            );
            
            sendEmail(adminEmail, subject, body);
            
        } catch (Exception e) {
            log.error("Failed to notify admin of new order", e);
        }
    }
    
    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
