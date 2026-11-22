package com.calemccammon.androidweather.core.notifications

import android.content.Intent

/** What an alert's notification puts in the intent that opens the app. */
object AlertIntents {
    internal const val EXTRA_SAVED_PLACE_ID = "com.calemccammon.androidweather.SAVED_PLACE_ID"

    /** The saved place an alert was about, if [intent] came from one. */
    fun savedPlaceId(intent: Intent?): Long? =
        intent?.takeIf { it.hasExtra(EXTRA_SAVED_PLACE_ID) }?.getLongExtra(EXTRA_SAVED_PLACE_ID, 0)
}
