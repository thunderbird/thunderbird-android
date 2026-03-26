package net.thunderbird.feature.funding.googleplay.ui.contribution

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.ui.contract.mvi.BaseViewModel
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.Effect
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.Event
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.State
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.ViewModel
import net.thunderbird.feature.funding.googleplay.ui.contribution.list.ContributionListSliceContract
import net.thunderbird.feature.funding.googleplay.ui.contribution.purchase.PurchaseSliceContract

@Suppress("TooManyFunctions")
internal class ContributionViewModel(
    listSliceFactory: ContributionListSliceContract.Slice.Factory,
    purchaseSliceFactory: PurchaseSliceContract.Slice.Factory,
    private val repository: FundingDomainContract.ContributionRepository,
    private val logger: Logger,
    initialState: State = State(),
) : BaseViewModel<State, Event, Effect>(initialState),
    ViewModel {

    private val listSlice = listSliceFactory.create(viewModelScope)
    private val purchaseSlice = purchaseSliceFactory.create(viewModelScope)

    override val listState = listSlice.state
    override val purchaseState = purchaseSlice.state

    init {
        observeListEffects()
        observePurchaseEffects()
    }

    override fun event(event: Event) {
        when (event) {
            is Event.List -> listSlice.event(event.event)
            is Event.Purchase -> purchaseSlice.event(event.event)
            Event.ShowContributionListClicked -> onShowContributionListClicked()
        }
    }

    private fun observeListEffects() {
        viewModelScope.launch {
            listSlice.effect.collect { effect ->
                when (effect) {
                    is ContributionListSliceContract.Effect.SelectionChanged -> {
                        logger.debug { "Contribution selection changed: ${effect.contributionId}" }
                        updateState { state ->
                            state.copy(
                                selectedContributionId = effect.contributionId,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun observePurchaseEffects() {
        viewModelScope.launch {
            purchaseSlice.effect.collect { effect ->
                when (effect) {
                    is PurchaseSliceContract.Effect.Purchased -> {
                        logger.debug { "Contribution purchased: ${effect.contributionId}" }
                        updateState { state ->
                            state.copy(
                                showContributionList = effect.contributionId == null,
                            )
                        }
                    }

                    is PurchaseSliceContract.Effect.ManageSubscription -> {
                        logger.debug {
                            "Manage subscription effect received for contribution: ${effect.contributionId}"
                        }
                        emitEffect(Effect.ManageSubscription(effect.contributionId))
                    }
                }
            }
        }
    }

    private fun onShowContributionListClicked() {
        updateState {
            it.copy(
                showContributionList = true,
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.clear()
    }
}
