package io.github.lucaslimacelino_dev.calju.auth;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.github.lucaslimacelino_dev.calju.auth.dto.AcceptedResponse;
import io.github.lucaslimacelino_dev.calju.auth.dto.LoginRequest;
import io.github.lucaslimacelino_dev.calju.auth.dto.RefreshRequest;
import io.github.lucaslimacelino_dev.calju.auth.dto.RegisterRequest;
import io.github.lucaslimacelino_dev.calju.auth.dto.TokenResponse;
import io.github.lucaslimacelino_dev.calju.auth.dto.VerifyRequest;

/** Rotas públicas de conta. São as únicas, além do health, que não exigem token. */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String CODE_SENT =
            "Se o e-mail puder ser usado, enviamos um código para ele. Confira a caixa de entrada.";

    private final AuthService auth;
    private final TokenService tokens;

    public AuthController(AuthService auth, TokenService tokens) {
        this.auth = auth;
        this.tokens = tokens;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AcceptedResponse register(@Valid @RequestBody RegisterRequest request) {
        auth.register(request.email());
        return new AcceptedResponse(CODE_SENT);
    }

    @PostMapping("/verify")
    public TokenResponse verify(@Valid @RequestBody VerifyRequest request) {
        return auth.verify(request);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return auth.login(request);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return tokens.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshRequest request) {
        tokens.logout(request.refreshToken());
    }
}
