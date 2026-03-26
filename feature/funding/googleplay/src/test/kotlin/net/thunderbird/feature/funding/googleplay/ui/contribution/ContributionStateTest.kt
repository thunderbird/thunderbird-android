package net.thunderbird.feature.funding.googleplay.ui.contribution

import assertk.assertThat
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test

class ContributionStateTest {

    @Test
    fun `should have default values`() {
        val state = ContributionContract.State()

        assertThat(state.selectedContributionId).isNull()
        assertThat(state.showContributionList).isTrue()
    }
}
