package net.thunderbird.feature.funding.googleplay.ui.contribution.list

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test
import net.thunderbird.feature.funding.googleplay.domain.entity.AvailableContributions

class ContributionListSliceStateTest {

    @Test
    fun `should have default values`() {
        val state = ContributionListSliceContract.State()

        assertThat(state.contributions).isEqualTo(AvailableContributions.Empty)

        assertThat(state.selectedType).isEqualTo(ContributionListSliceContract.ContributionType.Recurring)
        assertThat(state.selectedContribution).isNull()
        assertThat(state.error).isNull()
        assertThat(state.isLoading).isTrue()
    }
}
