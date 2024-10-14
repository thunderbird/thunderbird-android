package app.k9mail.feature.funding.googleplay.ui.contribution

import android.app.Activity
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
            is Event.OnPurchaseClicked -> onPurchaseClicked(event.activity)
            is Event.OnManagePurchaseClicked -> onManagePurchaseClicked(event.contribution)
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

    @Suppress("UnusedParameter")
    private fun onPurchaseClicked(activity: Activity) {
        // TODO: Implement purchase logic
    }

    @Suppress("UnusedParameter")
    private fun onManagePurchaseClicked(contribution: Contribution) {
        // TODO: Implement manage purchase logic
    }
}
