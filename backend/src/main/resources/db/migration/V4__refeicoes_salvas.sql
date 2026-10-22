-- Refeições salvas (moldes). Alterar um molde não altera registros já feitos no diário.
CREATE TABLE meals (
    id            uuid PRIMARY KEY,
    owner_user_id uuid         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name          varchar(200) NOT NULL,
    version       integer      NOT NULL CHECK (version >= 1),
    updated_at    timestamptz  NOT NULL,
    deleted_at    timestamptz
);

CREATE INDEX ix_meals_owner ON meals (owner_user_id);

-- food_id sem chave estrangeira: pode ser um alimento da TACO, que só existe no app.
CREATE TABLE meal_items (
    id         uuid PRIMARY KEY,
    meal_id    uuid          NOT NULL REFERENCES meals (id) ON DELETE CASCADE,
    food_id    uuid          NOT NULL,
    quantity_g numeric(12,4) NOT NULL CHECK (quantity_g > 0),
    position   integer       NOT NULL CHECK (position >= 0),
    CONSTRAINT uq_meal_items_position UNIQUE (meal_id, position)
);
