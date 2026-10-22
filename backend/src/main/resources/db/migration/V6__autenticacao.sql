-- Senha (opcional: contas só com Google não têm) e confirmação do e-mail.
ALTER TABLE users ADD COLUMN password_hash     varchar(200);
ALTER TABLE users ADD COLUMN email_verified_at timestamptz;

-- Refresh tokens opacos, guardados só como hash SHA-256. Cada login abre uma família (sessão);
-- cada uso troca o token (rotação). Reusar um token já trocado revoga a família inteira.
CREATE TABLE refresh_tokens (
    id             uuid PRIMARY KEY,
    user_id        uuid         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    family_id      uuid         NOT NULL,
    token_hash     varchar(64)  NOT NULL UNIQUE,
    device_label   varchar(100),
    created_at     timestamptz  NOT NULL,
    expires_at     timestamptz  NOT NULL,
    revoked_at     timestamptz,
    replaced_by_id uuid
);

CREATE INDEX ix_refresh_tokens_user ON refresh_tokens (user_id);
CREATE INDEX ix_refresh_tokens_family ON refresh_tokens (family_id);

-- Códigos de 6 dígitos enviados por e-mail (confirmar cadastro, redefinir senha).
-- Guardados como hash; valem 30 minutos e aceitam poucas tentativas.
CREATE TABLE email_codes (
    id         uuid PRIMARY KEY,
    user_id    uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    purpose    varchar(10) NOT NULL CHECK (purpose IN ('VERIFY', 'RESET')),
    code_hash  varchar(64) NOT NULL,
    created_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    used_at    timestamptz,
    attempts   integer     NOT NULL DEFAULT 0
);

CREATE INDEX ix_email_codes_user_purpose ON email_codes (user_id, purpose);
