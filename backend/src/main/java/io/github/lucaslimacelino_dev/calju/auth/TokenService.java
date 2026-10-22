package io.github.lucaslimacelino_dev.calju.auth;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.lucaslimacelino_dev.calju.auth.dto.TokenResponse;
import io.github.lucaslimacelino_dev.calju.common.AuthFailedException;
import io.github.lucaslimacelino_dev.calju.config.JwtProperties;

/**
 * Emite o token de acesso (JWT HS256, 15 min) e o refresh token (opaco, 256 bits, 30 dias).
 * Cada renovação troca o refresh token; reusar um já trocado indica roubo e encerra a sessão.
 */
@Service
public class TokenService {

    /** Claim com o id da sessão (família de refresh tokens): permite saber qual aparelho é "este". */
    public static final String SESSION_CLAIM = "sid";
    static final Duration ACCESS_VALIDITY = Duration.ofMinutes(15);
    static final Duration REFRESH_VALIDITY = Duration.ofDays(30);
    private static final String INVALID = "Sessão inválida ou expirada.";

    private final JwtEncoder encoder;
    private final JwtProperties properties;
    private final RefreshTokenRepository refreshTokens;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public TokenService(JwtEncoder encoder, JwtProperties properties, RefreshTokenRepository refreshTokens,
                        Clock clock) {
        this.encoder = encoder;
        this.properties = properties;
        this.refreshTokens = refreshTokens;
        this.clock = clock;
    }

    /** Login: abre uma sessão nova (família nova). */
    @Transactional
    public TokenResponse openSession(UUID userId, String deviceLabel) {
        return issue(userId, UUID.randomUUID(), deviceLabel).response();
    }

    /** Troca o refresh token por um par novo. Token já usado: encerra a sessão toda e recusa. */
    @Transactional(noRollbackFor = AuthFailedException.class)
    public TokenResponse refresh(String refreshToken) {
        Instant now = clock.instant();
        RefreshTokenEntity current = refreshTokens.findByTokenHash(Hashes.sha256(refreshToken))
                .orElseThrow(() -> new AuthFailedException(INVALID));
        if (current.isRevoked()) {
            refreshTokens.revokeFamily(current.getFamilyId(), now);
            throw new AuthFailedException(INVALID);
        }
        if (current.isExpired(now)) {
            throw new AuthFailedException(INVALID);
        }
        Issued next = issue(current.getUserId(), current.getFamilyId(), current.getDeviceLabel());
        current.replaceWith(next.refreshId(), now);
        return next.response();
    }

    /** Sair: encerra a sessão deste token. Token desconhecido não dá erro (sair é idempotente). */
    @Transactional
    public void logout(String refreshToken) {
        refreshTokens.findByTokenHash(Hashes.sha256(refreshToken))
                .ifPresent(token -> refreshTokens.revokeFamily(token.getFamilyId(), clock.instant()));
    }

    /** Senha redefinida: todas as sessões do usuário são encerradas. */
    @Transactional
    public void revokeAll(UUID userId) {
        refreshTokens.revokeAllOfUser(userId, clock.instant());
    }

    /** Senha trocada no app: as outras sessões são encerradas; a atual continua (se for conhecida). */
    @Transactional
    public void revokeAllExcept(UUID userId, UUID keepSessionId) {
        if (keepSessionId == null) {
            refreshTokens.revokeAllOfUser(userId, clock.instant());
        } else {
            refreshTokens.revokeAllOfUserExceptFamily(userId, keepSessionId, clock.instant());
        }
    }

    /** Sessões abertas (uma por aparelho), da usada mais recentemente para a mais antiga. */
    @Transactional(readOnly = true)
    public List<RefreshTokenEntity> activeSessions(UUID userId) {
        return refreshTokens.findActive(userId, clock.instant());
    }

    /** Encerra uma sessão do próprio usuário; falso se ela não existe ou é de outra pessoa. */
    @Transactional
    public boolean revokeSession(UUID userId, UUID sessionId) {
        return refreshTokens.revokeFamilyOfUser(userId, sessionId, clock.instant()) > 0;
    }

    private record Issued(UUID refreshId, TokenResponse response) {
    }

    private Issued issue(UUID userId, UUID familyId, String deviceLabel) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.jwtIssuer())
                .subject(userId.toString())
                .claim(SESSION_CLAIM, familyId.toString())
                .issuedAt(now)
                .expiresAt(now.plus(ACCESS_VALIDITY))
                .build();
        String accessToken = encoder.encode(
                JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        RefreshTokenEntity stored = new RefreshTokenEntity(userId, familyId, Hashes.sha256(refreshToken),
                deviceLabel, now, now.plus(REFRESH_VALIDITY));
        refreshTokens.save(stored);
        return new Issued(stored.getId(), new TokenResponse(accessToken, "Bearer", ACCESS_VALIDITY.toSeconds(),
                refreshToken, REFRESH_VALIDITY.toSeconds()));
    }
}
