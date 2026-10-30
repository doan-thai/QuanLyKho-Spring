package com.syncstock.ai.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class RestClientConfigTest {

    @Test
    @DisplayName("C-09: RestClientConfig cấu hình connectTimeout 10s và readTimeout 15s giống report-service")
    void restClientBuilder_ShouldConfigureTimeoutsMatchingReportService() {
        RestClientConfig config = new RestClientConfig();
        RestClient.Builder builder = config.restClientBuilder();
        assertNotNull(builder);

        RestClient client = builder.build();
        // DefaultRestClient has clientRequestFactory or requestFactory
        ClientHttpRequestFactory factory = null;
        for (Field f : client.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            try {
                Object val = f.get(client);
                if (val instanceof ClientHttpRequestFactory chf) {
                    factory = chf;
                    break;
                }
            } catch (Exception ignored) {}
        }

        if (factory == null) {
            for (Field f : builder.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                try {
                    Object val = f.get(builder);
                    if (val instanceof ClientHttpRequestFactory chf) {
                        factory = chf;
                        break;
                    }
                } catch (Exception ignored) {}
            }
        }

        assertNotNull(factory, "Should locate ClientHttpRequestFactory");
        assertTrue(factory instanceof SimpleClientHttpRequestFactory);

        SimpleClientHttpRequestFactory simpleFactory = (SimpleClientHttpRequestFactory) factory;
        int connectTimeout = (int) ReflectionTestUtils.getField(simpleFactory, "connectTimeout");
        int readTimeout = (int) ReflectionTestUtils.getField(simpleFactory, "readTimeout");

        assertEquals(10_000, connectTimeout, "Connect timeout must be 10 seconds");
        assertEquals(15_000, readTimeout, "Read timeout must be 15 seconds");
    }
}
