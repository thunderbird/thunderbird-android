package net.thunderbird.feature.funding.googleplay.ui.contribution.purchase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.outcome.handle
import net.thunderbird.core.ui.contract.udf.BaseUnidirectionalSlice
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.UseCase
import net.thunderbird.feature.funding.googleplay.domain.entity.ContributionId
import net.thunderbird.feature.funding.googleplay.ui.contribution.purchase.PurchaseSliceContract.Effect
import net.thunderbird.feature.funding.googleplay.ui.contribution.purchase.PurchaseSliceContract.Event
import net.thunderbird.feature.funding.googleplay.ui.contribution.purchase.PurchaseSliceContract.PurchaseFlow
import net.thunderbird.feature.funding.googleplay.ui.contribution.purchase.PurchaseSliceContract.State

private const val PURCHASE_FLOW_DELAY_MS = 300L

internal class PurchaseSlice(
    private val getLastestPurchase: UseCase.GetLatestPurchasedContribution,
    private val repository: FundingDomainContract.ContributionRepository,
    private val logger: Logger,
    private val scope: CoroutineScope,
    initialState: State = State(),
) : BaseUnidirectionalSlice<State, Event, Effect>(
    scope = scope,
    initialState = initialState,
),
    PurchaseSliceContract.Slice {

    private var purchaseJob: Job? = null

    init {
        loadLatestPurchase()
    }

    override fun event(event: Event) = when (event) {
        is Event.PurchaseClicked -> onPurchaseClicked(event.contributionId)
        Event.CancelPurchaseClicked -> onCancelPurchaseClicked()
        Event.DismissPurchaseErrorClicked -> onDismissPurchaseErrorClicked()
        is Event.ManagePurchaseClicked -> emitManageSubscriptionEffect(event.contributionId)
        Event.RefreshPurchase -> onRefreshPurchase()
    }

    private fun onPurchaseClicked(contributionId: ContributionId) {
        logger.debug { "Purchase clicked for contribution: $contributionId" }

        updateState { state ->
            state.copy(
                purchaseFlow = PurchaseFlow.Launching(contributionId),
            )
        }

        purchaseJob?.cancel()
        purchaseJob = scope.launch {
            // Delay to allow UI to update to the launching state before starting the heavyweight purchase flow.
            delay(PURCHASE_FLOW_DELAY_MS)
            repository.purchaseContribution(contributionId).handle(
                onSuccess = {
                    logger.debug { "Purchase successfully launched for contribution: $contributionId" }
                    updateState { state ->
                        state.copy(
                            purchaseFlow = PurchaseFlow.Waiting(contributionId),
                        )
                    }
                },

                onFailure = { error ->
                    logger.error { "Purchase failed for contribution: $contributionId, error: ${error.message}" }
                    updateState { state ->
                        state.copy(
                            purchaseFlow = PurchaseFlow.Failed(contributionId, error),
                        )
                    }
                },
            )
        }
    }

    private fun onCancelPurchaseClicked() {
        logger.debug { "Cancel purchase clicked" }
        purchaseJob?.cancel()
        purchaseJob = null
        updateState { state ->
            state.copy(
                purchaseFlow = PurchaseFlow.Idle,
            )
        }
    }

    private fun onDismissPurchaseErrorClicked() {
        logger.debug { "Dismiss purchase error clicked" }
        updateState { state ->
            state.copy(
                purchaseFlow = PurchaseFlow.Idle,
            )
        }
    }

    private fun onRefreshPurchase() {
        logger.debug { "Refresh purchase triggered  " }
        if (state.value.purchaseFlow is PurchaseFlow.Waiting) {
            logger.debug { "Already waiting for purchase update, ignoring refresh" }
            loadLatestPurchase()
        }
    }

    private fun loadLatestPurchase() {
        logger.debug { "Loading purchased contribution" }
        scope.launch {
            getLastestPurchase().collect { outcome ->
                outcome.handle(
                    onSuccess = { contribution ->
                        if (contribution != null) {
                            logger.debug { "Latest purchased contribution: ${contribution.id.value}" }
                        } else {
                            logger.debug { "No purchased contribution found" }
                        }

                        updateState { state ->
                            state.copy(
                                purchasedContribution = contribution,
                                purchaseFlow = PurchaseFlow.Idle,
                            )
                        }

                        emitPurchasedEffect(contribution?.id)
                    },

                    onFailure = { error ->
                        if (error is FundingDomainContract.ContributionError.UserCancelled) {
                            logger.debug { "User cancelled the purchase flow" }
                            updateState { state ->
                                state.copy(
                                    purchaseFlow = PurchaseFlow.Idle,
                                )
                            }
                        } else {
                            logger.error { "Failed to load latest purchased contribution: ${error.message}" }
                            updateState { state ->
                                state.copy(
                                    purchaseFlow = PurchaseFlow.Failed(
                                        contributionId = state.getCurrentContributionId(),
                                        error = error,
                                    ),
                                )
                            }
                        }
                    },
                )
            }
        }
    }

    private fun emitManageSubscriptionEffect(contributionId: ContributionId) {
        logger.debug { "Manage subscription clicked for contribution: $contributionId" }
        emitEffect(Effect.ManageSubscription(contributionId))
    }

    private fun emitPurchasedEffect(contributionId: ContributionId?) {
        logger.debug { "Emitting purchased effect for contribution: $contributionId" }
        emitEffect(Effect.Purchased(contributionId))
    }

    private fun State.getCurrentContributionId(): ContributionId {
        return when (purchaseFlow) {
            is PurchaseFlow.Launching -> purchaseFlow.contributionId
            is PurchaseFlow.Waiting -> purchaseFlow.contributionId
            is PurchaseFlow.Failed -> purchaseFlow.contributionId
            PurchaseFlow.Idle -> ContributionId("unknown")
        }
    }
}
