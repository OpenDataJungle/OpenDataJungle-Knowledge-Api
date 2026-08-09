package com.opendatajungle.knowledge.api.infra.conf.mdc;

import com.opendatajungle.knowledge.api.shared.util.UserUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        try {
            populateMdc(request);
            logger.info("Starting request processing");

            filterChain.doFilter(request, response);

            long duration = System.currentTimeMillis() - startTime;
            MDC.put(MDCConstant.TRANSACTION_STATUS, String.valueOf(response.getStatus()));
            MDC.put(MDCConstant.TRANSACTION_DURATION, String.valueOf(duration));
            logger.info("End request processing");
        } finally {
            clearMdc();
        }
    }

    private void populateMdc(HttpServletRequest request) {
        MDC.put(MDCConstant.TRANSACTION_ID, UUID.randomUUID().toString());
        MDC.put(MDCConstant.TRANSACTION_IP, UserUtils.getIpAddr(request));
        MDC.put(MDCConstant.TRANSACTION_PATH, request.getRequestURI());
        MDC.put(MDCConstant.TRANSACTION_QUERY, request.getQueryString());
        MDC.put(MDCConstant.TRANSACTION_STATUS, "");
    }

    private void clearMdc() {
        MDC.remove(MDCConstant.TRANSACTION_ID);
        MDC.remove(MDCConstant.TRANSACTION_IP);
        MDC.remove(MDCConstant.TRANSACTION_USER);
        MDC.remove(MDCConstant.TRANSACTION_PATH);
        MDC.remove(MDCConstant.TRANSACTION_QUERY);
        MDC.remove(MDCConstant.TRANSACTION_STATUS);
        MDC.remove(MDCConstant.TRANSACTION_DURATION);
        MDC.clear();
    }
}
