package com.claims.mvp.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Test-time Security: permits everything so integration tests can hit the API
 * without a JWT.
 *
 * Loads ONLY when {@code app.security.enabled=false} (set by IntegrationTestBase).
 * In that mode production SecurityConfig is excluded — only this chain exists,
 * so there's no order conflict and no JwtAuthFilter in the chain.
 */
@ConditionalOnProperty(name = "app.security.enabled", havingValue = "false")
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class TestSecurityConfig {

    @Bean
    public SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .addFilterBefore(new OncePerRequestFilter() {
                    @Override
                    protected void doFilterInternal(HttpServletRequest req,
                                                    HttpServletResponse res,
                                                    FilterChain chain)
                            throws ServletException, IOException {
                        // Sets a fixed test user so assertOwnerOrAdmin sees a real Authentication,
                        // not anonymous. Email must match what ClaimIntegrationTest uses when
                        // creating users (it creates a real user in DB and then accesses its claims).
                        if (SecurityContextHolder.getContext().getAuthentication() == null
                                || SecurityContextHolder.getContext().getAuthentication()
                                instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
                            var auth = new UsernamePasswordAuthenticationToken(
                                    "test@example.com", null,
                                    List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                            );
                            SecurityContextHolder.getContext().setAuthentication(auth);
                        }
                        chain.doFilter(req, res);
                    }
                }, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}