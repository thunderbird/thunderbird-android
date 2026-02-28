package net.thunderbird.feature.mail.message.list.ui.state

/**
 * Represents the UI state for pagination functionality in a list.
 *
 * This data class tracks the current state of pagination operations, including whether
 * loading is in progress, whether the end of the paginated data has been reached, and
 * any error that may have occurred during pagination.
 *
 * @property phase The current phase of the pagination operation. Defaults to [Phase.Idle].
 * @property endReached Indicates whether the end of available data has been reached. When `true`,
 *  no more data can be loaded. Defaults to `false`.
 * @property error An error message if pagination failed, or `null` if no error occurred.
 *  Defaults to `null`.
 */
data class PaginationUi(
    val phase: Phase = Phase.Idle,
    val endReached: Boolean = false,
    val error: String? = null,
) {
    /**
     * Represents the operational phase of a pagination process.
     *
     * This enum defines the possible states of pagination loading operations within the UI.
     * It is used to track whether data is currently being fetched or if the system is waiting
     * for user interaction.
     */
    enum class Phase {
        /**
         * Represents the idle state of a pagination operation where no data is currently being loaded.
         *
         * In this phase, the system is waiting for user interaction or another trigger to begin
         * loading the next page of data. This is the default state before and after pagination
         * loading operations.
         */
        Idle,

        /**
         * Represents the loading state of a pagination operation where data is currently being fetched.
         *
         * In this phase, the system is actively retrieving the next page of data from the data source.
         * This state typically triggers loading indicators in the UI to inform users that content
         * is being loaded.
         */
        Loading,
    }
}
