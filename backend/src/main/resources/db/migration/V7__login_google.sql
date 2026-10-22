-- Vínculo com a conta Google: o "sub" do token do Google (identificador estável; o e-mail pode mudar).
ALTER TABLE users ADD COLUMN google_subject varchar(255);

CREATE UNIQUE INDEX uq_users_google_subject ON users (google_subject);
