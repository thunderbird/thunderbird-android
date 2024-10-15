package app.k9mail.feature.funding.googleplay.ui.contribution

import app.k9mail.core.ui.compose.testing.MainDispatcherRule
import app.k9mail.core.ui.compose.testing.mvi.MviContext
import app.k9mail.core.ui.compose.testing.mvi.MviTurbines
import app.k9mail.core.ui.compose.testing.mvi.runMviTest
import app.k9mail.core.ui.compose.testing.mvi.turbinesWithInitialStateCheck
import app.k9mail.feature.funding.googleplay.domain.entity.Contribution
import app.k9mail.feature.funding.googleplay.ui.contribution.ContributionContract.Event
import app.k9mail.feature.funding.googleplay.ui.contribution.ContributionContract.State
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import org.junit.Rule

class ContributionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `should change selected contribution and selected type when on time contribution selected`() = runMviTest {
        val initialState = State(
            isRecurringContributionSelected = true,
            oneTimeContributions = FakeData.oneTimeContributions,
            recurringContributions = FakeData.recurringContributions,
            purchasedContribution = FakeData.oneTimeContributions.first(),
            selectedContribution = FakeData.recurringContributions.first(),
        )

        contributionRobot(initialState) {
            selectOneTimeContribution()
            verifyOneTimeContributionSelected()
        }
    }

    @Test
    fun `should change selected contribution and selected type when recurring contribution selected`() = runMviTest {
        val initialState = State(
            isRecurringContributionSelected = false,
            oneTimeContributions = FakeData.oneTimeContributions,
            recurringContributions = FakeData.recurringContributions,
            purchasedContribution = FakeData.oneTimeContributions.first(),
            selectedContribution = FakeData.oneTimeContributions.first(),
        )

        contributionRobot(initialState) {
            selectRecurringContribution()
            verifyRecurringContributionSelected()
        }
    }

    @Test
    fun `should change selected contribution when contribution item clicked`() = runMviTest {
        val initialState = State(
            isRecurringContributionSelected = true,
            oneTimeContributions = FakeData.oneTimeContributions,
            recurringContributions = FakeData.recurringContributions,
            purchasedContribution = FakeData.oneTimeContributions.first(),
            selectedContribution = FakeData.recurringContributions.first(),
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
    private val viewModel: ContributionContract.ViewModel = ContributionViewModel(
        initialState = initialState,
        billingManager = FakeBillingManager(),
    )
    private lateinit var turbines: MviTurbines<State, Nothing>

    suspend fun initialize() {
        turbines = mviContext.turbinesWithInitialStateCheck(viewModel, initialState)
    }

    fun selectOneTimeContribution() {
        viewModel.event(Event.OnOneTimeContributionSelected)
    }

    suspend fun verifyOneTimeContributionSelected() {
        assertThat(turbines.awaitStateItem()).isEqualTo(
            initialState.copy(
                isRecurringContributionSelected = false,
                selectedContribution = initialState.oneTimeContributions.first(),
            ),
        )
    }

    fun selectRecurringContribution() {
        viewModel.event(Event.OnRecurringContributionSelected)
    }

    suspend fun verifyRecurringContributionSelected() {
        assertThat(turbines.awaitStateItem()).isEqualTo(
            initialState.copy(
                isRecurringContributionSelected = true,
                selectedContribution = initialState.recurringContributions.first(),
            ),
        )
    }

    fun selectContributionItem(item: Contribution) {
        viewModel.event(Event.OnContributionItemClicked(item))
    }

    suspend fun verifyContributionItemSelected(item: Contribution) {
        assertThat(turbines.awaitStateItem()).isEqualTo(
            initialState.copy(
                selectedContribution = item,
            ),
        )
    }
}
