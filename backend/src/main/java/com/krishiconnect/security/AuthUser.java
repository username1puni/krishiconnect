package com.krishiconnect.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.krishiconnect.entity.Role;
import com.krishiconnect.entity.User;

import lombok.Getter;

/** The logged in user as Spring Security sees it. Keeps the id so services don't reload the user. */
@Getter
public class AuthUser implements UserDetails {

    private final Long id;
    private final String phone;
    private final String fullName;
    private final String passwordHash;
    private final Role role;
    private final boolean enabled;

    public AuthUser(User user) {
        this.id = user.getId();
        this.phone = user.getPhone();
        this.fullName = user.getFullName();
        this.passwordHash = user.getPasswordHash();
        this.role = user.getRole();
        this.enabled = user.isEnabled();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    /** Users log in with phone or email, so the stable username is the id. */
    @Override
    public String getUsername() {
        return String.valueOf(id);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
