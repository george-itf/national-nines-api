package uk.co.nationalninesgolf.api.controller;

import com.stripe.exception.StripeException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.co.nationalninesgolf.api.model.Entry;
import uk.co.nationalninesgolf.api.service.EntryService;
import uk.co.nationalninesgolf.api.service.StripeService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/entries")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"https://nationalninesgolf.co.uk", "http://localhost:4321", "http://localhost:3000"})
public class EntryController {
    
    private final EntryService entryService;
    private final StripeService stripeService;
    
    /**
     * Submit a new competition entry
     */
    @PostMapping
    public ResponseEntity<?> createEntry(@Valid @RequestBody Entry entry) {
        try {
            // Check if club has already entered
            if (entryService.clubHasEntered(entry.getEvent(), entry.getClubName())) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "This club has already entered " + entry.getEvent()));
            }
            
            Entry saved = entryService.createEntry(entry);
            
            // Create Stripe checkout session
            String checkoutUrl = stripeService.createEntryCheckoutSession(saved);
            
            Map<String, Object> response = new HashMap<>();
            response.put("entry", saved);
            response.put("checkoutUrl", checkoutUrl);
            
            return ResponseEntity.ok(response);
            
        } catch (StripeException e) {
            log.error("Stripe error creating entry checkout", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Payment system error. Please try again."));
        }
    }
    
    /**
     * Get entry by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Entry> getEntry(@PathVariable Long id) {
        return entryService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get all entries for an event
     */
    @GetMapping("/event/{event}")
    public ResponseEntity<List<Entry>> getEntriesByEvent(@PathVariable String event) {
        return ResponseEntity.ok(entryService.findByEvent(event));
    }
    
    /**
     * Get paid entries for an event (public leaderboard)
     */
    @GetMapping("/event/{event}/paid")
    public ResponseEntity<List<Entry>> getPaidEntriesByEvent(@PathVariable String event) {
        return ResponseEntity.ok(entryService.findPaidEntriesByEvent(event));
    }
    
    /**
     * Get entry count for an event
     */
    @GetMapping("/event/{event}/count")
    public ResponseEntity<Map<String, Long>> getEntryCount(@PathVariable String event) {
        long count = entryService.countPaidEntries(event);
        return ResponseEntity.ok(Map.of("count", count));
    }
}
