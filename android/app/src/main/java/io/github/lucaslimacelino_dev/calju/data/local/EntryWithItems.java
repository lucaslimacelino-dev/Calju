package io.github.lucaslimacelino_dev.calju.data.local;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

/** Lançamento com seus itens (e os nutrientes de cada item). */
public class EntryWithItems {

    @Embedded
    public LogEntryEntity entry;

    @Relation(entity = LogItemEntity.class, parentColumn = "id", entityColumn = "entry_id")
    public List<ItemWithNutrients> items;
}
