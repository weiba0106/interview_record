package com.interviewrecord.common.security;

import com.interviewrecord.preference.domain.Theme;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public record AuthenticatedUser(
        Long id,
        String email,
        String displayName,
        boolean emailVerified,
        String timeZone,
        Theme theme) {
    public Collection<? extends GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }
}
