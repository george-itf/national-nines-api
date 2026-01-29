package uk.co.nationalninesgolf.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.co.nationalninesgolf.api.service.EntryService;
import uk.co.nationalninesgolf.api.service.OrderService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Health and status endpoints
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HealthController {
    
    private final EntryService entryService;
    private final OrderService orderService;
    
    /**
     * Simple health check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "National Nines Golf API");
        return ResponseEntity.ok(response);
    }
    
    /**
     * Public stats (entry counts for display on frontend)
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        Map<String, Object> response = new HashMap<>();
        response.put("kentNinesEntries", entryService.countPaidEntries("KENT_NINES_2026"));
        response.put("essexNinesEntries", entryService.countPaidEntries("ESSEX_NINES_2026"));
        return ResponseEntity.ok(response);
    }
}
