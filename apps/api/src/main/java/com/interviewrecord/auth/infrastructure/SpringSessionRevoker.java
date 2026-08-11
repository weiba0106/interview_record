package com.interviewrecord.auth.infrastructure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnExpression("'${spring.datasource.url:}' != ''")
public class SpringSessionRevoker {
    private final FindByIndexNameSessionRepository<? extends Session> sessions;

    public SpringSessionRevoker(FindByIndexNameSessionRepository<? extends Session> sessions) {
        this.sessions = sessions;
    }

    public void revokeAllForPrincipal(String principalName) {
        sessions.findByPrincipalName(principalName).keySet().forEach(sessions::deleteById);
    }
}
