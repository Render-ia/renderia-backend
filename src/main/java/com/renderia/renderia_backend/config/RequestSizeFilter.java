package com.renderia.renderia_backend.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Caps the size of every request body before anything reads it, so a giant
 * paste (or a script sending gigabytes) is refused with 413 instead of filling
 * the server's memory. The largest legitimate body is a plan upload (10 MB);
 * the per-field limits of each endpoint still apply after this.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestSizeFilter extends OncePerRequestFilter {

    static final long MAX_BODY_BYTES = 12L * 1024 * 1024;
    static final String MESSAGE = "{\"message\":\"La petición es demasiado grande.\"}";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getContentLengthLong() > MAX_BODY_BYTES) {
            tooLarge(response);
            return;
        }
        // Bodies without Content-Length (chunked) are counted while they are read.
        chain.doFilter(request.getContentLengthLong() < 0 ? new Limited(request) : request, response);
    }

    static void tooLarge(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(MESSAGE);
    }

    /** Request whose body stream fails once more than MAX_BODY_BYTES are read. */
    static final class Limited extends HttpServletRequestWrapper {
        Limited(HttpServletRequest request) {
            super(request);
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            ServletInputStream in = super.getInputStream();
            return new ServletInputStream() {
                private long read;

                private void count(long n) throws IOException {
                    if (n > 0 && (read += n) > MAX_BODY_BYTES) {
                        throw new BodyTooLargeException();
                    }
                }

                @Override
                public int read() throws IOException {
                    int b = in.read();
                    if (b >= 0) count(1);
                    return b;
                }

                @Override
                public int read(byte[] buffer, int off, int len) throws IOException {
                    int n = in.read(buffer, off, len);
                    count(n);
                    return n;
                }

                @Override
                public boolean isFinished() {
                    return in.isFinished();
                }

                @Override
                public boolean isReady() {
                    return in.isReady();
                }

                @Override
                public void setReadListener(ReadListener listener) {
                    in.setReadListener(listener);
                }
            };
        }
    }

    /** Thrown when a chunked body passes the limit; the exception handler answers 413. */
    public static final class BodyTooLargeException extends IOException {
        BodyTooLargeException() {
            super("Request body larger than " + MAX_BODY_BYTES + " bytes");
        }
    }
}
