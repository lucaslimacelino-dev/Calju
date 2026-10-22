-- Cursor da sincronização: cada inserção ou alteração recebe o próximo número de uma sequência
-- única. O app pede "tudo do meu usuário com número maior que o último que vi" (GET /sync/pull).
-- O número é dado por gatilho no banco, então nenhum código pode esquecer de atualizá-lo.
-- As gravações de um usuário são serializadas (trava na linha de users), o que garante que os
-- números dele chegam em ordem de confirmação e o cursor nunca pula uma mudança.
CREATE SEQUENCE change_seq;

CREATE FUNCTION set_change_seq() RETURNS trigger AS $$
BEGIN
    NEW.change_seq := nextval('change_seq');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

ALTER TABLE nutrition_goals ADD COLUMN change_seq bigint;
UPDATE nutrition_goals SET change_seq = nextval('change_seq');
ALTER TABLE nutrition_goals ALTER COLUMN change_seq SET NOT NULL;
CREATE INDEX ix_goals_owner_seq ON nutrition_goals (owner_user_id, change_seq);
CREATE TRIGGER trg_goals_change_seq BEFORE INSERT OR UPDATE ON nutrition_goals
    FOR EACH ROW EXECUTE FUNCTION set_change_seq();

ALTER TABLE foods ADD COLUMN change_seq bigint;
UPDATE foods SET change_seq = nextval('change_seq');
ALTER TABLE foods ALTER COLUMN change_seq SET NOT NULL;
CREATE INDEX ix_foods_owner_seq ON foods (owner_user_id, change_seq);
CREATE TRIGGER trg_foods_change_seq BEFORE INSERT OR UPDATE ON foods
    FOR EACH ROW EXECUTE FUNCTION set_change_seq();

ALTER TABLE meals ADD COLUMN change_seq bigint;
UPDATE meals SET change_seq = nextval('change_seq');
ALTER TABLE meals ALTER COLUMN change_seq SET NOT NULL;
CREATE INDEX ix_meals_owner_seq ON meals (owner_user_id, change_seq);
CREATE TRIGGER trg_meals_change_seq BEFORE INSERT OR UPDATE ON meals
    FOR EACH ROW EXECUTE FUNCTION set_change_seq();

ALTER TABLE log_entries ADD COLUMN change_seq bigint;
UPDATE log_entries SET change_seq = nextval('change_seq');
ALTER TABLE log_entries ALTER COLUMN change_seq SET NOT NULL;
CREATE INDEX ix_log_entries_owner_seq ON log_entries (owner_user_id, change_seq);
CREATE TRIGGER trg_log_entries_change_seq BEFORE INSERT OR UPDATE ON log_entries
    FOR EACH ROW EXECUTE FUNCTION set_change_seq();
