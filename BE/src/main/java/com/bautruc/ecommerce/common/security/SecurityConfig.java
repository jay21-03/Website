package com.bautruc.ecommerce.common.security;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.bautruc.ecommerce.common.config.ApplicationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SecurityJsonResponseWriter responseWriter;
    private final ApplicationProperties properties;
    private final org.springframework.core.env.Environment environment;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            SecurityJsonResponseWriter responseWriter,
            ApplicationProperties properties,
            org.springframework.core.env.Environment environment
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.responseWriter = responseWriter;
        this.properties = properties;
        this.environment = environment;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CookieCsrfTokenRepository csrfTokenRepository,
            CorsConfigurationSource corsConfigurationSource
    ) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers(request ->
                                "POST".equals(request.getMethod())
                                        && "/api/v1/payments/webhook/payos".equals(request.getRequestURI()))
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .addFilterBefore(jwtAuthenticationFilter, LogoutFilter.class)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(authenticatedLogoutMatcher())
                        .addLogoutHandler(btAccessCookieCleanupHandler())
                        .logoutSuccessHandler(logoutSuccessHandler())
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/google").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/collections/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/workshops/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/support/settings").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/workshop/bookings").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments/webhook/payos").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").authenticated()
                        .requestMatchers("/api/v1/cart/**").hasRole("USER")
                        .requestMatchers("/api/v1/checkout").hasRole("USER")
                        .requestMatchers("/api/v1/me/orders/**").hasRole("USER")
                        .requestMatchers("/api/v1/payments/*").hasRole("USER")
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                );
        return http.build();
    }

    @Bean
    CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = new CookieCsrfTokenRepository();
        repository.setCookieName("XSRF-TOKEN");
        repository.setHeaderName("X-XSRF-TOKEN");
        repository.setCookieCustomizer(builder -> builder
                .path("/")
                .httpOnly(true)
                .secure(isProd())
                .sameSite("Lax"));
        return repository;
    }

    @Bean
    CsrfAuthenticationStrategy csrfAuthenticationStrategy(CsrfTokenRepository csrfTokenRepository) {
        return new CsrfAuthenticationStrategy(csrfTokenRepository);
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins());
        configuration.setAllowCredentials(true);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                "Content-Type",
                "X-XSRF-TOKEN",
                "X-Correlation-Id",
                "Idempotency-Key"
        ));
        configuration.setExposedHeaders(List.of("X-Correlation-Id"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private List<String> allowedOrigins() {
        Set<String> origins = new LinkedHashSet<>();
        List<String> configured = properties.allowedOrigins() == null ? List.of() : properties.allowedOrigins();
        for (String value : configured) {
            if (value == null) {
                continue;
            }
            for (String candidate : value.split(",")) {
                String normalized = candidate.trim();
                if (!normalized.isBlank()) {
                    origins.add(normalized);
                }
            }
        }
        if (origins.contains("*")) {
            throw new IllegalStateException("ALLOWED_ORIGINS cannot contain wildcard when credentials are enabled.");
        }
        return new ArrayList<>(origins);
    }

    private AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, exception) -> {
            String code = (String) request.getAttribute(JwtAuthenticationFilter.AUTH_FAILURE_CODE_ATTRIBUTE);
            if (code == null) {
                code = SecurityErrorCodes.AUTH_TOKEN_MISSING;
            }
            responseWriter.writeError(
                    response,
                    HttpStatus.UNAUTHORIZED.value(),
                    code,
                    authenticationMessage(code)
            );
        };
    }

    private AccessDeniedHandler accessDeniedHandler() {
        return (request, response, exception) -> {
            if (exception instanceof CsrfException) {
                responseWriter.writeError(
                        response,
                        HttpStatus.FORBIDDEN.value(),
                        SecurityErrorCodes.CSRF_INVALID,
                        "Invalid CSRF token."
                );
                return;
            }
            responseWriter.writeError(
                    response,
                    HttpStatus.FORBIDDEN.value(),
                    SecurityErrorCodes.ACCESS_DENIED,
                    "Access denied."
            );
        };
    }

    private LogoutSuccessHandler logoutSuccessHandler() {
        return (request, response, authentication) ->
                responseWriter.writeSuccess(response, HttpStatus.OK.value());
    }

    private LogoutHandler btAccessCookieCleanupHandler() {
        return (request, response, authentication) -> response.addHeader(
                HttpHeaders.SET_COOKIE,
                expiredBtAccessCookie().toString()
        );
    }

    private ResponseCookie expiredBtAccessCookie() {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(JwtAuthenticationFilter.ACCESS_COOKIE_NAME, "")
                .path("/")
                .httpOnly(true)
                .secure(isProd())
                .sameSite("Lax")
                .maxAge(0);
        String domain = properties.auth() == null ? null : properties.auth().cookieDomain();
        if (domain != null && !domain.isBlank()) {
            builder.domain(domain.trim());
        }
        return builder.build();
    }

    private RequestMatcher authenticatedLogoutMatcher() {
        AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();
        return request -> {
            if (!"POST".equals(request.getMethod()) || !"/api/v1/auth/logout".equals(request.getRequestURI())) {
                return false;
            }
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            return authentication != null
                    && authentication.isAuthenticated()
                    && !trustResolver.isAnonymous(authentication);
        };
    }

    private String authenticationMessage(String code) {
        return switch (code) {
            case SecurityErrorCodes.AUTH_TOKEN_EXPIRED -> "Authentication token is expired.";
            case SecurityErrorCodes.AUTH_TOKEN_INVALID -> "Authentication token is invalid.";
            case SecurityErrorCodes.USER_BLOCKED -> "User is blocked.";
            default -> "Authentication token is missing.";
        };
    }

    private boolean isProd() {
        return environment.matchesProfiles("prod");
    }
}
