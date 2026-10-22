package io.github.lucaslimacelino_dev.calju.account;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.lucaslimacelino_dev.calju.account.dto.AccountResponse;
import io.github.lucaslimacelino_dev.calju.account.dto.DeleteAccountRequest;
import io.github.lucaslimacelino_dev.calju.account.dto.SessionResponse;
import io.github.lucaslimacelino_dev.calju.auth.GoogleIdTokenDecoder;
import io.github.lucaslimacelino_dev.calju.auth.TokenService;
import io.github.lucaslimacelino_dev.calju.common.AccountNotFoundException;
import io.github.lucaslimacelino_dev.calju.common.BadRequestException;
import io.github.lucaslimacelino_dev.calju.common.NotFoundException;
import io.github.lucaslimacelino_dev.calju.common.RateLimiter;
import io.github.lucaslimacelino_dev.calju.user.CurrentUser;
import io.github.lucaslimacelino_dev.calju.user.UserEntity;
import io.github.lucaslimacelino_dev.calju.user.UserRepository;

/** Conta do usuário logado: dados, aparelhos conectados, troca de senha e exclusão. */
@Service
public class AccountService {

    static final String WRONG_PASSWORD = "Senha atual incorreta.";
    static final String WRONG_GOOGLE = "A confirmação do Google não é desta conta.";

    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final TokenService tokens;
    private final RateLimiter limiter;
    private final GoogleIdTokenDecoder google;
    private final Clock clock;

    public AccountService(UserRepository users, PasswordEncoder passwords, TokenService tokens, RateLimiter limiter,
                          GoogleIdTokenDecoder google, Clock clock) {
        this.users = users;
        this.passwords = passwords;
        this.tokens = tokens;
        this.limiter = limiter;
        this.google = google;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AccountResponse account(CurrentUser current) {
        UserEntity user = load(current);
        return new AccountResponse(user.getId(), user.getEmail(), user.getPasswordHash() != null);
    }

    public List<SessionResponse> sessions(CurrentUser current) {
        return tokens.activeSessions(current.id()).stream()
                .map(token -> new SessionResponse(token.getFamilyId(), token.getDeviceLabel(), token.getCreatedAt(),
                        token.getExpiresAt(), Objects.equals(token.getFamilyId(), current.sessionId())))
                .toList();
    }

    public void endSession(CurrentUser current, UUID sessionId) {
        if (!tokens.revokeSession(current.id(), sessionId)) {
            throw new NotFoundException("Sessão não encontrada.");
        }
    }

    /** Senha atual conferida; a nova passa a valer e os outros aparelhos saem da conta. */
    @Transactional(noRollbackFor = BadRequestException.class)
    public void changePassword(CurrentUser current, String currentPassword, String newPassword) {
        UserEntity user = load(current);
        requirePassword(user, currentPassword);
        user.changePasswordHash(passwords.encode(newPassword), clock.instant());
        tokens.revokeAllExcept(user.getId(), current.sessionId());
    }

    /** Apaga a conta e, em cascata, todos os dados dela (alimentos, refeições, diário, metas, sessões). */
    @Transactional
    public void delete(CurrentUser current, DeleteAccountRequest request) {
        UserEntity user = load(current);
        if (request.password() != null && !request.password().isBlank()) {
            requirePassword(user, request.password());
        } else {
            requireSameGoogleAccount(user, request.googleIdToken());
        }
        users.delete(user);
    }

    private void requireSameGoogleAccount(UserEntity user, String idToken) {
        limiter.check("account-password:user:" + user.getId(), 10, Duration.ofMinutes(15));
        String subject;
        try {
            subject = google.isEnabled() ? google.decode(idToken).getSubject() : null;
        } catch (JwtException e) {
            subject = null;
        }
        if (subject == null || !subject.equals(user.getGoogleSubject())) {
            throw new BadRequestException(WRONG_GOOGLE);
        }
    }

    private void requirePassword(UserEntity user, String password) {
        limiter.check("account-password:user:" + user.getId(), 10, Duration.ofMinutes(15));
        if (user.getPasswordHash() == null || !passwords.matches(password, user.getPasswordHash())) {
            throw new BadRequestException(WRONG_PASSWORD);
        }
    }

    private UserEntity load(CurrentUser current) {
        return users.findById(current.id()).orElseThrow(AccountNotFoundException::new);
    }
}
