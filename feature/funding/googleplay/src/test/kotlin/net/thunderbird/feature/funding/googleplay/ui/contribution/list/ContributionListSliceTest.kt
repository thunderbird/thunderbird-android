package net.thunderbird.feature.funding.googleplay.ui.contribution.list

import app.k9mail.core.ui.compose.testing.mvi.MviContext
import app.k9mail.core.ui.compose.testing.mvi.MviTurbines
import app.k9mail.core.ui.compose.testing.mvi.runMviTest
import app.k9mail.core.ui.compose.testing.mvi.turbines
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.funding.googleplay.domain.FundingDomainContract.UseCase
import net.thunderbird.feature.funding.googleplay.domain.entity.AvailableContributions
import net.thunderbird.feature.funding.googleplay.domain.entity.ContributionId
import net.thunderbird.feature.funding.googleplay.domain.entity.ContributionPreselection
import net.thunderbird.feature.funding.googleplay.ui.contribution.FakeData
import net.thunderbird.feature.funding.googleplay.ui.contribution.list.ContributionListSliceContract.ContributionType
import net.thunderbird.feature.funding.googleplay.ui.contribution.list.ContributionListSliceContract.Effect
import net.thunderbird.feature.funding.googleplay.ui.contribution.list.ContributionListSliceContract.Event
import net.thunderbird.feature.funding.googleplay.ui.contribution.list.ContributionListSliceContract.State
import org.junit.Test

class ContributionListSliceTest {

    @Test
    fun `should load contributions on init`() = runMviTest {
        // Arrange
        val initialState = State()
        val initialSelectedContributionId = AVAILABLE_CONTRIBUTIONS.recurringContributions[3].id

        contributionListRobot(initialState) {
            // Assert
            verifyLoadedState()
            verifySelectionChangedEffect(initialSelectedContributionId)
        }
    }

    @Test
    fun `should change selection when item clicked`() = runMviTest {
        // Arrange
        val initialState = State()
        val initialSelectedContributionId = AVAILABLE_CONTRIBUTIONS.recurringContributions[3].id
        val contributionId = AVAILABLE_CONTRIBUTIONS.recurringContributions[0].id

        contributionListRobot(initialState) {
            verifyLoadedState()
            verifySelectionChangedEffect(initialSelectedContributionId)

            // Act
            event(Event.ItemClicked(contributionId))

            // Assert
            verifyState {
                assertThat(it.selectedContribution?.id).isEqualTo(contributionId)
            }
            verifySelectionChangedEffect(contributionId)
        }
    }

    @Test
    fun `should change selection and type when type clicked`() = runMviTest {
        // Arrange
        val initialState = State()
        val initialSelectedContributionId = AVAILABLE_CONTRIBUTIONS.recurringContributions[3].id
        val expectedSelectedContribution = AVAILABLE_CONTRIBUTIONS.oneTimeContributions[3]

        contributionListRobot(initialState) {
            verifyLoadedState()
            verifySelectionChangedEffect(initialSelectedContributionId)

            // Act
            event(Event.TypeClicked(ContributionType.OneTime))

            // Assert
            verifyState {
                assertThat(it.selectedType).isEqualTo(ContributionType.OneTime)
                assertThat(it.selectedContribution).isEqualTo(expectedSelectedContribution)
            }
            verifySelectionChangedEffect(expectedSelectedContribution.id)
        }
    }

    @Test
    fun `should reload contributions when retry clicked`() = runMviTest {
        // Arrange
        val emptyContributions = AvailableContributions(
            oneTimeContributions = persistentListOf(),
            recurringContributions = persistentListOf(),
            preselection = ContributionPreselection(
                oneTimeId = null,
                recurringId = null,
            ),
        )
        val initialState = State(
            contributions = emptyContributions,
            isLoading = false,
        )

        val getAvailableContributions = UseCase.GetAvailableContributions {
            flowOf(Outcome.success(AVAILABLE_CONTRIBUTIONS))
        }

        val testSubject = ContributionListSlice(
            getAvailableContributions = getAvailableContributions,
            logger = TestLogger(),
            scope = testScope.backgroundScope,
            initialState = initialState,
        )
        val turbines = turbines(testSubject)
        val robot = ContributionListRobot(testSubject, turbines)

        // Initial State from State()
        robot.verifyState(initialState)

        // State and effect from loadContributions in init
        robot.verifyLoadedState(AVAILABLE_CONTRIBUTIONS)
        robot.verifySelectionChangedEffect(AVAILABLE_CONTRIBUTIONS.recurringContributions[3].id)

        // Act
        robot.event(Event.RetryClicked)

        // State update to isLoading = true
        robot.verifyState {
            assertThat(it.isLoading).isEqualTo(true)
        }

        // Assert
        robot.verifyLoadedState(AVAILABLE_CONTRIBUTIONS)
        robot.verifySelectionChangedEffect(AVAILABLE_CONTRIBUTIONS.recurringContributions[3].id)
    }

    private suspend fun MviContext.contributionListRobot(
        initialState: State,
        contributionsStateFlow: MutableStateFlow<AvailableContributions> = MutableStateFlow(AVAILABLE_CONTRIBUTIONS),
        block: suspend ContributionListRobot.() -> Unit,
    ) {
        val getAvailableContributions = UseCase.GetAvailableContributions {
            contributionsStateFlow.map { Outcome.success(it) }
        }

        val testSubject = ContributionListSlice(
            getAvailableContributions = getAvailableContributions,
            logger = TestLogger(),
            scope = testScope.backgroundScope,
            initialState = initialState,
        )
        val turbines = turbines(testSubject)
        assertThat(turbines.stateTurbine.awaitItem()).isEqualTo(initialState)

        ContributionListRobot(testSubject, turbines).block()
    }

    private class ContributionListRobot(
        private val testSubject: ContributionListSlice,
        private val turbines: MviTurbines<State, Effect>,
    ) {

        fun event(event: Event) {
            testSubject.event(event)
        }

        suspend fun verifyLoadedState(contributions: AvailableContributions = AVAILABLE_CONTRIBUTIONS) {
            turbines.stateTurbine.awaitItem().let { state ->
                assertThat(state.isLoading).isEqualTo(false)
                assertThat(state.contributions).isEqualTo(contributions)
            }
        }

        suspend fun verifyState(expectedState: State) {
            assertThat(turbines.stateTurbine.awaitItem()).isEqualTo(expectedState)
        }

        suspend fun verifyState(block: (State) -> Unit) {
            block(turbines.stateTurbine.awaitItem())
        }

        suspend fun verifySelectionChangedEffect(contributionId: ContributionId?) {
            assertThat(turbines.effectTurbine.awaitItem()).isEqualTo(Effect.SelectionChanged(contributionId))
        }
    }

    private companion object {
        private val AVAILABLE_CONTRIBUTIONS = AvailableContributions(
            oneTimeContributions = FakeData.oneTimeContributions,
            recurringContributions = FakeData.recurringContributions,
            preselection = FakeData.preselection,
        )
    }
}
