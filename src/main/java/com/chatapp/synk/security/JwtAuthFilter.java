package com.chatapp.synk.security;

import com.chatapp.synk.exceptionHandler.ServiceException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService customUserDetailsService;

    public JwtAuthFilter() {
        // Default constructor for Spring to inject dependencies
    }

    private static final List<String> EXCLUDED_URLS = List.of("/auth/authenticate", "/auth/register");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        //skip urls from jwt validation
        String requestPath = request.getRequestURI();
        for (String uri : EXCLUDED_URLS) {
            if (requestPath.contains(uri)) {
                logger.debug("Skipping JWT authentication for request: {}", requestPath);
                filterChain.doFilter(request, response);
                return; // Skip JWT authentication
            }
        }

        final String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            try {
                username = jwtUtil.extractUsername(token);
                logger.debug("JWT extracted for username: {}", username);
            } catch (Exception e) {
                logger.error("Failed to extract username from JWT token", e);
                throw new ServiceException(e.getMessage(), e);
            }
        } else {
            logger.warn("Missing or invalid Authorization header");
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

                if (jwtUtil.validateToken(token, userDetails)) {
                    logger.info("JWT validation succeeded for user: {}", username);

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                } else {
                    logger.warn("JWT validation failed for user: {}", username);
                }
            } catch (Exception e) {
                logger.error("Authentication setup failed for user: {}", username, e);
            }
        }

        filterChain.doFilter(request, response);
    }
}
