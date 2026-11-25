package com.calemccammon.androidweather

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.calemccammon.androidweather.feature.home.HomeDestination
import com.calemccammon.androidweather.feature.home.homeScreen
import com.calemccammon.androidweather.feature.places.PlacesDestination
import com.calemccammon.androidweather.feature.places.placesScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * The app's screens and the ways between them. A feature does not know the
 * other exists: each says what it needs done, and this joins them up.
 *
 * Each emission of [placeOpened] (an alert pressed) brings the home screen
 * forward, from wherever the user was, to show the place.
 */
@Composable
fun WeatherNavHost(placeOpened: Flow<Unit> = emptyFlow()) {
    val navigation = rememberNavController()

    LaunchedEffect(placeOpened) {
        placeOpened.collect { navigation.popBackStack(HomeDestination, inclusive = false) }
    }

    NavHost(navigation, startDestination = HomeDestination) {
        homeScreen(onOpenSavedPlaces = { navigation.navigate(PlacesDestination) })
        placesScreen(onBack = { navigation.popBackStack(HomeDestination, inclusive = false) })
    }
}
