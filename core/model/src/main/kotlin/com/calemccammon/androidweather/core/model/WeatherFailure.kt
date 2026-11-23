package com.calemccammon.androidweather.core.model

/** The ways getting weather can fail, as far as the app cares. */
enum class WeatherFailure {
    /** No answer in time. */
    Timeout,

    /** The service could not be reached at all. */
    NoConnection,

    /** An answer came, but not one the app understands. */
    BadResponse,

    /** The service refused the request. */
    Rejected,

    /** The service is in trouble or too busy; trying later may work. */
    Unavailable,

    /** The place asked about is not a point on the globe. */
    InvalidPlace,
    ;

    /** Whether the same request may well succeed later with nothing changed. */
    val isTemporary: Boolean
        get() = this == Timeout || this == NoConnection || this == Unavailable
}

/**
 * Thrown when a place search or a forecast cannot be had.
 *
 * It says what kind of failure it was; wording that for the user is the UI's
 * job.
 */
class WeatherException(
    val failure: WeatherFailure,
    /** Anything more the source said, for logs. Not for showing to the user. */
    val detail: String? = null,
    cause: Throwable? = null,
) : Exception(if (detail == null) failure.name else "${failure.name}: $detail", cause)
