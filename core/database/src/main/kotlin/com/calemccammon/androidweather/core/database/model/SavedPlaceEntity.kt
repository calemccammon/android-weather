package com.calemccammon.androidweather.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A place the user has kept. */
@Entity(
    tableName = "saved_places",
    // One row per spot on the map, however many times it is saved.
    indices = [Index(value = ["place_key"], unique = true)],
)
data class SavedPlaceEntity(
    /** Rows are numbered as they are added, so the order of ids is the order of saving. */
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Which spot on the map this is; what its forecast is filed under. */
    @ColumnInfo(name = "place_key") val placeKey: String,
    val latitude: Double,
    val longitude: Double,
    val name: String?,
    val region: String?,
    val country: String?,
)
