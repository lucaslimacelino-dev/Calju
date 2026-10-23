-- Código de 6 dígitos também para excluir a conta pela página web (sem precisar do app nem de senha).
ALTER TABLE email_codes DROP CONSTRAINT email_codes_purpose_check;

ALTER TABLE email_codes
    ADD CONSTRAINT ck_email_codes_purpose CHECK (purpose IN ('VERIFY', 'RESET', 'DELETE'));
