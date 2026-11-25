package com.calemccammon.androidweather.feature.home.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.calemccammon.androidweather.core.designsystem.IconSizes
import com.calemccammon.androidweather.core.designsystem.Spacing
import com.calemccammon.androidweather.core.ui.Message
import com.calemccammon.androidweather.core.ui.PlaceUiModel
import com.calemccammon.androidweather.core.ui.announced
import com.calemccammon.androidweather.core.ui.asString
import com.calemccammon.androidweather.feature.home.R
import com.calemccammon.androidweather.feature.home.model.CurrentUiModel
import com.calemccammon.androidweather.feature.home.model.DayUiModel
import com.calemccammon.androidweather.feature.home.model.ForecastState
import com.calemccammon.androidweather.feature.home.model.HomeState
import com.calemccammon.androidweather.feature.home.model.UnitsUiModel
import com.calemccammon.androidweather.feature.home.model.updatedText
import java.time.Instant
import com.calemccammon.androidweather.core.ui.R as CoreR

/**
 * Current conditions and the week ahead for the selected place. It scrolls,
 * so it can live inside the bottom sheet or the side panel.
 *
 * [now] is the time a forecast's age is reckoned from.
 */
@Composable
fun ForecastPanel(
    state: HomeState,
    now: Instant,
    onUnitsChange: (UnitsUiModel) -> Unit,
    onToggleSaved: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(Spacing.xl),
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        state.place?.let { PlaceHeading(it, state.saved, onToggleSaved) }

        when (val forecast = state.forecast) {
            is ForecastState.Loaded -> Loaded(forecast, now, state.units, onUnitsChange, onRetry)
            else -> ForecastStatus(forecast, onRetry)
        }
    }
}

/** The place's name, and the button that keeps it or stops keeping it. */
@Composable
private fun PlaceHeading(
    place: PlaceUiModel,
    saved: Boolean,
    onToggleSaved: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                place.title.asString(),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
            if (place.subtitle.isNotEmpty()) {
                Text(
                    place.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconToggleButton(checked = saved, onCheckedChange = { onToggleSaved() }) {
            Icon(
                if (saved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                contentDescription =
                    stringResource(
                        if (saved) R.string.feature_home_remove_place else R.string.feature_home_save_place,
                    ),
            )
        }
    }
}

/**
 * What is happening when there is no forecast to show. A screen reader reads
 * it out as it changes: loading, then failed.
 */
@Composable
private fun ForecastStatus(
    forecast: ForecastState,
    onRetry: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().announced(), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        when (forecast) {
            ForecastState.Idle ->
                Message(
                    Icons.Outlined.TouchApp,
                    stringResource(R.string.feature_home_forecast_hint),
                    iconTint = MaterialTheme.colorScheme.primary,
                )
            ForecastState.Loading -> {
                val loading = stringResource(R.string.feature_home_forecast_loading)
                LinearProgressIndicator(
                    Modifier.fillMaxWidth().padding(vertical = Spacing.xl).semantics { contentDescription = loading },
                )
            }
            is ForecastState.Failed -> {
                Message(
                    Icons.Filled.CloudOff,
                    forecast.message.asString(),
                    iconTint = MaterialTheme.colorScheme.error,
                )
                TryAgainButton(onRetry)
            }
            is ForecastState.Loaded -> Unit
        }
    }
}

@Composable
private fun TryAgainButton(onRetry: () -> Unit) {
    FilledTonalButton(onClick = onRetry, contentPadding = ButtonDefaults.ButtonWithIconContentPadding) {
        Icon(Icons.Filled.Refresh, contentDescription = null, Modifier.size(ButtonDefaults.IconSize))
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text(stringResource(CoreR.string.core_ui_try_again))
    }
}

/**
 * What is being done about a forecast that may be old: a newer one is on its
 * way, or could not be fetched. A screen reader reads it out as it changes.
 */
@Composable
private fun RefreshStatus(
    state: ForecastState.Loaded,
    onRetry: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().announced(), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        if (state.refreshing) {
            val loading = stringResource(R.string.feature_home_forecast_loading)
            LinearProgressIndicator(Modifier.fillMaxWidth().semantics { contentDescription = loading })
        }
        state.problem?.let { problem ->
            Message(
                Icons.Filled.CloudOff,
                stringResource(R.string.feature_home_showing_saved_forecast, problem.asString()),
                iconTint = MaterialTheme.colorScheme.error,
            )
            TryAgainButton(onRetry)
        }
    }
}

@Composable
private fun Loaded(
    state: ForecastState.Loaded,
    now: Instant,
    units: UnitsUiModel,
    onUnitsChange: (UnitsUiModel) -> Unit,
    onRetry: () -> Unit,
) {
    val forecast = state.forecast
    if (state.refreshing || state.problem != null) RefreshStatus(state, onRetry)

    Current(forecast.current)

    // The units choice drops below the heading when the text is too large for
    // both on one line.
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.feature_home_forecast_this_week),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        UnitsChoice(units, onUnitsChange)
    }

    Week(forecast.days)

    Column {
        Text(
            updatedText(state.fetchedAt, now).asString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(R.string.feature_home_forecast_credit),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Current(current: CurrentUiModel) {
    val description = current.description.asString()
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    // Read out as one sentence. Wraps onto a second line when the text is too
    // large for one.
    FlowRow(
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            Icon(
                current.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(IconSizes.current),
            )
            Text(current.temperature.asString(), style = MaterialTheme.typography.displayMedium)
        }
        Column {
            Text(current.condition.asString(), style = MaterialTheme.typography.titleMedium)
            Text(current.feelsLike.asString(), style = MaterialTheme.typography.bodyMedium, color = muted)
            Text(current.wind.asString(), style = MaterialTheme.typography.bodyMedium, color = muted)
        }
    }
}

@Composable
private fun UnitsChoice(
    selected: UnitsUiModel,
    onChange: (UnitsUiModel) -> Unit,
) {
    val choices =
        listOf(
            Triple(UnitsUiModel.Celsius, R.string.feature_home_units_celsius, R.string.feature_home_units_celsius_name),
            Triple(
                UnitsUiModel.Fahrenheit,
                R.string.feature_home_units_fahrenheit,
                R.string.feature_home_units_fahrenheit_name,
            ),
        )

    SingleChoiceSegmentedButtonRow {
        choices.forEachIndexed { index, (units, label, name) ->
            val spoken = stringResource(name)
            SegmentedButton(
                selected = units == selected,
                onClick = { onChange(units) },
                shape = SegmentedButtonDefaults.itemShape(index, choices.size),
                icon = {},
                modifier = Modifier.semantics { contentDescription = spoken },
            ) {
                Text(stringResource(label))
            }
        }
    }
}

@Composable
private fun Week(days: List<DayUiModel>) {
    if (days.isEmpty()) {
        Text(stringResource(R.string.feature_home_forecast_no_days), style = MaterialTheme.typography.bodyMedium)
        return
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = Spacing.sm)) {
            days.forEach { DayRow(it) }
        }
    }
}

@Composable
private fun DayRow(day: DayUiModel) {
    val description = day.description.asString()
    val typography = MaterialTheme.typography
    val colors = MaterialTheme.colorScheme
    // Each column is as wide as the widest text it can hold, measured in the
    // style it is drawn in, so it grows with the user's text size and nothing
    // is cut off. (A width in sp would not keep up: since Android 14 large
    // sizes are scaled up less than small ones.)
    val measurer = rememberTextMeasurer()
    val today = stringResource(R.string.feature_home_today)
    val (dayWidth, chanceWidth, temperatureWidth) =
        with(LocalDensity.current) {
            fun width(
                text: String,
                style: TextStyle,
            ) = measurer
                .measure(text, style, maxLines = 1)
                .size.width
                .toDp() + COLUMN_SLACK

            Triple(
                width(today, typography.bodyLarge),
                width(WIDEST_CHANCE, typography.labelMedium),
                width(WIDEST_TEMPERATURE, typography.bodyLarge),
            )
        }
    val fixedWidth = dayWidth + IconSizes.row + chanceWidth + temperatureWidth * 2 + Spacing.sm + Spacing.md

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            // Read out as one sentence.
            .clearAndSetSemantics { contentDescription = description }
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
    ) {
        // Very large text, or a very narrow window: the numbers matter more
        // than the bar.
        val hasRoomForBar = maxWidth - fixedWidth >= minBarWidth

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(day.dayLabel.asString(), Modifier.width(dayWidth), style = typography.bodyLarge, maxLines = 1)
            Icon(day.icon, contentDescription = null, Modifier.size(IconSizes.row), tint = colors.primary)
            Text(
                day.chance?.asString().orEmpty(),
                Modifier.width(chanceWidth),
                style = typography.labelMedium,
                color = colors.tertiary,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Text(
                day.low.asString(),
                Modifier.width(temperatureWidth),
                style = typography.bodyLarge,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.End,
                maxLines = 1,
            )
            if (hasRoomForBar) {
                RangeBar(
                    start = day.barStart,
                    end = day.barEnd,
                    modifier = Modifier.weight(1f).padding(start = Spacing.sm, end = Spacing.md),
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            Text(
                day.high.asString(),
                Modifier.width(temperatureWidth),
                style = typography.bodyLarge,
                textAlign = TextAlign.End,
                maxLines = 1,
            )
        }
    }
}

/**
 * A track for the week's whole temperature range with one day's part of it
 * filled in, from [start] to [end] (each 0 to 1).
 */
@Composable
private fun RangeBar(
    start: Float,
    end: Float,
    modifier: Modifier = Modifier,
) {
    val track = MaterialTheme.colorScheme.surface
    val fill = MaterialTheme.colorScheme.primary

    Canvas(modifier.height(barThickness)) {
        val round = CornerRadius(size.height / 2)
        // Never thinner than the bar is tall, or a day whose low and high
        // match would vanish; and never pushed off the end of the track.
        val width = (size.width * (end - start)).coerceIn(minOf(size.height, size.width), size.width)
        val left = (size.width * start).coerceAtMost(size.width - width)

        drawRoundRect(track, cornerRadius = round)
        drawRoundRect(fill, topLeft = Offset(left, 0f), size = Size(width, size.height), cornerRadius = round)
    }
}

// The widest a chance of precipitation and a temperature can be.
private const val WIDEST_CHANCE = "100%"
private const val WIDEST_TEMPERATURE = "-88°"

/** Room between a column's text and the next column. */
private val COLUMN_SLACK: Dp = 4.dp

private val barThickness: Dp = 6.dp

/** The range bar is dropped when it would be shorter than this. */
private val minBarWidth: Dp = 24.dp
