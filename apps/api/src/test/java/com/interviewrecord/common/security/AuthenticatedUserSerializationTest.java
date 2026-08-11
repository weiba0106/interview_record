package com.interviewrecord.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.interviewrecord.preference.domain.Theme;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

class AuthenticatedUserSerializationTest {

    @Test
    void serializesTheSecurityContextStoredByJdbcSessions() throws Exception {
        AuthenticatedUser user = new AuthenticatedUser(42L, "user@example.com", "小林", true,
                "Asia/Shanghai", Theme.GRAPHITE_CORAL);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, user.authorities()));

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(context);
        }
        SecurityContext restored;
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = (SecurityContext) input.readObject();
        }

        assertThat(restored.getAuthentication().getPrincipal()).isEqualTo(user);
    }
}
