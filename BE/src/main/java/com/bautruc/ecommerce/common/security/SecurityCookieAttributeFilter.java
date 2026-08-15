package com.bautruc.ecommerce.common.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class SecurityCookieAttributeFilter extends OncePerRequestFilter {
    private static final String SET_COOKIE = "Set-Cookie";
    private static final List<String> SECURITY_COOKIE_NAMES = List.of(
            "XSRF-TOKEN",
            JwtAuthenticationFilter.ACCESS_COOKIE_NAME
    );

    private final Environment environment;

    public SecurityCookieAttributeFilter(Environment environment) {
        this.environment = environment;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        filterChain.doFilter(request, response);
        hardenSecurityCookies(response);
    }

    private void hardenSecurityCookies(HttpServletResponse response) {
        Collection<String> headers = response.getHeaders(SET_COOKIE);
        if (headers.isEmpty()) {
            return;
        }

        List<String> hardened = headers.stream()
                .map(this::hardenCookie)
                .toList();

        response.setHeader(SET_COOKIE, hardened.getFirst());
        for (int i = 1; i < hardened.size(); i++) {
            response.addHeader(SET_COOKIE, hardened.get(i));
        }
    }

    private String hardenCookie(String header) {
        if (!isSecurityCookie(header)) {
            return header;
        }
        List<String> attributes = new ArrayList<>();
        if (!containsAttribute(header, "SameSite")) {
            attributes.add("SameSite=Lax");
        }
        if (isProd() && !containsAttribute(header, "Secure")) {
            attributes.add("Secure");
        }
        if (attributes.isEmpty()) {
            return header;
        }
        return header + "; " + String.join("; ", attributes);
    }

    private boolean isSecurityCookie(String header) {
        return SECURITY_COOKIE_NAMES.stream().anyMatch(name -> header.startsWith(name + "="));
    }

    private boolean containsAttribute(String header, String attribute) {
        return header.toLowerCase(java.util.Locale.ROOT)
                .contains(attribute.toLowerCase(java.util.Locale.ROOT));
    }

    private boolean isProd() {
        return environment.matchesProfiles("prod");
    }
}
