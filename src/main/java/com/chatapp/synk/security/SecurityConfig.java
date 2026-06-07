package com.chatapp.synk.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;
    private final PhoneNumberAuthenticationProvider phoneNumberAuthProvider;
    private final JwtAuthEntryPoint jwtAuthEntryPoint;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, PhoneNumberAuthenticationProvider phoneNumberAuthProvider) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.phoneNumberAuthProvider = phoneNumberAuthProvider;
        this.jwtAuthEntryPoint = new JwtAuthEntryPoint(); // Initialize the entry point
    }

    //set the user details service and password encoder for the custom authentication provider
    //passes in security filter chain method.
    @Bean
    public AuthenticationProvider authenticationProvider() {
        return phoneNumberAuthProvider; // use the instance Spring knows about
    }

    //we need to register our custom provider to authenticationManager we need this bean as we are explicitly calling
    //authenticationManager in our controller to authenticate the user.
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder builder = http.getSharedObject(AuthenticationManagerBuilder.class);
        builder.authenticationProvider(phoneNumberAuthProvider);
        return builder.build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth ->
                        auth.requestMatchers(
                                        "/auth/authenticate",
                                        "/auth/register",
                                        "/auth/refresh",
                                        "/auth/logout",
                                        "/auth/forgot-password",
                                        "/ws/chat",
                                        "/v3/api-docs/**",
                                        "/swagger-ui/**",
                                        "/swagger-ui.html",
                                        "/actuator/**").permitAll()
                        .anyRequest().authenticated())
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthEntryPoint))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class).build();
    }
}
