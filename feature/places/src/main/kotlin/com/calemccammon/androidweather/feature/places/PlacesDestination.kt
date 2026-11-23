package com.calemccammon.androidweather.feature.places

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/** Where the saved places screen is in the app's navigation. */
@Serializable
data object PlacesDestination

fun NavGraphBuilder.placesScreen(onBack: () -> Unit) {
    composable<PlacesDestination> { PlacesRoute(onBack) }
}
