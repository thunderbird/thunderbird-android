package net.thunderbird.feature.funding.googleplay.ui.contribution.purchase

import androidx.compose.runtime.Stable
import kotlinx.coroutines.CoroutineScope
import net.thunderbird.core.ui.contract.udf.UnidirectionalSlice
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.ContributionError
import net.thunderbird.feature.funding.googleplay.domain.entity.ContributionId
import net.thunderbird.feature.funding.googleplay.domain.entity.PurchasedContribution

internal class PurchaseSliceContract {

    interface Slice : UnidirectionalSlice<State, Event, Effect> {
        interface Factory {
            fun create(scope: CoroutineScope): Slice
        }
    }

    @Stable
    data class State(
        val purchasedContribution: PurchasedContribution? = null,
        val purchaseFlow: PurchaseFlow = PurchaseFlow.Idle,
    )

    sealed interface PurchaseFlow {
        data object Idle : PurchaseFlow

        data class Launching(val contributionId: ContributionId) : PurchaseFlow

        data class Waiting(val contributionId: ContributionId) : PurchaseFlow

        data class Failed(val contributionId: ContributionId, val error: ContributionError) : PurchaseFlow
    }

    sealed interface Event {
        data class PurchaseClicked(val contributionId: ContributionId) : Event
        data object CancelPurchaseClicked : Event
        data object DismissPurchaseErrorClicked : Event
        data class ManagePurchaseClicked(val contributionId: ContributionId) : Event
        data object RefreshPurchase : Event
    }

    sealed interface Effect {
        data class Purchased(val contributionId: ContributionId?) : Effect
        data class ManageSubscription(val contributionId: ContributionId) : Effect
    }
}
