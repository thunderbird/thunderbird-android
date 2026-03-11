package net.thunderbird.feature.funding.googleplay.ui.contribution

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.ContributionListState

class ContributionListStateTest {

    @Test
    fun `should have default values`() {
        val state = ContributionListState()

        assertThat(state.oneTimeContributions).isEqualTo(persistentListOf())
        assertThat(state.recurringContributions).isEqualTo(persistentListOf())
        assertThat(state.selectedContributionId).isNull()
        assertThat(state.isRecurringContributionSelected).isTrue()
        assertThat(state.error).isNull()
        assertThat(state.isLoading).isTrue()
    }
}
