package com.calemccammon.androidweather.feature.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.focusable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calemccammon.androidweather.core.designsystem.Breakpoints
import com.calemccammon.androidweather.core.designsystem.IconSizes
import com.calemccammon.androidweather.core.designsystem.Spacing
import com.calemccammon.androidweather.core.ui.asString
import com.calemccammon.androidweather.feature.home.component.ForecastPanel
import com.calemccammon.androidweather.feature.home.component.MapPin
import com.calemccammon.androidweather.feature.home.component.PlaceSearchBar
import com.calemccammon.androidweather.feature.home.component.WeatherMap
import com.calemccammon.androidweather.feature.home.model.HomeEvent
import com.calemccammon.androidweather.feature.home.model.HomeState
import com.calemccammon.androidweather.feature.home.model.SearchState
import com.calemccammon.androidweather.feature.home.model.UnitsUiModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

/**
 * Draws the map. The screen says where the pin goes, how much of each edge
 * of the map something else covers, and what to do with a press-and-hold.
 */
typealias MapSlot = @Composable (
    pin: MapPin?,
    covered: PaddingValues,
    onLongPress: (latitude: Double, longitude: Double) -> Unit,
) -> Unit

/** What the home screen can ask for. */
@Suppress("LongParameterList") // One for each thing a user can do on the screen.
class HomeActions(
    val onQueryChange: (String) -> Unit,
    val onSearchOpened: () -> Unit,
    /** The row of the search results that was chosen. */
    val onChooseResult: (index: Int) -> Unit,
    val onRetrySearch: () -> Unit,
    val onSelectPoint: (latitude: Double, longitude: Double) -> Unit,
    val onClearPlace: () -> Unit,
    val onToggleSaved: () -> Unit,
    val onUnitsChange: (UnitsUiModel) -> Unit,
    val onRetryForecast: () -> Unit,
    val onOpenSavedPlaces: () -> Unit,
    val onMyLocation: () -> Unit,
)

/** The home screen wired to its view models and the real map. */
@Composable
fun HomeRoute(
    onOpenSavedPlaces: () -> Unit,
    homeViewModel: HomeViewModel = hiltViewModel(),
    searchViewModel: SearchViewModel = hiltViewModel(),
) {
    val home by homeViewModel.state.collectAsStateWithLifecycle()
    val search by searchViewModel.state.collectAsStateWithLifecycle()
    // The time a forecast's age is reckoned from, moved on now and then so
    // "5 minutes ago" does not stay on screen for an hour.
    val now by produceState(Instant.now()) {
        while (true) {
            delay(clockStep)
            value = Instant.now()
        }
    }

    // Each time the screen comes back into view, not only when it is first made.
    LifecycleResumeEffect(homeViewModel) {
        homeViewModel.refreshIfStale()
        onPauseOrDispose {}
    }
    val snackbars = remember { SnackbarHostState() }
    val onMyLocation = rememberMyLocationAction(snackbars, homeViewModel::selectCurrentLocation)
    val locationUnavailable = stringResource(R.string.feature_home_location_unavailable)
    LaunchedEffect(homeViewModel, snackbars) {
        homeViewModel.events.collect { event ->
            when (event) {
                HomeEvent.LocationUnavailable -> snackbars.showSnackbar(locationUnavailable)
            }
        }
    }

    HomeScreen(
        home = home,
        search = search,
        now = now,
        snackbars = snackbars,
        actions =
            HomeActions(
                onQueryChange = searchViewModel::queryChanged,
                onSearchOpened = searchViewModel::reset,
                // The two view models meet here: the row chosen in one is the
                // place selected in the other.
                onChooseResult = { index -> searchViewModel.resultAt(index)?.let(homeViewModel::select) },
                onRetrySearch = searchViewModel::retry,
                onSelectPoint = homeViewModel::selectPoint,
                onClearPlace = homeViewModel::clear,
                onToggleSaved = homeViewModel::toggleSaved,
                onUnitsChange = homeViewModel::setUnits,
                onRetryForecast = homeViewModel::retryForecast,
                onOpenSavedPlaces = onOpenSavedPlaces,
                onMyLocation = onMyLocation,
            ),
        map = { pin, covered, onLongPress ->
            WeatherMap(pin, covered, dark = isSystemInDarkTheme(), onLongPress = onLongPress)
        },
    )
}

/**
 * What pressing the "my location" button does: asks the user to allow
 * approximate location if they have not, and calls [onAllowed] once they have.
 *
 * It asks at the moment the button is pressed, when the reason is plain, not
 * at launch. A refusal is answered with one line saying what is off and a way
 * to the app's settings, which is the only place to change it once the system
 * stops showing its own question.
 */
@Composable
private fun rememberMyLocationAction(
    snackbars: SnackbarHostState,
    onAllowed: () -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val denied = stringResource(R.string.feature_home_location_denied)
    val openSettings = stringResource(R.string.feature_home_open_settings)

    val request =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                onAllowed()
            } else {
                scope.launch {
                    // With a button, a snackbar would otherwise stay until pressed.
                    val result =
                        snackbars.showSnackbar(denied, actionLabel = openSettings, duration = SnackbarDuration.Long)
                    if (result == SnackbarResult.ActionPerformed) {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                .setData(Uri.fromParts("package", context.packageName, null)),
                        )
                    }
                }
            }
        }

    return {
        // Whatever was said about the last press is out of date now.
        snackbars.currentSnackbarData?.dismiss()
        val allowed =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        if (allowed) onAllowed() else request.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }
}

/**
 * The app's main screen: a map, a search bar over it, and the forecast for
 * the selected place.
 *
 * On a phone the forecast is a bottom sheet that drags up over the map. From
 * Material's "expanded" window width up, it is a panel at the side.
 *
 * It holds no view model, so a test or a preview can draw it from plain
 * values, with its own [map].
 */
@Composable
fun HomeScreen(
    home: HomeState,
    search: SearchState,
    now: Instant,
    actions: HomeActions,
    map: MapSlot,
    snackbars: SnackbarHostState = remember { SnackbarHostState() },
) {
    val pin = home.place?.let { MapPin(it.latitude, it.longitude) }
    val searchBar: @Composable (Modifier) -> Unit = { modifier ->
        PlaceSearchBar(
            placeTitle = home.place?.title?.asString(),
            search = search,
            onOpened = actions.onSearchOpened,
            onQueryChange = actions.onQueryChange,
            onChoose = actions.onChooseResult,
            onRetry = actions.onRetrySearch,
            onClear = actions.onClearPlace,
            onOpenSavedPlaces = actions.onOpenSavedPlaces,
            modifier = modifier,
        )
    }
    val forecast: @Composable (Modifier, PaddingValues) -> Unit = { modifier, padding ->
        ForecastPanel(
            state = home,
            now = now,
            onUnitsChange = actions.onUnitsChange,
            onToggleSaved = actions.onToggleSaved,
            onRetry = actions.onRetryForecast,
            modifier = modifier,
            contentPadding = padding,
        )
    }

    val locationButton: @Composable (Modifier) -> Unit = { modifier ->
        MyLocationButton(home.locating, actions.onMyLocation, modifier)
    }
    val overlays = Overlays(searchBar, forecast, locationButton, snackbars)

    // Focusable, so that a window opened from a keyboard gives its first
    // focus to the screen and not to the search field, which opens when focused.
    BoxWithConstraints(Modifier.fillMaxSize().focusable()) {
        if (maxWidth >= Breakpoints.expanded) {
            WideLayout(pin, actions.onSelectPoint, map, overlays)
        } else {
            PhoneLayout(maxHeight, pin, actions.onSelectPoint, map, overlays)
        }
    }
}

/** What lies over the map, for a layout to place. */
private class Overlays(
    val searchBar: @Composable (Modifier) -> Unit,
    val forecast: @Composable (Modifier, PaddingValues) -> Unit,
    val locationButton: @Composable (Modifier) -> Unit,
    val snackbars: SnackbarHostState,
)

/** Shows the weather where the device is. While the device finds itself, it shows that instead. */
@Composable
private fun MyLocationButton(
    locating: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SmallFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.primary,
    ) {
        if (locating) {
            val description = stringResource(R.string.feature_home_locating)
            CircularProgressIndicator(
                Modifier.size(IconSizes.row).semantics { contentDescription = description },
                strokeWidth = progressStroke,
            )
        } else {
            Icon(Icons.Filled.MyLocation, contentDescription = stringResource(R.string.feature_home_my_location))
        }
    }
}

/** The forecast in a panel floating over the map's left side, under the search bar. */
@Composable
private fun WideLayout(
    pin: MapPin?,
    onLongPress: (Double, Double) -> Unit,
    map: MapSlot,
    overlays: Overlays,
) {
    map(pin, PaddingValues(start = sidePanelWidth + Spacing.lg * 2), onLongPress)
    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(Spacing.lg)) {
        overlays.locationButton(Modifier.align(Alignment.TopEnd))
        SnackbarHost(overlays.snackbars, Modifier.align(Alignment.BottomEnd))
    }
    Box(
        Modifier
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(Spacing.lg)
            .width(sidePanelWidth),
    ) {
        // The sheet's look, rounded on every corner since it floats clear of
        // the window's edges.
        Surface(
            modifier = Modifier.padding(top = searchBarHeight + Spacing.md).fillMaxSize(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shadowElevation = panelElevation,
        ) {
            overlays.forecast(Modifier, PaddingValues(Spacing.xl))
        }
        overlays.searchBar(Modifier.fillMaxWidth())
    }
}

/** The forecast in a sheet that rests over the bottom of the map and drags up. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhoneLayout(
    screenHeight: Dp,
    pin: MapPin?,
    onLongPress: (Double, Double) -> Unit,
    map: MapSlot,
    overlays: Overlays,
) {
    val sheetResting = screenHeight * SHEET_RESTING
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navigationBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val underSearchBar = statusBar + Spacing.lg + searchBarHeight + Spacing.md

    BottomSheetScaffold(
        sheetPeekHeight = sheetResting,
        sheetContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        sheetShadowElevation = panelElevation,
        snackbarHost = { SnackbarHost(overlays.snackbars) },
        sheetContent = {
            overlays.forecast(
                // Pulled right up, the sheet stops just under the search bar.
                Modifier.fillMaxWidth().height(screenHeight - underSearchBar - dragHandleHeight),
                // Clear of the system's gesture bar at the bottom of the screen.
                PaddingValues(start = Spacing.xl, end = Spacing.xl, bottom = Spacing.xl + navigationBar),
            )
        },
    ) {
        map(pin, PaddingValues(top = underSearchBar, bottom = sheetResting), onLongPress)
        // On the map, under the search bar, where the sheet never reaches.
        Box(Modifier.fillMaxSize().padding(top = underSearchBar, end = Spacing.lg)) {
            overlays.locationButton(Modifier.align(Alignment.TopEnd))
        }
    }
    overlays.searchBar(
        Modifier
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(Spacing.lg)
            .fillMaxWidth(),
    )
}

private val sidePanelWidth = 400.dp

/** Material 3's search bar height. */
private val searchBarHeight = 56.dp

private val panelElevation = 1.dp

private val progressStroke = 2.dp

/** Material 3's bottom sheet handle, with the space around it. */
private val dragHandleHeight = 48.dp

/** How much of the screen's height the bottom sheet takes at rest. */
private const val SHEET_RESTING = 0.3f

/** How often the forecast's age on screen is reckoned again. */
private val clockStep = 30.seconds
