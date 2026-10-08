package net.thunderbird.feature.funding.googleplay.domain.entity

/**
 * Represents a preselection of contributions, which identifies specific contributions
 * to be highlighted or prioritized for one-time and recurring contribution types.
 *
 * @property oneTimeId The unique identifier for the preselected one-time contribution, if any.
 * @property recurringId The unique identifier for the preselected recurring contribution, if any.
 */
internal data class ContributionPreselection(
    val oneTimeId: ContributionId?,
    val recurringId: ContributionId?,
) {
    /**
     * Selects the contribution ID based on the contribution type.
     *
     * @param isRecurring A boolean flag indicating whether the desired contribution
     *        is recurring (`true`) or one-time (`false`).
     * @return The contribution ID associated with the specified contribution type.
     *         Returns `null` if no matching contribution is preselected.
     */
    fun select(isRecurring: Boolean): ContributionId? =
        if (isRecurring) recurringId else oneTimeId
}
