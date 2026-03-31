package com.salygin.ssubench.security;

import com.salygin.ssubench.entity.User;
import com.salygin.ssubench.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.authentication.BadCredentialsException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService tokenService;
    private final UserRepository users;
    private final AuthenticationEntryPoint authEntryPoint;
    private final AccessDeniedHandler deniedHandler;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        String uri = req.getRequestURI();

        return uri.startsWith("/auth/")
                || uri.startsWith("/swagger-ui")
                || uri.startsWith("/v3/api-docs")
                || uri.equals("/openapi.yaml")
                || uri.startsWith("/swagger")
                || uri.startsWith("/webjars")
                || uri.equals("/error");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest req,
            HttpServletResponse res,
            FilterChain chain
    ) throws ServletException, IOException {

        String authHeader = req.getHeader(HttpHeaders.AUTHORIZATION);

        if (!StringUtils.hasText(authHeader) || !authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            chain.doFilter(req, res);
            return;
        }

        String jwt = authHeader.substring(7).trim();
        Optional<JwtPrincipal> decoded = tokenService.decode(jwt);

        if (decoded.isEmpty()) {
            authEntryPoint.commence(
                    req,
                    res,
                    new BadCredentialsException("JWT is invalid or expired")
            );
            return;
        }

        JwtPrincipal tokenData = decoded.get();

        User account = users.getById(tokenData.id())
                .orElse(null);

        if (account == null) {
            authEntryPoint.commence(
                    req,
                    res,
                    new BadCredentialsException("Account not found")
            );
            return;
        }

        if (Boolean.TRUE.equals(account.getBlocked())) {
            deniedHandler.handle(
                    req,
                    res,
                    new AccessDeniedException("Account access is restricted")
            );
            return;
        }

        JwtPrincipal principal = new JwtPrincipal(
                account.getId(),
                account.getEmail(),
                account.getRole()
        );

        List<SimpleGrantedAuthority> roles =
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + account.getRole()));

        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(principal, null, roles);

        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
        SecurityContextHolder.getContext().setAuthentication(authToken);

        try {
            chain.doFilter(req, res);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }
}
