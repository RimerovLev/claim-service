package com.claims.mvp.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

/**
 * Represents the authenticated caller after JwtAuthFilter has validated the token.
 *
 * Spring Security knows nothing about JWT — it operates through the Authentication interface.
 * Our job: fill this interface with data extracted from the token and store it in
 * SecurityContextHolder. After that, Spring knows who is making the request everywhere
 * in the application — controllers, services, method-level security annotations.
 */
public class JwtAuthentication implements Authentication {

    private final String email;  // the principal — "who is logged in"
    private final String role;   // e.g. ROLE_USER or ROLE_ADMIN
    private boolean authenticated = true;

    public JwtAuthentication(String email, String role) {
        this.email = email;
        // Spring Security expects roles to be prefixed with "ROLE_".
        // hasRole("USER") internally compares against "ROLE_USER" — so we normalise here
        // rather than forcing every caller to remember the prefix.
        this.role = role.startsWith("ROLE_") ? role : "ROLE_" + role;
    }

    /**
     * The list of permissions this user has.
     * We store one role per user, so this is always a single-element list.
     * Spring Security uses this for checks like hasRole("ADMIN") or @PreAuthorize.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
    }

    /**
     * The password or secret — not needed here because the token was already
     * verified in JwtService.isValid(). Returning null prevents it from
     * accidentally appearing in logs or serialised responses.
     */
    @Override public Object getCredentials() { return null; }

    /**
     * Extra request metadata (IP address, session id, etc.). Not used — null.
     */
    @Override public Object getDetails() { return null; }

    /**
     * "Who is logged in" — we use email as the principal identifier.
     * Retrieve it in a controller via:
     *   (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal()
     */
    @Override public Object getPrincipal() { return email; }

    @Override public boolean isAuthenticated() { return authenticated; }
    @Override public void setAuthenticated(boolean isAuthenticated) { this.authenticated = isAuthenticated; }
    @Override public String getName() { return email; }
}