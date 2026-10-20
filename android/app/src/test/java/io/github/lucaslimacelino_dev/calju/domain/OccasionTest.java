package io.github.lucaslimacelino_dev.calju.domain;

import static org.junit.Assert.assertEquals;

import java.time.LocalTime;

import org.junit.Test;

public class OccasionTest {

    @Test
    public void manhaECafe() {
        assertEquals(Occasion.BREAKFAST, Occasion.suggestFor(LocalTime.of(5, 0)));
        assertEquals(Occasion.BREAKFAST, Occasion.suggestFor(LocalTime.of(10, 59)));
    }

    @Test
    public void meioDoDiaEAlmoco() {
        assertEquals(Occasion.LUNCH, Occasion.suggestFor(LocalTime.of(11, 0)));
        assertEquals(Occasion.LUNCH, Occasion.suggestFor(LocalTime.of(14, 59)));
    }

    @Test
    public void noiteEJantar() {
        assertEquals(Occasion.DINNER, Occasion.suggestFor(LocalTime.of(18, 0)));
        assertEquals(Occasion.DINNER, Occasion.suggestFor(LocalTime.of(21, 59)));
    }

    @Test
    public void restoELanche() {
        assertEquals(Occasion.SNACK, Occasion.suggestFor(LocalTime.of(16, 0)));
        assertEquals(Occasion.SNACK, Occasion.suggestFor(LocalTime.of(23, 30)));
        assertEquals(Occasion.SNACK, Occasion.suggestFor(LocalTime.of(3, 0)));
    }
}
