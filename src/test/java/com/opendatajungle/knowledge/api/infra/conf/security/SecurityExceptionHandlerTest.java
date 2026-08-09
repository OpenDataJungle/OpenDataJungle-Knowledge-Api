package com.opendatajungle.knowledge.api.infra.conf.security;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.JwtException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityExceptionHandlerTest {

    private final SecurityExceptionHandler handler = new SecurityExceptionHandler();

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private CapturingServletOutputStream capturedOutput;

    @BeforeEach
    void setUp() throws IOException {
        capturedOutput = new CapturingServletOutputStream();
        when(response.getOutputStream()).thenReturn(capturedOutput);
        when(request.getRequestURI()).thenReturn("/api/v1/resources");
    }

    @Test
    void commence_shouldWriteUnauthorizedJsonBody() throws IOException {
        // Given
        BadCredentialsException exception = new BadCredentialsException("bad token");

        // When
        handler.commence(request, response, exception);

        // Then
        verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
        verify(response).setStatus(HttpStatus.UNAUTHORIZED.value());
        String body = capturedOutput.contentAsString();
        assertThat(body)
                .contains("\"status\":401")
                .contains("\"error\":\"Unauthorized\"")
                .contains("\"path\":\"/api/v1/resources\"")
                .doesNotContain("\"details\"");
    }

    @Test
    void commence_shouldMentionInvalidJwt_whenCauseIsJwtException() throws IOException {
        // Given
        BadCredentialsException exception = new BadCredentialsException("bad token", new JwtException("expired"));

        // When
        handler.commence(request, response, exception);

        // Then
        assertThat(capturedOutput.contentAsString()).contains("\"details\":\"Invalid or expired JWT token\"");
    }

    @Test
    void handle_shouldWriteForbiddenJsonBody() throws IOException {
        // Given
        AccessDeniedException exception = new AccessDeniedException("no access");

        // When
        handler.handle(request, response, exception);

        // Then
        verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
        verify(response).setStatus(HttpStatus.FORBIDDEN.value());
        assertThat(capturedOutput.contentAsString())
                .contains("\"status\":403")
                .contains("\"error\":\"Forbidden\"")
                .contains("\"path\":\"/api/v1/resources\"");
    }

    private static final class CapturingServletOutputStream extends ServletOutputStream {
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        @Override
        public void write(int b) {
            buffer.write(b);
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
            // not needed for these synchronous tests
        }

        String contentAsString() {
            return buffer.toString(StandardCharsets.UTF_8);
        }
    }
}
