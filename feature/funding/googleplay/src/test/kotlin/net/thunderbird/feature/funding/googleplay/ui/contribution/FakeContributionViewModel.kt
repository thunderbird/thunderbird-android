package net.thunderbird.feature.funding.googleplay.ui.contribution

import app.k9mail.core.ui.compose.testing.BaseFakeViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.Effect
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.Event
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.State
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.ViewModel
import net.thunderbird.feature.funding.googleplay.ui.contribution.list.ContributionListSliceContract
import net.thunderbird.feature.funding.googleplay.ui.contribution.purchase.PurchaseSliceContract

internal class FakeContributionViewModel(
    initialState: State = State(),
) : BaseFakeViewModel<State, Event, Effect>(initialState), ViewModel {
    override val listState: StateFlow<ContributionListSliceContract.State> =
        MutableStateFlow(ContributionListSliceContract.State())

    override val purchaseState: StateFlow<PurchaseSliceContract.State> =
        MutableStateFlow(PurchaseSliceContract.State())
}
