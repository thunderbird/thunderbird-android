package net.thunderbird.feature.funding.googleplay.ui.contribution.purchase

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test

class PurchaseSliceStateTest {

    @Test
    fun `should have default values`() {
        val state = PurchaseSliceContract.State()

        assertThat(state.purchasedContribution).isNull()
        assertThat(state.purchaseFlow).isEqualTo(PurchaseSliceContract.PurchaseFlow.Idle)
    }
}
