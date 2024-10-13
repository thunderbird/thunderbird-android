package app.k9mail.feature.funding.googleplay.ui.contribution

import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.funding.googleplay.ui.contribution.ContributionContract.Event
import app.k9mail.feature.funding.googleplay.ui.contribution.ContributionContract.State
import app.k9mail.feature.funding.googleplay.ui.contribution.ContributionContract.ViewModel

internal class ContributionViewModel(
    initialState: State = State(),
) : BaseViewModel<State, Event, Nothing>(initialState),
    ViewModel {

    override fun event(event: Event) {
        when (event) {
            is Event.OnContributionClicked -> TODO()

            Event.OnOneTimeContributionClicked -> TODO()
            Event.OnPurchaseClicked -> {
                // TODO
            }

            Event.OnRecurringContributionClicked -> TODO()
        }
    }
}
