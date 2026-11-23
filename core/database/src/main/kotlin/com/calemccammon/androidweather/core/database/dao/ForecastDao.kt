package com.calemccammon.androidweather.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.calemccammon.androidweather.core.database.model.DailyForecastEntity
import com.calemccammon.androidweather.core.database.model.ForecastEntity
import com.calemccammon.androidweather.core.database.model.ForecastWithDays
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
abstract class ForecastDao {
    /** The forecast held for a spot, with its days in no particular order; null while there is none. */
    @Transaction
    @Query("SELECT * FROM forecasts WHERE place_key = :placeKey")
    abstract fun observe(placeKey: String): Flow<ForecastWithDays?>

    /**
     * Puts [forecast] and [days] in place of whatever was held for the spot.
     * In one transaction, so a reader never sees new conditions with old days.
     */
    @Transaction
    open suspend fun replace(
        forecast: ForecastEntity,
        days: List<DailyForecastEntity>,
    ) {
        upsert(forecast)
        deleteDays(forecast.placeKey)
        insertDays(days)
    }

    /**
     * Deletes forecasts fetched before [before] whose spot is neither a saved
     * place nor one of [keep]. Returns how many went.
     */
    @Query(
        """
        DELETE FROM forecasts
        WHERE fetched_at < :before
        AND place_key NOT IN (SELECT place_key FROM saved_places)
        AND place_key NOT IN (:keep)
        """,
    )
    abstract suspend fun deleteUnsavedFetchedBefore(
        before: Instant,
        keep: List<String> = emptyList(),
    ): Int

    @Upsert
    protected abstract suspend fun upsert(forecast: ForecastEntity)

    @Query("DELETE FROM daily_forecasts WHERE place_key = :placeKey")
    protected abstract suspend fun deleteDays(placeKey: String)

    @Insert
    protected abstract suspend fun insertDays(days: List<DailyForecastEntity>)
}
