package io.github.lucaslimacelino_dev.calju.data.sync;

import java.util.Locale;

/**
 * O que fazer com os dados do aparelho ao entrar numa conta. O banco local pertence a no máximo
 * uma conta (a "vinculada"). Sem Android: testável no computador.
 */
public final class AccountBinding {

    public enum Decision {
        /** Nada a perguntar: é a mesma conta, ou o aparelho não tem dados. */
        CONTINUE,
        /** Primeira conta neste aparelho e há dados anotados: enviar ou apagar? */
        ASK_UPLOAD,
        /** O aparelho tem dados de outra conta: para entrar, eles saem daqui. */
        OTHER_ACCOUNT
    }

    private AccountBinding() {
    }

    /**
     * @param boundEmail  conta vinculada ao banco local (nula = nunca vinculado)
     * @param signedEmail conta que acabou de entrar
     * @param hasLocalData se há registros, refeições, metas ou alimentos do usuário no aparelho
     */
    public static Decision decide(String boundEmail, String signedEmail, boolean hasLocalData) {
        if (boundEmail == null) {
            return hasLocalData ? Decision.ASK_UPLOAD : Decision.CONTINUE;
        }
        if (normalize(boundEmail).equals(normalize(signedEmail))) {
            return Decision.CONTINUE;
        }
        return hasLocalData ? Decision.OTHER_ACCOUNT : Decision.CONTINUE;
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
