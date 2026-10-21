package io.github.lucaslimacelino_dev.calju.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.time.LocalDate;
import java.util.List;

@Dao
public interface GoalDao {

    @Insert
    void insert(NutritionGoalEntity goal);

    @Update
    void update(NutritionGoalEntity goal);

    @Query("SELECT * FROM nutrition_goals ORDER BY effective_from")
    LiveData<List<NutritionGoalEntity>> observeAll();

    @Query("SELECT * FROM nutrition_goals ORDER BY effective_from")
    List<NutritionGoalEntity> all();

    @Query("SELECT * FROM nutrition_goals WHERE effective_from = :date")
    NutritionGoalEntity byEffectiveFrom(LocalDate date);
}
