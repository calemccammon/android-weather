package com.calemccammon.androidweather.feature.home.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.ExpandedDockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.calemccammon.androidweather.core.designsystem.Spacing
import com.calemccammon.androidweather.core.ui.Message
import com.calemccammon.androidweather.core.ui.PlaceUiModel
import com.calemccammon.androidweather.core.ui.announced
import com.calemccammon.androidweather.core.ui.asString
import com.calemccammon.androidweather.feature.home.R
import com.calemccammon.androidweather.feature.home.model.SearchState
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import com.calemccammon.androidweather.core.ui.R as CoreR

/**
 * Material 3 search bar that opens into a list of matching places.
 *
 * Closed, it shows the selected place's name ([placeTitle]). Open, it holds
 * what is being typed, and draws [search] under itself. It does no searching
 * of its own: it reports that it opened, what was typed and which row was
 * chosen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceSearchBar(
    placeTitle: String?,
    search: SearchState,
    onOpened: () -> Unit,
    onQueryChange: (String) -> Unit,
    onChoose: (index: Int) -> Unit,
    onRetry: () -> Unit,
    onClear: () -> Unit,
    onOpenSavedPlaces: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bar = rememberSearchBarState()
    val scope = rememberCoroutineScope()
    // The closed bar shows the place's name; the open one, what is typed.
    val title = remember(placeTitle) { TextFieldState(placeTitle.orEmpty()) }
    val typed = rememberTextFieldState()

    ReportTyping(bar, typed, onOpened, onQueryChange)

    SearchBar(
        state = bar,
        modifier = modifier,
        inputField = {
            InputField(title, bar) { ClosedBarButtons(showClear = placeTitle != null, onClear, onOpenSavedPlaces) }
        },
    )
    // Drawn over the closed bar, in a window of its own, while open. A press
    // anywhere else closes it.
    ExpandedDockedSearchBar(
        state = bar,
        // The open bar is as wide as its field, up to the closed bar's width.
        inputField = { InputField(typed, bar, Modifier.fillMaxWidth()) },
    ) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            when (search) {
                is SearchState.Results ->
                    search.places.forEachIndexed { index, place ->
                        PlaceRow(
                            place,
                            onClick = {
                                onChoose(index)
                                scope.launch { bar.animateToCollapsed() }
                            },
                        )
                    }
                else -> SearchStatus(search, onRetry)
            }
        }
    }
}

/**
 * Tells the caller when the bar opens, and what is typed in it while open.
 * Opening starts from an empty search: the place's name is a label, not
 * something to search for.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportTyping(
    bar: SearchBarState,
    typed: TextFieldState,
    onOpened: () -> Unit,
    onQueryChange: (String) -> Unit,
) {
    val currentOnOpened by rememberUpdatedState(onOpened)
    val currentOnQueryChange by rememberUpdatedState(onQueryChange)

    LaunchedEffect(bar, typed) {
        snapshotFlow { bar.targetValue }.collect { target ->
            if (target == SearchBarValue.Expanded) {
                typed.clearText()
                currentOnOpened()
            }
        }
    }
    LaunchedEffect(typed) {
        // The first value is what the field holds before anyone types.
        snapshotFlow { typed.text.toString() }.drop(1).collect { currentOnQueryChange(it) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InputField(
    text: TextFieldState,
    bar: SearchBarState,
    modifier: Modifier = Modifier,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    SearchBarDefaults.InputField(
        textFieldState = text,
        searchBarState = bar,
        onSearch = {},
        modifier = modifier,
        placeholder = { Text(stringResource(R.string.feature_home_search_hint)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = trailingIcon,
    )
}

/** What the closed bar offers beside the place's name. */
@Composable
private fun ClosedBarButtons(
    showClear: Boolean,
    onClear: () -> Unit,
    onOpenSavedPlaces: () -> Unit,
) {
    Row {
        if (showClear) {
            IconButton(onClick = onClear) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.feature_home_clear_place))
            }
        }
        IconButton(onClick = onOpenSavedPlaces) {
            Icon(
                Icons.Filled.Bookmarks,
                contentDescription = stringResource(R.string.feature_home_open_saved_places),
            )
        }
    }
}

@Composable
private fun PlaceRow(
    place: PlaceUiModel,
    onClick: () -> Unit,
) {
    ListItem(
        onClick = onClick,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { Icon(Icons.Outlined.Place, contentDescription = null) },
        supportingContent = if (place.subtitle.isEmpty()) null else ({ Text(place.subtitle) }),
    ) {
        Text(place.title.asString())
    }
}

/**
 * What is happening when there are no results to show. A screen reader reads
 * it out as it changes: searching, then what came of it.
 */
@Composable
private fun SearchStatus(
    search: SearchState,
    onRetry: () -> Unit,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val padding = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)

    Column(Modifier.fillMaxWidth().announced()) {
        when (search) {
            SearchState.Idle ->
                Message(
                    Icons.Filled.TravelExplore,
                    stringResource(R.string.feature_home_search_prompt),
                    padding,
                    textColor = muted,
                )
            is SearchState.Loading -> {
                val searching = stringResource(R.string.feature_home_search_searching)
                LinearProgressIndicator(Modifier.fillMaxWidth().semantics { contentDescription = searching })
            }
            is SearchState.Empty ->
                Message(
                    Icons.Filled.SearchOff,
                    stringResource(R.string.feature_home_search_nothing_found, search.query),
                    padding,
                    textColor = muted,
                )
            is SearchState.Failed ->
                Message(Icons.Filled.CloudOff, search.message.asString(), padding, textColor = muted) {
                    TextButton(onClick = onRetry) { Text(stringResource(CoreR.string.core_ui_try_again)) }
                }
            is SearchState.Results -> Unit
        }
    }
}
