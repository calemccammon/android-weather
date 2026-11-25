package com.calemccammon.androidweather.feature.places

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calemccammon.androidweather.core.designsystem.IconSizes
import com.calemccammon.androidweather.core.designsystem.Spacing
import com.calemccammon.androidweather.core.ui.Message
import com.calemccammon.androidweather.core.ui.asString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

/** The saved places screen wired to its view model. */
@Composable
fun PlacesRoute(
    onBack: () -> Unit,
    viewModel: PlacesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbars = remember { SnackbarHostState() }
    val canNotify = rememberCanNotify()

    PlacesScreen(
        // Alerts chosen in the app count for nothing once notifications are
        // turned off in the system's settings, and the button says so.
        state = state.copy(alertsEnabled = state.alertsEnabled && canNotify),
        snackbars = snackbars,
        onAlertsChange = rememberAlertsAction(snackbars, viewModel::setAlertsEnabled),
        events = viewModel.events,
        onOpen = { id ->
            viewModel.open(id)
            onBack()
        },
        onRemove = viewModel::remove,
        onUndoRemove = viewModel::undoRemove,
        onRefresh = viewModel::refresh,
        onBack = onBack,
    )
}

/** Whether the system will show this app's notifications, looked at again each time the screen returns. */
@Composable
private fun rememberCanNotify(): Boolean {
    val context = LocalContext.current
    var canNotify by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    LifecycleResumeEffect(context) {
        canNotify = NotificationManagerCompat.from(context).areNotificationsEnabled()
        onPauseOrDispose {}
    }
    return canNotify
}

/**
 * What pressing the alerts button does. Turning alerts on asks the user to
 * allow notifications first, where the system requires asking (Android 13
 * and up); alerts are only turned on once they are allowed. A refusal is
 * answered with one line and a way to the app's notification settings.
 */
@Composable
private fun rememberAlertsAction(
    snackbars: SnackbarHostState,
    onChange: (Boolean) -> Unit,
): (Boolean) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val turnedOn = stringResource(R.string.feature_places_alerts_on)
    val turnedOff = stringResource(R.string.feature_places_alerts_off)
    val denied = stringResource(R.string.feature_places_alerts_denied)
    val openSettings = stringResource(R.string.feature_places_open_settings)

    fun say(message: String) {
        scope.launch {
            snackbars.currentSnackbarData?.dismiss()
            snackbars.showSnackbar(message)
        }
    }

    fun refused() {
        scope.launch {
            snackbars.currentSnackbarData?.dismiss()
            val result = snackbars.showSnackbar(denied, actionLabel = openSettings, duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed) {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                )
            }
        }
    }

    fun turnOnIfAllowed() {
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            onChange(true)
            say(turnedOn)
        } else {
            refused()
        }
    }

    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { turnOnIfAllowed() }

    return { enable ->
        val mustAsk =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
        when {
            !enable -> {
                onChange(false)
                say(turnedOff)
            }
            mustAsk -> request.launch(Manifest.permission.POST_NOTIFICATIONS)
            else -> turnOnIfAllowed()
        }
    }
}

/**
 * The places the user has kept, each with the weather the device holds for
 * it. Pressing one shows it on the home screen; pulling down fetches them all.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacesScreen(
    state: PlacesState,
    onOpen: (id: Long) -> Unit,
    onRemove: (id: Long) -> Unit,
    onUndoRemove: (removal: Long) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    onAlertsChange: (Boolean) -> Unit = {},
    events: Flow<PlacesEvent> = emptyFlow(),
    snackbars: SnackbarHostState = remember { SnackbarHostState() },
) {
    Snackbars(events, snackbars, onUndoRemove)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feature_places_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.feature_places_back),
                        )
                    }
                },
                actions = { AlertsButton(state.alertsEnabled, onAlertsChange) },
            )
        },
        snackbarHost = { SnackbarHost(snackbars) },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            // A list even when empty, so there is something to pull on.
            LazyColumn(Modifier.fillMaxSize()) {
                if (state.ready && state.places.isEmpty()) {
                    item {
                        Message(
                            Icons.Filled.BookmarkBorder,
                            stringResource(R.string.feature_places_empty),
                            Modifier.padding(Spacing.xl),
                            iconTint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                items(state.places, key = { it.id }) { place ->
                    PlaceRow(
                        place,
                        onOpen = { onOpen(place.id) },
                        onRemove = { onRemove(place.id) },
                        Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

/** A bell that shows whether alerts are on, and turns them on or off. */
@Composable
private fun AlertsButton(
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    IconToggleButton(checked = enabled, onCheckedChange = onChange) {
        Icon(
            if (enabled) Icons.Filled.NotificationsActive else Icons.Outlined.NotificationsNone,
            contentDescription =
                stringResource(
                    if (enabled) R.string.feature_places_alerts_turn_off else R.string.feature_places_alerts_turn_on,
                ),
        )
    }
}

/** Tells each event in a snackbar, one after another. */
@Composable
private fun Snackbars(
    events: Flow<PlacesEvent>,
    snackbars: SnackbarHostState,
    onUndoRemove: (removal: Long) -> Unit,
) {
    val resources = LocalResources.current
    val currentOnUndoRemove by rememberUpdatedState(onUndoRemove)

    LaunchedEffect(events, snackbars) {
        events.collect { event ->
            when (event) {
                is PlacesEvent.Removed -> {
                    val result =
                        snackbars.showSnackbar(
                            message =
                                resources.getString(R.string.feature_places_removed, event.title.asString(resources)),
                            actionLabel = resources.getString(R.string.feature_places_undo),
                            duration = SnackbarDuration.Short,
                        )
                    if (result == SnackbarResult.ActionPerformed) currentOnUndoRemove(event.removal)
                }
                is PlacesEvent.RefreshFailed ->
                    snackbars.showSnackbar(
                        resources.getString(R.string.feature_places_refresh_failed, event.reason.asString(resources)),
                    )
            }
        }
    }
}

@Composable
private fun PlaceRow(
    place: SavedPlaceUiModel,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = place.place.title.asString()
    // Read out with where it is, so two places of the same name can be told
    // apart by ear as they are by eye.
    val spokenName = listOf(title, place.place.subtitle).filter { it.isNotEmpty() }.joinToString(", ")
    val weather = place.weather
    // The place and its weather are read out as one sentence; the remove
    // button stays a button of its own.
    val description =
        if (weather == null) {
            spokenName
        } else {
            stringResource(
                R.string.feature_places_row_description,
                spokenName,
                weather.condition.asString(),
                weather.spokenTemperature.asString(),
            )
        }

    ListItem(
        onClick = onOpen,
        modifier = modifier,
        leadingContent = {
            if (weather != null) {
                Icon(
                    weather.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(IconSizes.row),
                )
            }
        },
        supportingContent =
            if (place.place.subtitle.isEmpty()) {
                null
            } else {
                { Text(place.place.subtitle, Modifier.clearAndSetSemantics {}) }
            },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (weather != null) {
                    Text(
                        weather.temperature.asString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.clearAndSetSemantics {},
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.feature_places_remove, spokenName),
                    )
                }
            }
        },
    ) {
        Text(title, Modifier.clearAndSetSemantics { contentDescription = description })
    }
}
