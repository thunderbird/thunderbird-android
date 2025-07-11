package app.k9mail.feature.funding.googleplay.ui.contribution

import app.k9mail.core.ui.compose.testing.mvi.MviContext
import app.k9mail.core.ui.compose.testing.mvi.MviTurbines
import app.k9mail.core.ui.compose.testing.mvi.runMviTest
import app.k9mail.core.ui.compose.testing.mvi.turbinesWithInitialStateCheck
import app.k9mail.feature.funding.googleplay.domain.entity.AvailableContributions
import app.k9mail.feature.funding.googleplay.domain.entity.Contribution
import app.k9mail.feature.funding.googleplay.ui.contribution.ContributionContract.ContributionListState
import app.k9mail.feature.funding.googleplay.ui.contribution.ContributionContract.Effect
import app.k9mail.feature.funding.googleplay.ui.contribution.ContributionContract.Event
import app.k9mail.feature.funding.googleplay.ui.contribution.ContributionContract.State
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.testing.coroutines.MainDispatcherRule
import org.junit.Rule

class ContributionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `should change selected contribution and selected type when one time contribution selected`() = runMviTest {
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
            selectOneTimeContribution()
            verifyOneTimeContributionSelected()
        }
    }

    @Test
    fun `should change selected contribution and selected type when recurring contribution selected`() = runMviTest {
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
            selectRecurringContribution()
            verifyRecurringContributionSelected()
        }
    }

    @Test
    fun `should change selected contribution when contribution item clicked`() = runMviTest {
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
            selectContributionItem(selectedContribution)
            verifyContributionItemSelected(selectedContribution)
        }
    }
}

private suspend fun MviContext.contributionRobot(
    initialState: State = State(),
    interaction: suspend ContributionRobot.() -> Unit,
) = ContributionRobot(this, initialState).apply {
    initialize()
    interaction()
}

private class ContributionRobot(
    private val mviContext: MviContext,
    private val initialState: State = State(),
) {
    // FIX use case
    private val viewModel: ContributionContract.ViewModel = ContributionViewModel(
        getAvailableContributions = {
            Outcome.success(
                AvailableContributions(
                    oneTimeContributions = FakeData.oneTimeContributions,
                    recurringContributions = FakeData.recurringContributions,
                    purchasedContribution = FakeData.oneTimeContributions.first(),
                ),
            )
        },
        billingManager = FakeBillingManager(),
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
}
