-- Usuários: só o essencial. Senha e vínculo com Google entram na Fase 6 (login).
CREATE TABLE users (
    id         uuid PRIMARY KEY,
    email      varchar(254) NOT NULL,
    created_at timestamptz  NOT NULL,
    updated_at timestamptz  NOT NULL
);

CREATE UNIQUE INDEX uq_users_email ON users (lower(email));

-- Metas diárias com data de início de vigência (uma por dia por usuário).
CREATE TABLE nutrition_goals (
    id             uuid PRIMARY KEY,
    owner_user_id  uuid          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    effective_from date          NOT NULL,
    kcal           numeric(12,4) NOT NULL CHECK (kcal > 0),
    protein_g      numeric(12,4) CHECK (protein_g >= 0),
    carbs_g        numeric(12,4) CHECK (carbs_g >= 0),
    fat_g          numeric(12,4) CHECK (fat_g >= 0),
    version        integer       NOT NULL CHECK (version >= 1),
    updated_at     timestamptz   NOT NULL,
    CONSTRAINT uq_goals_owner_date UNIQUE (owner_user_id, effective_from)
);
