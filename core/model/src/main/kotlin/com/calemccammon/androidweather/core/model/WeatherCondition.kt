package com.calemccammon.androidweather.core.model

/** The kinds of weather the app tells apart. */
enum class WeatherCondition {
    Clear,
    MostlyClear,
    PartlyCloudy,
    Overcast,
    Fog,
    Drizzle,
    FreezingDrizzle,
    Rain,
    FreezingRain,
    Snow,
    RainShowers,
    SnowShowers,
    Thunderstorm,
    Unknown,
    ;

    /** Weather worth warning someone about before they go out in it. */
    val isSevere: Boolean
        get() = this == Thunderstorm || this == FreezingRain || this == FreezingDrizzle
}
