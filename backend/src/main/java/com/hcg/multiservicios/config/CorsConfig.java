package com.hcg.multiservicios.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Una misma politica CORS para Spring MVC y el filtro de Spring Security. */
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    private final String[] origins;
    private static final String[] METHODS = {"GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"};
    private static final String[] HEADERS = {"Authorization", "Content-Type", "Accept"};

    public CorsConfig(@Value("${hcg.cors.allowed-origins:http://localhost:3000,http://localhost:5173}") String origins) {
        this.origins = Arrays.stream(origins.split(",")).map(String::trim)
                .filter(s -> !s.isEmpty()).toArray(String[]::new);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**").allowedOrigins(origins).allowedMethods(METHODS)
                .allowedHeaders(HEADERS).allowCredentials(false).maxAge(3600);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(origins));
        config.setAllowedMethods(List.of(METHODS));
        config.setAllowedHeaders(List.of(HEADERS));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
