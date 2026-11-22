package com.biojaveriana.api.common.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.validation.ConstraintViolationException;
import com.biojaveriana.api.auth.dto.LoginRequest;
import com.biojaveriana.api.auth.exceptions.InvalidCredentialsException;
import com.biojaveriana.api.common.dto.ErrorResponse;
import com.biojaveriana.api.user.BanType;
import com.biojaveriana.api.user.exceptions.UserDeactivatedException;

class GlobalExceptionHandlerTest {

    private static class ParamFixture {
        void take(Object body) {
        }
    }

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/test");
        return request;
    }

    @Test
    @DisplayName("ServiceException maps to its status, code, message and path")
    void serviceExceptionIsMapped() {
        ResponseEntity<ErrorResponse> response = handler.handleServiceException(new InvalidCredentialsException(), request());

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertEquals(401, body.status());
        assertEquals("INVALID_CREDENTIALS", body.code());
        assertEquals("Incorrect email or password", body.message());
        assertEquals("/api/v1/test", body.path());
        assertNotNull(body.timestamp());
        assertTrue(body.details().isEmpty());
    }

    @Test
    @DisplayName("ServiceException details are passed through to the response")
    void serviceExceptionDetailsArePassedThrough() {
        Instant expiry = Instant.now().plusSeconds(10);
        UserDeactivatedException exception = new UserDeactivatedException(BanType.TEMPORAL, expiry);

        ResponseEntity<ErrorResponse> response = handler.handleServiceException(exception, request());

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("temporal", response.getBody().details().get("banType"));
        assertEquals(expiry, response.getBody().details().get("expiresAt"));
    }

    @Test
    @DisplayName("validation errors are joined into a 400 response")
    void validationErrorsAreJoined() throws Exception {
        BindException bindException = new BindException(new LoginRequest("jane@example.com", "correct-password-1"), "request");
        bindException.rejectValue("email", "email.invalid", "Invalid email");
        bindException.rejectValue("password", "size", "Too short");
        MethodArgumentNotValidException exception =
            new MethodArgumentNotValidException(new MethodParameter(ParamFixture.class.getDeclaredMethod("take", Object.class), 0), bindException);

        ResponseEntity<ErrorResponse> response = handler.handleValidation(exception, request());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("VALIDATION_ERROR", response.getBody().code());
        assertTrue(response.getBody().message().contains("email: Invalid email"));
        assertTrue(response.getBody().message().contains("password: Too short"));
    }

    @Test
    @DisplayName("unexpected exceptions produce a 500 without leaking details")
    void unexpectedExceptionIs500() {
        ResponseEntity<ErrorResponse> response = handler.handleUnexpectedException(new RuntimeException("secret"), request());

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().code());
        assertEquals("/api/v1/test", response.getBody().path());
    }

    @Test
    @DisplayName("unsupported HTTP methods map to 405")
    void methodNotAllowedIs405() {
        ResponseEntity<ErrorResponse> response = handler.handleMethodNotAllowed(
            new HttpRequestMethodNotSupportedException("PATCH"), request());

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertEquals("METHOD_NOT_ALLOWED", response.getBody().code());
    }

    @Test
    @DisplayName("unknown resources map to 404")
    void noResourceFoundIs404() {
        ResponseEntity<ErrorResponse> response = handler.handleNoResourceFound(
            new NoResourceFoundException(HttpMethod.GET, "nope", "resource not found"), request());

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("NOT_FOUND", response.getBody().code());
    }

    @Test
    @DisplayName("method-level constraint violations map to 400")
    void constraintViolationIs400() {
        jakarta.validation.ConstraintViolation<Object> violation = mock(jakarta.validation.ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("must be greater than or equal to 0");
        ConstraintViolationException exception =
            new ConstraintViolationException("failed", Set.of(violation));

        ResponseEntity<ErrorResponse> response = handler.handleConstraintViolation(exception, request());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("VALIDATION_ERROR", response.getBody().code());
        assertTrue(response.getBody().message().contains("must be greater than or equal to 0"));
    }
}
