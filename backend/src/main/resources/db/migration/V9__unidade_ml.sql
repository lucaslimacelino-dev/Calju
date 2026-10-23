-- Unidade em que o alimento é medido e mostrado: g ou ml. O app conta 1 ml como 1 g; os
-- valores continuam por 100 da unidade e a quantidade continua em quantity_g.
ALTER TABLE foods
    ADD COLUMN unit varchar(2) NOT NULL DEFAULT 'g' CONSTRAINT ck_foods_unit CHECK (unit IN ('g', 'ml'));

ALTER TABLE log_items
    ADD COLUMN unit varchar(2) NOT NULL DEFAULT 'g' CONSTRAINT ck_log_items_unit CHECK (unit IN ('g', 'ml'));
