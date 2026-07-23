package com.libromagico.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("LoginRequest: valid email and password should pass")
    void loginRequest_valid() {
        var req = new LoginRequest("user@example.com", "password123");
        assertTrue(validator.validate(req).isEmpty());
    }

    @Test
    @DisplayName("LoginRequest: blank email should fail")
    void loginRequest_blankEmail() {
        var req = new LoginRequest("", "password123");
        assertFalse(validator.validate(req).isEmpty());
    }

    @Test
    @DisplayName("LoginRequest: invalid email format should fail")
    void loginRequest_invalidEmail() {
        var req = new LoginRequest("not-an-email", "password123");
        assertFalse(validator.validate(req).isEmpty());
    }

    @Test
    @DisplayName("LoginRequest: blank password should fail")
    void loginRequest_blankPassword() {
        var req = new LoginRequest("user@example.com", "");
        assertFalse(validator.validate(req).isEmpty());
    }

    @Test
    @DisplayName("RegisterRequest: all valid should pass")
    void registerRequest_valid() {
        var req = new RegisterRequest("Juan", "juan@example.com", "pass123", "12345678", "+54911111111");
        assertTrue(validator.validate(req).isEmpty());
    }

    @Test
    @DisplayName("RegisterRequest: blank nombre should fail")
    void registerRequest_blankNombre() {
        var req = new RegisterRequest("", "juan@example.com", "pass123", "12345678", "+54911111111");
        assertFalse(validator.validate(req).isEmpty());
    }

    @Test
    @DisplayName("RegisterRequest: invalid email should fail")
    void registerRequest_invalidEmail() {
        var req = new RegisterRequest("Juan", "invalid", "pass123", "12345678", "+54911111111");
        assertFalse(validator.validate(req).isEmpty());
    }

    @Test
    @DisplayName("RegisterRequest: invalid dni (not 8 digits) should fail")
    void registerRequest_invalidDni() {
        var req = new RegisterRequest("Juan", "juan@example.com", "pass123", "1234", "+54911111111");
        assertFalse(validator.validate(req).isEmpty());
    }

    @Test
    @DisplayName("RegisterRequest: invalid telefono (no leading +) should fail")
    void registerRequest_invalidTelefono() {
        var req = new RegisterRequest("Juan", "juan@example.com", "pass123", "12345678", "54911111111");
        assertFalse(validator.validate(req).isEmpty());
    }

    @Test
    @DisplayName("ForgotPasswordRequest: valid email should pass")
    void forgotPasswordRequest_valid() {
        var req = new ForgotPasswordRequest("user@example.com");
        assertTrue(validator.validate(req).isEmpty());
    }

    @Test
    @DisplayName("ForgotPasswordRequest: blank email should fail")
    void forgotPasswordRequest_blankEmail() {
        var req = new ForgotPasswordRequest("");
        assertFalse(validator.validate(req).isEmpty());
    }

    @Test
    @DisplayName("ForgotPasswordRequest: invalid email should fail")
    void forgotPasswordRequest_invalidEmail() {
        var req = new ForgotPasswordRequest("not-an-email");
        assertFalse(validator.validate(req).isEmpty());
    }

    @Test
    @DisplayName("ResetPasswordRequest: valid token and password should pass")
    void resetPasswordRequest_valid() {
        var req = new ResetPasswordRequest("some-token", "newPass123");
        assertTrue(validator.validate(req).isEmpty());
    }

    @Test
    @DisplayName("ResetPasswordRequest: blank token should fail")
    void resetPasswordRequest_blankToken() {
        var req = new ResetPasswordRequest("", "newPass123");
        assertFalse(validator.validate(req).isEmpty());
    }

    @Test
    @DisplayName("ResetPasswordRequest: password too short (< 6 chars) should fail")
    void resetPasswordRequest_shortPassword() {
        var req = new ResetPasswordRequest("some-token", "abc12");
        assertFalse(validator.validate(req).isEmpty());
    }

    @Test
    @DisplayName("ResetPasswordRequest: blank password should fail")
    void resetPasswordRequest_blankPassword() {
        var req = new ResetPasswordRequest("some-token", "");
        assertFalse(validator.validate(req).isEmpty());
    }

    @Test
    @DisplayName("PrestamoRequest: valid usuarioId and libroIsbn should pass")
    void prestamoRequest_valid() {
        var req = new PrestamoRequest(1L, "978-3-16-148410-0");
        assertTrue(validator.validate(req).isEmpty());
    }

    @Test
    @DisplayName("PrestamoRequest: null usuarioId should fail")
    void prestamoRequest_nullUsuarioId() {
        var req = new PrestamoRequest(null, "978-3-16-148410-0");
        assertFalse(validator.validate(req).isEmpty());
    }

    @Test
    @DisplayName("PrestamoRequest: blank libroIsbn should fail")
    void prestamoRequest_blankLibroIsbn() {
        var req = new PrestamoRequest(1L, "");
        assertFalse(validator.validate(req).isEmpty());
    }

    @Test
    @DisplayName("UpdateRolRequest: non-null rol should pass")
    void updateRolRequest_valid() {
        var req = new UpdateRolRequest("ADMIN");
        assertTrue(validator.validate(req).isEmpty());
    }

    @Test
    @DisplayName("UpdateRolRequest: null rol should fail")
    void updateRolRequest_nullRol() {
        var req = new UpdateRolRequest(null);
        assertFalse(validator.validate(req).isEmpty());
    }

    @Test
    @DisplayName("UpdateEstadoRequest: non-null estado should pass")
    void updateEstadoRequest_valid() {
        var req = new UpdateEstadoRequest("ACTIVO");
        assertTrue(validator.validate(req).isEmpty());
    }

    @Test
    @DisplayName("UpdateEstadoRequest: null estado should fail")
    void updateEstadoRequest_nullEstado() {
        var req = new UpdateEstadoRequest(null);
        assertFalse(validator.validate(req).isEmpty());
    }
}
