package net.thunderbird.feature.funding.googleplay.ui.contribution.list

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.outcome.handle
import net.thunderbird.core.ui.contract.udf.BaseUnidirectionalSlice
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.UseCase
import net.thunderbird.feature.funding.googleplay.domain.entity.AvailableContributions
import net.thunderbird.feature.funding.googleplay.domain.entity.Contribution
import net.thunderbird.feature.funding.googleplay.domain.entity.ContributionId
import net.thunderbird.feature.funding.googleplay.ui.contribution.list.ContributionListSliceContract.ContributionType
import net.thunderbird.feature.funding.googleplay.ui.contribution.list.ContributionListSliceContract.Effect
import net.thunderbird.feature.funding.googleplay.ui.contribution.list.ContributionListSliceContract.Event
import net.thunderbird.feature.funding.googleplay.ui.contribution.list.ContributionListSliceContract.State

internal class ContributionListSlice(
    private val getAvailableContributions: UseCase.GetAvailableContributions,
    private val logger: Logger,
    private val scope: CoroutineScope,
    initialState: State = State(),
) : BaseUnidirectionalSlice<State, Event, Effect>(
    scope = scope,
    initialState = initialState,
),
    ContributionListSliceContract.Slice {

    init {
        loadContributions()
    }

    override fun event(event: Event) = when (event) {
        is Event.TypeClicked -> onTypeClicked(event.type)

        is Event.ItemClicked -> onItemClicked(event.contributionId)

        Event.RetryClicked -> onRetryClicked()
    }

    private fun onTypeClicked(type: ContributionType) {
        logger.debug { "Contribution type selected: $type" }

        val selectedContribution = selectContribution(
            contributions = state.value.contributions,
            type = type,
        )

        updateState { state ->
            state.copy(
                selectedType = type,
                selectedContribution = selectedContribution,
            )
        }

        emitEffect(
            Effect.SelectionChanged(contributionId = state.value.selectedContribution?.id),
        )
    }

    private fun onItemClicked(contributionId: ContributionId) {
        logger.debug { "Contribution item clicked: $contributionId" }

        updateState { state ->
            state.copy(
                selectedContribution = findContributionById(
                    contributions = state.contributions,
                    contributionId = contributionId,
                    contributionType = state.selectedType,
                ),
            )
        }

        emitEffect(
            Effect.SelectionChanged(contributionId = state.value.selectedContribution?.id),
        )
    }

    private fun onRetryClicked() {
        logger.debug { "Retrying to load contributions" }

        updateState { state ->
            state.copy(
                isLoading = true,
                error = null,
            )
        }

        loadContributions()
    }

    private fun loadContributions() {
        logger.debug { "Loading contributions" }

        scope.launch {
            getAvailableContributions().collect { outcome ->
                outcome.handle(
                    onSuccess = { contributions ->
                        logger.debug { "Contributions loaded successfully" }
                        updateState { state ->
                            state.onLoaded(contributions)
                        }
                        emitEffectSelectionChanged()
                    },
                    onFailure = { error ->
                        logger.error { "Failed to load contributions: ${error.message}" }
                        updateState { state ->
                            state.copy(
                                isLoading = false,
                                error = error,
                            )
                        }
                    },
                )
            }
        }
    }

    private fun State.onLoaded(contributions: AvailableContributions): State {
        val selectedContribution = selectContribution(
            contributions = contributions,
            type = selectedType,
        )

        return copy(
            contributions = contributions,
            selectedContribution = selectedContribution,
            isLoading = false,
            error = null,
        )
    }

    private fun emitEffectSelectionChanged() {
        emitEffect(
            Effect.SelectionChanged(contributionId = state.value.selectedContribution?.id),
        )
    }

    private fun selectContribution(
        contributions: AvailableContributions,
        type: ContributionType,
    ): Contribution? = when (type) {
        ContributionType.OneTime -> findContributionById(
            contributions = contributions,
            contributionId = contributions.preselection.oneTimeId,
            contributionType = type,
        )

        ContributionType.Recurring -> findContributionById(
            contributions = contributions,
            contributionId = contributions.preselection.recurringId,
            contributionType = type,
        )
    }

    private fun findContributionById(
        contributions: AvailableContributions,
        contributionId: ContributionId?,
        contributionType: ContributionType,
    ): Contribution? = when (contributionType) {
        ContributionType.OneTime -> {
            contributions.oneTimeContributions.firstOrNull { it.id == contributionId }
        }

        ContributionType.Recurring -> {
            contributions.recurringContributions.firstOrNull { it.id == contributionId }
        }
    }
}
