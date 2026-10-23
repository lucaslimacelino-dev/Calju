package io.github.lucaslimacelino_dev.calju.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.github.lucaslimacelino_dev.calju.auth.dto.AcceptedResponse;
import io.github.lucaslimacelino_dev.calju.auth.dto.DeletionConfirmRequest;
import io.github.lucaslimacelino_dev.calju.auth.dto.GoogleLoginRequest;
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
    private final AuthRateLimits limits;

    public AuthController(AuthService auth, TokenService tokens, AuthRateLimits limits) {
        this.auth = auth;
        this.tokens = tokens;
        this.limits = limits;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AcceptedResponse register(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
        limits.emailRequest("register", http.getRemoteAddr(), request.email());
        auth.register(request.email());
        return new AcceptedResponse(CODE_SENT);
    }

    @PostMapping("/verify")
    public TokenResponse verify(@Valid @RequestBody VerifyRequest request, HttpServletRequest http) {
        limits.codeAttempt(http.getRemoteAddr(), request.email());
        return auth.verify(request);
    }

    @PostMapping("/password-reset")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AcceptedResponse requestPasswordReset(@Valid @RequestBody RegisterRequest request,
                                                 HttpServletRequest http) {
        limits.emailRequest("reset", http.getRemoteAddr(), request.email());
        auth.requestPasswordReset(request.email());
        return new AcceptedResponse(CODE_SENT);
    }

    /** Mesmo corpo da confirmação do cadastro: e-mail, código e a senha nova. */
    @PostMapping("/password-reset/confirm")
    public TokenResponse resetPassword(@Valid @RequestBody VerifyRequest request, HttpServletRequest http) {
        limits.codeAttempt(http.getRemoteAddr(), request.email());
        return auth.resetPassword(request);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        limits.login(http.getRemoteAddr(), request.email());
        return auth.login(request);
    }

    @PostMapping("/google")
    public TokenResponse google(@Valid @RequestBody GoogleLoginRequest request, HttpServletRequest http) {
        limits.google(http.getRemoteAddr());
        return auth.loginWithGoogle(request.idToken(), request.deviceLabel());
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request, HttpServletRequest http) {
        limits.refresh(http.getRemoteAddr());
        return tokens.refresh(request.refreshToken());
    }

    /** Página web de exclusão (excluir-conta.html): pede o código por e-mail. */
    @PostMapping("/account-deletion")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AcceptedResponse requestAccountDeletion(@Valid @RequestBody RegisterRequest request,
                                                   HttpServletRequest http) {
        limits.emailRequest("delete", http.getRemoteAddr(), request.email());
        auth.requestAccountDeletion(request.email());
        return new AcceptedResponse(CODE_SENT);
    }

    @PostMapping("/account-deletion/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmAccountDeletion(@Valid @RequestBody DeletionConfirmRequest request, HttpServletRequest http) {
        limits.codeAttempt(http.getRemoteAddr(), request.email());
        auth.confirmAccountDeletion(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshRequest request) {
        tokens.logout(request.refreshToken());
    }
}
