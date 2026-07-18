package com.example.fruitmallmanagementsystem.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTests {
    @Test
    void ignoresForwardedHeaderUnlessProxyTrustIsEnabled() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.8");
        request.addHeader("X-Forwarded-For", "203.0.113.9");
        assertThat(new ClientIpResolver(false).resolve(request)).isEqualTo("10.0.0.8");
    }

    @Test
    void acceptsOnlyIpShapedForwardedValueFromTrustedProxy() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.8");
        request.addHeader("X-Forwarded-For", "203.0.113.9, 10.0.0.7");
        assertThat(new ClientIpResolver(true).resolve(request)).isEqualTo("203.0.113.9");
    }
}
