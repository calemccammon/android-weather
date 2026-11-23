package com.calemccammon.androidweather.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.calemccammon.androidweather.core.database.model.SavedPlaceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedPlaceDao {
    /** Every saved place, oldest first. */
    @Query("SELECT * FROM saved_places ORDER BY id")
    fun observeAll(): Flow<List<SavedPlaceEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM saved_places WHERE place_key = :placeKey)")
    fun observeIsSaved(placeKey: String): Flow<Boolean>

    /** Adds [place], unless its spot on the map is saved already. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(place: SavedPlaceEntity)

    @Query("DELETE FROM saved_places WHERE place_key = :placeKey")
    suspend fun delete(placeKey: String)
}
