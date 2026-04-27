package net.thunderbird.feature.funding.googleplay.ui.contribution

import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.StateFlow
import net.thunderbird.core.ui.contract.mvi.UnidirectionalViewModel
import net.thunderbird.feature.funding.googleplay.domain.entity.ContributionId
import net.thunderbird.feature.funding.googleplay.ui.contribution.list.ContributionListSliceContract
import net.thunderbird.feature.funding.googleplay.ui.contribution.purchase.PurchaseSliceContract

internal class ContributionContract {

    interface ViewModel : UnidirectionalViewModel<State, Event, Effect> {
        val listState: StateFlow<ContributionListSliceContract.State>
        val purchaseState: StateFlow<PurchaseSliceContract.State>
    }

    @Stable
    data class State(
        val selectedContributionId: ContributionId? = null,
        val showContributionList: Boolean = true,
    )

    sealed interface Event {
        data class List(val event: ContributionListSliceContract.Event) : Event
        data class Purchase(val event: PurchaseSliceContract.Event) : Event
        data object ShowContributionListClicked : Event
    }

    sealed interface Effect {
        data class ManageSubscription(val contributionId: ContributionId) : Effect
    }
}
