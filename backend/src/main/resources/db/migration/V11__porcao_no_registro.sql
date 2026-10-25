-- Peso da porção caseira no momento do registro (junto com portion_label_snap), para o app mostrar
-- a quantidade como "4 unidades". Nulo quando o alimento não tinha porção.
ALTER TABLE log_items
    ADD COLUMN portion_grams_snap numeric(12, 4) CONSTRAINT ck_log_items_portion_grams CHECK (portion_grams_snap > 0);
