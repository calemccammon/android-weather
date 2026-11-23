package com.calemccammon.androidweather.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.calemccammon.androidweather.core.designsystem.darkColors
import com.calemccammon.androidweather.core.designsystem.lightColors
import com.calemccammon.androidweather.core.ui.UiText
import com.calemccammon.androidweather.core.ui.asString
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

/**
 * A home-screen widget with the weather at the place the app shows.
 *
 * It is drawn with Glance, which turns composables into the remote views a
 * launcher can show. Pressing it opens the app.
 */
class WeatherWidget : GlanceAppWidget() {
    // The launcher is told both layouts and picks the one that fits.
    override val sizeMode = SizeMode.Responsive(setOf(compactSize, roomySize))

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        // A widget is made by the system, not by Hilt, so it asks for what it needs.
        val states =
            EntryPointAccessors
                .fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
                .observeWidgetState()()
        // Read before anything is drawn, so the widget never shows a wrong first frame.
        val first = states.first()
        val open = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let(::actionStartActivity)

        provideContent {
            val state by states.collectAsState(first)
            GlanceTheme(colors) { WidgetContent(state, open) }
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun observeWidgetState(): ObserveWidgetState
    }

    companion object {
        internal val compactSize = DpSize(110.dp, 40.dp)

        // A widget two cells by one is about this big on a phone.
        internal val roomySize = DpSize(160.dp, 90.dp)

        /** The app's own colours, light or dark as the system is. */
        private val colors = ColorProviders(light = lightColors, dark = darkColors)
    }
}

/** What the system sends widget events to. */
class WeatherWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeatherWidget()
}

@Composable
internal fun WidgetContent(
    state: WidgetState,
    onClick: Action?,
) {
    val resources = LocalContext.current.resources
    val roomy = LocalSize.current.height >= WeatherWidget.roomySize.height
    val frame =
        GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(cornerRadius)
            .padding(padding)
            .let { if (onClick == null) it else it.clickable(onClick) }

    when (state) {
        WidgetState.NoPlace ->
            Column(frame, verticalAlignment = Alignment.CenterVertically) {
                Words(uiText = null, text = resources.getString(R.string.widget_no_place), style = bodyStyle())
            }
        is WidgetState.Waiting ->
            Column(frame, verticalAlignment = Alignment.CenterVertically) {
                Words(state.place, style = titleStyle())
                Words(uiText = null, text = resources.getString(R.string.widget_waiting), style = bodyStyle())
            }
        is WidgetState.Weather -> {
            // Read out as one sentence.
            val spoken = state.spoken.asString(resources)
            val weather = frame.semantics { contentDescription = spoken }
            if (roomy) RoomyWeather(state, weather) else CompactWeather(state, weather)
        }
    }
}

/** One line: the temperature and the place. */
@Composable
private fun CompactWeather(
    state: WidgetState.Weather,
    modifier: GlanceModifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Words(state.temperature, style = temperatureStyle(compactTemperature))
        Spacer(GlanceModifier.width(gap))
        Words(state.place, style = titleStyle())
    }
}

/** The place, then the temperature large beside the condition and today's range. */
@Composable
private fun RoomyWeather(
    state: WidgetState.Weather,
    modifier: GlanceModifier,
) {
    Column(modifier, verticalAlignment = Alignment.CenterVertically) {
        Words(state.place, style = titleStyle())
        Row(verticalAlignment = Alignment.CenterVertically) {
            Words(state.temperature, style = temperatureStyle(roomyTemperature))
            Spacer(GlanceModifier.width(gap))
            Column {
                Words(state.condition, style = bodyStyle())
                state.range?.let { Words(it, style = bodyStyle()) }
            }
        }
    }
}

/** One line of text, cut short if the widget is too narrow for it. */
@Composable
private fun Words(
    uiText: UiText?,
    style: TextStyle,
    text: String = uiText?.asString(LocalContext.current.resources).orEmpty(),
) {
    Text(text, style = style, maxLines = 1)
}

@Composable
private fun titleStyle() =
    TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)

@Composable
private fun bodyStyle() = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp)

@Composable
private fun temperatureStyle(size: TextUnit) =
    TextStyle(color = GlanceTheme.colors.onSurface, fontSize = size, fontWeight = FontWeight.Medium)

private val cornerRadius = 24.dp
private val padding = 12.dp
private val gap = 8.dp
private val compactTemperature = 20.sp
private val roomyTemperature = 28.sp
