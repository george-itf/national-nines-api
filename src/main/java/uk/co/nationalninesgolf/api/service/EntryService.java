package uk.co.nationalninesgolf.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nationalninesgolf.api.model.Entry;
import uk.co.nationalninesgolf.api.repository.EntryRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EntryService {
    
    private final EntryRepository entryRepository;
    
    // Entry fees
    private static final BigDecimal KENT_NINES_FEE = new BigDecimal("150.00");
    private static final BigDecimal ESSEX_NINES_FEE = new BigDecimal("50.00");
    
    @Transactional
    public Entry createEntry(Entry entry) {
        // Set entry fee based on event
        if (entry.getEvent().contains("KENT")) {
            entry.setEntryFee(KENT_NINES_FEE);
        } else if (entry.getEvent().contains("ESSEX")) {
            entry.setEntryFee(ESSEX_NINES_FEE);
        }
        
        entry.setPaymentStatus(Entry.PaymentStatus.PENDING);
        
        Entry saved = entryRepository.save(entry);
        log.info("Created entry {} for {} from {}", saved.getId(), saved.getEvent(), saved.getClubName());
        
        return saved;
    }
    
    public Optional<Entry> findById(Long id) {
        return entryRepository.findById(id);
    }
    
    public List<Entry> findByEvent(String event) {
        return entryRepository.findByEvent(event);
    }
    
    public List<Entry> findPaidEntriesByEvent(String event) {
        return entryRepository.findByEventAndPaymentStatus(event, Entry.PaymentStatus.PAID);
    }
    
    public Optional<Entry> findByStripeSessionId(String sessionId) {
        return entryRepository.findByStripeSessionId(sessionId);
    }
    
    @Transactional
    public Entry markAsPaid(Long entryId, String paymentIntentId) {
        Entry entry = entryRepository.findById(entryId)
            .orElseThrow(() -> new IllegalArgumentException("Entry not found: " + entryId));
        
        entry.setPaymentStatus(Entry.PaymentStatus.PAID);
        entry.setStripePaymentIntentId(paymentIntentId);
        entry.setPaidAt(LocalDateTime.now());
        
        Entry updated = entryRepository.save(entry);
        log.info("Entry {} marked as PAID", entryId);
        
        return updated;
    }
    
    @Transactional
    public Entry updateStripeSession(Long entryId, String sessionId) {
        Entry entry = entryRepository.findById(entryId)
            .orElseThrow(() -> new IllegalArgumentException("Entry not found: " + entryId));
        
        entry.setStripeSessionId(sessionId);
        return entryRepository.save(entry);
    }
    
    public long countPaidEntries(String event) {
        return entryRepository.countPaidEntriesByEvent(event);
    }
    
    public boolean clubHasEntered(String event, String clubName) {
        return entryRepository.existsByEventAndClubName(event, clubName);
    }
    
    public List<Entry> findAll() {
        return entryRepository.findAll();
    }
    
    public BigDecimal getEntryFee(String event) {
        if (event.contains("KENT")) {
            return KENT_NINES_FEE;
        } else if (event.contains("ESSEX")) {
            return ESSEX_NINES_FEE;
        }
        throw new IllegalArgumentException("Unknown event: " + event);
    }
}
