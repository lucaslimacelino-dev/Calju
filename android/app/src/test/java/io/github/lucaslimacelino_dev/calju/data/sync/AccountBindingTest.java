package io.github.lucaslimacelino_dev.calju.data.sync;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AccountBindingTest {

    @Test
    public void primeiraContaComDadosPerguntaSeEnvia() {
        assertEquals(AccountBinding.Decision.ASK_UPLOAD, AccountBinding.decide(null, "a@exemplo.com", true));
    }

    @Test
    public void primeiraContaSemDadosSegueDireto() {
        assertEquals(AccountBinding.Decision.CONTINUE, AccountBinding.decide(null, "a@exemplo.com", false));
    }

    @Test
    public void mesmaContaSegueDiretoMesmoComMaiusculas() {
        assertEquals(AccountBinding.Decision.CONTINUE,
                AccountBinding.decide("a@exemplo.com", " A@Exemplo.com ", true));
    }

    @Test
    public void outraContaComDadosAvisaQueElesSaem() {
        assertEquals(AccountBinding.Decision.OTHER_ACCOUNT,
                AccountBinding.decide("a@exemplo.com", "b@exemplo.com", true));
    }

    @Test
    public void outraContaSemDadosSegueDireto() {
        assertEquals(AccountBinding.Decision.CONTINUE,
                AccountBinding.decide("a@exemplo.com", "b@exemplo.com", false));
    }
}
