package com.syncstock.gateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.Ordered;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorsConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(CorsConfig.class);

    @Test
    @DisplayName("Cấu hình origins hợp lệ - phân tách chính xác và tạo bean CorsFilter với highest precedence")
    void validOrigins_ShouldSucceed() {
        CorsConfig corsConfig = new CorsConfig("http://localhost:5173, http://127.0.0.1:5173, http://localhost:4173");
        corsConfig.validateCorsConfig();

        List<String> origins = corsConfig.resolveOrigins();
        assertThat(origins).containsExactly(
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "http://localhost:4173"
        );

        FilterRegistrationBean<CorsFilter> filterBean = corsConfig.corsFilterRegistrationBean();
        assertThat(filterBean).isNotNull();
        assertThat(filterBean.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
        assertThat(filterBean.getFilter()).isNotNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n", ",,,", " , ,  , "})
    @DisplayName("Cấu hình origins rỗng hoặc không hợp lệ - ném IllegalStateException ngay khi khởi tạo")
    void emptyOrInvalidOrigins_ShouldThrowIllegalStateException(String invalidOrigin) {
        CorsConfig corsConfig = new CorsConfig(invalidOrigin);

        assertThatThrownBy(corsConfig::validateCorsConfig)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CORS allowed origins configuration cannot be empty");

        assertThatThrownBy(corsConfig::corsFilterRegistrationBean)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CORS allowed origins configuration cannot be empty");
    }

    @Test
    @DisplayName("Spring Context - fail fast khi thuộc tính cors.allowed-origins bị cấu hình rỗng")
    void springContext_WhenAllowedOriginsEmpty_ShouldFailStartup() {
        contextRunner
                .withPropertyValues("cors.allowed-origins=")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("CORS allowed origins configuration cannot be empty");
                });
    }

    @Test
    @DisplayName("Spring Context - khởi động thành công khi có allowed origins mặc định")
    void springContext_WhenDefaultOrigins_ShouldStartSuccessfully() {
        contextRunner
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(CorsConfig.class);
                    assertThat(context).hasSingleBean(FilterRegistrationBean.class);
                });
    }
}

