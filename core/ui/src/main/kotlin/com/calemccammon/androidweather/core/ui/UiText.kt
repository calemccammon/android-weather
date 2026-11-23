package com.calemccammon.androidweather.core.ui

import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.platform.LocalResources

/**
 * Wording the view models choose and the screen looks up.
 *
 * A view model outlives a change of language and holds no `Context`, so it
 * says which string it means and what goes in the gaps; the words are found
 * when the screen is drawn, and found again if the language changes.
 */
@Immutable
sealed interface UiText {
    /** Text that is the same in every language, such as a place's name. */
    data class Verbatim(
        val text: String,
    ) : UiText

    /**
     * A string from a module's `strings.xml`, with [arguments] for its gaps:
     * text, whole numbers, or more [UiText].
     */
    data class Resource(
        @param:StringRes val id: Int,
        val arguments: List<Any> = emptyList(),
    ) : UiText

    /** A string whose wording depends on [quantity], which is also its first argument. */
    data class Plural(
        @param:PluralsRes val id: Int,
        val quantity: Int,
    ) : UiText
}

fun uiText(
    @StringRes id: Int,
    vararg arguments: Any,
): UiText = UiText.Resource(id, arguments.toList())

/** The words, in the device's language. */
@Composable
fun UiText.asString(): String = asString(LocalResources.current)

/** The words, for code that draws without Compose: a notification, say. */
@Suppress("SpreadOperator") // The lookup takes its arguments one by one.
fun UiText.asString(resources: Resources): String =
    when (this) {
        is UiText.Verbatim -> text
        is UiText.Resource ->
            resources.getString(id, *arguments.map { if (it is UiText) it.asString(resources) else it }.toTypedArray())
        is UiText.Plural -> resources.getQuantityString(id, quantity, quantity)
    }
