package io.github.lucaslimacelino_dev.calju.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import io.github.lucaslimacelino_dev.calju.domain.Occasion;

@Dao
public interface DiaryDao {

    @Insert
    void insertEntry(LogEntryEntity entry);

    @Insert
    void insertItem(LogItemEntity item);

    @Insert
    void insertItemNutrients(List<LogItemNutrientEntity> nutrients);

    /** LiveData: o Room avisa a tela sempre que as tabelas do diário mudam. */
    @Transaction
    @Query("SELECT * FROM log_entries WHERE log_date = :date AND deleted_at IS NULL ORDER BY created_at")
    LiveData<List<EntryWithItems>> observeDay(LocalDate date);

    @Transaction
    @Query("SELECT * FROM log_entries WHERE log_date = :date AND deleted_at IS NULL ORDER BY created_at")
    List<EntryWithItems> day(LocalDate date);

    @Transaction
    @Query("SELECT * FROM log_entries WHERE id = :entryId")
    EntryWithItems entry(String entryId);

    @Query("UPDATE log_items SET quantity_g = :grams WHERE id = :itemId")
    int updateItemQuantity(String itemId, BigDecimal grams);

    @Query("UPDATE log_entries SET occasion = :occasion WHERE id = :entryId")
    int updateEntryOccasion(String entryId, Occasion occasion);

    /** Marca o lançamento como alterado: nova versão, pendente de sincronização. */
    @Query("UPDATE log_entries SET updated_at = :now, version = version + 1, sync_state = '"
            + LogEntryEntity.SYNC_PENDING + "' WHERE id = :entryId")
    int touchEntry(String entryId, long now);

    @Query("UPDATE log_entries SET deleted_at = :now WHERE id = :entryId")
    int markDeleted(String entryId, long now);

    @Query("UPDATE log_entries SET deleted_at = NULL WHERE id = :entryId")
    int clearDeleted(String entryId);

    @Query("SELECT entry_id FROM log_items WHERE id = :itemId")
    String entryIdOf(String itemId);

    @Query("SELECT id FROM log_items WHERE entry_id = :entryId")
    List<String> itemIdsOf(String entryId);

    @Query("UPDATE log_items SET quantity_g = :grams, position = :position WHERE id = :itemId")
    int updateItem(String itemId, BigDecimal grams, int position);

    /** Os nutrientes copiados do item são apagados junto (chave estrangeira com CASCADE). */
    @Query("DELETE FROM log_items WHERE id = :itemId")
    int deleteItem(String itemId);
}
