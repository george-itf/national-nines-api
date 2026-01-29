package uk.co.nationalninesgolf.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Security configuration for the API
 * - Public endpoints: entries, orders submission, webhooks
 * - Protected endpoints: admin dashboard and management
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Value("${app.admin-api-key:}")
    private String adminApiKey;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/entries/**").permitAll()
                .requestMatchers("/api/orders/**").permitAll()
                .requestMatchers("/api/webhooks/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                // Admin endpoints require API key
                .requestMatchers("/api/admin/**").authenticated()
                .anyRequest().permitAll()
            )
            .addFilterBefore(new ApiKeyAuthFilter(adminApiKey), UsernamePasswordAuthenticationFilter.class);
        
        // Allow H2 console frames
        http.headers(headers -> headers.frameOptions(frame -> frame.disable()));
        
        return http.build();
    }
    
    /**
     * Simple API key filter for admin endpoints
     */
    static class ApiKeyAuthFilter extends OncePerRequestFilter {
        
        private final String apiKey;
        
        public ApiKeyAuthFilter(String apiKey) {
            this.apiKey = apiKey;
        }
        
        @Override
        protected void doFilterInternal(HttpServletRequest request, 
                                        HttpServletResponse response, 
                                        FilterChain filterChain) throws ServletException, IOException {
            
            String path = request.getRequestURI();
            
            // Only check admin endpoints
            if (path.startsWith("/api/admin")) {
                String providedKey = request.getHeader("X-API-Key");
                
                // If no API key configured, allow access (for development)
                if (apiKey == null || apiKey.isEmpty()) {
                    filterChain.doFilter(request, response);
                    return;
                }
                
                // Check API key
                if (providedKey == null || !providedKey.equals(apiKey)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Invalid or missing API key\"}");
                    return;
                }
            }
            
            filterChain.doFilter(request, response);
        }
    }
}
