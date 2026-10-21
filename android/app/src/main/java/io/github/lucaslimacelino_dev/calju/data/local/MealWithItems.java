package io.github.lucaslimacelino_dev.calju.data.local;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

public class MealWithItems {

    @Embedded
    public MealEntity meal;

    @Relation(parentColumn = "id", entityColumn = "meal_id")
    public List<MealItemEntity> items;
}
