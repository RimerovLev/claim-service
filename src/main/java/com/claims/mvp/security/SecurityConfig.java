package com.claims.mvp.security;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Central Spring Security configuration.
 *
 * Two responsibilities:
 *   1. Declare which URLs are public and which require a valid JWT.
 *   2. Insert JwtAuthFilter into the filter chain before Spring's default login filter.
 *
 * How the filter chain works:
 *   Every HTTP request passes through a list of filters in order.
 *   Spring Security adds its own by default (CSRF, form login, session management, etc.).
 *   We prepend JwtAuthFilter — it reads the token and populates SecurityContextHolder.
 *   Subsequent filters see that authentication is already set and pass the request through.
 */
@ConditionalOnProperty(name = "app.security.enabled", havingValue = "true", matchIfMissing = true)
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF protection defends against attacks that exploit the browser's automatic
                // cookie sending. With JWT in the Authorization header the browser never sends
                // the token automatically, so CSRF is not a threat here — disable it.
                .csrf(AbstractHttpConfigurer::disable)

                // Tell Spring not to create an HTTP session.
                // JWT is stateless: every request carries its own token, the server remembers
                // nothing between calls. A session would just waste memory.
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // Registration and login endpoints — publicly accessible, no token needed
                        .requestMatchers("/api/auth/**").permitAll()
                        // Thymeleaf pages and static assets — also public
                        .requestMatchers("/", "/app.html", "/css/**", "/js/**").permitAll()
                        // Everything else (including /api/claims/**) requires a valid token
                        .anyRequest().authenticated()
                )
                // Insert our filter BEFORE UsernamePasswordAuthenticationFilter.
                // That default filter handles HTML form login, which we don't use.
                // Our filter runs first, validates the JWT, and sets the Authentication —
                // the rest of the chain sees an authenticated request and lets it through.
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();

    }

    /**
     * BCrypt password encoder used when registering and verifying users.
     * BCrypt automatically generates a random salt per password — two identical
     * passwords produce different hashes, making precomputed rainbow-table attacks
     * useless. Strength 12 means ~250 ms per hash on modern hardware — slow enough
     * to deter brute-force, fast enough for normal login traffic.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


}
