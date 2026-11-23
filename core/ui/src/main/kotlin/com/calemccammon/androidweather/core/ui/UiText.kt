package com.calemccammon.androidweather.core.ui

import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.platform.LocalResources
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

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
     * A string from a module's `strings.xml`, with [arguments] for its gaps.
     * Made with [uiText].
     */
    data class Resource(
        @param:StringRes val id: Int,
        val arguments: ImmutableList<Argument> = persistentListOf(),
    ) : UiText

    /** A string whose wording depends on [quantity], which is also its first argument. */
    data class Plural(
        @param:PluralsRes val id: Int,
        val quantity: Int,
    ) : UiText
}

/**
 * What can fill a gap in a string: text, a whole number, or more [UiText],
 * which is looked up in turn. A closed set, so that wording stays a plain
 * value two of which can be compared.
 */
@Immutable
sealed interface Argument {
    data class Word(
        val word: String,
    ) : Argument

    data class Number(
        val number: Int,
    ) : Argument

    data class Text(
        val text: UiText,
    ) : Argument
}

/** The string [id] with [arguments] for its gaps: text, whole numbers, or more [UiText]. */
fun uiText(
    @StringRes id: Int,
    vararg arguments: Any,
): UiText =
    UiText.Resource(
        id,
        arguments
            .map {
                when (it) {
                    is UiText -> Argument.Text(it)
                    is String -> Argument.Word(it)
                    is Int -> Argument.Number(it)
                    else -> throw IllegalArgumentException("A string's gap cannot be filled with ${it::class}")
                }
            }.toImmutableList(),
    )

/** The words, in the device's language. */
@Composable
fun UiText.asString(): String = asString(LocalResources.current)

/** The words, for code that draws without Compose: a notification, say. */
@Suppress("SpreadOperator") // The lookup takes its arguments one by one.
fun UiText.asString(resources: Resources): String =
    when (this) {
        is UiText.Verbatim -> text
        is UiText.Resource ->
            resources.getString(
                id,
                *arguments
                    .map<Argument, Any> {
                        when (it) {
                            is Argument.Word -> it.word
                            is Argument.Number -> it.number
                            is Argument.Text -> it.text.asString(resources)
                        }
                    }.toTypedArray(),
            )
        is UiText.Plural -> resources.getQuantityString(id, quantity, quantity)
    }
