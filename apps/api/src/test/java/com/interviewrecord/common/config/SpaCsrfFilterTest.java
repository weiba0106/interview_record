package com.interviewrecord.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

class SpaCsrfFilterTest {
    @Test
    void acceptsTheRawXsrfCookieHeaderPairUsedByAxios() throws Exception {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieName("XSRF-TOKEN");
        repository.setHeaderName("X-XSRF-TOKEN");
        MockHttpServletRequest bootstrapRequest = new MockHttpServletRequest("GET", "/api/v1/auth/csrf");
        MockHttpServletResponse bootstrapResponse = new MockHttpServletResponse();
        CsrfToken token = repository.generateToken(bootstrapRequest);
        repository.saveToken(token, bootstrapRequest, bootstrapResponse);

        MockHttpServletRequest mutation = new MockHttpServletRequest("POST", "/api/v1/auth/register");
        mutation.setCookies(bootstrapResponse.getCookie("XSRF-TOKEN"));
        mutation.addHeader("X-XSRF-TOKEN", token.getToken());
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainReached = new AtomicBoolean();
        CsrfFilter filter = new CsrfFilter(repository);
        filter.setRequestHandler(new CsrfTokenRequestAttributeHandler());

        filter.doFilter(mutation, response, (request, servletResponse) -> chainReached.set(true));

        assertThat(chainReached).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
