package io.github.lucaslimacelino_dev.calju.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import io.github.lucaslimacelino_dev.calju.domain.DailyValues.Reference;
import io.github.lucaslimacelino_dev.calju.domain.GoalCalculator.Activity;
import io.github.lucaslimacelino_dev.calju.domain.GoalCalculator.Objective;
import io.github.lucaslimacelino_dev.calju.domain.GoalCalculator.Sex;

public class DailyValuesTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 27);

    private static Reference reference(List<Reference> references, String code) {
        for (Reference reference : references) {
            if (reference.code().equals(code)) {
                return reference;
            }
        }
        throw new AssertionError(code);
    }

    private static Reference reference(String code) {
        return reference(DailyValues.ALL, code);
    }

    private static UserProfile person(Sex sex, int age, boolean special) {
        return UserProfile.fromAge(sex, age, DAY, new BigDecimal("170"), new BigDecimal("70"), Activity.LIGHT,
                Objective.KEEP, null, special);
    }

    private static BigDecimal value(List<Reference> references, String code) {
        return reference(references, code).dailyValue();
    }

    @Test
    public void porcentagemDoValorDiario() {
        assertEquals(Integer.valueOf(50), DailyValues.percent(reference("FE"), new BigDecimal("7")));
        assertEquals(Integer.valueOf(130), DailyValues.percent(reference("NA"), new BigDecimal("2600")));
        assertEquals(Integer.valueOf(0), DailyValues.percent(reference("CA"), BigDecimal.ZERO));
    }

    @Test
    public void cobreUsaOValorEmMiligramas() {
        // 900 µg = 0,9 mg, a unidade da TACO.
        assertEquals(Integer.valueOf(100), DailyValues.percent(reference("CU"), new BigDecimal("0.9")));
    }

    @Test
    public void vitaminaAEmRaeEB12TemPorcentagem() {
        assertEquals(Integer.valueOf(50), DailyValues.percent(reference("VITA_RAE"), new BigDecimal("400")));
        assertEquals(Integer.valueOf(50), DailyValues.percent(reference("VITB12"), new BigDecimal("1.2")));
    }

    @Test
    public void gorduraTotalTemLimiteDe30PorCentoDaMeta() {
        // 30% de 2660 kcal / 9 = 88,7 g.
        assertEquals(new BigDecimal("88.7"),
                value(DailyValues.forPerson(person(Sex.MALE, 30, false), DAY, new BigDecimal("2660")), "FAT"));
        // Sem meta: 65 g da ANVISA. Sem perfil, mas com meta: segue a meta.
        assertEquals(new BigDecimal("65"), value(DailyValues.forPerson(person(Sex.MALE, 30, false), DAY, null), "FAT"));
        assertEquals(new BigDecimal("60.0"), value(DailyValues.forPerson(null, DAY, new BigDecimal("1800")), "FAT"));
    }

    @Test
    public void sodioEGorduraSaoLimites() {
        assertTrue(DailyValues.isLimit(reference("NA")));
        assertTrue(DailyValues.isLimit(reference("FAT")));
        assertFalse(DailyValues.isLimit(reference("FE")));
    }

    @Test
    public void listaNaoTemMacrosRepetidosColesterolUmidadeCinzasNemRetinol() {
        Set<String> codes = new HashSet<>();
        for (Reference reference : DailyValues.ALL) {
            assertTrue("repetido: " + reference.code(), codes.add(reference.code()));
        }
        for (String removed : new String[]{"ENERC", "PROCNT", "CHOCDF", "CHOLE", "WATER", "ASH", "RETOL", "VITA"}) {
            assertFalse(removed, codes.contains(removed));
        }
        assertEquals(18, codes.size());
    }

    @Test
    public void mulherDe30AnosUsaAsDris() {
        List<Reference> refs = DailyValues.forPerson(person(Sex.FEMALE, 30, false), DAY, null);

        assertEquals(new BigDecimal("18"), value(refs, "FE"));
        assertEquals(new BigDecimal("8"), value(refs, "ZN"));
        assertEquals(new BigDecimal("310"), value(refs, "MG"));
        assertEquals(new BigDecimal("2600"), value(refs, "K"));
        assertEquals(new BigDecimal("700"), value(refs, "VITA_RAE"));
        assertEquals(new BigDecimal("75"), value(refs, "VITC"));
        assertEquals(new BigDecimal("2300"), value(refs, "NA"));
        assertEquals(new BigDecimal("25"), value(refs, "FIBTG"));
        assertEquals(DailyValues.ALL.size(), refs.size());
    }

    @Test
    public void homemDe60AnosUsaAsDris() {
        List<Reference> refs = DailyValues.forPerson(person(Sex.MALE, 60, false), DAY, null);

        assertEquals(new BigDecimal("8"), value(refs, "FE"));
        assertEquals(new BigDecimal("11"), value(refs, "ZN"));
        assertEquals(new BigDecimal("420"), value(refs, "MG"));
        assertEquals(new BigDecimal("1000"), value(refs, "CA"));
        assertEquals(new BigDecimal("1.7"), value(refs, "VITB6A"));
        assertEquals(new BigDecimal("30"), value(refs, "FIBTG"));
    }

    @Test
    public void mudancasDeFaixaPorIdade() {
        assertEquals(new BigDecimal("8"), value(DailyValues.forPerson(person(Sex.FEMALE, 51, false), DAY, null), "FE"));
        assertEquals(new BigDecimal("1200"),
                value(DailyValues.forPerson(person(Sex.FEMALE, 51, false), DAY, null), "CA"));
        assertEquals(new BigDecimal("1200"),
                value(DailyValues.forPerson(person(Sex.MALE, 71, false), DAY, null), "CA"));
    }

    @Test
    public void fibrasAcompanhamAMetaDeCalorias() {
        // 14 g a cada 1.000 kcal: 2.050 kcal → 28,7 g.
        List<Reference> refs = DailyValues.forPerson(person(Sex.FEMALE, 30, false), DAY, new BigDecimal("2050"));
        assertEquals(new BigDecimal("28.7"), value(refs, "FIBTG"));
    }

    @Test
    public void semPerfilAdultoUsaAAnvisa() {
        assertSame(DailyValues.ALL, DailyValues.forPerson(null, DAY, null));
        assertSame(DailyValues.ALL, DailyValues.forPerson(person(Sex.FEMALE, 30, true), DAY, null));
        assertSame(DailyValues.ALL, DailyValues.forPerson(person(Sex.FEMALE, 18, false), DAY, null));
        assertSame(DailyValues.ALL, DailyValues.forPerson(person(Sex.FEMALE, 81, false), DAY, null));
    }

    @Test
    public void idadeAvancaComOAno() {
        UserProfile profile = person(Sex.FEMALE, 50, false);
        assertEquals(50, profile.age(DAY));
        assertEquals(new BigDecimal("8"), value(DailyValues.forPerson(profile, DAY.plusYears(1), null), "FE"));
    }
}
