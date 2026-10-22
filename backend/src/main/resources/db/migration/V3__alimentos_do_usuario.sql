-- Nutrientes (códigos INFOODS), copiados de data/catalog/nutrients.csv. NutrientCatalogTest
-- confere que as duas listas continuam iguais.
CREATE TABLE nutrients (
    code          varchar(16) PRIMARY KEY,
    name_pt       varchar(100) NOT NULL,
    unit          varchar(8)   NOT NULL CHECK (unit IN ('g', 'mg', 'µg', 'kcal')),
    display_order integer      NOT NULL
);

INSERT INTO nutrients (code, name_pt, unit, display_order) VALUES
    ('ENERC', 'Energia', 'kcal', 1),
    ('PROCNT', 'Proteína', 'g', 2),
    ('CHOCDF', 'Carboidrato', 'g', 3),
    ('FAT', 'Gorduras totais', 'g', 4),
    ('FIBTG', 'Fibra alimentar', 'g', 5),
    ('CHOLE', 'Colesterol', 'mg', 6),
    ('NA', 'Sódio', 'mg', 7),
    ('K', 'Potássio', 'mg', 8),
    ('CA', 'Cálcio', 'mg', 9),
    ('FE', 'Ferro', 'mg', 10),
    ('MG', 'Magnésio', 'mg', 11),
    ('P', 'Fósforo', 'mg', 12),
    ('ZN', 'Zinco', 'mg', 13),
    ('CU', 'Cobre', 'mg', 14),
    ('MN', 'Manganês', 'mg', 15),
    ('VITC', 'Vitamina C', 'mg', 16),
    ('VITA', 'Vitamina A (RE)', 'µg', 17),
    ('RETOL', 'Retinol', 'µg', 18),
    ('THIA', 'Tiamina (B1)', 'mg', 19),
    ('RIBF', 'Riboflavina (B2)', 'mg', 20),
    ('VITB6A', 'Piridoxina (B6)', 'mg', 21),
    ('NIA', 'Niacina', 'mg', 22),
    ('WATER', 'Umidade', 'g', 23),
    ('ASH', 'Cinzas', 'g', 24);

-- Alimentos criados pelo usuário. O catálogo oficial (TACO) fica só no app.
CREATE TABLE foods (
    id            uuid PRIMARY KEY,
    owner_user_id uuid         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name          varchar(200) NOT NULL,
    brand         varchar(200),
    version       integer      NOT NULL CHECK (version >= 1),
    updated_at    timestamptz  NOT NULL,
    deleted_at    timestamptz
);

CREATE INDEX ix_foods_owner ON foods (owner_user_id);

-- Valor por 100 g. Linha ausente = desconhecido; TRACE = traço (sem quantidade).
CREATE TABLE food_nutrients (
    food_id        uuid          NOT NULL REFERENCES foods (id) ON DELETE CASCADE,
    nutrient_code  varchar(16)   NOT NULL REFERENCES nutrients (code),
    amount_per_100g numeric(12,4),
    qualifier      varchar(5)    NOT NULL,
    PRIMARY KEY (food_id, nutrient_code),
    CONSTRAINT ck_food_nutrient_value CHECK (
        (qualifier = 'VALUE' AND amount_per_100g IS NOT NULL AND amount_per_100g >= 0)
        OR (qualifier = 'TRACE' AND amount_per_100g IS NULL))
);

-- Porção padrão definida pelo usuário (no máximo uma por alimento, como no app).
CREATE TABLE food_portions (
    food_id uuid          PRIMARY KEY REFERENCES foods (id) ON DELETE CASCADE,
    label   varchar(100)  NOT NULL,
    grams   numeric(12,4) NOT NULL CHECK (grams > 0)
);
