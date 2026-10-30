package com.syncstock.auth.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegisterRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Password123", "Admin@123", "Str0ngP@ssword!", "Abcdef12"})
    @DisplayName("M-06: Mật khẩu hợp lệ (>=8 ký tự, có chữ hoa, thường, số) -> Không có lỗi validation")
    void password_ValidPasswords_ShouldPassValidation(String validPassword) {
        RegisterRequest request = RegisterRequest.builder()
                .username("testuser")
                .password(validPassword)
                .build();

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Mật khẩu hợp lệ không được gây lỗi validation: " + validPassword);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Pass1",          // Quá ngắn (< 8 ký tự)
            "password123",    // Thiếu chữ hoa
            "PASSWORD123",    // Thiếu chữ thường
            "PasswordOnly",   // Thiếu chữ số
            "12345678",       // Thiếu chữ hoa và chữ thường
            "   "             // Trống / blank
    })
    @DisplayName("M-06: Mật khẩu không thỏa mãn quy tắc mạnh -> Báo lỗi validation")
    void password_InvalidPasswords_ShouldFailValidation(String invalidPassword) {
        RegisterRequest request = RegisterRequest.builder()
                .username("testuser")
                .password(invalidPassword)
                .build();

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Mật khẩu yếu phải gây lỗi validation: " + invalidPassword);
        boolean hasPasswordError = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("password"));
        assertTrue(hasPasswordError);
    }
}

