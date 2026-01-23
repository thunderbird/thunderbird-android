package net.thunderbird.feature.funding.googleplay.ui.contribution

import androidx.lifecycle.viewModelScope
import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import net.thunderbird.core.outcome.handle
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.UseCase
import net.thunderbird.feature.funding.googleplay.domain.entity.AvailableContributions
import net.thunderbird.feature.funding.googleplay.domain.entity.Contribution
import net.thunderbird.feature.funding.googleplay.domain.entity.RecurringContribution
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.Effect
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.Event
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.State
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.ViewModel

@Suppress("TooManyFunctions")
internal class ContributionViewModel(
    private val getAvailableContributions: UseCase.GetAvailableContributions,
    private val contributionManager: FundingDomainContract.ContributionManager,
    initialState: State = State(),
) : BaseViewModel<State, Event, Effect>(initialState),
    ViewModel {

    init {
        viewModelScope.launch {
            loadAvailableContributions()
        }

        viewModelScope.launch {
            contributionManager.purchasedContribution.collect { result ->
                result.handle(
                    onSuccess = { purchasedContribution ->
                        updateState { state ->
                            state.copy(
                                listState = state.listState.copy(
                                    isLoading = false,
                                ),
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
                                purchasedContribution = null,
                                showContributionList = true,
                                purchaseError = it,
                            )
                        }
                    },
                )
            }
        }
    }

    private suspend fun loadAvailableContributions() {
        getAvailableContributions().handle(
            onSuccess = { data ->
                updateState { state ->
                    val selectedContribution = selectContribution(data)

                    state.copy(
                        listState = state.listState.copy(
                            oneTimeContributions = data.oneTimeContributions.toImmutableList(),
                            recurringContributions = data.recurringContributions.toImmutableList(),
                            selectedContribution = selectedContribution,
                            isRecurringContributionSelected = selectedContribution is RecurringContribution,
                            isLoading = false,
                        ),
                        purchasedContribution = data.purchasedContribution,
                        showContributionList = data.purchasedContribution == null,
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

    private fun selectContribution(data: AvailableContributions): Contribution? {
        val hasSelectedContribution = state.value.listState.selectedContribution != null &&
            (
                data.oneTimeContributions.contains(state.value.listState.selectedContribution) ||
                    data.recurringContributions.contains(state.value.listState.selectedContribution)
                )

        return if (hasSelectedContribution) {
            state.value.listState.selectedContribution
        } else {
            if (state.value.listState.isRecurringContributionSelected) {
                data.recurringContributions.getSecondLowestOrNull()
            } else {
                data.oneTimeContributions.getSecondLowestOrNull()
            }
        }
    }

    override fun event(event: Event) {
        when (event) {
            Event.OnOneTimeContributionSelected -> onOneTimeContributionSelected()
            Event.OnRecurringContributionSelected -> onRecurringContributionSelected()
            is Event.OnContributionItemClicked -> onContributionItemClicked(event.item)
            is Event.OnPurchaseClicked -> onPurchaseClicked()
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

    private fun onOneTimeContributionSelected() {
        updateState {
            it.copy(
                listState = it.listState.copy(
                    isRecurringContributionSelected = false,
                    selectedContribution = it.listState.oneTimeContributions.getSecondLowestOrNull(),
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
                    selectedContribution = it.listState.recurringContributions.getSecondLowestOrNull(),
                ),
                showContributionList = true,
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
                it.listState.copy(
                    selectedContribution = item,
                ),
            )
        }
    }

    private fun onPurchaseClicked() {
        val selectedContribution = state.value.listState.selectedContribution ?: return

        updateState {
            it.copy(
                listState = it.listState.copy(
                    isLoading = true,
                ),
            )
        }
        emitEffect(
            Effect.PurchaseContribution(
                startPurchaseFlow = { activity ->
                    viewModelScope.launch {
                        contributionManager.purchaseContribution(activity, selectedContribution).handle(
                            onSuccess = {
                                // we need to wait for the callback to be called
                            },
                            onFailure = { error ->
                                updateState { state ->
                                    state.copy(
                                        listState = state.listState.copy(
                                            isLoading = false,
                                        ),
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

    private fun onManagePurchaseClicked(contribution: Contribution) {
        emitEffect(Effect.ManageSubscription(contribution.id))
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
        contributionManager.clear()
    }
}
