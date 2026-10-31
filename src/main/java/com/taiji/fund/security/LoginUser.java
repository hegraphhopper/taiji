package com.taiji.fund.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class LoginUser implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String username;
    private final String password;
    private final String realName;
    private final Long groupId;
    private final String groupName;
    private final List<String> roles;
    private final boolean enabled;

    public LoginUser(Long id, String username, String password, String realName, Long groupId,
                     String groupName, List<String> roles, boolean enabled) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.realName = realName;
        this.groupId = groupId;
        this.groupName = groupName;
        this.roles = roles == null ? Collections.emptyList() : roles;
        this.enabled = enabled;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
