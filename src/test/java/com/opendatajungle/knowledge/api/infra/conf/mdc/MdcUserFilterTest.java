package com.opendatajungle.knowledge.api.infra.conf.mdc;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MdcUserFilterTest {

    private final MdcUserFilter filter = new MdcUserFilter();

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    void doFilter_shouldPutAuthenticatedUsernameInMdc_beforeDelegatingToChain() throws Exception {
        // Given
        Authentication authentication = new TestingAuthenticationToken("alice", "creds");
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        AtomicReference<String> userDuringChain = new AtomicReference<>();
        doAnswer(_ -> {
            userDuringChain.set(MDC.get(MDCConstant.TRANSACTION_USER));
            return null;
        }).when(filterChain).doFilter(request, response);

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        assertThat(userDuringChain.get()).isEqualTo("alice");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_shouldPutUnauthenticatedMarker_whenNoAuthenticationPresent() throws Exception {
        // Given
        AtomicReference<String> userDuringChain = new AtomicReference<>();
        doAnswer(_ -> {
            userDuringChain.set(MDC.get(MDCConstant.TRANSACTION_USER));
            return null;
        }).when(filterChain).doFilter(request, response);

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        assertThat(userDuringChain.get()).isEqualTo("UNAUTHENTICATED");
    }

    @Test
    void doFilter_shouldRemoveUsernameFromMdc_afterChainCompletesSuccessfully() throws Exception {
        // Given / When
        filter.doFilter(request, response, filterChain);

        // Then
        assertThat(MDC.get(MDCConstant.TRANSACTION_USER)).isNull();
    }

    @Test
    void doFilter_shouldRemoveUsernameFromMdcAndPropagateException_whenChainThrows() throws Exception {
        // Given
        ServletException chainException = new ServletException("boom");
        doThrow(chainException).when(filterChain).doFilter(request, response);

        // When & Then
        assertThatThrownBy(() -> filter.doFilter(request, response, filterChain))
                .isSameAs(chainException);
        assertThat(MDC.get(MDCConstant.TRANSACTION_USER)).isNull();
    }
}
