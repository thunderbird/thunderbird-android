package net.thunderbird.feature.funding.googleplay.ui.contribution

import kotlinx.coroutines.flow.StateFlow
import net.thunderbird.core.ui.contract.mvi.BaseViewModel
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.Effect
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.Event
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.State
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.ViewModel
import net.thunderbird.feature.funding.googleplay.ui.contribution.list.ContributionListSliceContract
import net.thunderbird.feature.funding.googleplay.ui.contribution.purchase.PurchaseSliceContract

internal class FakeContributionViewModel(
    initialState: State,
    override val listState: StateFlow<ContributionListSliceContract.State>,
    override val purchaseState: StateFlow<PurchaseSliceContract.State>,

) : BaseViewModel<State, Event, Effect>(initialState = initialState), ViewModel {

    val events = mutableListOf<Event>()

    override fun event(event: Event) {
        events.add(event)
    }

    fun applyState(state: State) {
        updateState { state }
    }
}
