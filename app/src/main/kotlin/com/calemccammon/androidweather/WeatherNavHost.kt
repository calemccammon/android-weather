package com.calemccammon.androidweather

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.calemccammon.androidweather.feature.home.HomeDestination
import com.calemccammon.androidweather.feature.home.homeScreen
import com.calemccammon.androidweather.feature.places.PlacesDestination
import com.calemccammon.androidweather.feature.places.placesScreen

/**
 * The app's screens and the ways between them. A feature does not know the
 * other exists: each says what it needs done, and this joins them up.
 */
@Composable
fun WeatherNavHost() {
    val navigation = rememberNavController()

    NavHost(navigation, startDestination = HomeDestination) {
        homeScreen(onOpenSavedPlaces = { navigation.navigate(PlacesDestination) })
        placesScreen(onBack = { navigation.popBackStack(HomeDestination, inclusive = false) })
    }
}
