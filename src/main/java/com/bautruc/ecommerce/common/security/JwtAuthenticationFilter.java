package com.bautruc.ecommerce.common.security;

import java.io.IOException;
import java.util.List;

import com.bautruc.ecommerce.identity.domain.User;
import com.bautruc.ecommerce.identity.domain.UserStatus;
import com.bautruc.ecommerce.identity.infrastructure.UserJpaRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    public static final String ACCESS_COOKIE_NAME = "BT_ACCESS";
    public static final String AUTH_FAILURE_CODE_ATTRIBUTE = "bautruc.auth.failureCode";

    private final JwtTokenService jwtTokenService;
    private final UserJpaRepository userRepository;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService, UserJpaRepository userRepository) {
        this.jwtTokenService = jwtTokenService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = accessToken(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            ValidatedJwt jwt = jwtTokenService.parseAndValidate(token);
            User user = userRepository.findById(jwt.userId())
                    .orElseThrow(() -> new JwtAuthenticationException(
                            SecurityErrorCodes.AUTH_TOKEN_INVALID,
                            "JWT user no longer exists."
                    ));
            authenticateActiveUser(user);
        } catch (JwtAuthenticationException exception) {
            SecurityContextHolder.clearContext();
            request.setAttribute(AUTH_FAILURE_CODE_ATTRIBUTE, exception.code());
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateActiveUser(User user) {
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new JwtAuthenticationException(SecurityErrorCodes.USER_BLOCKED, "User is blocked.");
        }

        AuthenticatedUser principal = new AuthenticatedUser(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    private String accessToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (ACCESS_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
