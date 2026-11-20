package com.renderia.renderia_backend.security.ratelimit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

/**
 * IP del cliente para el límite de intentos.
 *
 * Detrás del proxy de Render la conexión llega desde el proxy, y la IP real es la
 * última entrada de X-Forwarded-For (la que agrega el proxy). Las primeras entradas
 * las puede escribir cualquiera, así que no se usan.
 */
@Component
public class ClientIpResolver {

    private final boolean behindProxy;

    public ClientIpResolver(@Value("${app.behind-proxy:false}") boolean behindProxy) {
        this.behindProxy = behindProxy;
    }

    public String resolve(HttpServletRequest request) {
        if (behindProxy) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                String[] parts = forwarded.split(",");
                String last = parts[parts.length - 1].strip();
                if (!last.isEmpty()) {
                    return last;
                }
            }
        }
        return request.getRemoteAddr();
    }
}
