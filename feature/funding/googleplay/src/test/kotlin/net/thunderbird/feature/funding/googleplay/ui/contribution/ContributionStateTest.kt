package net.thunderbird.feature.funding.googleplay.ui.contribution

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.ContributionListState
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.State

internal class ContributionStateTest {

    @Test
    fun `should set default values`() {
        // Arrange
        val state = State()

        // Assert
        assertThat(state).isEqualTo(
            State(
                listState = ContributionListState(
                    recurringContributions = persistentListOf(),
                    oneTimeContributions = persistentListOf(),
                    selectedContribution = null,
                    isRecurringContributionSelected = true,
                    error = null,
                    isLoading = true,
                ),
                purchasedContribution = null,
                showContributionList = true,
                showRecurringContributions = false,
                purchaseError = null,
            ),
        )
    }
}
