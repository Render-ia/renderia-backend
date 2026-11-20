package com.renderia.renderia_backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.ServletRequest;

class RequestSizeFilterTests {

    private final RequestSizeFilter filter = new RequestSizeFilter();

    @Test
    void refusesABodyAnnouncedAsTooLargeWithoutReadingIt() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/projects") {
            @Override
            public long getContentLengthLong() {
                return RequestSizeFilter.MAX_BODY_BYTES + 1;
            }
        };
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(response.getContentAsString()).contains("demasiado grande");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void letsNormalRequestsThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/projects");
        request.setContent("{\"name\":\"Casa\"}".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void stopsAChunkedBodyOnceItPassesTheLimit() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/projects") {
            @Override
            public long getContentLengthLong() {
                return -1;
            }
        };
        request.setContent(new byte[(int) RequestSizeFilter.MAX_BODY_BYTES + 10]);
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        ServletRequest wrapped = chain.getRequest();
        InputStream in = wrapped.getInputStream();
        byte[] buffer = new byte[1024 * 1024];
        assertThatThrownBy(() -> {
            while (in.read(buffer) > 0) {
                // read everything
            }
        }).isInstanceOf(RequestSizeFilter.BodyTooLargeException.class);
    }
}
