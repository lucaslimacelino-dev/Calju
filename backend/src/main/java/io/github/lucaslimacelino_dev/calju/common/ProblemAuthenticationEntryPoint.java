package io.github.lucaslimacelino_dev.calju.common;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Resposta 401 no formato ProblemDetail. O cabeçalho WWW-Authenticate (padrão de tokens
 * Bearer) continua vindo do Spring Security; o corpo nunca repete o token nem o motivo técnico.
 */
@Component
public class ProblemAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String BODY = "{\"type\":\"about:blank\",\"title\":\"Unauthorized\",\"status\":401,"
            + "\"detail\":\"Autenticação necessária ou token inválido.\"}";

    private final BearerTokenAuthenticationEntryPoint bearer = new BearerTokenAuthenticationEntryPoint();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        bearer.commence(request, response, exception);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(BODY);
    }
}
