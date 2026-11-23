package com.calemccammon.androidweather.feature.home.model

import com.calemccammon.androidweather.core.ui.UiText
import com.calemccammon.androidweather.core.ui.uiText
import com.calemccammon.androidweather.feature.home.R
import java.time.Duration
import java.time.Instant

/**
 * How old a forecast fetched at [fetchedAt] is at [now], in the largest unit
 * that fits: "Updated just now", "Updated 5 minutes ago", "Updated 2 days ago".
 * A forecast dated after [now], as when the clock was moved, is "just now".
 */
fun updatedText(
    fetchedAt: Instant,
    now: Instant,
): UiText {
    val age = Duration.between(fetchedAt, now)
    return when {
        age < Duration.ofMinutes(1) -> uiText(R.string.feature_home_updated_just_now)
        age < Duration.ofHours(1) -> UiText.Plural(R.plurals.feature_home_updated_minutes, age.toMinutes().toInt())
        age < Duration.ofDays(1) -> UiText.Plural(R.plurals.feature_home_updated_hours, age.toHours().toInt())
        else -> UiText.Plural(R.plurals.feature_home_updated_days, age.toDays().toInt())
    }
}
