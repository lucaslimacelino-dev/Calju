package io.github.lucaslimacelino_dev.calju.user;

import java.util.Locale;

/** E-mail como é guardado e buscado: sem espaços nas pontas e em minúsculas. */
public final class Emails {

    private Emails() {
    }

    public static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
