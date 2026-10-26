package com.syncstock.common.config;

import com.syncstock.common.exception.GlobalExceptionHandler;
import com.syncstock.common.security.JwtAuthenticationFilter;
import com.syncstock.common.security.JwtUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class CommonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JwtUtils jwtUtils() {
        return new JwtUtils(
                com.syncstock.common.security.SecurityConstants.DEFAULT_SECRET,
                com.syncstock.common.security.SecurityConstants.DEFAULT_ISSUER,
                com.syncstock.common.security.SecurityConstants.DEFAULT_AUDIENCE,
                com.syncstock.common.security.SecurityConstants.DEFAULT_EXPIRATION_MS
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtUtils jwtUtils) {
        return new JwtAuthenticationFilter(jwtUtils);
    }

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
