package io.github.lucaslimacelino_dev.calju.data.local;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

/** Item do diário junto com os nutrientes copiados; o Room monta a relação. */
public class ItemWithNutrients {

    @Embedded
    public LogItemEntity item;

    @Relation(parentColumn = "id", entityColumn = "log_item_id")
    public List<LogItemNutrientEntity> nutrients;
}
