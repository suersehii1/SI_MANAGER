package com.sipm.security;

import com.sipm.user.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class AppUserPrincipal implements UserDetails {
    private final Long id;
    private final String username;
    private final String name;
    private final String passwordHash;
    private final boolean enabled;
    private final List<String> roles;

    public AppUserPrincipal(Long id, String username, String name, String passwordHash, boolean enabled,
                            List<String> roles) {
        this.id = id;
        this.username = username;
        this.name = name;
        this.passwordHash = passwordHash;
        this.enabled = enabled;
        this.roles = List.copyOf(roles);
    }

    public static AppUserPrincipal from(User user) {
        return new AppUserPrincipal(user.getId(), user.getUsername(), user.getName(), user.getPasswordHash(),
                user.isEnabled(), user.getRoleNames());
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public List<String> getRoles() { return roles; }

    public boolean isAdmin() {
        return roles.contains("ROLE_ADMIN");
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream().map(SimpleGrantedAuthority::new).toList();
    }

    @Override
    public String getPassword() { return passwordHash; }

    @Override
    public String getUsername() { return username; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return enabled; }
}
