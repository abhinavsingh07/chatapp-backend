package com.chatapp.synk.security;

import com.chatapp.synk.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);
    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    private static final List<String> EXCLUDED_URLS = List.of(
     "/auth/authenticate",
     "/auth/register", 
     "/auth/refresh", 
     "/auth/logout");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

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
                // Signature is verified here inside JwtUtil
                String username = jwtUtil.extractUsername(token);
                List<String> roles = jwtUtil.extractRoles(token);
                /**
                 * "If token signature is valid,
                 * then token was issued by my server."
                 * That is the core idea of JWT authentication.
                 */
                if (jwtUtil.isTokenValid(token)) { // validate signature + expiry

                    // Convert roles into Spring Security authorities
                    List<GrantedAuthority> authorities = roles.stream().map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

                    // Build Authentication object directly from JWT claims
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username,
                            null, authorities);

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                } else {
                    // sendUnauthorizedResponse(response, "Invalid or expired JWT token");
                    // return;
                    // invalid or expired -> let Spring handle
                    throw new BadCredentialsException("Invalid or expired JWT token");
                }
            } catch (ExpiredJwtException ex) {
                //go to our jwt auth entry point to send a 401 with our error response wrapper
                throw new BadCredentialsException("JWT token expired", ex);
            } catch (Exception e) {
                throw new BadCredentialsException("JWT token validation failed: " + e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
