package com.renderia.renderia_backend.security.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTests {

    @Test
    void behindTheProxyUsesTheLastIpAddedByTheProxy() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "6.6.6.6, 200.1.2.3");
        assertThat(new ClientIpResolver(true).resolve(request)).isEqualTo("200.1.2.3");
    }

    @Test
    void withoutProxyIgnoresTheHeaderBecauseAnyoneCanWriteIt() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("190.5.5.5");
        request.addHeader("X-Forwarded-For", "6.6.6.6");
        assertThat(new ClientIpResolver(false).resolve(request)).isEqualTo("190.5.5.5");
    }
}
