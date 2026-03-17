package net.thunderbird.feature.funding.googleplay.ui.contribution

import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.outcome.handle
import net.thunderbird.core.ui.contract.mvi.BaseViewModel
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.UseCase
import net.thunderbird.feature.funding.googleplay.domain.entity.Contribution
import net.thunderbird.feature.funding.googleplay.domain.entity.ContributionId
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.ContributionType
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.Effect
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.Event
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.State
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.ViewModel

@Suppress("TooManyFunctions")
internal class ContributionViewModel(
    private val getAvailableContributions: UseCase.GetAvailableContributions,
    private val repository: FundingDomainContract.ContributionRepository,
    private val logger: Logger,
    initialState: State = State(),
) : BaseViewModel<State, Event, Effect>(initialState),
    ViewModel {

    private var purchaseJob: Job? = null

    init {
        viewModelScope.launch {
            loadAvailableContributions()
        }

        viewModelScope.launch {
            repository.purchasedContribution.collect { result ->
                result.handle(
                    onSuccess = { purchasedContribution ->
                        updateState { state ->
                            state.copy(
                                listState = state.listState.copy(
                                    isLoading = false,
                                ),
                                isPurchasing = false,
                                purchasedContribution = purchasedContribution,
                                showContributionList = purchasedContribution == null,
                                purchaseError = null,
                            )
                        }
                    },
                    onFailure = {
                        updateState { state ->
                            state.copy(
                                listState = state.listState.copy(
                                    isLoading = false,
                                ),
                                isPurchasing = false,
                                purchaseError = it.takeIf {
                                    it !is FundingDomainContract.ContributionError.UserCancelled
                                },
                            )
                        }
                    },
                )
            }
        }
    }

    private suspend fun loadAvailableContributions() {
        getAvailableContributions().collect { outcome ->
            outcome.handle(
                onSuccess = { data ->
                    updateState { state ->
                        val selectedContributionId = state.listState.selectedContributionId ?: data.preselection.select(
                            isRecurring = state.listState.isRecurringContributionSelected,
                        )
                        val isRecurringContributionSelected = selectedContributionId == data.preselection.recurringId

                        state.copy(
                            listState = state.listState.copy(
                                oneTimeContributions = data.oneTimeContributions.toImmutableList(),
                                recurringContributions = data.recurringContributions.toImmutableList(),
                                preselection = data.preselection,
                                selectedContributionId = selectedContributionId,
                                isRecurringContributionSelected = isRecurringContributionSelected,
                                isLoading = false,
                            ),
                            purchasedContribution = data.purchasedContribution ?: state.purchasedContribution,
                            showContributionList =
                            data.purchasedContribution == null && state.purchasedContribution == null,
                            purchaseError = null,
                        )
                    }
                },
                onFailure = {
                    updateState { state ->
                        state.copy(
                            listState = state.listState.copy(
                                isLoading = false,
                                error = it,
                            ),
                        )
                    }
                },
            )
        }
    }

    override fun event(event: Event) {
        when (event) {
            is Event.OnContributionTypeSelected -> onContributionTypeSelected(event.type)
            is Event.OnContributionItemClicked -> onContributionItemClicked(event.contributionId)
            is Event.OnPurchaseClicked -> onPurchaseClicked()
            Event.OnCancelPurchaseClicked -> onCancelPurchaseClicked()
            is Event.OnManagePurchaseClicked -> onManagePurchaseClicked(event.contribution)
            Event.OnShowContributionListClicked -> onShowContributionListClicked()
            Event.OnDismissPurchaseErrorClicked -> updateState {
                it.copy(
                    purchaseError = null,
                )
            }

            Event.OnRetryClicked -> onRetryClicked()
        }
    }

    private fun onContributionTypeSelected(type: ContributionType) {
        when (type) {
            ContributionType.OneTime -> onOneTimeContributionSelected()
            ContributionType.Recurring -> onRecurringContributionSelected()
        }
    }

    private fun onOneTimeContributionSelected() {
        updateState {
            it.copy(
                listState = it.listState.copy(
                    isRecurringContributionSelected = false,
                    selectedContributionId = it.listState.preselection.oneTimeId,
                ),
                showContributionList = true,
            )
        }
    }

    private fun onRecurringContributionSelected() {
        updateState {
            it.copy(
                listState = it.listState.copy(
                    isRecurringContributionSelected = true,
                    selectedContributionId = it.listState.preselection.recurringId,
                ),
                showContributionList = true,
            )
        }
    }

    private fun onContributionItemClicked(contributionId: ContributionId) {
        updateState {
            it.copy(
                listState = it.listState.copy(
                    selectedContributionId = contributionId,
                ),
            )
        }
    }

    private fun onPurchaseClicked() {
        val selectedContributionId = state.value.listState.selectedContributionId ?: return

        updateState {
            it.copy(
                isPurchasing = true,
            )
        }
        emitEffect(
            Effect.PurchaseContribution(
                startPurchaseFlow = {
                    purchaseJob?.cancel()
                    purchaseJob = viewModelScope.launch {
                        repository.purchaseContribution(selectedContributionId).handle(
                            onSuccess = {
                                logger.debug { "Purchase flow successfully launched" }
                                // we need to wait for the callback to be called
                            },
                            onFailure = { error ->
                                logger.error { "Purchase failed: $error" }
                                updateState { state ->
                                    state.copy(
                                        isPurchasing = false,
                                        purchaseError = error,
                                    )
                                }
                            },
                        )
                    }
                },
            ),
        )
    }

    private fun onCancelPurchaseClicked() {
        purchaseJob?.cancel()
        purchaseJob = null
        updateState {
            it.copy(
                isPurchasing = false,
            )
        }
    }

    private fun onManagePurchaseClicked(contribution: Contribution) {
        emitEffect(Effect.ManageSubscription(contribution.id.value))
    }

    private fun onShowContributionListClicked() {
        updateState {
            it.copy(
                showContributionList = true,
            )
        }
    }

    private fun onRetryClicked() {
        updateState {
            it.copy(
                listState = it.listState.copy(
                    isLoading = true,
                    error = null,
                ),
            )
        }
        viewModelScope.launch {
            loadAvailableContributions()
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.clear()
    }
}
