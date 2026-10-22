package io.github.lucaslimacelino_dev.calju.user;

import java.util.UUID;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import io.github.lucaslimacelino_dev.calju.auth.TokenService;
import io.github.lucaslimacelino_dev.calju.common.AccountNotFoundException;

/** Preenche parâmetros {@link CurrentUser}: id do token, conferido no banco (conta excluída → 401). */
@Component
public class CurrentUserResolver implements HandlerMethodArgumentResolver {

    private final UserRepository users;

    public CurrentUserResolver(UserRepository users) {
        this.users = users;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return CurrentUser.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken token)) {
            throw new AccountNotFoundException();
        }
        UUID id;
        try {
            id = UUID.fromString(token.getToken().getSubject());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new AccountNotFoundException();
        }
        if (!users.existsById(id)) {
            throw new AccountNotFoundException();
        }
        return new CurrentUser(id, sessionOf(token));
    }

    private static UUID sessionOf(JwtAuthenticationToken token) {
        String sid = token.getToken().getClaimAsString(TokenService.SESSION_CLAIM);
        try {
            return sid == null ? null : UUID.fromString(sid);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
