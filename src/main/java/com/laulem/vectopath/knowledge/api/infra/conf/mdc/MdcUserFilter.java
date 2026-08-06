package com.laulem.vectopath.knowledge.api.infra.conf.mdc;

import com.laulem.vectopath.knowledge.api.shared.util.UserUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class MdcUserFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            MDC.put(MDCConstant.TRANSACTION_USER, UserUtils.getUsername());
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDCConstant.TRANSACTION_USER);
        }
    }
}
