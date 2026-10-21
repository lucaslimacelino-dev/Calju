package io.github.lucaslimacelino_dev.calju.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.time.LocalDate;

/**
 * Pedido de "abrir o Diário neste dia", compartilhado entre abas. Fica no ViewModel da Activity,
 * que todas as abas enxergam; o Diário consome o pedido ao atendê-lo.
 */
public class DateRequest extends ViewModel {

    private final MutableLiveData<LocalDate> requested = new MutableLiveData<>();

    public LiveData<LocalDate> requested() {
        return requested;
    }

    public void open(LocalDate date) {
        requested.setValue(date);
    }

    public void consumed() {
        requested.setValue(null);
    }
}
