package net.thunderbird.feature.funding.googleplay.ui.contribution

import app.k9mail.core.ui.compose.testing.mvi.MviContext
import app.k9mail.core.ui.compose.testing.mvi.MviTurbines
import app.k9mail.core.ui.compose.testing.mvi.runMviTest
import app.k9mail.core.ui.compose.testing.mvi.turbinesWithInitialStateCheck
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.core.testing.coroutines.MainDispatcherHelper
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.Effect
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.Event
import net.thunderbird.feature.funding.googleplay.ui.contribution.ContributionContract.State
import net.thunderbird.feature.funding.googleplay.ui.contribution.list.ContributionListSliceContract
import net.thunderbird.feature.funding.googleplay.ui.contribution.purchase.PurchaseSliceContract

class ContributionViewModelTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val mainDispatcher = MainDispatcherHelper(UnconfinedTestDispatcher())

    @BeforeTest
    fun setUp() {
        mainDispatcher.setUp()
    }

    @AfterTest
    fun tearDown() {
        mainDispatcher.tearDown()
    }

    @Test
    fun `should update state when list selection changed effect received`() = runMviTest {
        // Arrange
        val initialState = State()
        val contributionId = FakeData.oneTimeContribution.id

        contributionRobot(initialState) {
            // Act
            listEffectFlow.emit(ContributionListSliceContract.Effect.SelectionChanged(contributionId))

            // Assert
            verifyState {
                assertThat(it.selectedContributionId).isEqualTo(contributionId)
            }
        }
    }

    @Test
    fun `should hide list when purchased effect received`() = runMviTest {
        // Arrange
        val initialState = State(showContributionList = true)
        val contributionId = FakeData.oneTimeContribution.id

        contributionRobot(initialState) {
            // Act
            purchaseEffectFlow.emit(PurchaseSliceContract.Effect.Purchased(contributionId))

            // Assert
            verifyState {
                assertThat(it.showContributionList).isEqualTo(false)
            }
        }
    }

    @Test
    fun `should show list when purchased effect received with null id`() = runMviTest {
        // Arrange
        val initialState = State(showContributionList = false)

        contributionRobot(initialState) {
            // Act
            purchaseEffectFlow.emit(PurchaseSliceContract.Effect.Purchased(null))

            // Assert
            verifyState {
                assertThat(it.showContributionList).isEqualTo(true)
            }
        }
    }

    @Test
    fun `should emit ManageSubscription effect when purchase slice emits it`() = runMviTest {
        // Arrange
        val initialState = State()
        val contributionId = FakeData.oneTimeContribution.id

        contributionRobot(initialState) {
            // Act
            purchaseEffectFlow.emit(PurchaseSliceContract.Effect.ManageSubscription(contributionId))

            // Assert
            verifyEffect(Effect.ManageSubscription(contributionId))
        }
    }

    @Test
    fun `should show list when ShowContributionListClicked event received`() = runMviTest {
        // Arrange
        val initialState = State(showContributionList = false)

        contributionRobot(initialState) {
            // Act
            event(Event.ShowContributionListClicked)

            // Assert
            verifyState {
                assertThat(it.showContributionList).isEqualTo(true)
            }
        }
    }

    private suspend fun MviContext.contributionRobot(
        initialState: State,
        block: suspend ContributionRobot.() -> Unit,
    ) {
        val listStateFlow = MutableStateFlow(ContributionListSliceContract.State())
        val listEffectFlow = MutableSharedFlow<ContributionListSliceContract.Effect>()
        val purchaseStateFlow = MutableStateFlow(PurchaseSliceContract.State())
        val purchaseEffectFlow = MutableSharedFlow<PurchaseSliceContract.Effect>()

        val listSlice = object : ContributionListSliceContract.Slice {
            override val state: StateFlow<ContributionListSliceContract.State> = listStateFlow
            override val effect: SharedFlow<ContributionListSliceContract.Effect> = listEffectFlow
            override fun event(event: ContributionListSliceContract.Event) = Unit
        }
        val purchaseSlice = object : PurchaseSliceContract.Slice {
            override val state: StateFlow<PurchaseSliceContract.State> = purchaseStateFlow
            override val effect: SharedFlow<PurchaseSliceContract.Effect> = purchaseEffectFlow
            override fun event(event: PurchaseSliceContract.Event) = Unit
        }

        val listSliceFactory = object : ContributionListSliceContract.Slice.Factory {
            override fun create(scope: kotlinx.coroutines.CoroutineScope) = listSlice
        }
        val purchaseSliceFactory = object : PurchaseSliceContract.Slice.Factory {
            override fun create(scope: kotlinx.coroutines.CoroutineScope) = purchaseSlice
        }

        val testSubject = ContributionViewModel(
            listSliceFactory = listSliceFactory,
            purchaseSliceFactory = purchaseSliceFactory,
            repository = FakeContributionRepository(),
            logger = TestLogger(),
            initialState = initialState,
        )

        val turbines = turbinesWithInitialStateCheck(testSubject, initialState)

        ContributionRobot(testSubject, turbines, listEffectFlow, purchaseEffectFlow).block()
    }

    private class ContributionRobot(
        private val viewModel: ContributionContract.ViewModel,
        private val turbines: MviTurbines<State, Effect>,
        val listEffectFlow: MutableSharedFlow<ContributionListSliceContract.Effect>,
        val purchaseEffectFlow: MutableSharedFlow<PurchaseSliceContract.Effect>,
    ) {

        fun event(event: Event) {
            viewModel.event(event)
        }

        suspend fun verifyState(block: (State) -> Unit) {
            block(turbines.stateTurbine.awaitItem())
        }

        suspend fun verifyEffect(effect: Effect) {
            assertThat(turbines.effectTurbine.awaitItem()).isEqualTo(effect)
        }
    }
}
