package com.syncstock.report.config;

import com.syncstock.common.security.SecurityConstants;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean
    @ConditionalOnMissingBean
    public RestClient.Builder restClientBuilder() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(15));

        return RestClient.builder()
                .requestFactory(requestFactory)
                .requestInterceptor((request, body, execution) -> {
                    if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                        String bearerToken = resolveCurrentBearerToken();
                        if (bearerToken != null && !bearerToken.isBlank()) {
                            request.getHeaders().set(HttpHeaders.AUTHORIZATION, bearerToken);
                        }
                    }
                    return execution.execute(request, body);
                });
    }

    private String resolveCurrentBearerToken() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletAttributes) {
            HttpServletRequest currentRequest = servletAttributes.getRequest();
            String authHeader = currentRequest.getHeader(HttpHeaders.AUTHORIZATION);
            if (authHeader != null && !authHeader.isBlank()) {
                String trimmed = authHeader.trim();
                return trimmed.regionMatches(true, 0, "Bearer ", 0, 7) ? trimmed : "Bearer " + trimmed;
            }

            Cookie[] cookies = currentRequest.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (SecurityConstants.COOKIE_NAME.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                        return "Bearer " + cookie.getValue().trim();
                    }
                }
            }
        }
        return null;
    }
}
