package app.k9mail.feature.funding.googleplay.ui.contribution

import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.funding.googleplay.ui.contribution.ContributionContract.Effect
import app.k9mail.feature.funding.googleplay.ui.contribution.ContributionContract.Event
import app.k9mail.feature.funding.googleplay.ui.contribution.ContributionContract.State
import app.k9mail.feature.funding.googleplay.ui.contribution.ContributionContract.ViewModel

internal class FakeContributionViewModel(
    initialState: State,
) : BaseViewModel<State, Event, Effect>(initialState = initialState), ViewModel {

    val events = mutableListOf<Event>()

    override fun event(event: Event) {
        events.add(event)
    }

    fun applyState(state: State) {
        updateState { state }
    }
}
