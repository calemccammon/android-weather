package com.calemccammon.androidweather.core.data.repository

import com.calemccammon.androidweather.core.data.key
import com.calemccammon.androidweather.core.data.toEntity
import com.calemccammon.androidweather.core.data.toModel
import com.calemccammon.androidweather.core.database.dao.SavedPlaceDao
import com.calemccammon.androidweather.core.database.model.SavedPlaceEntity
import com.calemccammon.androidweather.core.domain.repository.SavedPlacesRepository
import com.calemccammon.androidweather.core.model.Place
import com.calemccammon.androidweather.core.model.SavedPlace
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Saved places, kept in the database. */
internal class RoomSavedPlacesRepository
    @Inject
    constructor(
        private val dao: SavedPlaceDao,
    ) : SavedPlacesRepository {
        override val places: Flow<List<SavedPlace>> = dao.observeAll().map { it.map(SavedPlaceEntity::toModel) }

        override fun isSaved(place: Place): Flow<Boolean> = dao.observeIsSaved(place.key)

        override suspend fun save(place: Place) = dao.insert(place.toEntity())

        override suspend fun remove(place: Place) = dao.delete(place.key)
    }
