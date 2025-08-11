package net.thunderbird.core.ui.compose.common.date

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames

/**
 * Configuration for date and time formatting.
 *
 * @property monthNames The names of the months.
 * @property dayOfWeekNames The names of the days of the week.
 */
data class DateTimeConfiguration(
    val monthNames: MonthNames,
    val dayOfWeekNames: DayOfWeekNames,
)

/**
 * CompositionLocal that provides the default [DateTimeConfiguration] for date and time formatting.
 * This configuration uses abbreviated English month and day of the week names by default.
 * It can be overridden at a lower level in the composition tree to customize date and time formatting.
 */
val LocalDateTimeConfiguration = staticCompositionLocalOf {
    DateTimeConfiguration(
        monthNames = MonthNames.ENGLISH_ABBREVIATED,
        dayOfWeekNames = DayOfWeekNames.ENGLISH_ABBREVIATED,
    )
}
