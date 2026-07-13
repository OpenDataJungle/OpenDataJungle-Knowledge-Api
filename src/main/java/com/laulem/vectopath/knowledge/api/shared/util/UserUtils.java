package com.laulem.vectopath.knowledge.api.shared.util;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.util.Strings;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UserUtils {
    private static final String LOCALHOST_IP = "0:0:0:0:0:0:0:1";
    private static final List<String> IP_HEADERS = Arrays.asList("X-FORWARDED-FOR", "X-FORWARDED-FRONT", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR");

    private UserUtils() {
    }

    public static String getIpAddr(final HttpServletRequest request) {
        String ipAdresse = Stream.concat(Stream.ofNullable(request.getRemoteAddr()), UserUtils.IP_HEADERS.stream().map(request::getHeader))
                .filter(Objects::nonNull)
                .flatMap(str -> Arrays.asList(str.split(",")).reversed().stream())
                .map(String::trim)
                .filter(UserUtils::isNonLocalIp)
                .collect(Collectors.joining(","));

        if (ipAdresse.isBlank()) {
            return "UNKNOWN";
        }
        return ipAdresse;
    }

    private static boolean isNonLocalIp(final String ip) {
        try {
            return Strings.isNotBlank(ip) && !UserUtils.LOCALHOST_IP.equals(ip) && !InetAddress.getByName(ip).isSiteLocalAddress();
        } catch (final Exception _) {
            return false;
        }
    }

    public static String getUsername() {
        return Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName)
                .orElse("UNAUTHENTICATED");
    }
}
