-- Diário: cada lançamento (data, ocasião) tem um ou mais itens com a cópia (snapshot) dos
-- nutrientes no momento do consumo. Alterar alimento ou refeição não muda o passado.
CREATE TABLE log_entries (
    id             uuid PRIMARY KEY,
    owner_user_id  uuid         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    log_date       date         NOT NULL,
    occasion       varchar(10)  NOT NULL CHECK (occasion IN ('BREAKFAST', 'LUNCH', 'DINNER', 'SNACK')),
    title          varchar(200),
    source_meal_id uuid,
    created_at     timestamptz  NOT NULL,
    version        integer      NOT NULL CHECK (version >= 1),
    updated_at     timestamptz  NOT NULL,
    deleted_at     timestamptz
);

CREATE INDEX ix_log_entries_owner_date ON log_entries (owner_user_id, log_date);

-- food_id sem chave estrangeira: o snapshot vale mesmo que o alimento suma.
-- A posição única é conferida no fim da transação, para permitir reordenar itens.
CREATE TABLE log_items (
    id                 uuid PRIMARY KEY,
    entry_id           uuid          NOT NULL REFERENCES log_entries (id) ON DELETE CASCADE,
    food_id            uuid,
    food_name_snap     varchar(200)  NOT NULL,
    brand_snap         varchar(200),
    source_snap        varchar(50),
    quantity_g         numeric(12,4) NOT NULL CHECK (quantity_g > 0),
    portion_label_snap varchar(100),
    position           integer       NOT NULL CHECK (position >= 0),
    CONSTRAINT uq_log_items_position UNIQUE (entry_id, position) DEFERRABLE INITIALLY DEFERRED
);

CREATE TABLE log_item_nutrients (
    log_item_id     uuid          NOT NULL REFERENCES log_items (id) ON DELETE CASCADE,
    nutrient_code   varchar(16)   NOT NULL REFERENCES nutrients (code),
    amount_per_100g numeric(12,4),
    qualifier       varchar(5)    NOT NULL,
    PRIMARY KEY (log_item_id, nutrient_code),
    CONSTRAINT ck_log_item_nutrient_value CHECK (
        (qualifier = 'VALUE' AND amount_per_100g IS NOT NULL AND amount_per_100g >= 0)
        OR (qualifier = 'TRACE' AND amount_per_100g IS NULL))
);
