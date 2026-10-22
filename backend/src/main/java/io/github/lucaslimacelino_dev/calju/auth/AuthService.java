package io.github.lucaslimacelino_dev.calju.auth;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.lucaslimacelino_dev.calju.auth.dto.LoginRequest;
import io.github.lucaslimacelino_dev.calju.auth.dto.TokenResponse;
import io.github.lucaslimacelino_dev.calju.auth.dto.VerifyRequest;
import io.github.lucaslimacelino_dev.calju.common.AuthFailedException;
import io.github.lucaslimacelino_dev.calju.common.BadRequestException;
import io.github.lucaslimacelino_dev.calju.user.Emails;
import io.github.lucaslimacelino_dev.calju.user.UserEntity;
import io.github.lucaslimacelino_dev.calju.user.UserRepository;

/**
 * Cadastro, confirmação e login por e-mail e senha. As respostas não revelam se um e-mail tem
 * conta: o cadastro responde sempre igual e o login erra sempre com a mesma mensagem.
 */
@Service
public class AuthService {

    static final String INVALID_CODE = "Código inválido ou expirado.";
    static final String INVALID_LOGIN = "E-mail ou senha incorretos.";

    private final UserRepository users;
    private final EmailCodeService codes;
    private final CodeMailer mailer;
    private final PasswordEncoder passwords;
    private final TokenService tokens;
    private final Clock clock;
    /** Hash de uma senha aleatória: conferido quando o e-mail não existe, para o tempo não denunciar. */
    private final String dummyHash;

    public AuthService(UserRepository users, EmailCodeService codes, CodeMailer mailer, PasswordEncoder passwords,
                       TokenService tokens, Clock clock) {
        this.users = users;
        this.codes = codes;
        this.mailer = mailer;
        this.passwords = passwords;
        this.tokens = tokens;
        this.clock = clock;
        this.dummyHash = passwords.encode(UUID.randomUUID().toString());
    }

    /** E-mail novo ou não confirmado: manda um código. Já confirmado: avisa o dono. */
    @Transactional
    public void register(String rawEmail) {
        String email = Emails.normalize(rawEmail);
        Optional<UserEntity> existing = users.findByEmail(email);
        if (existing.isPresent() && existing.get().isEmailVerified()) {
            mailer.sendAlreadyRegistered(email);
            return;
        }
        UserEntity user = existing.orElseGet(() -> users.save(new UserEntity(UUID.randomUUID(), email, clock.instant())));
        mailer.sendVerificationCode(email, codes.issue(user.getId(), CodePurpose.VERIFY));
    }

    /**
     * Código certo: define a senha, confirma o e-mail e abre a sessão. Código errado conta uma
     * tentativa, que precisa ficar gravada mesmo com o erro (por isso o noRollbackFor).
     */
    @Transactional(noRollbackFor = BadRequestException.class)
    public TokenResponse verify(VerifyRequest request) {
        UserEntity user = users.findByEmail(Emails.normalize(request.email()))
                .filter(found -> !found.isEmailVerified())
                .orElseThrow(() -> new BadRequestException(INVALID_CODE));
        if (!codes.consume(user.getId(), CodePurpose.VERIFY, request.code())) {
            throw new BadRequestException(INVALID_CODE);
        }
        user.confirmWithPassword(passwords.encode(request.password()), clock.instant());
        return tokens.openSession(user.getId(), request.deviceLabel());
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        Optional<UserEntity> user = users.findByEmail(Emails.normalize(request.email()))
                .filter(UserEntity::isEmailVerified)
                .filter(found -> found.getPasswordHash() != null);
        String hash = user.map(UserEntity::getPasswordHash).orElse(dummyHash);
        boolean matches = passwords.matches(request.password(), hash);
        if (user.isEmpty() || !matches) {
            throw new AuthFailedException(INVALID_LOGIN);
        }
        UserEntity found = user.get();
        if (passwords.upgradeEncoding(found.getPasswordHash())) {
            found.changePasswordHash(passwords.encode(request.password()), clock.instant());
        }
        return tokens.openSession(found.getId(), request.deviceLabel());
    }
}
