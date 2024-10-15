package app.k9mail.feature.funding.googleplay.ui.contribution

import android.app.Activity
import androidx.lifecycle.viewModelScope
import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.funding.googleplay.domain.DomainContract
import app.k9mail.feature.funding.googleplay.domain.entity.Contribution
import app.k9mail.feature.funding.googleplay.ui.contribution.ContributionContract.Event
import app.k9mail.feature.funding.googleplay.ui.contribution.ContributionContract.State
import app.k9mail.feature.funding.googleplay.ui.contribution.ContributionContract.ViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions")
internal class ContributionViewModel(
    private val billingManager: DomainContract.BillingManager,
    initialState: State = State(),
) : BaseViewModel<State, Event, Nothing>(initialState),
    ViewModel {

    init {
        viewModelScope.launch {
            loadOneTimeContributions()
            loadRecurringContributions()
            loadPurchasedContribution()
            selectDefaultContribution()
        }
    }

    private suspend fun loadOneTimeContributions() {
        val result = billingManager.loadOneTimeContributions()

        updateState { state ->
            state.copy(
                oneTimeContributions = result.toImmutableList(),
                selectedContribution = if (
                    !state.isRecurringContributionSelected &&
                    result.contains(state.selectedContribution).not()
                ) {
                    result.firstOrNull()
                } else {
                    state.selectedContribution
                },
            )
        }
    }

    private suspend fun loadRecurringContributions() {
        val result = billingManager.loadRecurringContributions()

        updateState { state ->
            state.copy(
                recurringContributions = result.toImmutableList(),
                selectedContribution = if (
                    state.isRecurringContributionSelected &&
                    result.contains(state.selectedContribution).not()
                ) {
                    result.firstOrNull()
                } else {
                    state.selectedContribution
                },
            )
        }
    }

    private suspend fun loadPurchasedContribution() {
        val purchasedContribution = billingManager.loadPurchasedContributions().firstOrNull()
        updateState { state ->
            state.copy(
                purchasedContribution = purchasedContribution,
            )
        }
    }

    private fun selectDefaultContribution() {
        val selectedContribution = state.value.selectedContribution ?: return
        if (state.value.oneTimeContributions.contains(selectedContribution)) {
            onOneTimeContributionSelected()
        } else if (state.value.recurringContributions.contains(selectedContribution)) {
            onRecurringContributionSelected()
        }
    }

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
                selectedContribution = it.oneTimeContributions.getSecondLowestOrNull(),
            )
        }
    }

    private fun onRecurringContributionSelected() {
        updateState {
            it.copy(
                isRecurringContributionSelected = true,
                selectedContribution = it.recurringContributions.getSecondLowestOrNull(),
            )
        }
    }

    private fun List<Contribution>.getSecondLowestOrNull(): Contribution? {
        return when {
            this.size > 1 -> this.sortedBy { it.price }[1]
            this.size == 1 -> this[0]
            else -> null
        }
    }

    private fun onContributionItemClicked(item: Contribution) {
        updateState {
            it.copy(
                selectedContribution = item,
            )
        }
    }

    private fun onPurchaseClicked(activity: Activity) {
        viewModelScope.launch {
            val result = billingManager.purchaseContribution(activity, state.value.selectedContribution!!)

            if (result != null) {
                updateState {
                    it.copy(
                        purchasedContribution = result,
                    )
                }
            }
        }
    }

    @Suppress("UnusedParameter")
    private fun onManagePurchaseClicked(contribution: Contribution) {
        // TODO: Implement manage purchase logic
    }

    override fun onCleared() {
        super.onCleared()
        billingManager.clear()
    }
}
