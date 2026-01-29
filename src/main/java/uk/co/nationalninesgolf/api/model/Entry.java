package uk.co.nationalninesgolf.api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Competition entry - a pair entering Kent Nines or Essex Nines
 */
@Entity
@Table(name = "entries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Entry {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Column(nullable = false)
    private String event; // "KENT_NINES_2026" or "ESSEX_NINES_2026"
    
    @NotBlank
    @Column(nullable = false)
    private String clubName;
    
    // Player 1
    @NotBlank
    private String player1Name;
    
    @Email
    @NotBlank
    private String player1Email;
    
    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("54.0")
    private BigDecimal player1Handicap;
    
    // Player 2
    @NotBlank
    private String player2Name;
    
    @Email
    @NotBlank
    private String player2Email;
    
    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("54.0")
    private BigDecimal player2Handicap;
    
    // Contact
    @NotBlank
    private String contactPhone;
    
    private boolean marketingOptIn;
    
    // Payment
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;
    
    private String stripePaymentIntentId;
    private String stripeSessionId;
    
    @NotNull
    private BigDecimal entryFee;
    
    // Timestamps
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime paidAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    public enum PaymentStatus {
        PENDING,
        PAID,
        FAILED,
        REFUNDED,
        CANCELLED
    }
}
