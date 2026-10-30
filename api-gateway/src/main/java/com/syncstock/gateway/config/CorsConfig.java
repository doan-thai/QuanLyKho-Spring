package com.syncstock.gateway.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    private final String allowedOrigins;

    public CorsConfig(
            @Value("${cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173,http://localhost:4173}")
            String allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @PostConstruct
    public void validateCorsConfig() {
        resolveOrigins();
    }

    public List<String> resolveOrigins() {
        if (allowedOrigins == null || allowedOrigins.isBlank()) {
            throw new IllegalStateException(
                    "CORS allowed origins configuration cannot be empty. Please configure 'cors.allowed-origins' with valid origins.");
        }

        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        if (origins.isEmpty()) {
            throw new IllegalStateException(
                    "CORS allowed origins configuration cannot be empty. Please configure 'cors.allowed-origins' with valid origins.");
        }

        return origins;
    }

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistrationBean() {
        List<String> origins = resolveOrigins();

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);

        for (String origin : origins) {
            config.addAllowedOriginPattern(origin);
        }

        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.addExposedHeader("Content-Disposition");
        config.addExposedHeader("Authorization");
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}
