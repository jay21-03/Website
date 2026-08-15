package com.bautruc.ecommerce.identity.api;

import java.util.List;

import com.bautruc.ecommerce.common.logging.LogContext;
import com.bautruc.ecommerce.common.response.ApiResponse;
import com.bautruc.ecommerce.common.security.AuthenticatedUser;
import com.bautruc.ecommerce.common.security.BtAccessCookieFactory;
import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.identity.api.request.GoogleLoginRequest;
import com.bautruc.ecommerce.identity.api.response.AuthResponse;
import com.bautruc.ecommerce.identity.api.response.CurrentUserResponse;
import com.bautruc.ecommerce.identity.application.GoogleAuthenticationResult;
import com.bautruc.ecommerce.identity.application.GoogleAuthenticationService;
import com.bautruc.ecommerce.identity.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {
    private final GoogleAuthenticationService googleAuthenticationService;
    private final CsrfAuthenticationStrategy csrfAuthenticationStrategy;
    private final BtAccessCookieFactory btAccessCookieFactory;
    private final BusinessClock businessClock;

    public AuthController(
            GoogleAuthenticationService googleAuthenticationService,
            CsrfAuthenticationStrategy csrfAuthenticationStrategy,
            BtAccessCookieFactory btAccessCookieFactory,
            BusinessClock businessClock
    ) {
        this.googleAuthenticationService = googleAuthenticationService;
        this.csrfAuthenticationStrategy = csrfAuthenticationStrategy;
        this.btAccessCookieFactory = btAccessCookieFactory;
        this.businessClock = businessClock;
    }

    @PostMapping("/api/v1/auth/google")
    public ApiResponse<AuthResponse> google(
            @Valid @RequestBody GoogleLoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        GoogleAuthenticationResult result = googleAuthenticationService.authenticate(request.credential());
        Authentication authentication = authentication(result.user());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        csrfAuthenticationStrategy.onAuthentication(authentication, servletRequest, servletResponse);
        servletResponse.addHeader(HttpHeaders.SET_COOKIE, btAccessCookieFactory.create(result.accessToken()).toString());

        return ApiResponse.success(
                new AuthResponse(CurrentUserResponse.from(result.user()), result.expiresAt()),
                null,
                businessClock.businessNow().toOffsetDateTime(),
                LogContext.currentCorrelationId()
        );
    }

    private Authentication authentication(User user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getEmail(), user.getRole().name());
        return UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}
