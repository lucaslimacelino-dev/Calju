package io.github.lucaslimacelino_dev.calju.auth;

import java.time.Clock;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import io.github.lucaslimacelino_dev.calju.auth.dto.DeletionConfirmRequest;
import io.github.lucaslimacelino_dev.calju.auth.dto.LoginRequest;
import io.github.lucaslimacelino_dev.calju.auth.dto.TokenResponse;
import io.github.lucaslimacelino_dev.calju.auth.dto.VerifyRequest;
import io.github.lucaslimacelino_dev.calju.common.AuthFailedException;
import io.github.lucaslimacelino_dev.calju.common.BadRequestException;
import io.github.lucaslimacelino_dev.calju.common.ConflictException;
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
    static final String INVALID_GOOGLE = "Não foi possível entrar com o Google.";
    static final String GOOGLE_LINK_CONFLICT =
            "Já existe uma conta com este e-mail. Entre com a senha para continuar.";

    private final UserRepository users;
    private final EmailCodeService codes;
    private final CodeMailer mailer;
    private final PasswordEncoder passwords;
    private final TokenService tokens;
    private final GoogleIdTokenDecoder google;
    private final Clock clock;
    /** Hash de uma senha aleatória: conferido quando o e-mail não existe, para o tempo não denunciar. */
    private final String dummyHash;

    public AuthService(UserRepository users, EmailCodeService codes, CodeMailer mailer, PasswordEncoder passwords,
                       TokenService tokens, GoogleIdTokenDecoder google, Clock clock) {
        this.users = users;
        this.codes = codes;
        this.mailer = mailer;
        this.passwords = passwords;
        this.tokens = tokens;
        this.google = google;
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

    /** Só conta confirmada recebe código; para qualquer outro e-mail nada acontece (resposta igual). */
    @Transactional
    public void requestPasswordReset(String rawEmail) {
        users.findByEmail(Emails.normalize(rawEmail))
                .filter(UserEntity::isEmailVerified)
                .ifPresent(user -> mailer.sendResetCode(user.getEmail(), codes.issue(user.getId(), CodePurpose.RESET)));
    }

    /** Código certo: senha nova, todas as sessões antigas encerradas e uma sessão nova aberta. */
    @Transactional(noRollbackFor = BadRequestException.class)
    public TokenResponse resetPassword(VerifyRequest request) {
        UserEntity user = users.findByEmail(Emails.normalize(request.email()))
                .filter(UserEntity::isEmailVerified)
                .orElseThrow(() -> new BadRequestException(INVALID_CODE));
        if (!codes.consume(user.getId(), CodePurpose.RESET, request.code())) {
            throw new BadRequestException(INVALID_CODE);
        }
        user.changePasswordHash(passwords.encode(request.password()), clock.instant());
        tokens.revokeAll(user.getId());
        return tokens.openSession(user.getId(), request.deviceLabel());
    }

    /**
     * Exclusão pela página web: só conta confirmada recebe código; para qualquer outro e-mail
     * nada acontece (a resposta é a mesma, para não revelar quem tem conta).
     */
    @Transactional
    public void requestAccountDeletion(String rawEmail) {
        users.findByEmail(Emails.normalize(rawEmail))
                .filter(UserEntity::isEmailVerified)
                .ifPresent(user -> mailer.sendDeletionCode(user.getEmail(),
                        codes.issue(user.getId(), CodePurpose.DELETE)));
    }

    /** Código certo: a conta e, em cascata, todos os dados dela são apagados. */
    @Transactional(noRollbackFor = BadRequestException.class)
    public void confirmAccountDeletion(DeletionConfirmRequest request) {
        UserEntity user = users.findByEmail(Emails.normalize(request.email()))
                .filter(UserEntity::isEmailVerified)
                .orElseThrow(() -> new BadRequestException(INVALID_CODE));
        if (!codes.consume(user.getId(), CodePurpose.DELETE, request.code())) {
            throw new BadRequestException(INVALID_CODE);
        }
        users.lockForWrite(user.getId());
        users.delete(user);
    }

    /**
     * Entrar com o Google. A conta é achada pelo "sub" do Google. Sem vínculo ainda: se já existe
     * conta com o mesmo e-mail, só vincula quando o Google é a autoridade sobre o endereço (Gmail
     * ou Google Workspace); em outros casos pede a senha, para ninguém tomar a conta alheia.
     */
    @Transactional
    public TokenResponse loginWithGoogle(String idToken, String deviceLabel) {
        if (!google.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Login com Google indisponível.");
        }
        Jwt token;
        try {
            token = google.decode(idToken);
        } catch (JwtException e) {
            throw new AuthFailedException(INVALID_GOOGLE);
        }
        String subject = token.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new AuthFailedException(INVALID_GOOGLE);
        }
        Optional<UserEntity> linked = users.findByGoogleSubject(subject);
        if (linked.isPresent()) {
            return tokens.openSession(linked.get().getId(), deviceLabel);
        }
        String email = token.getClaimAsString("email");
        if (email == null || !Boolean.TRUE.equals(token.getClaimAsBoolean("email_verified"))) {
            throw new AuthFailedException(INVALID_GOOGLE);
        }
        email = Emails.normalize(email);
        Optional<UserEntity> sameEmail = users.findByEmail(email);
        UserEntity user;
        if (sameEmail.isPresent()) {
            if (!googleOwnsAddress(email, token.getClaimAsString("hd"))) {
                throw new ConflictException(GOOGLE_LINK_CONFLICT);
            }
            user = sameEmail.get();
        } else {
            user = users.save(new UserEntity(UUID.randomUUID(), email, clock.instant()));
        }
        user.linkGoogle(subject, clock.instant());
        return tokens.openSession(user.getId(), deviceLabel);
    }

    /** Segundo o Google, ele só é autoridade sobre o e-mail em contas Gmail e Google Workspace (hd). */
    private static boolean googleOwnsAddress(String email, String hostedDomain) {
        return email.toLowerCase(Locale.ROOT).endsWith("@gmail.com")
                || (hostedDomain != null && !hostedDomain.isBlank());
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
