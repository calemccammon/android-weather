package com.calemccammon.androidweather.core.domain.usecase

import com.calemccammon.androidweather.core.domain.repository.PlaceSearchRepository
import com.calemccammon.androidweather.core.model.Place
import javax.inject.Inject

/** Finds places by name. */
class SearchPlaces
    @Inject
    constructor(
        private val repository: PlaceSearchRepository,
    ) {
        /**
         * Places matching [query], best match first. Empty, without asking the
         * repository, when the query is too short.
         */
        suspend operator fun invoke(query: String): List<Place> {
            if (!isSearchable(query)) return emptyList()
            return repository.search(query.trim())
        }

        companion object {
            /** A query shorter than this is not worth sending: one letter matches nothing useful. */
            const val MIN_QUERY_LENGTH = 2

            /** Whether [query] is long enough to search for, spaces around it aside. */
            fun isSearchable(query: String): Boolean = query.trim().length >= MIN_QUERY_LENGTH
        }
    }
