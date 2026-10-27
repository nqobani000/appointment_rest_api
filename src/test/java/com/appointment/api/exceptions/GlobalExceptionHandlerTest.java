package com.appointment.api.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleResourceNotFound_returns404WithMessage() {
        UUID id = UUID.randomUUID();

        ResponseEntity<ErrorResponse> response =
                handler.handleResourceNotFound(new ResourceNotFoundException("Active service", id));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(404);
        assertThat(body.error()).isEqualTo("Not Found");
        assertThat(body.message()).isEqualTo("Active service not found: " + id);
        assertThat(body.timestamp()).isNotNull();
    }

    @Test
    void handleInvalidArgument_returns400WithMessage() {
        ResponseEntity<ErrorResponse> response =
                handler.handleInvalidArgument(new InvalidArgumentException("bad id"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(400);
        assertThat(body.error()).isEqualTo("Bad Request");
        assertThat(body.message()).isEqualTo("bad id");
        assertThat(body.timestamp()).isNotNull();
    }

    @Test
    void handleTypeMismatch_returns400NamingParameter() {
        var exception = new org.springframework.web.method.annotation.MethodArgumentTypeMismatchException(
                "abc", UUID.class, "serviceId", null, new IllegalArgumentException());

        ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Invalid value for parameter 'serviceId': abc");
    }

    @Test
    void handleRuntimeException_returns500WithGenericMessage() {
        ResponseEntity<ErrorResponse> response =
                handler.handleRuntimeException(new RuntimeException("secret internals"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(500);
        assertThat(body.error()).isEqualTo("Internal Server Error");
        assertThat(body.message()).isEqualTo("An unexpected error occurred.");
    }
}
