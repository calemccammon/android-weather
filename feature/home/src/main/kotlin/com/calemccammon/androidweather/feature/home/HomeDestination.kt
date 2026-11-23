package com.calemccammon.androidweather.feature.home

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/** Where the home screen is in the app's navigation. */
@Serializable
data object HomeDestination

fun NavGraphBuilder.homeScreen(onOpenSavedPlaces: () -> Unit) {
    composable<HomeDestination> { HomeRoute(onOpenSavedPlaces) }
}
