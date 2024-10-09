package app.k9mail.feature.funding.googleplay.ui.contribution

import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.funding.googleplay.domain.entity.Contribution
import app.k9mail.feature.funding.googleplay.ui.contribution.ContributionContract.Event
import app.k9mail.feature.funding.googleplay.ui.contribution.ContributionContract.State
import app.k9mail.feature.funding.googleplay.ui.contribution.ContributionContract.ViewModel

internal class ContributionViewModel(
    initialState: State = State(),
) : BaseViewModel<State, Event, Nothing>(initialState),
    ViewModel {

    override fun event(event: Event) {
        when (event) {
            Event.OnOneTimeContributionSelected -> onOneTimeContributionSelected()
            Event.OnRecurringContributionSelected -> onRecurringContributionSelected()
            is Event.OnContributionItemClicked -> onContributionItemClicked(event.item)
            Event.OnPurchaseClicked -> onPurchaseClicked()
        }
    }

    private fun onOneTimeContributionSelected() {
        updateState {
            it.copy(
                isRecurringContributionSelected = false,
                selectedContribution = it.oneTimeContributions.firstOrNull(),
            )
        }
    }

    private fun onRecurringContributionSelected() {
        updateState {
            it.copy(
                isRecurringContributionSelected = true,
                selectedContribution = it.recurringContributions.firstOrNull(),
            )
        }
    }

    private fun onContributionItemClicked(item: Contribution) {
        updateState {
            it.copy(
                selectedContribution = item,
            )
        }
    }

    private fun onPurchaseClicked() {
        // TODO: Implement purchase logic
    }
}
