package uk.co.nationalninesgolf.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.co.nationalninesgolf.api.model.Entry;

import java.util.List;
import java.util.Optional;

@Repository
public interface EntryRepository extends JpaRepository<Entry, Long> {
    
    List<Entry> findByEvent(String event);
    
    List<Entry> findByEventAndPaymentStatus(String event, Entry.PaymentStatus status);
    
    List<Entry> findByClubNameContainingIgnoreCase(String clubName);
    
    Optional<Entry> findByStripeSessionId(String stripeSessionId);
    
    Optional<Entry> findByStripePaymentIntentId(String paymentIntentId);
    
    @Query("SELECT e FROM Entry e WHERE e.player1Email = ?1 OR e.player2Email = ?1")
    List<Entry> findByPlayerEmail(String email);
    
    @Query("SELECT COUNT(e) FROM Entry e WHERE e.event = ?1 AND e.paymentStatus = 'PAID'")
    long countPaidEntriesByEvent(String event);
    
    boolean existsByEventAndClubName(String event, String clubName);
}
