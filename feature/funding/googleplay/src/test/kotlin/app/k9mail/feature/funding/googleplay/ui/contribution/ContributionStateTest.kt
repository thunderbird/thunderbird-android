package app.k9mail.feature.funding.googleplay.ui.contribution

import app.k9mail.feature.funding.googleplay.ui.contribution.ContributionContract.ContributionListState
import app.k9mail.feature.funding.googleplay.ui.contribution.ContributionContract.State
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.collections.immutable.persistentListOf

internal class ContributionStateTest {

    @Test
    fun `should set default values`() {
        val state = State()

        assertThat(state).isEqualTo(
            State(
                listState = ContributionListState(
                    recurringContributions = persistentListOf(),
                    oneTimeContributions = persistentListOf(),
                    selectedContribution = null,
                    isRecurringContributionSelected = false,
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
