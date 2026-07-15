package com.prodwatch.api.security;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.prodwatch.api.entity.AppRole;

public class CurrentUser implements UserDetails {

    private final UUID userId;
    private final UUID ecosystemId;
    private final String email;
    private final AppRole role;
    private final boolean active;

    public CurrentUser(UUID userId, UUID ecosystemId, String email, AppRole role, boolean active) {
        this.userId = userId;
        this.ecosystemId = ecosystemId;
        this.email = email;
        this.role = role;
        this.active = active;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getEcosystemId() {
        return ecosystemId;
    }

    public AppRole getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
