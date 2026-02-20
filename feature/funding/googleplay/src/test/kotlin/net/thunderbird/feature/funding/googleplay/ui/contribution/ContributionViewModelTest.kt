package net.thunderbird.feature.funding.googleplay.ui.contribution

import app.k9mail.core.ui.compose.testing.mvi.MviContext
import app.k9mail.core.ui.compose.testing.mvi.MviTurbines
import app.k9mail.core.ui.compose.testing.mvi.runMviTest
import app.k9mail.core.ui.compose.testing.mvi.turbinesWithInitialStateCheck
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.flow.flowOf
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.testing.coroutines.MainDispatcherRule
import net.thunderbird.feature.funding.googleplay.domain.entity.AvailableContributions
import net.thunderbird.feature.funding.googleplay.domain.entity.Contribution
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.ContributionListState
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.Effect
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.Event
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.State
import org.junit.Rule

class ContributionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `should change selected contribution and selected type when one time contribution selected`() = runMviTest {
        // Arrange
        val initialState = State(
            listState = ContributionListState(
                oneTimeContributions = FakeData.oneTimeContributions,
                recurringContributions = FakeData.recurringContributions,
                selectedContribution = FakeData.recurringContributions[FakeData.recurringContributions.size - 2],
                isRecurringContributionSelected = true,
                isLoading = false,
            ),
            purchasedContribution = null,
            showContributionList = true,
        )

        contributionRobot(initialState) {
            // Act
            selectOneTimeContribution()

            // Assert
            verifyOneTimeContributionSelected()
        }
    }

    @Test
    fun `should change selected contribution and selected type when recurring contribution selected`() = runMviTest {
        // Arrange
        val initialState = State(
            listState = ContributionListState(
                oneTimeContributions = FakeData.oneTimeContributions,
                recurringContributions = FakeData.recurringContributions,
                selectedContribution = FakeData.oneTimeContributions[FakeData.oneTimeContributions.size - 2],
                isRecurringContributionSelected = false,
                isLoading = false,
            ),
            purchasedContribution = null,
            showContributionList = true,
        )

        contributionRobot(initialState) {
            // Act
            selectRecurringContribution()

            // Assert
            verifyRecurringContributionSelected()
        }
    }

    @Test
    fun `should change selected contribution when contribution item clicked`() = runMviTest {
        // Arrange
        val initialState = State(
            listState = ContributionListState(
                oneTimeContributions = FakeData.oneTimeContributions,
                recurringContributions = FakeData.recurringContributions,
                selectedContribution = FakeData.recurringContributions[FakeData.recurringContributions.size - 2],
                isRecurringContributionSelected = true,
                isLoading = false,
            ),
            purchasedContribution = null,
            showContributionList = true,
        )
        val selectedContribution = FakeData.recurringContributions[2]

        contributionRobot(initialState) {
            // Act
            selectContributionItem(selectedContribution)

            // Assert
            verifyContributionItemSelected(selectedContribution)
        }
    }

    @Test
    fun `should hide list when purchase successful`() = runMviTest {
        // Arrange
        val repository = FakeContributionRepository()
        val contribution = FakeData.oneTimeContributions[0]
        val initialState = State(
            listState = ContributionListState(
                oneTimeContributions = FakeData.oneTimeContributions,
                recurringContributions = FakeData.recurringContributions,
                selectedContribution = FakeData.oneTimeContributions[0],
                isRecurringContributionSelected = false,
                isLoading = false,
            ),
            purchasedContribution = null,
            showContributionList = true,
        )

        contributionRobot(initialState = initialState, repository = repository) {
            // Act
            repository.purchasedContribution.value = Outcome.success(contribution)

            // Assert
            verifyListHidden(contribution)
        }
    }
}

private suspend fun MviContext.contributionRobot(
    initialState: State = State(),
    repository: FakeContributionRepository = FakeContributionRepository(),
    interaction: suspend ContributionRobot.() -> Unit,
) = ContributionRobot(this, initialState, repository).apply {
    initialize()
    interaction()
}

private class ContributionRobot(
    private val mviContext: MviContext,
    private val initialState: State = State(),
    private val repository: FakeContributionRepository = FakeContributionRepository(),
) {
    // FIX use case
    private val viewModel: ContributionContract.ViewModel = ContributionViewModel(
        getAvailableContributions = {
            flowOf(
                Outcome.success(
                    AvailableContributions(
                        oneTimeContributions = FakeData.oneTimeContributions,
                        recurringContributions = FakeData.recurringContributions,
                        purchasedContribution = null,
                    ),
                ),
            )
        },
        repository = repository,
        initialState = initialState,
    )
    private lateinit var turbines: MviTurbines<State, Effect>

    suspend fun initialize() {
        turbines = mviContext.turbinesWithInitialStateCheck(viewModel, initialState)
    }

    fun selectOneTimeContribution() {
        viewModel.event(Event.OnOneTimeContributionSelected)
    }

    suspend fun verifyOneTimeContributionSelected() {
        val oneTimeContributions = initialState.listState.oneTimeContributions

        assertThat(turbines.awaitStateItem()).isEqualTo(

            initialState.copy(
                listState = initialState.listState.copy(
                    isRecurringContributionSelected = false,
                    selectedContribution = oneTimeContributions[oneTimeContributions.size - 2],
                ),
                showContributionList = true,
            ),
        )
    }

    fun selectRecurringContribution() {
        viewModel.event(Event.OnRecurringContributionSelected)
    }

    suspend fun verifyRecurringContributionSelected() {
        val recurringContributions = initialState.listState.recurringContributions

        assertThat(turbines.awaitStateItem()).isEqualTo(
            initialState.copy(
                listState = initialState.listState.copy(
                    isRecurringContributionSelected = true,
                    selectedContribution = recurringContributions[recurringContributions.size - 2],
                ),
                showContributionList = true,
            ),
        )
    }

    fun selectContributionItem(item: Contribution) {
        viewModel.event(Event.OnContributionItemClicked(item))
    }

    suspend fun verifyContributionItemSelected(item: Contribution) {
        assertThat(turbines.awaitStateItem()).isEqualTo(
            initialState.copy(
                listState = initialState.listState.copy(
                    selectedContribution = item,
                ),
            ),
        )
    }

    suspend fun verifyListHidden(purchasedContribution: Contribution) {
        assertThat(turbines.awaitStateItem()).isEqualTo(
            initialState.copy(
                listState = initialState.listState.copy(
                    isLoading = false,
                ),
                purchasedContribution = purchasedContribution,
                showContributionList = false,
            ),
        )
    }
}
