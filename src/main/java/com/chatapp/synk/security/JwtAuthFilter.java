package com.chatapp.synk.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.chatapp.synk.exceptionHandler.InvalidTokenException;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);
    private final JwtUtil jwtUtil;
    private final JwtAuthEntryPoint jwtAuthEntryPoint;

    public JwtAuthFilter(JwtUtil jwtUtil, JwtAuthEntryPoint jwtAuthEntryPoint) {
        this.jwtUtil = jwtUtil;
        this.jwtAuthEntryPoint = jwtAuthEntryPoint;
    }

    private static final List<String> EXCLUDED_URLS = List.of(
            "/auth/authenticate",
            "/auth/register",
            "/auth/refresh",
            "/auth/logout",
            "/auth/forgot-password");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String requestPath = request.getRequestURI();
            for (String uri : EXCLUDED_URLS) {
                if (requestPath.contains(uri)) {
                    filterChain.doFilter(request, response);
                    return; // Skip JWT authentication
                }
            }

            final String authHeader = request.getHeader("Authorization");
            String token = null;

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }

            if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    // Parse token ONCE and extract all claims from the same parse operation
                    // This is much more efficient than parsing the token multiple times
                    Claims claims = jwtUtil.getTokenClaims(token);

                    String username = claims.getSubject();
                    @SuppressWarnings("unchecked")
                    List<String> roles = (List<String>) claims.get("roles");

                    if (roles == null) {
                        roles = List.of();
                    }

                    /**
                     * "If token signature is valid,
                     * then token was issued by my server."
                     * That is the core idea of JWT authentication.
                     */

                    // Convert roles into Spring Security authorities
                    List<GrantedAuthority> authorities = roles.stream().map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

                    // Build Authentication object directly from JWT claims
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username,
                            null, authorities);

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Set user details in request attribute for later use in controllers
                    request.setAttribute("userDetails", claims);
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                } catch (InvalidTokenException ex) {
                    SecurityContextHolder.clearContext();
                    AuthenticationException authEx = new BadCredentialsException(
                            "JWT token validation failed: " + ex.getMessage());
                    jwtAuthEntryPoint.commence(request, response, authEx);
                    return; // Important: return here to prevent further filter chain execution
                } catch (Exception e) {
                    SecurityContextHolder.clearContext();
                    AuthenticationException authEx = new BadCredentialsException(
                            "JWT token validation failed: " + e.getMessage());
                    jwtAuthEntryPoint.commence(request, response, authEx);
                    return; // Important: return here
                }

            }

            // This ALWAYS executes
            // if above if block dont runs So the request continues through the filter chain
            // without setting any authentication in SecurityContextHolder.
            // which results to 401.
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            logger.error("Unexpected error in JWT filter", e);
            SecurityContextHolder.clearContext();
            try {
                AuthenticationException authEx = new BadCredentialsException("Authentication failed");
                jwtAuthEntryPoint.commence(request, response, authEx);
            } catch (IOException ioException) {
                logger.error("Error sending error response", ioException);
            }
        }
    }
}
