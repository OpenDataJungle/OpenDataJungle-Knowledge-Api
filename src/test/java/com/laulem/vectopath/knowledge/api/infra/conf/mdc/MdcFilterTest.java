package com.laulem.vectopath.knowledge.api.infra.conf.mdc;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MdcFilterTest {

    private final MdcFilter filter = new MdcFilter();

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Test
    void doFilter_shouldPopulateMdcDuringChainExecution_andClearItAfterward() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/resources");
        when(request.getQueryString()).thenReturn("name=test");
        when(request.getRemoteAddr()).thenReturn("203.0.113.5");
        when(response.getStatus()).thenReturn(200);

        AtomicReference<Map<String, String>> mdcDuringChain = new AtomicReference<>();
        doAnswer(_ -> {
            mdcDuringChain.set(MDC.getCopyOfContextMap());
            return null;
        }).when(filterChain).doFilter(request, response);

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        assertThat(mdcDuringChain.get())
                .containsEntry(MDCConstant.TRANSACTION_PATH, "/api/v1/resources")
                .containsEntry(MDCConstant.TRANSACTION_QUERY, "name=test")
                .containsEntry(MDCConstant.TRANSACTION_IP, "203.0.113.5");
        assertThat(mdcDuringChain.get().get(MDCConstant.TRANSACTION_ID)).isNotBlank();

        assertThat(MDC.get(MDCConstant.TRANSACTION_ID)).isNull();
        assertThat(MDC.get(MDCConstant.TRANSACTION_PATH)).isNull();
        assertThat(MDC.get(MDCConstant.TRANSACTION_STATUS)).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_shouldClearMdc_evenWhenChainThrows() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/resources");
        when(request.getRemoteAddr()).thenReturn("203.0.113.5");
        doThrow(new RuntimeException("downstream failure")).when(filterChain).doFilter(request, response);

        // When & Then
        assertThatThrownBy(() -> filter.doFilter(request, response, filterChain))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("downstream failure");

        assertThat(MDC.get(MDCConstant.TRANSACTION_ID)).isNull();
        assertThat(MDC.get(MDCConstant.TRANSACTION_PATH)).isNull();
    }
}
