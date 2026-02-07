package net.thunderbird.feature.mail.message.list.domain.model

import androidx.compose.runtime.Stable

/**
 * Represents the current sorting criteria for a list of messages.
 *
 * Whenever the [primary] is either:
 * - [SortType.DateAsc]
 * - [SortType.DateDesc]
 * - [SortType.ArrivalAsc]
 * - [SortType.ArrivalDesc]
 * The [secondary] criteria must always be `null`. Otherwise, an [IllegalArgumentException] is thrown.
 *
 * Whenever the [primary] any other value present in the [SortType] enum, the secondary must be either:
 * - [SortType.DateAsc]
 * - [SortType.DateDesc]
 * Otherwise, an [IllegalArgumentException] is thrown.
 *
 * @param primary The primary sorting criterion.
 * @param secondary The secondary sorting criterion
 */
@Stable
data class SortCriteria(
    val primary: SortType,
    val secondary: SortType? = null,
) {
    init {
        // using require here would imply to either not have a proper error message or a wrong one.
        @Suppress("UseRequire")
        if (primary in SecondaryNotRequiredForSortTypes && secondary != null) {
            throw IllegalArgumentException("Secondary sorting criterion must be null for $primary")
        }
        @Suppress("UseRequire")
        if (primary !in SecondaryNotRequiredForSortTypes && secondary == null) {
            throw IllegalArgumentException("Secondary sorting criterion is missing for $primary")
        }
        @Suppress("UseRequire")
        if (primary !in SecondaryNotRequiredForSortTypes && secondary != null && secondary !in DateSortTypeOnly) {
            throw IllegalArgumentException("Secondary sorting criterion $secondary is not supported for $primary")
        }
    }

    val isSecondarySortRequired: Boolean = primary !in SecondaryNotRequiredForSortTypes

    companion object {
        val DateSortTypeOnly = setOf(SortType.DateAsc, SortType.DateDesc)

        /**
         * A set of [SortType] values for which a secondary sorting criterion is not required.
         * When the primary sort type is one of these, the secondary sort type must be `null`.
         */
        val SecondaryNotRequiredForSortTypes = DateSortTypeOnly + setOf(SortType.ArrivalAsc, SortType.ArrivalDesc)
    }
}
