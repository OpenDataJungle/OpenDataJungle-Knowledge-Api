package com.laulem.vectopath.knowledge.api.shared.util;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserUtilsTest {

    @Mock
    private HttpServletRequest request;

    @Test
    void getIpAddr_shouldReturnRemoteAddr_whenItIsAPublicIp() {
        // Given
        when(request.getRemoteAddr()).thenReturn("8.8.8.8");

        // When
        String result = UserUtils.getIpAddr(request);

        // Then
        assertThat(result).isEqualTo("8.8.8.8");
    }

    @Test
    void getIpAddr_shouldReturnUnknown_whenRemoteAddrIsBlank() {
        // Given
        when(request.getRemoteAddr()).thenReturn("");

        // When
        String result = UserUtils.getIpAddr(request);

        // Then
        assertThat(result).isEqualTo("UNKNOWN");
    }

    @Test
    void getIpAddr_shouldReturnUnknown_whenRemoteAddrIsLocalhostIpv6() {
        // Given
        when(request.getRemoteAddr()).thenReturn("0:0:0:0:0:0:0:1");

        // When
        String result = UserUtils.getIpAddr(request);

        // Then
        assertThat(result).isEqualTo("UNKNOWN");
    }

    @Test
    void getIpAddr_shouldReturnUnknown_whenRemoteAddrIsSiteLocalAddress() {
        // Given
        when(request.getRemoteAddr()).thenReturn("192.168.1.10");

        // When
        String result = UserUtils.getIpAddr(request);

        // Then
        assertThat(result).isEqualTo("UNKNOWN");
    }

    @Test
    void getIpAddr_shouldReturnUnknown_whenRemoteAddrCannotBeResolved() {
        // Given: an unresolvable host name triggers UnknownHostException inside isNonLocalIp, which is swallowed.
        when(request.getRemoteAddr()).thenReturn("this-host-does-not-exist-xyz123");

        // When
        String result = UserUtils.getIpAddr(request);

        // Then
        assertThat(result).isEqualTo("UNKNOWN");
    }

    @Test
    void getIpAddr_shouldCombineRemoteAddrAndForwardedHeader_whenBothAreNonLocal() {
        // Given
        when(request.getRemoteAddr()).thenReturn("203.0.113.1");
        when(request.getHeader("X-FORWARDED-FOR")).thenReturn("203.0.113.2");
        when(request.getHeader("X-FORWARDED-FRONT")).thenReturn(null);
        when(request.getHeader("HTTP_CLIENT_IP")).thenReturn(null);
        when(request.getHeader("HTTP_X_FORWARDED_FOR")).thenReturn(null);

        // When
        String result = UserUtils.getIpAddr(request);

        // Then
        assertThat(result).isEqualTo("203.0.113.1,203.0.113.2");
    }

    @Test
    void getIpAddr_shouldSplitCommaSeparatedHeaderValues_reversedAndTrimmed() {
        // Given
        when(request.getRemoteAddr()).thenReturn(null);
        when(request.getHeader("X-FORWARDED-FOR")).thenReturn("203.0.113.1, 203.0.113.2 ,203.0.113.3");
        when(request.getHeader("X-FORWARDED-FRONT")).thenReturn(null);
        when(request.getHeader("HTTP_CLIENT_IP")).thenReturn(null);
        when(request.getHeader("HTTP_X_FORWARDED_FOR")).thenReturn(null);

        // When
        String result = UserUtils.getIpAddr(request);

        // Then
        assertThat(result).isEqualTo("203.0.113.3,203.0.113.2,203.0.113.1");
    }

    @Test
    void getIpAddr_shouldFilterOutOnlyLocalAddresses_andKeepPublicOnes() {
        // Given
        when(request.getRemoteAddr()).thenReturn("10.0.0.5");
        when(request.getHeader("X-FORWARDED-FOR")).thenReturn("198.51.100.7");
        when(request.getHeader("X-FORWARDED-FRONT")).thenReturn(null);
        when(request.getHeader("HTTP_CLIENT_IP")).thenReturn(null);
        when(request.getHeader("HTTP_X_FORWARDED_FOR")).thenReturn(null);

        // When
        String result = UserUtils.getIpAddr(request);

        // Then
        assertThat(result).isEqualTo("198.51.100.7");
    }
}
