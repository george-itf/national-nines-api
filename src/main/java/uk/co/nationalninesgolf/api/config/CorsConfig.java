package uk.co.nationalninesgolf.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {
    
    @Value("${app.frontend-url:https://nationalninesgolf.co.uk}")
    private String frontendUrl;
    
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        
        // Allowed origins
        config.setAllowedOrigins(Arrays.asList(
            frontendUrl,
            "https://nationalninesgolf.co.uk",
            "https://www.nationalninesgolf.co.uk",
            "http://localhost:4321",  // Astro dev
            "http://localhost:3000"   // Next.js/other dev
        ));
        
        // Allowed methods
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        
        // Allowed headers
        config.setAllowedHeaders(List.of("*"));
        
        // Allow credentials (cookies, auth headers)
        config.setAllowCredentials(true);
        
        // Expose response headers
        config.setExposedHeaders(Arrays.asList("X-Total-Count", "Link"));
        
        // Max age for preflight cache (1 hour)
        config.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        
        return new CorsFilter(source);
    }
}
