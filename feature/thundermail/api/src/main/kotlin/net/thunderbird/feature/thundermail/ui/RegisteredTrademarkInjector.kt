package net.thunderbird.feature.thundermail.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

private const val THUNDERBIRD_NAME = "Thunderbird"

/**
 * A utility object that scans a given string for occurrences of the brand name "Thunderbird"
 * and appends the registered trademark symbol (®) with specific superscript styling.
 *
 * It returns an [AnnotatedString] formatted for display in Jetpack Compose UI components.
 */
object RegisteredTrademarkInjector {
    /**
     * Injects a superscript registered trademark symbol (®) after every occurrence of
     * "Thunderbird" within the provided [text].
     *
     * @param text The text string to process.
     * @return An [AnnotatedString] with the trademark symbols styled as superscripts.
     */
    fun inject(text: String): AnnotatedString = buildAnnotatedString {
        val iterator = text.iterator()
        while (iterator.hasNext()) {
            when (val char = iterator.nextChar()) {
                'T' -> {
                    val current = buildString {
                        append(char)
                        repeat(THUNDERBIRD_NAME.length - 1) { append(iterator.nextChar()) }
                    }
                    append(current)
                    if (current == THUNDERBIRD_NAME) {
                        withStyle(
                            style = SpanStyle(
                                fontSize = 10.sp,
                                baselineShift = BaselineShift.Superscript,
                            ),
                        ) {
                            append("®")
                        }
                    }
                }

                else -> append(char)
            }
        }
    }
}
